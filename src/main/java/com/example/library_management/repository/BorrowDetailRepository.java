package com.example.library_management.repository;

import com.example.library_management.entity.BorrowDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BorrowDetailRepository extends JpaRepository<BorrowDetail,String> {
    @Query(" SELECT COUNT(bd) FROM BorrowDetail bd WHERE " +
            " bd.borrowSlip.libraryCard.id = :cardId AND bd.status = 1 ")
    long countBorrowing(@Param("cardId") String cardId);

    @Query(" SELECT COUNT(bd) FROM BorrowDetail bd WHERE " +
            " bd.borrowSlip.libraryCard.id = :cardId AND bd.status = 1 AND " +
            " bd.borrowSlip.dueDate < CURRENT_DATE ")
    long countOverdue(@Param("cardId") String cardId);

    @Query(" SELECT bd FROM BorrowDetail bd WHERE bd.status = 1 AND " +
            " bd.borrowSlip.libraryCard.id = :cardId ")
    List<BorrowDetail> findBorrowingByCardId(String cardId);

}
