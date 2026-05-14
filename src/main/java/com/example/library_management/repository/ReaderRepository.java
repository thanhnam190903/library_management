package com.example.library_management.repository;

import com.example.library_management.entity.Category;
import com.example.library_management.entity.Reader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReaderRepository extends JpaRepository<Reader,String> {
    @Query("SELECT r FROM Reader r WHERE " +
            " r.deleted = false")
    List<Reader> getAllReader();
}
