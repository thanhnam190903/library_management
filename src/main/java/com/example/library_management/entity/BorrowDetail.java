package com.example.library_management.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "borrow_details")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BorrowDetail {
    @Id
    private String id;
    @ManyToOne
    @JoinColumn(name = "borrow_slip_id")
    private BorrowSlip borrowSlip;
    @ManyToOne
    @JoinColumn(name = "book_copy_id")
    private BookCopy bookCopy;
    @ManyToOne
    @JoinColumn(name = "digital_book_id")
    private DigitalBook digitalBook;
    private String type; // PHYSICAL / DIGITAL
    private LocalDate returnDate;
    private double fineAmount;
    private Integer status;
}
