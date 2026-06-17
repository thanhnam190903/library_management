package com.example.library_management.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatisticsDTO {
    long   totalBooks;
    long   totalBooksDiff;
    long   currentlyBorrowing;
    double borrowingChangePct;
    long   activeMembers;
    long   newMembersThisMonth;
    long   borrowCountThisMonth;
    double borrowMonthChangePct;
    List<DailyBorrowDTO> dailyBorrows;
    List<CategoryDTO> categoryStats;
    List<BookDTO> topBooks;
    String fromDate;
    String toDate;
    String period;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyBorrowDTO {
        int    day;
        long   count;
        String dayType;
    }
}
