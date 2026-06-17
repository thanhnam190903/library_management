package com.example.library_management.repository;

import com.example.library_management.entity.BookTitle;
import com.example.library_management.entity.BorrowDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BorrowDetailRepository extends JpaRepository<BorrowDetail,String> {
    @Query(" SELECT COUNT(bd) FROM BorrowDetail bd WHERE " +
            " bd.borrowSlip.libraryCard.id = :cardId AND bd.status = 1 ")
    long countBorrowing(@Param("cardId") String cardId);

    @Query(" SELECT COUNT(bd) FROM BorrowDetail bd WHERE " +
            " bd.borrowSlip.libraryCard.id = :cardId AND bd.status = 1 AND " +
            " bd.borrowSlip.dueDate < CURRENT_DATE ")
    long countOverdue(@Param("cardId") String cardId);

    @Query("""
    SELECT bd
    FROM BorrowDetail bd
    WHERE
    ( :keyword = '' 
     OR LOWER(bd.bookCopy.bookTitle.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
     OR LOWER(bd.borrowSlip.libraryCard.reader.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
    AND(:filter = ''
        OR (:filter = 'borrowing' AND bd.status = 1)
        OR (:filter = 'returned' AND bd.status = 0)
        OR (:filter = 'overdue' AND bd.status = 1 AND bd.borrowSlip.dueDate < CURRENT_DATE))
    """)
    Page<BorrowDetail> searchHistory(
            @Param("keyword") String keyword,
            @Param("filter") String filter,
            Pageable pageable
    );

    @Query(" SELECT bd FROM BorrowDetail bd WHERE bd.status = 1 AND " +
            " bd.borrowSlip.libraryCard.id = :cardId ")
    List<BorrowDetail> findBorrowingByCardId(String cardId);

    @Query(" SELECT bd FROM BorrowDetail bd WHERE " +
            "bd.status = 1 AND bd.borrowSlip.dueDate < CURRENT_DATE ")
    List<BorrowDetail> findOverdueBooks();

    @Query(" SELECT COUNT(bd) FROM BorrowDetail bd " +
            "WHERE bd.status = 1 " +
            "AND bd.borrowSlip.dueDate < CURRENT_DATE ")
    long countOverdueBooks();

    @Query(" SELECT bd FROM BorrowDetail bd WHERE bd.status = 0 AND bd.returnDate = CURRENT_DATE ")
    List<BorrowDetail> findReturnedToday();

    @Query(" SELECT bd FROM BorrowDetail bd WHERE bd.borrowSlip.id = :id ")
    List<BorrowDetail> findByBorrowSlipId(@Param("id") String id);

    @Query("SELECT bd FROM BorrowDetail bd WHERE bd.status = 1 AND" +
            " (bd.id = :keyword OR" +
            " bd.borrowSlip.id = :keyword)")
    List<BorrowDetail> findBorrowingBooks(@Param("keyword") String keyword);


    @Query("SELECT COUNT(bd) FROM BorrowDetail bd WHERE bd.status = 1 AND" +
            " bd.borrowSlip.dueDate < CURRENT_DATE")
    long overdueAllBooks();

    @Query("SELECT bt FROM BorrowDetail bd " +
            "JOIN bd.bookCopy bc " +
            "JOIN bc.bookTitle bt " +
            "GROUP BY bt ORDER BY COUNT(bd.id) DESC ")
    List<BookTitle> findTopFeaturedBooks(Pageable pageable);

    @Query(" SELECT bd FROM BorrowDetail bd WHERE bd.borrowSlip.libraryCard.id = :cardId ")
    List<BorrowDetail> findBorrowByCardId(String cardId);

    @Query(" SELECT bd FROM BorrowDetail bd JOIN bd.borrowSlip bs WHERE " +
            "bs.libraryCard.id = :cardId AND bs.renewed = true ")
    List<BorrowDetail> findByCardIdAndRenewedTrue(@Param("cardId") String cardId);

    @Query(" SELECT COUNT(bd) FROM BorrowDetail bd WHERE " +
            " bd.borrowSlip.libraryCard.id = :cardId AND bd.status = 0 ")
    long countBorrowReturn(@Param("cardId") String cardId);

    @Query("SELECT bd FROM BorrowDetail bd WHERE bd.status = 1 AND bd.borrowSlip.renewed = true " )
    List<BorrowDetail> findRenewedBorrowDetails();

    //Báo cáo thống kê
    @Query("SELECT COUNT(bd) FROM BorrowDetail bd WHERE bd.status = 1")
    long countAllBorrowing();

    @Query(" SELECT COUNT(bd) FROM BorrowDetail bd " +
            "JOIN bd.borrowSlip bs WHERE bd.status = 1 " +
            "AND bs.borrowDate BETWEEN :from AND :to ")
    long countBorrowingInRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT FUNCTION('DAY', bs.borrowDate) AS day, " +
            "COUNT(bd) AS cnt FROM BorrowDetail bd " +
            "JOIN bd.borrowSlip bs " +
            "WHERE FUNCTION('YEAR',  bs.borrowDate) = :year " +
            "AND FUNCTION('MONTH', bs.borrowDate) = :month " +
            "GROUP BY FUNCTION('DAY', bs.borrowDate) ORDER BY FUNCTION('DAY', bs.borrowDate) ")
    List<Object[]> countByDayInMonth(@Param("year") int year, @Param("month") int month);

    @Query(" SELECT COUNT(bd) FROM BorrowDetail bd " +
            "JOIN bd.borrowSlip bs WHERE FUNCTION('YEAR',  bs.borrowDate) = :year " +
            "AND FUNCTION('MONTH', bs.borrowDate) = :month ")
    long countByMonth(@Param("year") int year, @Param("month") int month);

    @Query("""
        SELECT bc.bookTitle.id,
               bc.bookTitle.title,
               bc.bookTitle.author.authorName,
               bc.bookTitle.category.categoryName,
               COUNT(bd) AS totalBorrows,
               SUM(CASE WHEN bd.status = 1 THEN 1 ELSE 0 END) AS activeBorrows,
               bc.bookTitle.coverImage
        FROM BorrowDetail bd
        JOIN bd.bookCopy bc
        WHERE bd.borrowSlip.borrowDate BETWEEN :from AND :to
        GROUP BY bc.bookTitle.id, bc.bookTitle.title,
                 bc.bookTitle.author.authorName,
                 bc.bookTitle.category.categoryName,
                 bc.bookTitle.coverImage
        ORDER BY totalBorrows DESC
        """)
    List<Object[]> findTopBorrowedBooks(@Param("from") LocalDate from,
                                        @Param("to")   LocalDate to,
                                        Pageable pageable);

    @Query("""
    SELECT CAST(bs.borrowDate AS date), COUNT(bd)
    FROM BorrowDetail bd JOIN bd.borrowSlip bs
    WHERE bs.borrowDate BETWEEN :from AND :to
    GROUP BY CAST(bs.borrowDate AS date)
    ORDER BY CAST(bs.borrowDate AS date)
    """)
    List<Object[]> countByDayInRange(@Param("from") LocalDate from,
                                     @Param("to")   LocalDate to);

    @Query("""
    SELECT FUNCTION('YEAR',  bs.borrowDate),
           FUNCTION('MONTH', bs.borrowDate),
           COUNT(bd)
    FROM BorrowDetail bd JOIN bd.borrowSlip bs
    WHERE bs.borrowDate BETWEEN :from AND :to
    GROUP BY FUNCTION('YEAR',  bs.borrowDate),
             FUNCTION('MONTH', bs.borrowDate)
    ORDER BY FUNCTION('YEAR',  bs.borrowDate),
             FUNCTION('MONTH', bs.borrowDate)
    """)
    List<Object[]> countByMonthInRange(@Param("from") LocalDate from,
                                       @Param("to")   LocalDate to);

    // xuất báo cáo
    @Query("""
    SELECT COUNT(bd) FROM BorrowDetail bd
    JOIN bd.borrowSlip bs
    WHERE bs.borrowDate BETWEEN :from AND :to
    """)
    long countBorrowInRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // Đếm lượt trả trong range
    @Query("""
    SELECT COUNT(bd) FROM BorrowDetail bd
    WHERE bd.status = 0
    AND bd.returnDate BETWEEN :from AND :to
    """)
    long countReturnInRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // Tổng tiền phạt trong range
    @Query("""
    SELECT COALESCE(SUM(bd.fineAmount), 0) FROM BorrowDetail bd
    JOIN bd.borrowSlip bs
    WHERE bd.returnDate BETWEEN :from AND :to
    AND bd.fineAmount > 0
    """)
    double sumFineInRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // Export danh sách mượn
    @Query("""
    SELECT bd.id,
           bs.id,
           r.name,
           bt.title,
           bs.borrowDate,
           bs.dueDate,
           bd.returnDate,
           CASE WHEN bd.status = 1 AND bs.dueDate < CURRENT_DATE THEN 'QUÁ HẠN'
                WHEN bd.status = 1 THEN 'ĐANG MƯỢN'
                ELSE 'ĐÃ TRẢ' END,
           bd.fineAmount
    FROM BorrowDetail bd
    JOIN bd.borrowSlip bs
    JOIN bs.libraryCard lc
    JOIN lc.reader r
    JOIN bd.bookCopy bc
    JOIN bc.bookTitle bt
    WHERE bs.borrowDate BETWEEN :from AND :to
    ORDER BY bs.borrowDate DESC
    """)
    List<Object[]> findBorrowsForExport(@Param("from") LocalDate from,
                                        @Param("to")   LocalDate to);

    @Query("""
    SELECT bs.id,
           r.name,
           bt.title,
           bs.borrowDate,
           bs.dueDate,
           DATEDIFF(CURRENT_DATE, bs.dueDate),
           bd.fineAmount
    FROM BorrowDetail bd
    JOIN bd.borrowSlip bs
    JOIN bs.libraryCard lc
    JOIN lc.reader r
    JOIN bd.bookCopy bc
    JOIN bc.bookTitle bt
    WHERE bd.status = 1
    AND bs.dueDate < CURRENT_DATE
    ORDER BY bs.dueDate ASC
    """)
    List<Object[]> findOverdueForExport();

    // Export tài chính
    @Query("""
    SELECT bs.id,
           r.name,
           bd.returnDate,
           'Phí phạt quá hạn',
           bd.fineAmount
    FROM BorrowDetail bd
    JOIN bd.borrowSlip bs
    JOIN bs.libraryCard lc
    JOIN lc.reader r
    WHERE bd.status = 0
    AND bd.returnDate BETWEEN :from AND :to
    AND bd.fineAmount > 0
    ORDER BY bd.returnDate DESC
    """)
    List<Object[]> findFinanceForExport(@Param("from") LocalDate from,
                                        @Param("to")   LocalDate to);


    @Query(" SELECT bt FROM BorrowDetail bd " +
            "JOIN bd.bookCopy bc JOIN bc.bookTitle bt " +
            "GROUP BY bt " +
            "ORDER BY MAX(bd.createdAt) DESC ")
    List<BookTitle> findTopRecentBorrowedBooks(Pageable pageable);

}
