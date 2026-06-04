package com.example.library_management.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "book_copies")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookCopy {
    @Id
    String id;
    @Column(unique = true, nullable = false)
    String barcode;
    String shelfLocation;
    // Trạng thái mượn trả
    String circulationStatus;
    // Tình trạng vật lý
    String bookCondition;
    @CreationTimestamp
    LocalDateTime importDate;
    @ManyToOne
    @JoinColumn(name = "book_title_id")
    BookTitle bookTitle;
    @OneToMany(mappedBy = "bookCopy")
    List<BorrowDetail> borrowDetails;
    boolean status ;


}
