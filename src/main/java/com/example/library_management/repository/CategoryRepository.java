package com.example.library_management.repository;

import com.example.library_management.entity.Category;
import com.example.library_management.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category,String> {
    @Query("SELECT c FROM Category c WHERE " +
            " c.deleted = false")
    List<Category> getAllCategory();

    @Modifying
    @Transactional
    @Query("update Category c set c.deleted = true where c.id = :id")
    void deleteCategory(@Param("id") String id);

    List<Category> findByParent(String parent);
    List<Category> findByParentIsNull();
}
