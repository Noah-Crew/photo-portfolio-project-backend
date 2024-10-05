package com.example.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.portfolio.model.Admin;

@Repository
public interface AdminRepository extends JpaRepository<Admin, String>{

}
