package com.example.scms.repository;

import com.example.scms.domain.entity.ComplaintImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComplaintImageRepository extends JpaRepository<ComplaintImage, UUID> {
}
