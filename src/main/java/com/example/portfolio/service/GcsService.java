package com.example.portfolio.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.portfolio.exception.CustomException;
import com.example.portfolio.exception.ErrorCode;
import com.example.portfolio.model.Photo;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.luciad.imageio.webp.WebPWriteParam;

@Service
public class GcsService {

    @Value("${spring.cloud.gcp.storage.bucket}")
    private String bucketName;

    private final Storage storage;
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

//    public GcsService(@Value("${spring.cloud.gcp.storage.credentials.location}") String keyFileName) throws IOException {
//        InputStream keyFile = ResourceUtils.getURL(keyFileName).openStream();
//        this.storage = StorageOptions.newBuilder()
//                .setCredentials(GoogleCredentials.fromStream(keyFile))
//                .build()
//                .getService();
//    }
    public GcsService() {
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            this.storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize GCS credentials", e);
        }
    }


    public String uploadWebpFile(MultipartFile multipartFile, Long projectId) {
        try {
            // 이미지를 BufferedImage로 읽기
            BufferedImage originalImage = ImageIO.read(multipartFile.getInputStream());

            // WebP로 변환 및 사실상 무손실 압축 설정
            ByteArrayOutputStream webpOutputStream = new ByteArrayOutputStream();
            ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
            WebPWriteParam writeParam = new WebPWriteParam(writer.getLocale());

            // 최대 품질로 압축 설정
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(1.0f);  // 최대 품질 (무손실에 가까운 압축)
            }

            ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(webpOutputStream);
            writer.setOutput(imageOutputStream);
            writer.write(null, new IIOImage(originalImage, null, null), writeParam);
            writer.dispose();

            byte[] webpBytes = webpOutputStream.toByteArray();

            // 파일명 생성
            String uuid = UUID.randomUUID().toString();
            String objectName = projectId + "/" + uuid + ".webp";

            // BlobInfo 설정
            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                .setContentType("image/webp")
                .build();

            // GCS에 업로드
            storage.create(blobInfo, webpBytes);

            return "https://storage.googleapis.com/" + bucketName + "/" + objectName;
        } catch (IOException e) {
            throw new CustomException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.STORAGE_IO_ERROR,
                "Failed to upload file: " + e.getMessage()
            );
        }
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
