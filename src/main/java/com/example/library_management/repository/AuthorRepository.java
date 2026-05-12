package com.example.library_management.repository;

import com.example.library_management.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author,String> {
    Author findByAuthorName(String authorName);
}
