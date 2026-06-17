package com.example.library_management.repository;

import com.example.library_management.entity.BorrowSlip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BorrowSlipRepository extends JpaRepository<BorrowSlip,String> {
    @Query(" SELECT COUNT(bs) FROM BorrowSlip bs WHERE bs.statusRenewed = 0 ")
    long countPendingRenew();

    @Query("""
    SELECT bs FROM BorrowSlip bs
    JOIN bs.details bd
    WHERE bd.status = 1
    AND bs.dueDate BETWEEN :start AND :end
    """)
    List<BorrowSlip> findUpcomingDue(LocalDate start, LocalDate end);
}
