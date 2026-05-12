package com.example.library_management.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "readers")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Reader {
    @Id
    String id;
    String name;
    String gender;
    @Column(name = "birth_date")
    LocalDate birthDate;
    String address;
    @Column(name = "job_title")
    String jobTitle;
    @Column(name = "deposit_fee")
    double depositFee;
    boolean status;
    @Column(name = "created_date")
    @CreationTimestamp
    LocalDateTime createdDate;
    @Column(name = "updated_date")
    @UpdateTimestamp
    LocalDateTime updatedDate;
    @OneToMany(mappedBy = "reader")
    List<LibraryCard> cards;
}
