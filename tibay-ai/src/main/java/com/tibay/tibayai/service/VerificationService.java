package com.tibay.tibayai.service;

import java.io.File;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tibay.tibayai.entity.GovernmentIdVerification;
import com.tibay.tibayai.entity.VerificationStatus;
import com.tibay.tibayai.entity.WorkerProfile;
import com.tibay.tibayai.repo.GovernmentIdVerificationRepository;
import com.tibay.tibayai.repo.WorkerProfileRepository;
import com.tibay.tibayai.service.ai.InsightFaceClient;
import com.tibay.tibayai.service.ai.OcrSpaceClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VerificationService {
	private final GovernmentIdVerificationRepository verificationRepository;
	private final WorkerProfileRepository workerProfileRepository;
	private final StorageService storageService;
	private final OcrSpaceClient ocrSpaceClient;
	private final InsightFaceClient insightFaceClient;

	@Transactional
	public GovernmentIdVerification verify(WorkerProfile workerProfile, String idImagePath, String selfiePath) {
		File idFile = storageService.resolveAbsolute(idImagePath).toFile();
		File selfieFile = storageService.resolveAbsolute(selfiePath).toFile();

		String ocrText = ocrSpaceClient.extractText(idFile).orElse("");
		boolean ocrConfigured = ocrSpaceClient.enabled();
		boolean idLooksValid = !ocrConfigured || looksLikePhilippineId(ocrText);

		Double faceMatchScore = null;
		boolean faceUnavailable = true;
		boolean faceMismatch = false;
		try {
			var match = insightFaceClient.match(idFile, selfieFile).orElse(null);
			if (match != null) {
				faceUnavailable = false;
				faceMatchScore = Math.max(0.0, Math.min(1.0, match.score()));
				faceMismatch = !match.samePerson();
			}
		} catch (Exception e) {
			faceUnavailable = true;
		}

		VerificationStatus status;
		String summary;
		if (!idLooksValid || faceUnavailable || faceMismatch) {
			status = VerificationStatus.FAILED;
			StringBuilder sb = new StringBuilder("AI-assisted identity verification: FAILED. Reasons: ");
			if (!idLooksValid) {
				sb.append("Invalid ID format detected. ");
			}
			if (faceUnavailable) {
				sb.append("Face check unavailable. ");
			} else if (faceMismatch) {
				sb.append("Face mismatch detected. ");
			}
			summary = sb.toString().trim();
		} else {
			status = VerificationStatus.VERIFIED;
			StringBuilder sb = new StringBuilder("AI-assisted identity verification: VERIFIED. ");
			if (ocrConfigured) {
				sb.append("ID format appears valid. ");
			} else {
				sb.append("ID text check unavailable (OCR not configured or no text extracted). ");
			}
			sb.append("Face match score: ").append(String.format(Locale.ROOT, "%.2f", faceMatchScore)).append(". ");
			sb.append("This is not a legal verification.");
			summary = sb.toString().trim();
		}

		GovernmentIdVerification v = new GovernmentIdVerification();
		v.setWorkerProfile(workerProfile);
		v.setIdImagePath(idImagePath);
		v.setOcrText(truncate(ocrText, 4000));
		v.setFaceMatchScore(faceMatchScore == null ? null : (int) Math.round(faceMatchScore * 100.0));
		v.setStatus(status);
		v.setSummary(truncate(summary, 1200));
		verificationRepository.save(v);

		workerProfile.setIdVerificationStatus(status);
		workerProfile.setIdVerificationSummary(truncate(summary, 800));
		workerProfileRepository.save(workerProfile);

		return v;
	}

	private static boolean looksLikePhilippineId(String text) {
		if (!StringUtils.hasText(text)) {
			return false;
		}
		String t = text.toLowerCase(Locale.ROOT);
		return t.contains("republic of the philippines")
				|| t.contains("philippine identification")
				|| t.contains("philippines")
				|| t.contains("driver")
				|| t.contains("passport")
				|| t.contains("sss")
				|| t.contains("umid")
				|| t.contains("philhealth")
				|| t.contains("tin");
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
