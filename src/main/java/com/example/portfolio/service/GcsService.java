package com.example.portfolio.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;

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
		String fileName = UUID.randomUUID().toString();
		File originalFile = null;
		File webpFile = null;

		try {
			// 원본 파일을 /tmp에 임시 저장
			originalFile = new File("/tmp/" + fileName);
			multipartFile.transferTo(originalFile);

			// WebP 무손실 변환
			webpFile = convertToWebpWithLossless(fileName, originalFile);

			// 변환된 파일을 GCS에 업로드
			BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, projectId + "/" + webpFile.getName())
					.setContentType("image/webp").build();
			storage.create(blobInfo, Files.readAllBytes(webpFile.toPath()));

			return "https://storage.googleapis.com/" + bucketName + "/" + webpFile.getName();

		} catch (IOException e) {
			throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.STORAGE_IO_ERROR,
					"Failed to upload file: " + e.getMessage());
		} finally {
			// 임시 파일 삭제
			if (originalFile != null && originalFile.exists()) {
				originalFile.delete();
			}
			if (webpFile != null && webpFile.exists()) {
				webpFile.delete();
			}
		}
	}

	// WebP 무손실 변환 메서드
	public File convertToWebpWithLossless(String fileName, File originalFile) {
		try {
			File webpFile = new File("/tmp/" + fileName + ".webp");
			ImmutableImage.loader().fromFile(originalFile).output(WebpWriter.DEFAULT.withLossless(), webpFile);
			return webpFile;
		} catch (Exception e) {
			throw new RuntimeException("Failed to convert to WebP: " + e.getMessage(), e);
		}
	}

	// 썸네일 파일 삭제
	public void deleteThumbnailFile(String thumbnailUrl) {
		String objectName = getObjectNameFromUrl(thumbnailUrl);
		Blob blob = storage.get(bucketName, objectName);

		if (blob != null) {
			storage.delete(bucketName, objectName);
		} else {
			throw new CustomException(HttpStatus.NOT_FOUND, ErrorCode.STORAGE_FILE_NOT_FOUND,
					"Blob not found: " + objectName);
		}
	}

	// photo 파일 삭제
	public void deletePhotoToGcs(List<Photo> photos) {
		List<String> urls = photos.stream().map(Photo::getImageUrl).collect(Collectors.toList());

		for (String url : urls) {
			String objectName = getObjectNameFromUrl(url);
			Blob blob = storage.get(bucketName, objectName);

			if (blob != null) {
				storage.delete(bucketName, objectName);
			} else {
				throw new CustomException(HttpStatus.NOT_FOUND, ErrorCode.STORAGE_FILE_NOT_FOUND,
						"File not found in storage: " + objectName);
			}
		}
	}

	// url로 object 이름 가져오는 메서드
	public String getObjectNameFromUrl(String url) {
		int index = url.indexOf("minography_gcs/") + "minography_gcs/".length();
		return url.substring(index);
	}
}
