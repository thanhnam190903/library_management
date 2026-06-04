package com.example.library_management.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "borrow_details")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BorrowDetail {
    @Id
    String id;
    @ManyToOne
    @JoinColumn(name = "borrow_slip_id")
    BorrowSlip borrowSlip;
    @ManyToOne
    @JoinColumn(name = "book_copy_id")
    BookCopy bookCopy;
    LocalDate returnDate;
    LocalTime returnHour;
    double fineAmount;
    Integer status;
    boolean overdueMailSent = false;
}
