package com.example.library_management.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@Table(name = "authors")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Author {
    @Id
    String id;
    @Column(name = "author_name")
    String authorName;
    Boolean deleted = false;
    @OneToMany(mappedBy = "author")
    List<BookTitle> bookTitles;
}
