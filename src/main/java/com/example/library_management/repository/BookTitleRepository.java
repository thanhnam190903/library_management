package com.example.library_management.repository;

import com.example.library_management.entity.BookTitle;
import com.example.library_management.entity.Category;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookTitleRepository extends JpaRepository<BookTitle,String> {
    @Query("SELECT b FROM BookTitle b WHERE " +
            " b.deleted = false")
    List<BookTitle> getAllBookTitle();
    @Modifying
    @Transactional
    @Query("update BookTitle b set b.deleted = true where b.id = :id")
    void deleteBookTitle(@Param("id") String id);
}
