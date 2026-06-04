package com.example.library_management.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "borrow_slips")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BorrowSlip {
    @Id
    String id;
    @Column(name = "borrow_date")
    LocalDate borrowDate;
    @Column(name = "due_date")
    LocalDate dueDate;
    @Column(name = "total_fine")
    Double totalFine;
    @Column(name = "old_due_date")
    LocalDate oldDueDate;
    @Column(name = "renewed")
    Boolean renewed = false;
    boolean status;
    @ManyToOne
    @JoinColumn(name = "card_id")
    LibraryCard libraryCard;
    @OneToMany(mappedBy = "borrowSlip")
    List<BorrowDetail> details;
}
