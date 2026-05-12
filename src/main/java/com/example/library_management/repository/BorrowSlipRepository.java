package com.example.library_management.repository;

import com.example.library_management.entity.BorrowSlip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowSlipRepository extends JpaRepository<BorrowSlip,String> {
}
