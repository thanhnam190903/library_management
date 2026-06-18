package com.example.library_management.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@Table(name = "publishers")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Publisher {
    @Id
    String id;
    @Column(name = "publisher_name")
    String publisherName;
    String address;
    Boolean deleted = false;
    @OneToMany(mappedBy = "publisher")
    List<BookTitle> bookTitles;
}
