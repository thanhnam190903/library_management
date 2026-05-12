package com.example.library_management.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@Table(name = "digital_books")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DigitalBook {
    @Id
    String id;
    @ManyToOne
    @JoinColumn(name = "book_title_id")
    BookTitle bookTitle;
    String fileUrl;
    String fileType;
    String accessType;
    Integer status;
    @OneToMany(mappedBy = "digitalBook")
    List<BorrowDetail> borrowDetails;
}
