package com.example.library_management.repository;

import com.example.library_management.entity.BookCopy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookCopyRepository extends JpaRepository<BookCopy,String> {
    List<BookCopy> findByBookTitleId(String bookTitleId);
    List<BookCopy> findByBookTitleIdAndCirculationStatus(String bookTitleId,
                                                         String circulationStatus);
}
