package com.example.library_management.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    @Column(name = "role_name")
    String roleName;
    @Column(name = "created_date")
            @CreationTimestamp
    LocalDateTime createdDate;
    @Column(name = "updated_date")
    @CreationTimestamp
    LocalDateTime updatedDate;
    @ManyToMany(mappedBy = "roles")
    List<User> users;
}
