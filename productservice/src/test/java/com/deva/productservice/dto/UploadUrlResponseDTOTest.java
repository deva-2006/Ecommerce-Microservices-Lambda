package com.deva.productservice.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UploadUrlResponseDTOTest {

    @Test
    void builder_shouldBuildCorrectly() {
        UploadUrlResponseDTO dto = UploadUrlResponseDTO.builder()
                .uploadUrl("https://s3.example.com/upload")
                .publicUrl("https://s3.example.com/public/img.jpg")
                .build();

        assertThat(dto.getUploadUrl()).isEqualTo("https://s3.example.com/upload");
        assertThat(dto.getPublicUrl()).isEqualTo("https://s3.example.com/public/img.jpg");
    }

    @Test
    void settersAndGetters_shouldWork() {
        UploadUrlResponseDTO dto = UploadUrlResponseDTO.builder().build();
        dto.setUploadUrl("https://upload.url");
        dto.setPublicUrl("https://public.url");

        assertThat(dto.getUploadUrl()).isEqualTo("https://upload.url");
        assertThat(dto.getPublicUrl()).isEqualTo("https://public.url");
    }

    @Test
    void equals_sameValues_shouldBeEqual() {
        UploadUrlResponseDTO a = UploadUrlResponseDTO.builder()
                .uploadUrl("url1").publicUrl("url2").build();
        UploadUrlResponseDTO b = UploadUrlResponseDTO.builder()
                .uploadUrl("url1").publicUrl("url2").build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_differentValues_shouldNotBeEqual() {
        UploadUrlResponseDTO a = UploadUrlResponseDTO.builder()
                .uploadUrl("url1").build();
        UploadUrlResponseDTO b = UploadUrlResponseDTO.builder()
                .uploadUrl("url2").build();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_sameInstance_shouldBeEqual() {
        UploadUrlResponseDTO dto = UploadUrlResponseDTO.builder().build();
        assertThat(dto).isEqualTo(dto);
    }

    @Test
    void equals_null_shouldNotBeEqual() {
        UploadUrlResponseDTO dto = UploadUrlResponseDTO.builder().build();
        assertThat(dto).isNotEqualTo(null);
    }

    @Test
    void equals_differentType_shouldNotBeEqual() {
        UploadUrlResponseDTO dto = UploadUrlResponseDTO.builder().build();
        assertThat(dto).isNotEqualTo("string");
    }

    @Test
    void toString_shouldContainFieldInfo() {
        UploadUrlResponseDTO dto = UploadUrlResponseDTO.builder()
                .uploadUrl("https://upload").publicUrl("https://public").build();

        String result = dto.toString();

        assertThat(result)
                .contains("uploadUrl=https://upload")
                .contains("publicUrl=https://public")
                .contains("UploadUrlResponseDTO");
    }

    @Test
    void builder_withNulls_shouldBuild() {
        UploadUrlResponseDTO dto = UploadUrlResponseDTO.builder().build();

        assertThat(dto.getUploadUrl()).isNull();
        assertThat(dto.getPublicUrl()).isNull();
    }
}
