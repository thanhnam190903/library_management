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

    // ngày cấp
    @Column(name = "issue_date")
    LocalDate issueDate;

    // ngày hết hạn
    @Column(name = "expiry_date")
    LocalDate expiryDate;

    // số sách tối đa được mượn
    @Column(name = "max_books_allowed")
    int maxBooksAllowed;

    // tổng số lượt đã mượn
    Integer totalBorrow;

    // số lần quá hạn
    Integer overdueCount;
    // trạng thái thẻ
    boolean status;
    // bị khóa hay không
    boolean locked;
    @ManyToOne
    @JoinColumn(name = "reader_id")
    Reader reader;

    @OneToMany(mappedBy = "libraryCard")
    List<BorrowSlip> borrowSlips;
}
