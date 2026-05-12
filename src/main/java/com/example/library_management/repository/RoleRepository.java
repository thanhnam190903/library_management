package com.example.library_management.repository;

import com.example.library_management.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleRepository extends JpaRepository<Role,Long> {
    List<Role> findByRoleNameIn(List<String> roleNames);
}
