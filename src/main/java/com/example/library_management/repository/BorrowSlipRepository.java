package com.example.library_management.repository;

import com.example.library_management.entity.BorrowSlip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BorrowSlipRepository extends JpaRepository<BorrowSlip,String> {

}
