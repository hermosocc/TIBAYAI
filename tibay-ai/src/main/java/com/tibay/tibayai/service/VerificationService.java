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

		String ocrText = ocrSpaceClient.extractText(idFile).orElse("");
		boolean idLooksValid = looksLikePhilippineId(ocrText);

		Integer faceMatchScore = null;
		boolean faceOk = false;
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
				faceOk = faceMatchScore >= 80;
			}
		} catch (Exception e) {
			faceOk = false;
		}

		VerificationStatus status;
		String summary;
		if (idLooksValid && faceOk) {
			status = VerificationStatus.VERIFIED;
			summary = "AI-assisted identity verification: VERIFIED. Checks passed: ID format appears valid; Face match confidence: HIGH; Identity consistency detected.";
		} else {
			status = VerificationStatus.FAILED;
			StringBuilder sb = new StringBuilder("AI-assisted identity verification: FAILED. Reasons: ");
			if (!idLooksValid) {
				sb.append("Invalid ID format detected. ");
			}
			if (!faceOk) {
				sb.append("Face mismatch or face not detected. ");
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
		int x1 = det.x() - det.width() / 2;
		int y1 = det.y() - det.height() / 2;
		return ImageUtils.crop(img, x1, y1, det.width(), det.height());
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

