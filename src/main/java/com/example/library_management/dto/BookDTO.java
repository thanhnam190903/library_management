package com.example.library_management.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookDTO {
    String bookTitleId;
    String title;
    String authorName;
    String categoryName;
    long   borrowCount;
    long   activeBorrows;
    String coverImage;
}
