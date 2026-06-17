package com.example.library_management.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
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
    String fileName;
    String fileUrl;
    String fileType;
    String accessType;
    Integer viewCount;
    Integer status;
    @Column(length = 1000)
    String audioUrl;
    Boolean audioGenerated;
    @CreationTimestamp
    LocalDateTime createdAt;
    @Column(name = "last_modified")
    @UpdateTimestamp
    LocalDateTime lastModified;
}
