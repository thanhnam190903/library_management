package com.example.library_management.repository;

import com.example.library_management.entity.LibraryCard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryCardRepository extends JpaRepository<LibraryCard,String> {
}
