package com.example.portfolio.dto;

public class PhotoListDto {
	private Long id;
	private String imageUrl; 
	
	public PhotoListDto() {}

	public PhotoListDto(Long id, String imageUrl) {
		super();
		this.id = id;
		this.imageUrl = imageUrl;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}


	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String  imageUrl) {
		this.imageUrl = imageUrl;
	}
	
}
