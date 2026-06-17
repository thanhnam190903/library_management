package com.example.library_management.repository;

import com.example.library_management.entity.Category;
import com.example.library_management.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category,String> {
    @Query("SELECT c FROM Category c WHERE " +
            " c.deleted = false")
    List<Category> getAllCategory();

    @Query("""
    SELECT c FROM Category c
    WHERE c.deleted = false
    AND (:keyword IS NULL OR :keyword = ''
        OR LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')))
    AND (:status IS NULL OR c.status = :status)
    """)
    Page<Category> searchCategories(
            @Param("keyword") String keyword,
            @Param("status") Boolean status,
            Pageable pageable);

    @Modifying
    @Transactional
    @Query("update Category c set c.deleted = true where c.id = :id")
    void deleteCategory(@Param("id") String id);

    List<Category> findByParent(String parent);
    List<Category> findByParentIsNull();

    @Query("SELECT DISTINCT c FROM Category c JOIN BookTitle b ON b.category = c " +
            "WHERE c.deleted = false")
    List<Category> findCategoriesHasBooks();


}
