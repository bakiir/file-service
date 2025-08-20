package com.example.file_service.service;

import com.example.file_service.dto.FileDTO;
import com.example.file_service.mapper.FileMapper;
import com.example.file_service.model.File;
import com.example.file_service.repository.FileRepository;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.hamcrest.Matchers.in;
import static org.junit.jupiter.api.Assertions.*;


import static org.hamcrest.Matchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    private FileService fileService;

    private FileRepository fileRepository;

    private MinioClient minioClient;

    private FileMapper fileMapper;
    private final String bucket = "test-bucket";



    @BeforeEach
    void setup(){
        fileRepository = mock(FileRepository.class);
        minioClient = mock(MinioClient.class);
        fileMapper = mock(FileMapper.class);
        fileService = new FileService(minioClient, fileRepository, fileMapper);

        ReflectionTestUtils.setField(fileService, "bucket", bucket);
    }


    //============================ upload() method's tests ============================

    @Test
    void uploadFile() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello".getBytes()
        );

        File saved =  new File();
        saved.setId(1L);
        saved.setOriginalName("test.txt");
        saved.setSize(multipartFile.getSize());
        saved.setMimeType(multipartFile.getContentType());

        when(fileRepository.save(ArgumentMatchers.any(File.class))).thenReturn(saved);

        String result = fileService.uploadFile(multipartFile);
        assertNotNull(result);
        assertTrue(result.matches("[a-f0-9]{40}"));

        verify(fileRepository, times(2)).save(ArgumentMatchers.any(File.class));

        verify(minioClient, times(1)).putObject(ArgumentMatchers.any(PutObjectArgs.class));

    }

    @Test
    void testNullFile(){
        assertThrows(NullPointerException.class, ()->{
            fileService.uploadFile(null);
        });
        verifyNoInteractions(minioClient);
    }

    @Test
    void testEmptyFile() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", new byte[0]
        );

        File saved =  new File();
        saved.setId(2L);

        when(fileRepository.save(ArgumentMatchers.any(File.class))).thenReturn(saved);

        String result = fileService.uploadFile(multipartFile);

        assertNotNull(result);
        assertTrue(result.matches("[a-f0-9]{40}"));

        verify(fileRepository, times(2)).save(ArgumentMatchers.any(File.class));

        verify(minioClient, times(1)).putObject(ArgumentMatchers.any(PutObjectArgs.class));
    }

    @Test
    void testDatabaseFailure() {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello".getBytes()
        );

        when(fileRepository.save(ArgumentMatchers.any(File.class)))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> fileService.uploadFile(multipartFile));

        verifyNoInteractions(minioClient);
    }

    @Test
    void MinIOFailure() throws Exception{
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello".getBytes()
        );

        File saved = new File();
        saved.setId(3L);
        when(fileRepository.save(ArgumentMatchers.any(File.class))).thenReturn(saved);

        doThrow(new RuntimeException("MinIO down"))
                .when(minioClient)
                .putObject(ArgumentMatchers.any(PutObjectArgs.class));

        String result = fileService.uploadFile(multipartFile);

        assertEquals(result, "File is not uploaded!");


    }

    //============================ download() method's tests ============================
            @Test
        void testDownload_SuccessfulRetrieval_ReturnsByteArrayResource() throws Exception {
            // Arrange
            String hashName = "test-hash";
            String originalName = "707.png";
            String expectedExtension = ".png";
            String expectedDownloadName = hashName + expectedExtension;
            byte[] expectedBytes = "test content".getBytes();

            // Create a ByteArrayInputStream to simulate file content
            ByteArrayInputStream inputStream = new ByteArrayInputStream(expectedBytes);

            // Create a GetObjectResponse that wraps the ByteArrayInputStream
            GetObjectResponse getObjectResponse = new GetObjectResponse(
                    null, // Headers (can be null for testing)
                    bucket, // Bucket name
                    null, // Region (can be null for testing)
                    hashName, // Object name
                    inputStream // InputStream
            );

            File file = new File();
            file.setOriginalName(originalName);

            // Mock repository and MinioClient
            when(fileRepository.findByFileName(hashName)).thenReturn(file);
            when(minioClient.getObject(ArgumentMatchers.any(GetObjectArgs.class))).thenReturn(getObjectResponse);

            // Act
            ByteArrayResource result = fileService.download(hashName);

            // Assert
            assertNotNull(result, "ByteArrayResource should not be null");
            assertArrayEquals(expectedBytes, result.getByteArray(), "ByteArrayResource content should match expected bytes");
            verify(fileRepository).findByFileName(hashName);
            verify(minioClient).getObject(argThat(args ->
                    args.bucket().equals(bucket) && args.object().equals(hashName)));
        }



    @Test
    void testDownload_FileNotInDb() {
        // given
        when(fileRepository.findByFileName("unknown")).thenReturn(null);

        // when
        ByteArrayResource result = fileService.download("unknown");

        // then
        assertNull(result);
        verifyNoInteractions(minioClient);
    }

    @Test
    void testDownload_MinioFailure() throws Exception {
        // given
        String hashName = "abc123";
        File entity = new File();
        entity.setFileName(hashName);
        entity.setOriginalName("test.png");

        when(fileRepository.findByFileName(hashName)).thenReturn(entity);
        when(minioClient.getObject(ArgumentMatchers.any(GetObjectArgs.class)))
                .thenThrow(new RuntimeException("MinIO error"));

        // when
        ByteArrayResource result = fileService.download(hashName);

        // then
        assertNull(result);
    }



}