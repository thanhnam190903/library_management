package com.example.library_management.repository;

import com.example.library_management.entity.LibraryRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LibararyRuleRepository extends JpaRepository<LibraryRule,Integer> {
    Optional<LibraryRule> findByRuleKey(String ruleKey);
}
