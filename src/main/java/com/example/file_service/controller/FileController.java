package com.example.file_service.controller;

import com.example.file_service.dto.FileDTO;
import com.example.file_service.model.File;
import com.example.file_service.service.FileService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/file")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public String upload(@RequestBody MultipartFile file){
        return fileService.uploadFile(file);
    }

    @GetMapping("/download/{hashName}")
    public ResponseEntity<ByteArrayResource> download(@PathVariable String hashName) {
        // Получаем DTO
        FileDTO fileDTO = fileService.getFileByName(hashName);
        if (fileDTO == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Получаем байты
        ByteArrayResource resource = fileService.download(hashName);
        if (resource == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Получаем расширение из оригинального имени (например ".pdf")
        String originalName = fileDTO.getOriginalName();
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }

        // Формируем имя скачиваемого файла: хеш + расширение
        String downloadName = hashName + extension;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                .contentLength(fileDTO.getSize())
                .contentType(MediaType.parseMediaType(fileDTO.getMimeType()))
                .body(resource);
    }


    @GetMapping
    public List<FileDTO> getFiles(){
        return fileService.getFiles();
    }

    @GetMapping("/{id}")
    public FileDTO getFile(@PathVariable Long id){
        return fileService.getFile(id);
    }

    @GetMapping("file-name/{fileName}")
    public FileDTO getFileByName(@PathVariable String fileName){
        return fileService.getFileByName(fileName);
    }



}
