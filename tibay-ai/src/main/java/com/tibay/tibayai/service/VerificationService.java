package com.tibay.tibayai.service;

import java.io.File;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

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

	@Transactional
	public GovernmentIdVerification verify(WorkerProfile workerProfile, String idImagePath, String selfiePath) {
		File idFile = storageService.resolveAbsolute(idImagePath).toFile();
		File selfieFile = storageService.resolveAbsolute(selfiePath).toFile();

		boolean ocrEnabled = ocrSpaceClient.enabled();
		boolean faceEnabled = roboflowClient.facesEnabled();

		String ocrText = ocrEnabled ? ocrSpaceClient.extractText(idFile).orElse("") : "";
		Boolean idLooksValid = ocrEnabled && StringUtils.hasText(ocrText) ? looksLikePhilippineId(ocrText) : null;

		Integer faceMatchScore = null;
		Boolean faceOk = null;
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
					if (faceMatchScore >= 80) {
						faceOk = true;
					} else if (faceMatchScore <= 40) {
						faceOk = false;
					} else {
						faceOk = null;
					}
				}
			} catch (Exception e) {
				faceOk = null;
			}
		}

		VerificationStatus status;
		String summary;
		if (Boolean.TRUE.equals(idLooksValid) && Boolean.TRUE.equals(faceOk)) {
			status = VerificationStatus.VERIFIED;
			summary = "AI-assisted identity verification: VERIFIED. Checks passed: ID format appears valid; Face match confidence: HIGH; Identity consistency detected.";
		} else if (Boolean.FALSE.equals(idLooksValid) && ocrEnabled) {
			status = VerificationStatus.FAILED;
			summary = "AI-assisted identity verification: FAILED. Reasons: Invalid ID format detected.";
		} else {
			status = VerificationStatus.REVIEW_REQUIRED;
			StringBuilder sb = new StringBuilder("AI-assisted identity verification: REVIEW_REQUIRED. ");
			sb.append("This is not a legal verification. ");
			sb.append("Checks: ");
			if (!ocrEnabled) {
				sb.append("OCR=UNAVAILABLE; ");
			} else if (idLooksValid == null) {
				sb.append("OCR=INCONCLUSIVE; ");
			} else if (Boolean.TRUE.equals(idLooksValid)) {
				sb.append("OCR=PASS; ");
			} else {
				sb.append("OCR=FAIL; ");
			}

			if (!faceEnabled) {
				sb.append("FACE=UNAVAILABLE; ");
			} else if (faceOk == null) {
				sb.append("FACE=INCONCLUSIVE; ");
			} else if (Boolean.TRUE.equals(faceOk)) {
				sb.append("FACE=PASS; ");
			} else {
				sb.append("FACE=FAIL; ");
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
