package com.example.portfolio.model;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Admin implements Serializable {
	
	@Id
	private String id;
	
	private String password;
	
	public Admin() {}
	
	
	public Admin(String id, String password) {
		super();
		this.id = id;
		this.password = password;
	}
	
	// Getter & Setter
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	@Override
	public String toString() {
		return "Admin [id='" + id + "', password='" + password + "']";
	}
}
