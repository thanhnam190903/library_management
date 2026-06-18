package com.example.library_management.repository;

import com.example.library_management.entity.Author;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthorRepository extends JpaRepository<Author,String> {
    Author findByAuthorName(String authorName);

    Page<Author> findByDeletedFalseAndAuthorNameContainingIgnoreCase(
            String keyword,
            Pageable pageable
    );
    @Modifying
    @Transactional
    @Query("update Author c set c.deleted = true where c.id = :id")
    void deleteauthor(@Param("id") String id);
}
