package com.example.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.example.portfolio.dto.ProjectCreateDto;
import com.example.portfolio.model.Photo;
import com.example.portfolio.repository.PhotoRepository;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

	@InjectMocks
	private PhotoService photoService;
	
	@Mock
	private ProjectService projectService;
	
	@Mock
	private GcsService gcsService;
	
	@Mock
	private PhotoRepository photoRepository;
	
	@Test
	void testname() {
		try {
		// Given
		
		ProjectCreateDto projectCreateDto = new ProjectCreateDto();
		projectCreateDto.setTitle("서비스테스트제목");
		projectCreateDto.setCategoryId(1L);
		projectCreateDto.setSubcategoryId(1L);
		
		// 썸네일 사진 생성
		MockMultipartFile thumbnailFile = new MockMultipartFile(
				"썸네일",
				"thumbnail.jpg",
				"image/jpeg",
				"thumbnail content".getBytes()
				);
				
		 // 여러 개의 사진 파일 생성
	    MockMultipartFile photoFile1 = new MockMultipartFile(
	        "포토1", 
	        "photo1.jpg", 
	        "image/jpeg", 
	        "photo1 content".getBytes()
	    );
	    MockMultipartFile photoFile2 = new MockMultipartFile(
	        "포토2", 
	        "photo2.jpg", 
	        "image/jpeg", 
	        "photo2 content".getBytes()
	    );
	    
	 // MultipartFile 배열 생성
	    MultipartFile[] photoFiles = {photoFile1, photoFile2};
	    
	    projectCreateDto.setThumbnailMultipartFile(thumbnailFile);
	    projectCreateDto.setPhotoMultipartFiles(photoFiles);
	    
	    Long projectId = 1L;
	    
	    // GcsService mock 설정
	    // eq는 모키토 라이브러리에서 제공하는 인자 매처로 정확하게 일치하는 값만 인정함
			when(gcsService.uploadWebpFile(any(MultipartFile.class), eq(projectId)))
			    .thenReturn("http://example.com/photo1.jpg")
			    .thenReturn("http://example.com/photo2.jpg");

		// When
	    when(photoRepository.save(any(Photo.class))).then(AdditionalAnswers.returnsFirstArg());
	    photoService.createPhotos(projectCreateDto, projectId);

		// Then
	    // 모키토 라이브러리 캡처를 사용해서 값을 확인
	    ArgumentCaptor<Photo> photoCaptor = ArgumentCaptor.forClass(Photo.class);
	    
	    // 포토 레포지토리가 2번 실행되었는지 확인하고 캡처를 통해서 그 값들을 검증할 수 있게함
	    verify(photoRepository, times(2)).save(photoCaptor.capture());
	    
	    // 캡처에 저장된 모든 값을 불러옴
	    List<Photo> savedPhotos = photoCaptor.getAllValues();
	    
	    // 2개인지 확인
	    assertThat(savedPhotos).hasSize(2);
	    
	    // 첫 번째 Photo 객체 검사
	    Photo firstPhoto = savedPhotos.get(0);
	    assertThat(firstPhoto.getImageUrl()).isEqualTo("http://example.com/photo1.jpg");
        assertThat(firstPhoto.getImgoname()).isEqualTo("photo1.jpg");
        assertThat(firstPhoto.getImgtype()).isEqualTo("image/webp");
        assertThat(firstPhoto.getProjectId()).isEqualTo(projectId);
        
        // 두 번째 Photo 객체 검사
        Photo secondPhoto = savedPhotos.get(1);
        assertThat(secondPhoto.getImageUrl()).isEqualTo("http://example.com/photo2.jpg");
        assertThat(secondPhoto.getImgoname()).isEqualTo("photo2.jpg");
        assertThat(secondPhoto.getImgtype()).isEqualTo("image/webp");
        assertThat(secondPhoto.getProjectId()).isEqualTo(projectId);
        
        // GcsService 호출 확인
        verify(gcsService, times(2)).uploadWebpFile(any(MultipartFile.class), eq(projectId));
	    
	    
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
