package com.example.library_management.repository;

import com.example.library_management.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,String> {

    @Query("""
    SELECT DISTINCT u FROM User u
    JOIN u.roles r
    WHERE u.deleted = false
    AND (:keyword IS NULL OR :keyword = ''
        OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
    AND (:role IS NULL OR :role = '' OR r.roleName = :role)
    AND (:status IS NULL OR u.status = :status)
    """)
    Page<User> searchUsers(@Param("keyword") String keyword, @Param("role") String role,
            @Param("status") Boolean status, Pageable pageable);

    Optional<User> findByEmail(String email);

    @Modifying
    @Transactional
    @Query("update User u set u.deleted = true where u.id = :id")
    void deleteUser(@Param("id") String id);
}
