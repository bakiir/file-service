package com.example.file_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.sql.results.internal.StandardEntityGraphTraversalStateImpl;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class FileDTO {
    private Long id;
    private String fileName;
    private String originalName;
    private long size;
    private LocalDateTime addedTime;
    private String mimeType;
}
