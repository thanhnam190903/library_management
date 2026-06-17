package com.example.library_management.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "borrow_request")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BorrowRequest {
    @Id
    String id;
    @ManyToOne
    @JoinColumn(name = "card_id")
    LibraryCard libraryCard;
    @ManyToOne
    @JoinColumn(name = "book_copy_id")
    BookCopy bookCopy;
    LocalDateTime requestDateTime;
    Integer requestDays;
    String note;
    Integer status;
    @Column(name = "create_at")
    @CreationTimestamp
    LocalDateTime createdAt;
    @Column(name = "last_modified")
    @UpdateTimestamp
    LocalDateTime lastModified;
}
