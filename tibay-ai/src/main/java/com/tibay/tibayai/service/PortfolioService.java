package com.tibay.tibayai.service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tibay.tibayai.entity.AiAssessment;
import com.tibay.tibayai.entity.WorkerProfile;
import com.tibay.tibayai.repo.WorkerProfileRepository;
import com.tibay.tibayai.service.ai.OpenRouterClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortfolioService {
	private final OpenRouterClient openRouterClient;
	private final WorkerProfileRepository workerProfileRepository;

	@Transactional
	public String generateAndSave(WorkerProfile workerProfile, String rawTaglishText, AiAssessment assessment, String cvResults) {
		String portfolio = generate(rawTaglishText, workerProfile, assessment, cvResults);
		workerProfile.setAtsPortfolio(portfolio);
		workerProfileRepository.save(workerProfile);
		return portfolio;
	}

	public String generate(String rawTaglishText, WorkerProfile workerProfile, AiAssessment assessment, String cvResults) {
		String raw = StringUtils.hasText(rawTaglishText) ? rawTaglishText.trim() : "";
		String cv = StringUtils.hasText(cvResults) ? cvResults.trim() : "";
		String assessmentSummary = assessment != null ? safe(assessment.getAssessmentSummary()) : "";
		Integer score = assessment != null ? assessment.getSkillScore() : workerProfile.getLatestSkillScore();
		String ppe = assessment != null ? safe(assessment.getPpeCompliance()) : "";
		String stability = assessment != null ? safe(assessment.getWeldingStability()) : "";
		String straightness = assessment != null ? safe(assessment.getWeldLineStraightness()) : "";

		if (openRouterClient.enabled()) {
			String systemPrompt = """
					You evaluate informal worker submissions and generate professional, structured outputs for hiring platforms.

					Write in clear, professional English. Be realistic and evidence-based. Do not exaggerate.
					Never claim certifications you don't know. Never mention "AI" in the output.
					If data is incomplete, make reasonable assumptions and state them briefly.

					Output format must be exactly:

					[WORKER PORTFOLIO]

					Professional Summary:
					- <2–3 concise sentences>

					Key Skills:
					- <4–6 skills>

					Work Evidence Insights:
					- PPE Compliance: <High/Medium/Low + short explanation>
					- Stability: <evaluation + short explanation>
					- Precision: <evaluation + short explanation>
					- Consistency: <evaluation + short explanation>

					Skill Indicators (Pre-Score):
					- Safety Awareness: <0–100>
					- Technical Skill: <0–100>
					- Work Consistency: <0–100>

					Overall Hireability:
					- <1–2 sentences>
					""";
			String userPrompt = """
					Worker location: %s, %s
					Worker description (informal): %s

					Computer vision / work evidence results (raw): %s

					Stored visual signals (if available):
					- PPE: %s
					- Stability: %s
					- Weld line straightness: %s
					- Summary: %s
					- Skill score (0–100): %s

					Interpret work evidence to assess PPE compliance, stability, precision, and consistency.
					Combine both text and visual insights to infer welding skills. Generate the portfolio now.
					"""
					.formatted(
							safe(workerProfile.getBarangay()),
							safe(workerProfile.getCity()),
							raw,
							StringUtils.hasText(cv) ? cv : "(not provided)",
							StringUtils.hasText(ppe) ? ppe : "(not available)",
							StringUtils.hasText(stability) ? stability : "(not available)",
							StringUtils.hasText(straightness) ? straightness : "(not available)",
							StringUtils.hasText(assessmentSummary) ? assessmentSummary : "(not available)",
							score == null ? "(not available)" : String.valueOf(score));

			return openRouterClient.chat(systemPrompt, userPrompt)
					.map(s -> truncate(s, 6000))
					.orElseGet(() -> fallback(raw, score, ppe, stability, straightness, assessmentSummary, cv));
		}

		return fallback(raw, score, ppe, stability, straightness, assessmentSummary, cv);
	}

	private String fallback(String raw, Integer score, String ppe, String stability, String straightness, String assessmentSummary, String cv) {
		String normalized = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
		for (Map.Entry<String, String> e : slangMap().entrySet()) {
			normalized = normalized.replace(e.getKey(), e.getValue());
		}

		String ppeLevel = mapLevel(ppe, "PASS", "FAIL", "PENDING");
		String stabilityLevel = mapLevel(stability, "GOOD", "WEAK", "PENDING");
		String precisionLevel = mapLevel(straightness, "GOOD", "WEAK", "PENDING");
		String consistencyLevel = stabilityLevel;

		int safety = mapScore(ppe, 80, 40, 50);
		int technical = score == null ? 60 : Math.max(0, Math.min(100, score));
		int workConsistency = mapScore(stability, 75, 45, 50);

		String evidenceNote = StringUtils.hasText(assessmentSummary) ? assessmentSummary : (StringUtils.hasText(cv) ? cv : "Limited work evidence signals were provided.");

		return """
				[WORKER PORTFOLIO]

				Professional Summary:
				- Welding worker with practical exposure to common site tasks such as basic repair work, joint preparation, and maintaining steady tool handling. Communicates a hands-on, task-focused work profile and is best suited for entry to intermediate welding support roles. Some details are inferred due to limited written or visual evidence.

				Key Skills:
				- Basic welding operation and safe tool handling
				- Joint preparation support (cleaning, fit-up assistance, grinding)
				- Worksite safety habits and PPE awareness
				- Consistent hand control during short welding tasks
				- Following instructions and quality checks (basic)

				Work Evidence Insights:
				- PPE Compliance: %s (based on available signals: %s)
				- Stability: %s (based on available signals: %s)
				- Precision: %s (based on available signals: %s)
				- Consistency: %s (limited evidence; inferred mainly from stability signals)

				Skill Indicators (Pre-Score):
				- Safety Awareness: %d
				- Technical Skill: %d
				- Work Consistency: %d

				Overall Hireability:
				- Appears employable for general welding support work with supervision, especially if paired with clear safety expectations. Stronger hireability would require clearer work history details and more complete work evidence signals.
				""".formatted(
						ppeLevel, summarizeSignal(ppe, evidenceNote),
						stabilityLevel, summarizeSignal(stability, evidenceNote),
						precisionLevel, summarizeSignal(straightness, evidenceNote),
						consistencyLevel,
						safety, technical, workConsistency);
	}

	private static Map<String, String> slangMap() {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("taga-ayos", "specialized in repair");
		m.put("lamat", "structural crack");
		m.put("barko", "ship");
		m.put("pier", "dockside area");
		m.put("nagwewelding", "performing welding work");
		m.put("nasa pier", "assigned to dockside work");
		return m;
	}

	private static String safe(String s) {
		return s == null ? "" : s;
	}

	private static String truncate(String s, int max) {
		if (s == null) {
			return null;
		}
		if (s.length() <= max) {
			return s;
		}
		return s.substring(0, max);
	}

	private static String mapLevel(String signal, String goodToken, String badToken, String pendingToken) {
		String s = safe(signal).trim().toUpperCase(Locale.ROOT);
		if (!StringUtils.hasText(s) || pendingToken.equals(s)) {
			return "Medium";
		}
		if (goodToken.equals(s) || "PASS".equals(s)) {
			return "High";
		}
		if (badToken.equals(s) || "FAIL".equals(s)) {
			return "Low";
		}
		if ("MODERATE".equals(s)) {
			return "Medium";
		}
		return "Medium";
	}

	private static int mapScore(String signal, int goodScore, int badScore, int pendingScore) {
		String s = safe(signal).trim().toUpperCase(Locale.ROOT);
		if (!StringUtils.hasText(s) || "PENDING".equals(s)) {
			return pendingScore;
		}
		if ("PASS".equals(s) || "GOOD".equals(s)) {
			return goodScore;
		}
		if ("FAIL".equals(s) || "WEAK".equals(s)) {
			return badScore;
		}
		if ("MODERATE".equals(s)) {
			return (goodScore + badScore) / 2;
		}
		return pendingScore;
	}

	private static String summarizeSignal(String primarySignal, String evidenceNote) {
		String s = safe(primarySignal).trim();
		if (StringUtils.hasText(s) && !"PENDING".equalsIgnoreCase(s)) {
			return s;
		}
		String e = safe(evidenceNote).trim();
		if (e.length() > 160) {
			return e.substring(0, 160);
		}
		return StringUtils.hasText(e) ? e : "not provided";
	}
}
