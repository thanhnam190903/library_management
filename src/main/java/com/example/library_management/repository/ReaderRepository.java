package com.example.library_management.repository;

import com.example.library_management.entity.Category;
import com.example.library_management.entity.Reader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReaderRepository extends JpaRepository<Reader,String> {
    @Query("SELECT r FROM Reader r WHERE " +
            " r.deleted = false")
    List<Reader> getAllReader();

    @Query("""
    SELECT DISTINCT r FROM Reader r
    JOIN r.cards c
    WHERE r.deleted = false
    AND (
        :keyword = ''
        OR LOWER(r.name)  LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(r.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(c.id)    LIKE LOWER(CONCAT('%', :keyword, '%'))
    )
    AND (
        :status = 'all'
        OR (:status = 'active'  AND c.expiryDate >= CURRENT_DATE)
        OR (:status = 'expired' AND c.expiryDate <  CURRENT_DATE)
    )
    ORDER BY r.name ASC
    """)
    Page<Reader> searchReaders(
            @Param("keyword") String keyword,
            @Param("status")  String status,
            Pageable pageable);
    Optional<Reader> findByEmail(String email);

    @Query("SELECT COUNT(DISTINCT r) FROM Reader r JOIN r.cards c " +
            "WHERE c.status = true AND c.locked = false " +
            "AND c.expiryDate >= :today AND r.deleted = false ")
    long countActiveMembers(@Param("today") LocalDate today);

    @Query(" SELECT COUNT(r) FROM Reader r " +
            "WHERE FUNCTION('YEAR',  r.createdDate) = :year " +
            "AND FUNCTION('MONTH', r.createdDate) = :month AND r.deleted = false ")
    long countNewThisMonth(@Param("year") int year, @Param("month") int month);

    @Query("""
    SELECT lc.id,
           r.name,
           r.email,
           r.phone,
           lc.issueDate,
           lc.expiryDate,
           CASE WHEN lc.expiryDate >= CURRENT_DATE THEN 'ĐANG HOẠT ĐỘNG'
                ELSE 'HẾT HẠN' END
    FROM LibraryCard lc
    JOIN lc.reader r
    ORDER BY lc.issueDate DESC
    """)
    List<Object[]> findAllForExport();
}
