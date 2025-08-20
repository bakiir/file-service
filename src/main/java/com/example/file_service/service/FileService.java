package com.example.file_service.service;

import com.example.file_service.dto.FileDTO;
import com.example.file_service.mapper.FileMapper;
import com.example.file_service.model.File;
import com.example.file_service.repository.FileRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final MinioClient minioClient;
    private final FileRepository fileRepository;
    private final FileMapper fileMapper;

    @Value("${MINIO_BUCKET}")
    private String bucket;

    public String uploadFile(MultipartFile file){

        if (file == null){
            throw new NullPointerException();
        }else {
            File fileToDb = new File();
            fileToDb.setSize(file.getSize());
            fileToDb.setOriginalName(file.getOriginalFilename());
            fileToDb.setMimeType(file.getContentType());

            fileToDb = fileRepository.save(fileToDb);

            String convertedName = convertToSha1("File_" + fileToDb.getId());


            try {

                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(convertedName)
                                .stream(file.getInputStream(), file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );

                fileToDb.setFileName(convertedName);
                fileRepository.save(fileToDb);

                return fileToDb.getFileName();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return "File is not uploaded!";
        }
    }


    public ByteArrayResource download(String hashName) {

        if (hashName == null) {
            throw  new NullPointerException();
        }

        File file = fileRepository.findByFileName(hashName);

        if (file == null){
            return null;
        }

        String originalName = file.getOriginalName(); // например: "707.png"
        String extension = "";

        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex); // ".png"
        }

        String downloadName = hashName + extension;

        try {
            GetObjectArgs args = GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(hashName)
                    .build();

            InputStream inputStream = minioClient.getObject(args);
            byte[] bytes = IOUtils.toByteArray(inputStream);
            inputStream.close();

            ByteArrayResource resource = new ByteArrayResource(bytes);
            return resource;

        } catch (Exception e) {
            log.error("Ошибка при загрузке файла {}: {}", hashName, e.getMessage());
            return null;
        }
    }

    private String convertToSha1(String text){
        return DigestUtils.sha1Hex(text);
    }

    public List<FileDTO> getFiles(){
        return fileMapper.toDtoList(fileRepository.findAll());
    }

    public FileDTO getFile (Long id){
        return fileMapper.toDto(fileRepository.findById(id).orElse(null));
    }

    public FileDTO getFileByName (String name){
        return fileMapper.toDto(fileRepository.findByFileName(name));
    }


}
