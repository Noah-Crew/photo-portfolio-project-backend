package com.example.portfolio.service;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.portfolio.exception.CustomException;
import com.example.portfolio.exception.ErrorCode;
import com.example.portfolio.model.Photo;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;

@Service
public class GcsService {

    @Value("${spring.cloud.gcp.storage.bucket}")
    private String bucketName;

    private final Storage storage;
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    public GcsService(@Value("${spring.cloud.gcp.storage.credentials.location}") String keyFileName) throws IOException {
        InputStream keyFile = ResourceUtils.getURL(keyFileName).openStream();
        this.storage = StorageOptions.newBuilder()
                .setCredentials(GoogleCredentials.fromStream(keyFile))
                .build()
                .getService();
    }
//    public GcsService() {
//        try {
//            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
//            this.storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to initialize GCS credentials", e);
//        }
//    }


    public String uploadWebpFile(MultipartFile multipartFile, Long projectId) {
        try {
        	String uuid = UUID.randomUUID().toString();
            String objectName = projectId + "/" + uuid + ".webp";

            // WebP 이미지 변환
            ImmutableImage image = ImmutableImage.loader().fromStream(multipartFile.getInputStream());
            WebpWriter writer = WebpWriter.DEFAULT.withQ(80).withM(4).withZ(9);
            byte[] webpBytes = image.bytes(writer);

            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                    .setContentType("image/webp")
                    .build();

            // 동기적으로 GCS에 업로드
            storage.create(blobInfo, webpBytes);

            return "https://storage.googleapis.com/" + bucketName + "/" + objectName;


        } catch (IOException e) {
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.STORAGE_IO_ERROR,
                    "Failed to upload file: " + e.getMessage()
            );
        } catch (Exception e) {
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.STORAGE_IO_ERROR,
                    "File upload failed due to async exception: " + e.getMessage()
            );
        }
    }


    private BufferedImage resizeImage(BufferedImage originalImage) {
        int targetWidth = 1024; // 적절한 크기로 조정
        int targetHeight = (int) (originalImage.getHeight() * ((double) targetWidth / originalImage.getWidth()));
        
        Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(resultingImage, 0, 0, null);
        graphics2D.dispose();
        
        return resizedImage;
    }

    // 썸네일 파일 삭제
    public void deleteThumbnailFile(String thumbnailUrl) {
        String objectName = getObjectNameFromUrl(thumbnailUrl);
        Blob blob = storage.get(bucketName, objectName);

        if (blob != null) {
            storage.delete(bucketName, objectName);
        } else {
            throw new CustomException(
                    HttpStatus.NOT_FOUND,
                    ErrorCode.STORAGE_FILE_NOT_FOUND,
                    "Blob not found: " + objectName
            );
        }
    }

    // photo 파일 삭제
    public void deletePhotoToGcs(List<Photo> photos) {
        List<String> urls = photos.stream()
                .map(Photo::getImageUrl)
                .collect(Collectors.toList());

        for (String url : urls) {
            String objectName = getObjectNameFromUrl(url);
            Blob blob = storage.get(bucketName, objectName);

            if (blob != null) {
                storage.delete(bucketName, objectName);
            } else {
                throw new CustomException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.STORAGE_FILE_NOT_FOUND,
                        "File not found in storage: " + objectName
                );
            }
        }
    }

    // url로 object 이름 가져오는 메서드
    public String getObjectNameFromUrl(String url) {
        int index = url.indexOf("minography_gcs/") + "minography_gcs/".length();
        return url.substring(index);
    }
}
