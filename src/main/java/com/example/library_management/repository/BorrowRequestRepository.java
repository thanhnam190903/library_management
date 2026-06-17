package com.example.library_management.repository;

import com.example.library_management.entity.BorrowRequest;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BorrowRequestRepository extends JpaRepository<BorrowRequest,String> {

    @Query(" SELECT br FROM BorrowRequest br WHERE br.libraryCard.id = :cardId AND br.status <> 0 ")
    List<BorrowRequest> findByCardIdAndStatusNotZero(@Param("cardId") String cardId);

    @Modifying
    @Transactional
    @Query("UPDATE BorrowRequest br SET br.status = :status WHERE br.id = :id")
    void updateStatus(@Param("id") String id, @Param("status") Integer status);

    @Query("SELECT COUNT(br) FROM BorrowRequest br WHERE br.libraryCard.id = :cardId AND br.status = 1")
    long countByCardIdAndStatus1(@Param("cardId") String cardId);

//    @Query(" SELECT br FROM BorrowRequest br WHERE br.status <> 0 " +
//            "AND br.libraryCard.status = true " +
//            "ORDER BY br.libraryCard.overdueCount DESC, br.requestDateTime ASC ")
//    List<BorrowRequest> findPriorityRequests();

    @Query("SELECT COUNT(br) FROM BorrowRequest br " +
            "WHERE br.status = 1 AND br.libraryCard.status = true")
    long countPendingRequests();

    @Query("""
    SELECT br
    FROM BorrowRequest br
    WHERE br.status <> 0
      AND br.libraryCard.status = true
      AND (
            :keyword IS NULL
            OR :keyword = ''
            OR LOWER(br.id) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(br.bookCopy.bookTitle.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(br.libraryCard.reader.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )AND (
            :status IS NULL
            OR br.status = :status)
    ORDER BY br.libraryCard.overdueCount DESC,
             br.requestDateTime ASC
    """)
    List<BorrowRequest> findPriorityRequests(
            @Param("keyword") String keyword,
            @Param("status") Integer status
    );

    @Query("""
    select br
    from BorrowRequest br
    where br.status = 2
    and br.requestDateTime <= :expiredTime
    """)
    List<BorrowRequest> findExpiredConfirmedRequests(
            @Param("expiredTime") LocalDateTime expiredTime);
}
