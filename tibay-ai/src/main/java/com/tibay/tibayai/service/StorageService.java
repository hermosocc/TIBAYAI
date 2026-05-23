package com.tibay.tibayai.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.tibay.tibayai.config.TibayProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StorageService {
	private final TibayProperties properties;

	public String store(MultipartFile file, String subdir) throws IOException {
		return storeValidated(file, subdir, AllowedKind.ANY);
	}

	public String storeImage(MultipartFile file, String subdir) throws IOException {
		return storeValidated(file, subdir, AllowedKind.IMAGE);
	}

	public String storeImageOrVideo(MultipartFile file, String subdir) throws IOException {
		return storeValidated(file, subdir, AllowedKind.IMAGE_OR_VIDEO);
	}

	private String storeValidated(MultipartFile file, String subdir, AllowedKind allowedKind) throws IOException {
		String originalFilename = file.getOriginalFilename();
		String ext = "";
		if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
			ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
		}
		String contentType = file.getContentType();
		validateAllowed(contentType, ext, allowedKind);
		String filename = UUID.randomUUID() + ext;

		Path baseDir = Path.of(properties.getUploadDir()).toAbsolutePath().normalize();
		Path dir = baseDir.resolve(subdir).normalize();
		Files.createDirectories(dir);

		Path dest = dir.resolve(filename).normalize();
		if (!dest.startsWith(dir)) {
			throw new IOException("Invalid upload path.");
		}
		file.transferTo(dest);

		return (subdir + "/" + filename).replace("\\", "/");
	}

	public Path resolveAbsolute(String relativePath) {
		Path baseDir = Path.of(properties.getUploadDir()).toAbsolutePath().normalize();
		return baseDir.resolve(relativePath).normalize();
	}

	private enum AllowedKind {
		ANY,
		IMAGE,
		IMAGE_OR_VIDEO
	}

	private static final Set<String> IMAGE_EXT = Set.of(".jpg", ".jpeg", ".png", ".webp");
	private static final Set<String> VIDEO_EXT = Set.of(".mp4", ".mov", ".webm");

	private static void validateAllowed(String contentType, String ext, AllowedKind allowedKind) throws IOException {
		if (allowedKind == AllowedKind.ANY) {
			return;
		}
		String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
		String e = ext == null ? "" : ext.toLowerCase(Locale.ROOT);

		boolean isImage = ct.startsWith("image/") || IMAGE_EXT.contains(e);
		boolean isVideo = ct.startsWith("video/") || VIDEO_EXT.contains(e);

		if (allowedKind == AllowedKind.IMAGE && !isImage) {
			throw new IOException("Only image uploads are allowed.");
		}
		if (allowedKind == AllowedKind.IMAGE_OR_VIDEO && !(isImage || isVideo)) {
			throw new IOException("Only image or video uploads are allowed.");
		}
	}
}
