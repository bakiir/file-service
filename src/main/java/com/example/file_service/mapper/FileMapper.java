package com.example.file_service.mapper;

import com.example.file_service.dto.FileDTO;
import com.example.file_service.model.File;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FileMapper {
    FileDTO toDto(File file);
    File toEntity(FileDTO dto);
    List<FileDTO> toDtoList(List<File> list);

}
