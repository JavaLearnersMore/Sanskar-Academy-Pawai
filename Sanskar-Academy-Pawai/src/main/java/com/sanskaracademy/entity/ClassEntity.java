package com.sanskaracademy.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_name", nullable = false)
    private String className;

    @Column(name = "academic_year")
    private String academicYear;

    @OneToMany(
            mappedBy = "classEntity",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Subject> subjects;   // ✅ This must be your entity
}