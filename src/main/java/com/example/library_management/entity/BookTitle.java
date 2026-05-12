package com.example.library_management.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
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
    @Min(value = 1900, message = "Năm xuất bản không hợp lệ")
    String publishYear;
    String isbn;
    double price;
    String language;
    @Column(columnDefinition = "LONGTEXT")
    String coverImage;
    int quantity;
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
}
