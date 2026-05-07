package com.tibay.tibayai.util;

import java.io.File;
import java.util.Optional;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

public final class MediaAuthenticityUtil {
	private MediaAuthenticityUtil() {
	}

	public static Optional<String> findSuspiciousSoftwareTag(File file) {
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(file);
			for (Directory dir : metadata.getDirectories()) {
				for (Tag tag : dir.getTags()) {
					String name = tag.getTagName();
					if (name == null) {
						continue;
					}
					String value = tag.getDescription();
					if (value == null) {
						continue;
					}
					String n = name.toLowerCase();
					String v = value.toLowerCase();
					if (n.contains("software") || n.contains("processing") || n.contains("creator")) {
						if (v.contains("stable diffusion") || v.contains("midjourney") || v.contains("dall-e")
								|| v.contains("generative") || v.contains("ai")) {
							return Optional.of(name + ": " + value);
						}
					}
				}
			}
			return Optional.empty();
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}

