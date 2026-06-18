package com.example.library_management.repository;

import com.example.library_management.entity.Publisher;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublisherRepository extends JpaRepository<Publisher,String> {
    Publisher findByPublisherName(String publisherName);

    Page<Publisher> findByDeletedFalseAndPublisherNameContainingIgnoreCase(
            String keyword,
            Pageable pageable
    );
    @Modifying
    @Transactional
    @Query("update Publisher c set c.deleted = true where c.id = :id")
    void deletepublisher(@Param("id") String id);
}
