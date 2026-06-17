package com.example.library_management.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "book_titles")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookTitle {
    @Id
    String id;
    String title;
    @Column(name = "publish_year")
    String publishYear;
    @Column(nullable = true)
    String isbn;
    @Column(nullable = true)
    Double price;
    String language;
    @Column(columnDefinition = "LONGTEXT")
    String coverImage;
    @Column(nullable = true)
    Integer quantity;
    @Column(columnDefinition = "TEXT")
    private String note;
    boolean deleted;
    @ManyToOne
    @JoinColumn(name = "author_id")
    Author author;
    @ManyToOne
    @JoinColumn(name = "publisher_id")
    Publisher publisher;
    @ManyToOne
    @JoinColumn(name = "category_id")
    Category category;
    @OneToMany(mappedBy = "bookTitle")
    List<BookCopy> copies;
    @OneToMany(mappedBy = "bookTitle")
    List<DigitalBook> digitalBooks;
    @Column(name = "create_at")
    @CreationTimestamp
    LocalDateTime createdAt;
    @Column(name = "last_modified")
    @UpdateTimestamp
    LocalDateTime lastModified;
}
