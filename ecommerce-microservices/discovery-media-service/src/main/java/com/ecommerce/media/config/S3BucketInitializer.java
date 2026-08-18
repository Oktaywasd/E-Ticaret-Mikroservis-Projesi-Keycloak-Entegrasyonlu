package com.ecommerce.media.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3BucketInitializer {

    private final S3Client s3Client;
    private final S3Config s3Config;

    @PostConstruct
    public void initBuckets() {
        List<String> requiredBuckets = List.of(s3Config.getVideosBucket(), s3Config.getThumbnailsBucket());

        for (String bucket : requiredBuckets) {
            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
                log.info("Bucket zaten mevcut: {}", bucket);
            } catch (S3Exception e) {
                log.info("Bucket bulunamadı veya erişilemedi ({}), oluşturuluyor: {}", e.awsErrorDetails().errorMessage(), bucket);
                try {
                    s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                    setPublicReadPolicy(s3Client, bucket);
                } catch (S3Exception createEx) {
                    log.error("Bucket oluşturulurken hata: {}", createEx.awsErrorDetails().errorMessage(), createEx);
                }
            }
        }
    }

    private void setPublicReadPolicy(S3Client client, String bucketName) {
        String policy = """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Principal": "*",
                  "Action": ["s3:GetObject"],
                  "Resource": ["arn:aws:s3:::%s/*"]
                }
              ]
            }
            """.formatted(bucketName);

        client.putBucketPolicy(PutBucketPolicyRequest.builder()
                .bucket(bucketName)
                .policy(policy)
                .build());
        log.info("Public-read politikası uygulandı: {}", bucketName);
    }
}