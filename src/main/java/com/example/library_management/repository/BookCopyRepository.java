package com.example.library_management.repository;

import com.example.library_management.entity.BookCopy;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookCopyRepository extends JpaRepository<BookCopy,String> {

    List<BookCopy> findByBookTitleId(String bookTitleId);

    @Query("""
    SELECT b
    FROM BookCopy b
    WHERE b.bookTitle.id = :bookTitleId
      AND b.deleted = false
      AND (
            :keyword IS NULL
            OR :keyword = ''
            OR LOWER(b.bookCondition) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(b.shelfLocation) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
      AND (
            :circulationStatus IS NULL
            OR :circulationStatus = ''
            OR b.circulationStatus = :circulationStatus)
    """)
    Page<BookCopy> searchBookCopies(
            @Param("bookTitleId") String bookTitleId,
            @Param("keyword") String keyword,
            @Param("circulationStatus") String circulationStatus,
            Pageable pageable);

    @Modifying
    @Transactional
    @Query("update BookCopy b set b.deleted = true where b.id = :id")
    void deleteBookCopy(@Param("id") String id);

    long countByBookTitleId(String bookTitleId);

    List<BookCopy> findByBookTitleIdAndCirculationStatus(String bookTitleId,
                                                         String circulationStatus);

    @Query(" SELECT COUNT(b) FROM BookCopy b WHERE b.bookTitle.id = :bookTitleId " +
           " AND deleted = false AND b.circulationStatus = 'available' AND b.status = true")
    long countAvailableBooks(@Param("bookTitleId") String bookTitleId);

    @Query(" SELECT b FROM BookCopy b WHERE ( b.bookTitle.id = :keyword OR " +
            " b.bookTitle.isbn = :keyword) AND b.circulationStatus = 'available' " +
            " AND b.status = true AND deleted = false")
    List<BookCopy> findAvailableBooks(@Param("keyword") String keyword);

}
