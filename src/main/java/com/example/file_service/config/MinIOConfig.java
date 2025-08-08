package com.example.file_service.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinIOConfig {

    @Value("${MINIO_URL}")
    private String url;

    @Value("${MINIO_USER}")
    private String user;

    @Value("${MINIO_PASSWORD}")
    private String password;

    @Bean
    public MinioClient minioClient(){
        return MinioClient
                .builder()
                .endpoint(url)
                .credentials(user, password)
                .build();
    }


}
