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
	public String generateAndSave(WorkerProfile workerProfile, String rawTaglishText, AiAssessment assessment) {
		String portfolio = generate(rawTaglishText, workerProfile, assessment);
		workerProfile.setAtsPortfolio(portfolio);
		workerProfileRepository.save(workerProfile);
		return portfolio;
	}

	public String generate(String rawTaglishText, WorkerProfile workerProfile, AiAssessment assessment) {
		String raw = StringUtils.hasText(rawTaglishText) ? rawTaglishText.trim() : "";
		String assessmentSummary = assessment != null ? safe(assessment.getAssessmentSummary()) : "";
		Integer score = assessment != null ? assessment.getSkillScore() : workerProfile.getLatestSkillScore();

		if (openRouterClient.enabled()) {
			String systemPrompt = """
					You are TIBAY AI. Convert informal Filipino/Tagalog/Taglish worker statements into recruiter-ready English.
					Output must be ATS-optimized for industrial welding roles (maritime, ship repair, offshore energy, structural welding).
					Never claim certifications you don't know. Never mention "AI" inside the portfolio.
					Format exactly with headings:
					PROFESSIONAL SUMMARY
					CORE SKILLS
					WORK EXPERIENCE (BULLETS)
					INDUSTRY KEYWORDS
					""";
			String userPrompt = """
					Worker location: %s, %s
					Worker notes (Tagalog/Taglish/slang): %s
					Latest AI-assisted assessment signals (for context only): %s
					Latest skill score (context): %s
					Generate the portfolio now.
					"""
					.formatted(safe(workerProfile.getBarangay()), safe(workerProfile.getCity()), raw, assessmentSummary,
							score == null ? "" : (score + "/100"));
			return openRouterClient.chat(systemPrompt, userPrompt).map(s -> truncate(s, 4000)).orElseGet(() -> fallback(raw, score));
		}

		return fallback(raw, score);
	}

	private String fallback(String raw, Integer score) {
		String normalized = raw.toLowerCase(Locale.ROOT);
		for (Map.Entry<String, String> e : slangMap().entrySet()) {
			normalized = normalized.replace(e.getKey(), e.getValue());
		}
		String scoreLine = score == null ? "" : ("Skill Score (AI-assisted): " + score + "/100\n");
		return """
				PROFESSIONAL SUMMARY
				Industrial welding worker with hands-on exposure to ship repair, structural welding, and heavy maintenance tasks. %sFocused on safe work practices, jobsite discipline, and consistent weld quality.

				CORE SKILLS
				- SMAW / stick welding (basic to intermediate)
				- Structural repair and reinforcement
				- Dockside / shipyard support welding
				- PPE compliance and basic safety checks

				WORK EXPERIENCE (BULLETS)
				- Supported maintenance and repair tasks in industrial and maritime-adjacent environments.
				- Assisted in hull/structure patching and weld joint preparation (cleaning, fit-up, basic grinding).
				- Followed basic QC checks: bead consistency, porosity spotting, alignment checks.

				INDUSTRY KEYWORDS
				maritime welding, ship repair, hull maintenance, structural welding, heavy industrial maintenance, dockside operations, SMAW, safety compliance
				""".formatted(scoreLine);
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
}

