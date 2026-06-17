package com.example.library_management.repository;

import com.example.library_management.entity.BookCopy;
import com.example.library_management.entity.DigitalBook;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DigitalBookRepository extends JpaRepository<DigitalBook,String> {
    List<DigitalBook> findByBookTitleId(String bookTitleId);

    @Query(" SELECT d FROM DigitalBook d WHERE d.status = 1 ORDER BY d.viewCount DESC ")
    List<DigitalBook> findTopDigitalBooks(Pageable pageable);
}
