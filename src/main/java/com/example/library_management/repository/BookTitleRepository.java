package com.example.library_management.repository;

import com.example.library_management.entity.BookTitle;
import com.example.library_management.entity.BorrowDetail;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookTitleRepository extends JpaRepository<BookTitle, String> {
    @Query("SELECT b FROM BookTitle b WHERE " +
            " b.deleted = false")
    List<BookTitle> getAllBookTitle();

    @Query("""
    SELECT b FROM BookTitle b
    WHERE b.deleted = false
    AND (
        :keyword IS NULL OR :keyword = ''
        OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(b.author.authorName) LIKE LOWER(CONCAT('%', :keyword, '%'))
    )AND (:categoryId IS NULL OR :categoryId = '' OR b.category.id = :categoryId)
    """)
    Page<BookTitle> searchBooks(
            @Param("keyword") String keyword,
            @Param("categoryId") String categoryId,
            Pageable pageable);

    @Modifying
    @Transactional
    @Query("update BookTitle b set b.deleted = true where b.id = :id")
    void deleteBookTitle(@Param("id") String id);

    @Query(" SELECT DISTINCT b FROM BookTitle b " +
            "LEFT JOIN b.author a LEFT JOIN b.category c " +
            "WHERE b.deleted = false " +
            "AND(:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(a.authorName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND ( :category IS NULL OR LOWER(c.categoryName) = LOWER(:category)) " +
            "AND ( :avail IS NULL OR (:avail = 'avail' " +
            "AND EXISTS ( SELECT 1 FROM BookCopy bc WHERE bc.bookTitle = b " +
            "AND bc.circulationStatus = 'AVAILABLE')) " +
            "OR (:avail = 'unavail' AND NOT EXISTS ( " +
            "SELECT 1 FROM BookCopy bc " +
            "WHERE bc.bookTitle = b " +
            "AND bc.circulationStatus = 'AVAILABLE'))) " +
            "AND ( :format IS NULL OR (:format = 'digital' AND SIZE(b.digitalBooks) > 0) " +
            "OR (:format = 'physical' AND SIZE(b.digitalBooks) = 0)) " +
            "ORDER BY b.id DESC ")
    List<BookTitle> searchBooks(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("avail") String avail,
            @Param("format") String format,
            Pageable pageable
    );

    long countByDeletedFalse();

    @Query("SELECT COUNT(bt) FROM BookTitle bt WHERE bt.createdAt BETWEEN :from AND :to AND bt.deleted = false")
    long countCreatedBetween(@Param("from") LocalDateTime from, @Param("to")   LocalDateTime to);

    @Query(" SELECT c.categoryName, COUNT(bt) FROM BookTitle bt " +
            "JOIN bt.category c WHERE bt.deleted = false " +
            "GROUP BY c.categoryName " +
            "ORDER BY COUNT(bt) DESC ")
    List<Object[]> countByCategory();

    // Export kho sách
    @Query("""
    SELECT bt.title,
           bt.author.authorName,
           bt.category.categoryName,
           COUNT(bc),
           SUM(CASE WHEN bc.circulationStatus = 'BORROWED' THEN 1 ELSE 0 END)
    FROM BookTitle bt
    LEFT JOIN bt.copies bc
    WHERE bt.deleted = false
    GROUP BY bt.id, bt.title, bt.author.authorName, bt.category.categoryName
    ORDER BY bt.title
    """)
    List<Object[]> findInventoryForExport();

    @Query("""
    SELECT b FROM BookTitle b
    LEFT JOIN b.author a
    LEFT JOIN b.category c
    WHERE (:keyword IS NULL OR :keyword = '' OR
           LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
           LOWER(a.authorName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
           LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
           LOWER(b.note) LIKE LOWER(CONCAT('%', :keyword, '%')))
    
    ORDER BY
        CASE
            WHEN LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) THEN 1
            WHEN LOWER(a.authorName) LIKE LOWER(CONCAT('%', :keyword, '%')) THEN 2
            WHEN LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')) THEN 3
            WHEN LOWER(b.note) LIKE LOWER(CONCAT('%', :keyword, '%')) THEN 4
            ELSE 5
        END,
        b.createdAt DESC
    """)
    Page<BookTitle> searchSmart(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
