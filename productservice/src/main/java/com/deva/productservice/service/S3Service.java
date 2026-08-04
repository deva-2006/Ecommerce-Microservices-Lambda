package com.deva.productservice.service;

import com.deva.productservice.dto.UploadUrlResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    @Value("${aws.s3.bucket-name:my-ecommerce-images-deva2006}")
    private String bucketName;

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    public UploadUrlResponseDTO generatePresignedUploadUrl(String fileName, String contentType) {
        String s3Key = generateS3Key(fileName);

        try (S3Presigner presigner = S3Presigner.builder().build()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .build();

            PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(
                    builder -> builder.signatureDuration(Duration.ofMinutes(5))
                            .putObjectRequest(putObjectRequest)
            );

            String uploadUrl = presignedRequest.url().toString();
            String publicUrl = buildPublicUrl(s3Key);

            return UploadUrlResponseDTO.builder()
                    .uploadUrl(uploadUrl)
                    .publicUrl(publicUrl)
                    .build();
        }
    }

    private String generateS3Key(String fileName) {
        String uuid = UUID.randomUUID().toString();
        return "products/" + uuid + "-" + fileName;
    }

    private String buildPublicUrl(String s3Key) {
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, s3Key);
    }
}
