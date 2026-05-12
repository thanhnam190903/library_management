package com.example.library_management.repository;

import com.example.library_management.entity.BorrowDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowDetailRepository extends JpaRepository<BorrowDetail,String> {
}
