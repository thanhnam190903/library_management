package com.example.library_management.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@Table(name = "categories")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Category {
    @Id
    String id;
    @Column(name = "category_name", nullable = false)
    String categoryName;
    String parent;
    @OneToMany(mappedBy = "category")
    List<BookTitle> bookTitles;
    boolean status;
    boolean deleted;
}
