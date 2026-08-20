package com.ecommerce.media.service;

import com.ecommerce.media.config.S3Config;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final S3Client s3Client;
    private final S3Config s3Config;

    @PostConstruct
    public void init() {
        createBucketIfNotExists(s3Config.getVideosBucket());
        createBucketIfNotExists(s3Config.getThumbnailsBucket());
    }

    private void createBucketIfNotExists(String bucketName) {
        if (bucketName == null || bucketName.isBlank()) return;
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (S3Exception e) {
            try {
                log.info("MinIO bucket oluşturuluyor: {}", bucketName);
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                log.info("MinIO bucket başarıyla oluşturuldu: {}", bucketName);
            } catch (Exception ex) {
                log.error("Bucket oluşturulamadı: {}", bucketName, ex);
            }
        }
    }

    public String uploadVideo(MultipartFile file) {
        return uploadFile(file, s3Config.getVideosBucket());
    }

    public String uploadThumbnail(MultipartFile file) {
        return uploadFile(file, s3Config.getThumbnailsBucket());
    }

    private String uploadFile(MultipartFile file, String bucketName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Yüklenecek dosya boş olamaz.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String key = UUID.randomUUID() + extension;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // Format: http://localhost:9000/{bucketName}/{key}
            return String.format("%s/%s/%s", s3Config.getEndpoint(), bucketName, key);
        } catch (IOException e) {
            log.error("Dosya yükleme hatası: {}", e.getMessage(), e);
            throw new RuntimeException("Dosya nesne depolama sunucusuna yüklenemedi.", e);
        }
    }
}