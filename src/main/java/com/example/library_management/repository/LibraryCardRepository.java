package com.example.library_management.repository;

import com.example.library_management.entity.LibraryCard;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LibraryCardRepository extends JpaRepository<LibraryCard,String> {
    @Query(" SELECT lc FROM LibraryCard lc JOIN FETCH lc.reader r ORDER BY lc.totalBorrow DESC")
    List<LibraryCard> findTopBorrowers(Pageable pageable);
}
