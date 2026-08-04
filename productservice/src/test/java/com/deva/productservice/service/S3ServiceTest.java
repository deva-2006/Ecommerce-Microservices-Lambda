package com.deva.productservice.service;

import com.deva.productservice.dto.UploadUrlResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URL;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @InjectMocks
    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(s3Service, "region", "us-east-1");
    }

    @Test
    void generateS3Key_shouldContainProductsPrefix() throws Exception {
        java.lang.reflect.Method method = S3Service.class.getDeclaredMethod("generateS3Key", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(s3Service, "photo.jpg");

        assertThat(result)
                .startsWith("products/")
                .endsWith("-photo.jpg");
    }

    @Test
    void generateS3Key_shouldContainUuid() throws Exception {
        java.lang.reflect.Method method = S3Service.class.getDeclaredMethod("generateS3Key", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(s3Service, "image.png");

        String withoutPrefix = result.replace("products/", "");
        String uuidPart = withoutPrefix.replace("-image.png", "");
        assertThat(uuidPart).hasSize(36);
    }

    @Test
    void generateS3Key_shouldCombinePrefixUuidAndFileName() throws Exception {
        java.lang.reflect.Method method = S3Service.class.getDeclaredMethod("generateS3Key", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(s3Service, "doc.pdf");

        assertThat(result)
                .startsWith("products/")
                .endsWith("-doc.pdf")
                .contains("-doc.pdf");
    }

    @Test
    void buildPublicUrl_shouldFormatCorrectly() throws Exception {
        java.lang.reflect.Method method = S3Service.class.getDeclaredMethod("buildPublicUrl", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(s3Service, "products/abc-photo.jpg");

        assertThat(result).isEqualTo("https://test-bucket.s3.amazonaws.com/products/abc-photo.jpg");
    }

    @Test
    void buildPublicUrl_withDifferentKey() throws Exception {
        java.lang.reflect.Method method = S3Service.class.getDeclaredMethod("buildPublicUrl", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(s3Service, "products/uuid-img.png");

        assertThat(result)
                .startsWith("https://test-bucket.s3.amazonaws.com/")
                .endsWith("products/uuid-img.png");
    }

    @Test
    void buildPublicUrl_shouldContainBucketName() throws Exception {
        java.lang.reflect.Method method = S3Service.class.getDeclaredMethod("buildPublicUrl", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(s3Service, "products/test-key.jpg");

        assertThat(result).contains("test-bucket");
    }

    @Test
    void buildPublicUrl_shouldStartWithHttps() throws Exception {
        java.lang.reflect.Method method = S3Service.class.getDeclaredMethod("buildPublicUrl", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(s3Service, "products/key.jpg");

        assertThat(result).startsWith("https://");
    }

    @Test
    void generatePresignedUploadUrl_shouldReturnUploadAndPublicUrls() throws Exception {
        S3Presigner presigner = mock(S3Presigner.class);
        S3Presigner.Builder builder = mock(S3Presigner.Builder.class);
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        URL uploadUrl = mock(URL.class);

        try (MockedStatic<S3Presigner> staticMock = mockStatic(S3Presigner.class)) {
            staticMock.when(S3Presigner::builder).thenReturn(builder);
            when(builder.build()).thenReturn(presigner);
            when(presigner.presignPutObject(any(Consumer.class))).thenReturn(presignedRequest);
            when(presignedRequest.url()).thenReturn(uploadUrl);
            when(uploadUrl.toString()).thenReturn("https://s3.upload.example/put-object");

            UploadUrlResponseDTO dto = s3Service.generatePresignedUploadUrl("photo.jpg", "image/jpeg");

            assertThat(dto).isNotNull();
            assertThat(dto.getUploadUrl()).isEqualTo("https://s3.upload.example/put-object");
            assertThat(dto.getPublicUrl()).startsWith("https://test-bucket.s3.amazonaws.com/products/");
            assertThat(dto.getPublicUrl()).endsWith("-photo.jpg");
        }
    }
}
