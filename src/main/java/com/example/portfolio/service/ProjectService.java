package com.example.portfolio.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.portfolio.dto.PhotoListDto;
import com.example.portfolio.dto.ProjectCreateDto;
import com.example.portfolio.dto.ProjectDetailDto;
import com.example.portfolio.dto.ProjectDetailPageDto;
import com.example.portfolio.dto.ProjectListCustomDto;
import com.example.portfolio.dto.ProjectListDto;
import com.example.portfolio.dto.ProjectUpdateDto;
import com.example.portfolio.exception.CustomException;
import com.example.portfolio.exception.ErrorCode;
import com.example.portfolio.mapper.ProjectMapper;
import com.example.portfolio.model.Project;
import com.example.portfolio.repository.PhotoRepository;
import com.example.portfolio.repository.ProjectRepository;

@Service
public class ProjectService {

	private final ProjectRepository projectRepository;
	private final GcsService gcsService;
	private final PhotoService photoService;
	private final ProjectMapper projectMapper;
	private final PhotoRepository photoRepository;

	// 여러 의존성을 생성자로 주입
	public ProjectService(ProjectRepository projectRepository, GcsService gcsService, PhotoService photoService,
			ProjectMapper projectMapper, PhotoRepository photoRepository) {
		this.projectRepository = projectRepository;
		this.gcsService = gcsService;
		this.photoService = photoService;
		this.projectMapper = projectMapper;
		this.photoRepository = photoRepository;
	}

	@Transactional
	@Caching(evict = { @CacheEvict(value = "projectList", allEntries = true),
			@CacheEvict(value = "adminProjectList", allEntries = true) })
	public void createProject(ProjectCreateDto projectCreateDtos) {
		Project project = projectMapper.createDtoToProject(projectCreateDtos);

		Long projectId = projectRepository.save(project).getId();

		MultipartFile multipartFile = projectCreateDtos.getThumbnailMultipartFile();
		String url = gcsService.uploadWebpFile(multipartFile, projectId);
		project.setThumbnailUrl(url);

		photoService.createPhotos(projectCreateDtos, projectId);

		projectRepository.save(project);
	}

	// 프로젝트 업데이트
	@Transactional
	@Caching(evict = { @CacheEvict(value = "project", allEntries = true),
			@CacheEvict(value = "projectList", allEntries = true),
			@CacheEvict(value = "adminProjectList", allEntries = true),
			@CacheEvict(value = "adminProject", key = "#projectUpdateDto.id") })
	public void updateProject(ProjectUpdateDto projectUpdateDto) {

		Project existingProject = projectRepository.findById(projectUpdateDto.getId())
				.orElseThrow(() -> new RuntimeException("Project not found"));

		Project project = projectMapper.upadateDtoToProject(projectUpdateDto);
		project.setCreatedAt(existingProject.getCreatedAt());
		project.setThumbnailUrl(existingProject.getThumbnailUrl());

		// 썸네일이 있는 경우에만 업데이트
		if (projectUpdateDto.getThumbnailMultipartFile() != null
				&& !projectUpdateDto.getThumbnailMultipartFile().isEmpty()) {

			// 기존 썸네일 삭제
			gcsService.deleteThumbnailFile(project.getThumbnailUrl());

			// 새 썸네일 업로드
			String url = gcsService.uploadWebpFile(projectUpdateDto.getThumbnailMultipartFile(), project.getId());
			project.setThumbnailUrl(url);
		}

		// 기존 사진 삭제
		if (projectUpdateDto.getDeletedPhotoIds() != null && !projectUpdateDto.getDeletedPhotoIds().isEmpty()) {
			photoService.deleteSelectedPhotos(projectUpdateDto.getDeletedPhotoIds());
		}

		// 사진이 있는 경우 업데이트
		if (projectUpdateDto.getPhotoMultipartFiles() != null && projectUpdateDto.getPhotoMultipartFiles().length > 0) {
			photoService.updatePhotos(projectUpdateDto);
		}

		projectRepository.save(project);
	}

	// 프로젝트 삭제
	@Transactional
	@Caching(evict = { @CacheEvict(value = "project", allEntries = true),
			@CacheEvict(value = "projectList", allEntries = true),
			@CacheEvict(value = "adminProjectList", allEntries = true),
			@CacheEvict(value = "adminProject", key = "#id") })
	public void deleteProject(Long id) {
		Project project = projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Project not found"));
		// GCS 썸네일과 관련 사진들 삭제
		gcsService.deleteThumbnailFile(project.getThumbnailUrl());
		photoService.deletePhotosByProjectId(id);

		projectRepository.delete(project);
	}

	// 프로젝트 불러오기
	@Transactional
	@Cacheable(value = "projectList", key = "(#categoryId != null ? #categoryId : 'all') + '-' + (#subCategoryId != null ? #subCategoryId : 'all') + '-' + #pageable.pageNumber")
	public Slice<ProjectListDto> getProjectList(Pageable pageable, Long categoryId, Long subCategoryId) {
		if (categoryId == null && subCategoryId == null) {
			return projectRepository.findAllProject(pageable);
		} else if (subCategoryId == null) {
			return projectRepository.findByCategory_id(pageable, categoryId);
		} else {
			return projectRepository.findBySubCategory_id(pageable, subCategoryId);
		}
	}

	// admin page 프로젝트 불러오기
	@Transactional
	@Cacheable(value = "adminProjectList", key = "#keyWord + '-' + #pageable.pageNumber + #pageable.sort.toString()")
	public ProjectListCustomDto getAdminProjectList(Pageable pageable, String keyWord) {
		Page<ProjectListDto> projectListDto = projectRepository.findByKeyWord(pageable, keyWord);
		ProjectListCustomDto pojectListCustomDto = new ProjectListCustomDto();
		pojectListCustomDto.setContent(projectListDto.getContent());
		pojectListCustomDto.setTotalPages(projectListDto.getTotalPages());
		return pojectListCustomDto;
	}

	// admin 프로젝트 디테일 정보 가져오기
	@Transactional(readOnly = true)
	@Cacheable(value = "adminProject", key = "#projectId")
	public ProjectDetailDto getAdminProject(Long projectId) {
		ProjectDetailDto projectDetail = projectRepository.findProjectDetailByProjectId(projectId);
		List<PhotoListDto> photoList = photoRepository.findDetailPhotoByProjectId(projectId);
		projectDetail.setPhotos(photoList);
		return projectDetail;
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "project", key = "#projectId + '-' + #pageable.pageNumber")
	public ProjectDetailPageDto getPhotoList(Pageable pageable, Long projectId) {

		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FIND_PROJECT,
						"Project not found with id: " + projectId));

		Slice<PhotoListDto> photos = photoRepository.findByPhotosProjectId(projectId, pageable);
		return new ProjectDetailPageDto(project.getTitle(), project.getThumbnailUrl(), photos);
	}

	@CacheEvict(value = "adminProjectList", allEntries = true)
	public void updateViewCount(Long projectId) {
		projectRepository.updateViewCount(projectId);
	}

}
