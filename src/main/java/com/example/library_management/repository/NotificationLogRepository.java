package com.example.library_management.repository;

import com.example.library_management.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationLogRepository extends JpaRepository<NotificationLog,Integer> {
    @Query("""
        select coalesce(sum(n.totalSent),0)
        from NotificationLog n
        where n.notificationType = :type
        and DATE(n.createdAt) = CURRENT_DATE
    """)
    Long sumTodayByType(@Param("type") String type);
}
