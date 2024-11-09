package com.example.portfolio.service;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.portfolio.dto.ProjectCreateDto;
import com.example.portfolio.dto.ProjectUpdateDto;
import com.example.portfolio.exception.CustomException;
import com.example.portfolio.model.Photo;
import com.example.portfolio.repository.PhotoRepository;
import com.example.portfolio.repository.ProjectRepository;

@Service
public class PhotoService {

	private final GcsService gcsService;
	private final PhotoRepository photoRepository;
	private final ProjectRepository projectRepository;

	//생성자 주입
	public PhotoService(GcsService gcsService, PhotoRepository photoRepository, ProjectRepository projectRepository) {
		this.gcsService = gcsService;
		this.photoRepository = photoRepository;
		this.projectRepository = projectRepository;
	}

	// 사진 생성
	public void createPhotos(ProjectCreateDto projectCreateDto, Long projectId) {
	    MultipartFile[] multipartFiles = projectCreateDto.getPhotoMultipartFiles();

	    for (MultipartFile multipartFile : multipartFiles) {
	            Photo photo = new Photo();
	            // WebP 형식으로 변환된 이미지를 GCS에 업로드
	            // uploadWebpFile 안에서 try catch로 예외 잡고 있어서 여기서는 예외 처리 불필요
	            String url = gcsService.uploadWebpFile(multipartFile, projectId); // WebP 형식 업로드 메서드 호출
	            photo.setImageUrl(url);
	            photo.setImgoname(multipartFile.getOriginalFilename());
	            photo.setImgtype("image/webp"); // 변환 후 이미지 형식을 webp로 설정
	            photo.setProjectId(projectId);
	            photoRepository.save(photo);
	    }
	}

	// 사진 업데이트
	public void updatePhotos(ProjectUpdateDto projectUpdateDto) {
		MultipartFile[] multipartFiles = projectUpdateDto.getPhotoMultipartFiles();

		for (MultipartFile multipartFile : multipartFiles) {
			Photo newPhoto = createPhoto(multipartFile, projectUpdateDto.getId());
			String url = gcsService.uploadWebpFile(multipartFile, projectUpdateDto.getId());
			newPhoto.setImageUrl(url);
			newPhoto.setImgoname(multipartFile.getOriginalFilename());
			newPhoto.setImgtype(multipartFile.getContentType());
			photoRepository.save(newPhoto);
		
		}
	}

	// 사진 삭제
	public void deletePhotosByProjectId(Long projectId) {
		List<Photo> photos = photoRepository.findAllByProjectId(projectId);
		photoRepository.deleteAll(photos);
		gcsService.deletePhotoToGcs(photos);
	}

	// 있다면 true, 없다면 false
	private boolean isPhotoInFiles(Photo existingPhoto, MultipartFile[] multipartFiles, Long projectId) {
		for (MultipartFile multipartFile : multipartFiles) {
			Photo newPhoto = createPhoto(multipartFile, projectId);
			if (existingPhoto.equals(newPhoto)) {
				return true;
			}
		}
		return false;
	}

	// 기존에 사진이 존재하는지 확인
	private Photo createPhoto(MultipartFile file, Long projectId) {
		Photo photo = new Photo();
		photo.setImgoname(file.getOriginalFilename());
		photo.setProjectId(projectId);
		photo.setImgtype(file.getContentType());
		return photo;
	}
	
	// edit에서 삭제
	public void deleteSelectedPhotos(List<Long> deletedPhotoIds) {
		List<Photo> selecetedPhotos = photoRepository.findAllById(deletedPhotoIds);
		photoRepository.deleteAll(selecetedPhotos);
		gcsService.deletePhotoToGcs(selecetedPhotos);
	}
}
