package com.example.library_management.repository;

import com.example.library_management.entity.BorrowDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BorrowDetailRepository extends JpaRepository<BorrowDetail,String> {
    @Query("""
        SELECT COUNT(bd)
        FROM BorrowDetail bd
        WHERE bd.borrowSlip.libraryCard.id = :cardId
        AND bd.status = 0
        """)
    long countBorrowing(@Param("cardId") String cardId);
    @Query("""
        SELECT COUNT(bd)
        FROM BorrowDetail bd
        WHERE bd.borrowSlip.libraryCard.id = :cardId
        AND bd.status = 0
        AND bd.borrowSlip.dueDate < CURRENT_DATE
       """)
    long countOverdue(@Param("cardId") String cardId);
}
