package com.example.library_management.repository;

import com.example.library_management.entity.BookTitle;
import com.example.library_management.entity.BorrowDetail;
import org.springframework.data.domain.Pageable;
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

    @Query(" SELECT bd FROM BorrowDetail bd WHERE " +
            "bd.status = 1 AND bd.borrowSlip.dueDate < CURRENT_DATE ")
    List<BorrowDetail> findOverdueBooks();

    @Query(" SELECT bd FROM BorrowDetail bd WHERE bd.status = 0 AND bd.returnDate = CURRENT_DATE ")
    List<BorrowDetail> findReturnedToday();

    @Query(" SELECT bd FROM BorrowDetail bd WHERE bd.borrowSlip.id = :id ")
    List<BorrowDetail> findByBorrowSlipId(@Param("id") String id);

    @Query("SELECT bd FROM BorrowDetail bd WHERE bd.status = 1 AND" +
            " (bd.id = :keyword OR" +
            " bd.borrowSlip.id = :keyword)")
    List<BorrowDetail> findBorrowingBooks(@Param("keyword") String keyword);

    @Query("SELECT COUNT(bd) FROM BorrowDetail bd WHERE bd.status = 1")
    long countAllBorrowing();

    @Query("SELECT COUNT(bd) FROM BorrowDetail bd WHERE bd.status = 1 AND" +
            " bd.borrowSlip.dueDate < CURRENT_DATE")
    long overdueAllBooks();

    @Query("SELECT bt FROM BorrowDetail bd " +
            "JOIN bd.bookCopy bc " +
            "JOIN bc.bookTitle bt " +
            "GROUP BY bt ORDER BY COUNT(bd.id) DESC ")
    List<BookTitle> findTopFeaturedBooks(Pageable pageable);

}
