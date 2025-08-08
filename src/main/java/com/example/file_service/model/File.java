package com.example.file_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_attachments")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalName;
    private String mimeType;
    private long size;

    @Column(unique = true)
    private String fileName;
    private LocalDateTime addedTime;

    @PrePersist
    private void setTime(){
        addedTime = LocalDateTime.now();
    }
}
