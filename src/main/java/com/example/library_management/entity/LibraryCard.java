package com.example.library_management.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "library_cards")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LibraryCard {
    @Id
    String id;
    @Column(name = "card_name")
    String cardName;
    @Column(name = "issue_date")
    LocalDate issueDate;
    @Column(name = "expiry_date")
    LocalDate expiryDate;
    @Column(name = "max_books_allowed")
    int maxBooksAllowed;
    boolean status;
    @ManyToOne
    @JoinColumn(name = "reader_id")
    Reader reader;
    @OneToMany(mappedBy = "libraryCard")
    List<BorrowSlip> borrowSlips;
}
