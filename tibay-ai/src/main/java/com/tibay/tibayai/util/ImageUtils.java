package com.tibay.tibayai.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public final class ImageUtils {
	private ImageUtils() {
	}

	public static BufferedImage read(File file) throws IOException {
		return ImageIO.read(file);
	}

	public static BufferedImage crop(BufferedImage src, int x, int y, int w, int h) {
		int cx = Math.max(0, x);
		int cy = Math.max(0, y);
		int cw = Math.min(w, src.getWidth() - cx);
		int ch = Math.min(h, src.getHeight() - cy);
		return src.getSubimage(cx, cy, cw, ch);
	}

	public static BufferedImage resize(BufferedImage src, int w, int h) {
		Image tmp = src.getScaledInstance(w, h, Image.SCALE_SMOOTH);
		BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = resized.createGraphics();
		g2.drawImage(tmp, 0, 0, null);
		g2.dispose();
		return resized;
	}

	public static long averageHash(BufferedImage img) {
		BufferedImage small = resize(img, 8, 8);
		long sum = 0;
		int[] px = new int[64];
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {
				int rgb = small.getRGB(x, y);
				int r = (rgb >> 16) & 0xff;
				int g = (rgb >> 8) & 0xff;
				int b = rgb & 0xff;
				int gray = (r + g + b) / 3;
				px[y * 8 + x] = gray;
				sum += gray;
			}
		}
		int avg = (int) (sum / 64);
		long hash = 0;
		for (int i = 0; i < 64; i++) {
			if (px[i] >= avg) {
				hash |= (1L << i);
			}
		}
		return hash;
	}

	public static int hammingDistance(long a, long b) {
		return Long.bitCount(a ^ b);
	}

	public static int similarityScore(long a, long b) {
		int dist = hammingDistance(a, b);
		int max = 64;
		int score = (int) Math.round((1.0 - (double) dist / max) * 100);
		return Math.max(0, Math.min(100, score));
	}
}

