package com.tibay.tibayai.service;

import java.io.File;
import java.util.Comparator;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tibay.tibayai.entity.GovernmentIdVerification;
import com.tibay.tibayai.entity.VerificationStatus;
import com.tibay.tibayai.entity.WorkerProfile;
import com.tibay.tibayai.repo.GovernmentIdVerificationRepository;
import com.tibay.tibayai.repo.WorkerProfileRepository;
import com.tibay.tibayai.service.ai.OcrSpaceClient;
import com.tibay.tibayai.service.ai.RoboflowClient;
import com.tibay.tibayai.util.ImageUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VerificationService {
	private final GovernmentIdVerificationRepository verificationRepository;
	private final WorkerProfileRepository workerProfileRepository;
	private final StorageService storageService;
	private final OcrSpaceClient ocrSpaceClient;
	private final RoboflowClient roboflowClient;

	private enum CheckResult {
		PASS,
		FAIL,
		INCONCLUSIVE,
		UNAVAILABLE
	}

	@Transactional
	public GovernmentIdVerification verify(WorkerProfile workerProfile, String idImagePath, String selfiePath) {
		File idFile = storageService.resolveAbsolute(idImagePath).toFile();
		File selfieFile = storageService.resolveAbsolute(selfiePath).toFile();

		boolean ocrEnabled = ocrSpaceClient.enabled();
		boolean faceEnabled = roboflowClient.facesEnabled();

		CheckResult ocrResult = CheckResult.UNAVAILABLE;
		String ocrText = "";
		if (ocrEnabled) {
			ocrText = ocrSpaceClient.extractText(idFile).orElse("");
			if (!StringUtils.hasText(ocrText)) {
				ocrResult = CheckResult.INCONCLUSIVE;
			} else if (looksLikePhilippineId(ocrText)) {
				ocrResult = CheckResult.PASS;
			} else {
				ocrResult = CheckResult.FAIL;
			}
		}

		Integer faceMatchScore = null;
		CheckResult faceResult = faceEnabled ? CheckResult.INCONCLUSIVE : CheckResult.UNAVAILABLE;
		if (faceEnabled) {
			try {
				var idFace = roboflowClient.detectFaces(idFile).stream()
						.max(Comparator.comparingDouble(RoboflowClient.Detection::confidence))
						.orElse(null);
				var selfieFace = roboflowClient.detectFaces(selfieFile).stream()
						.max(Comparator.comparingDouble(RoboflowClient.Detection::confidence))
						.orElse(null);

				if (idFace != null && selfieFace != null) {
					var idImg = ImageUtils.read(idFile);
					var selfieImg = ImageUtils.read(selfieFile);

					var idCrop = cropFromCenterBox(idImg, idFace);
					var selfieCrop = cropFromCenterBox(selfieImg, selfieFace);

					long h1 = ImageUtils.averageHash(idCrop);
					long h2 = ImageUtils.averageHash(selfieCrop);
					faceMatchScore = ImageUtils.similarityScore(h1, h2);
					faceResult = faceMatchScore >= 80 ? CheckResult.PASS : CheckResult.FAIL;
				} else {
					faceResult = CheckResult.FAIL;
				}
			} catch (Exception e) {
				faceResult = CheckResult.INCONCLUSIVE;
			}
		}

		VerificationStatus status;
		String summary;
		if (ocrResult == CheckResult.PASS && faceResult == CheckResult.PASS) {
			status = VerificationStatus.VERIFIED;
			StringBuilder sb = new StringBuilder("AI-assisted identity verification: VERIFIED. ");
			sb.append("Checks passed: OCR=PASS; FACE=PASS; ");
			if (faceMatchScore != null) {
				sb.append("Face match score: ").append(faceMatchScore).append("/100; ");
			}
			sb.append("Identity consistency detected.");
			summary = sb.toString().trim();
		} else if (ocrResult == CheckResult.FAIL || faceResult == CheckResult.FAIL) {
			status = VerificationStatus.FAILED;
			StringBuilder sb = new StringBuilder("AI-assisted identity verification: FAILED. Reasons: ");
			if (ocrResult == CheckResult.FAIL) {
				sb.append("OCR check failed (ID text did not resemble a Philippine government ID). ");
			}
			if (faceResult == CheckResult.FAIL) {
				if (faceMatchScore == null) {
					sb.append("Face check failed (face not detected or mismatch). ");
				} else {
					sb.append("Face check failed (low similarity score: ").append(faceMatchScore).append("/100). ");
				}
			}
			sb.append("This is not a legal verification.");
			summary = sb.toString().trim();
		} else {
			status = VerificationStatus.REVIEW_REQUIRED;
			StringBuilder sb = new StringBuilder("AI-assisted identity verification: REVIEW_REQUIRED. This is not a legal verification. ");
			sb.append("Checks: ");
			sb.append("OCR=").append(ocrResult).append("; ");
			sb.append("FACE=").append(faceResult).append("; ");
			if (faceMatchScore != null) {
				sb.append("Face match score=").append(faceMatchScore).append("/100; ");
			}
			summary = sb.toString().trim();
		}

		GovernmentIdVerification v = new GovernmentIdVerification();
		v.setWorkerProfile(workerProfile);
		v.setIdImagePath(idImagePath);
		v.setOcrText(truncate(ocrText, 4000));
		v.setFaceMatchScore(faceMatchScore);
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

	private static java.awt.image.BufferedImage cropFromCenterBox(java.awt.image.BufferedImage img,
			RoboflowClient.Detection det) {
		int w = Math.max(1, det.width());
		int h = Math.max(1, det.height());
		int x1 = det.x() - w / 2;
		int y1 = det.y() - h / 2;
		return ImageUtils.crop(img, x1, y1, w, h);
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
