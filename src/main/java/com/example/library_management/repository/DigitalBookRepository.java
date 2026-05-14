package com.example.library_management.repository;

import com.example.library_management.entity.BookCopy;
import com.example.library_management.entity.DigitalBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DigitalBookRepository extends JpaRepository<DigitalBook,String> {
    List<DigitalBook> findByBookTitleId(String bookTitleId);
}
