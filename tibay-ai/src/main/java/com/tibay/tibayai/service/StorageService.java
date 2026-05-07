package com.tibay.tibayai.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
		String originalFilename = file.getOriginalFilename();
		String ext = "";
		if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
			ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
		}
		String filename = UUID.randomUUID() + ext;

		Path baseDir = Path.of(properties.getUploadDir()).toAbsolutePath().normalize();
		Path dir = baseDir.resolve(subdir).normalize();
		Files.createDirectories(dir);

		Path dest = dir.resolve(filename).normalize();
		file.transferTo(dest);

		return (subdir + "/" + filename).replace("\\", "/");
	}

	public Path resolveAbsolute(String relativePath) {
		Path baseDir = Path.of(properties.getUploadDir()).toAbsolutePath().normalize();
		return baseDir.resolve(relativePath).normalize();
	}
}

