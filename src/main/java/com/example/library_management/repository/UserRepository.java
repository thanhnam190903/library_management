package com.example.library_management.repository;

import com.example.library_management.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,String> {
    @Query("SELECT u FROM User u WHERE " +
            " u.deleted = false")
    List<User> getAllUser();

    Optional<User> findByEmail(String email);

    @Modifying
    @Transactional
    @Query("update User u set u.deleted = true where u.id = :id")
    void deleteUser(@Param("id") String id);
}
