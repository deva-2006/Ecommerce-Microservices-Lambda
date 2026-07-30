package com.deva.productservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(result).startsWith("products/");
        assertThat(result).endsWith("-photo.jpg");
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

        assertThat(result).startsWith("products/");
        assertThat(result).endsWith("-doc.pdf");
        assertThat(result).contains("-doc.pdf");
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

        assertThat(result).startsWith("https://test-bucket.s3.amazonaws.com/");
        assertThat(result).endsWith("products/uuid-img.png");
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
}
