package com.example.library_management.repository;

import com.example.library_management.entity.LibraryCard;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryCardRepository extends JpaRepository<LibraryCard,String> {

}
