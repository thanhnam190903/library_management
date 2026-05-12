package com.example.library_management.repository;

import com.example.library_management.entity.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublisherRepository extends JpaRepository<Publisher,String> {
    Publisher findByPublisherName(String publisherName);
}
