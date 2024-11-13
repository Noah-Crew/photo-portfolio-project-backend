package com.example.portfolio.service;

import java.io.File;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;

public class ImageService {

    private static final String TMP_DIR = "/tmp/";

    public File convertToWebpWithLossless(String fileName, File originalFile) {
        try {
            // 변환 후 저장할 WebP 파일 경로 생성
            File webpFile = new File(TMP_DIR + fileName + ".webp");
            
            // 원본 파일을 WebP로 무손실 변환하여 저장
            ImmutableImage.loader()
                .fromFile(originalFile)
                .output(WebpWriter.DEFAULT.withLossless(), webpFile);

            return webpFile;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert image to WebP: " + e.getMessage(), e);
        }
    }
}
