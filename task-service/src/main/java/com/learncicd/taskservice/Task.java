package com.learncicd.taskservice;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tasks")
@Getter
@Setter
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String status; // e.g. "Pending", "In Progress", "Completed"

    @Column(name = "user_id")
    private Long userId;

    // Getters and Setters
}
