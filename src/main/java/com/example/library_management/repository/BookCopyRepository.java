package com.example.library_management.repository;

import com.example.library_management.entity.BookCopy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookCopyRepository extends JpaRepository<BookCopy,String> {

    List<BookCopy> findByBookTitleId(String bookTitleId);

    long countByBookTitleId(String bookTitleId);

    List<BookCopy> findByBookTitleIdAndCirculationStatus(String bookTitleId,
                                                         String circulationStatus);

    @Query(" SELECT COUNT(b) FROM BookCopy b WHERE b.bookTitle.id = :bookTitleId " +
           " AND b.circulationStatus = 'available'")
    long countAvailableBooks(@Param("bookTitleId") String bookTitleId);

    @Query(" SELECT b FROM BookCopy b WHERE ( b.bookTitle.id = :keyword OR " +
            " b.bookTitle.isbn = :keyword) AND b.circulationStatus = 'available'")
    List<BookCopy> findAvailableBooks(@Param("keyword") String keyword);
}
