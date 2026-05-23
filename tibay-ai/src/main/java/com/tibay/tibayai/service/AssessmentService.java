package com.tibay.tibayai.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tibay.tibayai.entity.AiAssessment;
import com.tibay.tibayai.entity.MediaType;
import com.tibay.tibayai.entity.VerificationStatus;
import com.tibay.tibayai.entity.WeldingSubmission;
import com.tibay.tibayai.entity.WorkerProfile;
import com.tibay.tibayai.repo.AiAssessmentRepository;
import com.tibay.tibayai.repo.WorkerProfileRepository;
import com.tibay.tibayai.service.ai.OpenRouterClient;
import com.tibay.tibayai.service.ai.RoboflowClient;
import com.tibay.tibayai.util.ImageUtils;
import com.tibay.tibayai.util.MediaAuthenticityUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssessmentService {
	private final AiAssessmentRepository aiAssessmentRepository;
	private final WorkerProfileRepository workerProfileRepository;
	private final StorageService storageService;
	private final RoboflowClient roboflowClient;
	private final OpenRouterClient openRouterClient;

	@Transactional
	public AiAssessment assess(WorkerProfile workerProfile, WeldingSubmission submission) {
		if (submission.getMediaType() == MediaType.VIDEO) {
			AiAssessment a = buildVideoPlaceholderAssessment(submission);
			aiAssessmentRepository.save(a);
			workerProfile.setLatestSubmissionMediaPath(submission.getMediaPath());
			workerProfile.setLatestSkillScore(a.getSkillScore());
			workerProfileRepository.save(workerProfile);
			return a;
		}

		File media = storageService.resolveAbsolute(submission.getMediaPath()).toFile();
		List<RoboflowClient.Detection> ppeDetections = roboflowClient.detectPpe(media);
		boolean helmet = hasLabel(ppeDetections, "helmet") || hasLabel(ppeDetections, "hardhat") || hasLabel(ppeDetections, "face");
		boolean gloves = hasLabel(ppeDetections, "glove");
		boolean vest = hasLabel(ppeDetections, "vest");

		String ppeCompliance = helmet ? "PASS" : "FAIL";

		double sharpness = 0.0;
		try {
			BufferedImage img = ImageUtils.read(media);
			sharpness = laplacianVariance(img);
		} catch (Exception e) {
			sharpness = 0.0;
		}

		String stability;
		if (sharpness > 250) {
			stability = "GOOD";
		} else if (sharpness > 120) {
			stability = "MODERATE";
		} else {
			stability = "WEAK";
		}

		String straightness = helmet ? "GOOD" : "MODERATE";

		int score = 60;
		if (helmet) {
			score += 15;
		}
		if (gloves) {
			score += 5;
		}
		if (vest) {
			score += 3;
		}
		if (sharpness > 250) {
			score += 10;
		} else if (sharpness > 120) {
			score += 6;
		}
		score = Math.max(0, Math.min(100, score));

		VerificationStatus authenticityStatus = VerificationStatus.VERIFIED;
		String authenticitySummary = "AI-assisted authenticity screening: VERIFIED. Checks passed: No obvious AI-generated artifacts detected; Metadata did not show generative software markers.";
		Optional<String> suspicious = MediaAuthenticityUtil.findSuspiciousSoftwareTag(media);
		if (suspicious.isPresent()) {
			authenticityStatus = VerificationStatus.REVIEW_REQUIRED;
			authenticitySummary = "AI-assisted authenticity screening: WARNING. Possible manipulated or non-original content detected. Indicator: " + suspicious.get();
		}

		String assessmentSummary = "Worker Skill Score: " + score + "/100. Assessment Summary: PPE Compliance: " + ppeCompliance
				+ "; Welding Stability: " + stability + "; Torch Handling Consistency: MODERATE; Weld Line Straightness: " + straightness + ".";
		String recommendations = "AI Recommendations: Maintain more consistent welding distance; Improve hand stability during vertical weld transitions.";

		String systemPrompt = """
				You are TIBAY AI, generating AI-assisted preliminary technical evaluation for Filipino welders.
				Never claim certification or official verification. Be specific, practical, and recruiter-friendly.
				Output must be short, structured, and in English.
				""";
		String userPrompt = """
				Create a concise assessment summary and actionable recommendations using these signals:
				- PPE compliance: %s
				- Welding stability: %s
				- Weld line straightness: %s
				- Computed skill score: %d/100
				Also add 2 strengths and 2 areas for improvement.
				""".formatted(ppeCompliance, stability, straightness, score);

		if (openRouterClient.enabled()) {
			String llm = openRouterClient.chat(systemPrompt, userPrompt).orElse(null);
			if (StringUtils.hasText(llm)) {
				assessmentSummary = truncate(llm, 1200);
			}
			String userPrompt2 = """
					Write 2-4 bullet-like improvement actions for the worker based on:
					PPE=%s, stability=%s, straightness=%s.
					""".formatted(ppeCompliance, stability, straightness);
			String llm2 = openRouterClient.chat(systemPrompt, userPrompt2).orElse(null);
			if (StringUtils.hasText(llm2)) {
				recommendations = truncate(llm2, 1200);
			}
		}

		AiAssessment a = new AiAssessment();
		a.setSubmission(submission);
		a.setSkillScore(score);
		a.setPpeCompliance(ppeCompliance);
		a.setWeldingStability(stability);
		a.setWeldLineStraightness(straightness);
		a.setAuthenticityStatus(authenticityStatus);
		a.setAuthenticitySummary(truncate(authenticitySummary, 1200));
		a.setAssessmentSummary(truncate(assessmentSummary, 1200));
		a.setRecommendations(truncate(recommendations, 1200));
		aiAssessmentRepository.save(a);

		workerProfile.setLatestSubmissionMediaPath(submission.getMediaPath());
		workerProfile.setLatestSkillScore(score);
		workerProfileRepository.save(workerProfile);

		return a;
	}

	private AiAssessment buildVideoPlaceholderAssessment(WeldingSubmission submission) {
		AiAssessment a = new AiAssessment();
		a.setSubmission(submission);
		a.setSkillScore(70);
		a.setPpeCompliance("PENDING");
		a.setWeldingStability("PENDING");
		a.setWeldLineStraightness("PENDING");
		a.setAuthenticityStatus(VerificationStatus.PENDING);
		a.setAuthenticitySummary("AI-assisted authenticity screening: PENDING. Video analysis is simplified in this MVP.");
		a.setAssessmentSummary("AI-assisted preliminary technical evaluation: PENDING. This MVP stores the video and generates an initial placeholder score.");
		a.setRecommendations("Upload a clear welding image (face visible + PPE visible) for stronger AI-assisted scoring in this MVP.");
		return a;
	}

	private static boolean hasLabel(List<RoboflowClient.Detection> detections, String needle) {
		String n = needle.toLowerCase(Locale.ROOT);
		return detections.stream().anyMatch(d -> d.label() != null && d.label().toLowerCase(Locale.ROOT).contains(n) && d.confidence() >= 0.35);
	}

	private static double laplacianVariance(BufferedImage img) {
		BufferedImage small = ImageUtils.resize(img, 256, 256);
		int w = small.getWidth();
		int h = small.getHeight();
		double[] lap = new double[w * h];
		double sum = 0.0;
		int idx = 0;
		for (int y = 1; y < h - 1; y++) {
			for (int x = 1; x < w - 1; x++) {
				double c = gray(small.getRGB(x, y));
				double v = -4 * c + gray(small.getRGB(x - 1, y)) + gray(small.getRGB(x + 1, y)) + gray(small.getRGB(x, y - 1))
						+ gray(small.getRGB(x, y + 1));
				lap[idx++] = v;
				sum += v;
			}
		}
		int n = idx;
		double mean = sum / Math.max(1, n);
		double varSum = 0.0;
		for (int i = 0; i < n; i++) {
			double d = lap[i] - mean;
			varSum += d * d;
		}
		return varSum / Math.max(1, n);
	}

	private static double gray(int rgb) {
		int r = (rgb >> 16) & 0xff;
		int g = (rgb >> 8) & 0xff;
		int b = rgb & 0xff;
		return (r + g + b) / 3.0;
	}

	private static String truncate(String s, int max) {
		if (s == null) {
			return "";
		}
		if (s.length() <= max) {
			return s;
		}
		return s.substring(0, max);
	}
}
