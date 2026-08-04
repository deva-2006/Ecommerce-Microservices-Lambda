package com.deva.reviewservice.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewResponseDTOTest {

    @Test
    void noArgsConstructor_andSetters() {
        ReviewResponseDTO dto = new ReviewResponseDTO();
        dto.setReviewId("rev-1");
        dto.setProductId("prod-1");
        dto.setUserId("user-1");
        dto.setUserName("Alice");
        dto.setRating(5);
        dto.setComment("Great");
        dto.setVerifiedBuyer(true);
        dto.setCreatedAt("2026-08-01T10:00:00Z");

        assertThat(dto.getReviewId()).isEqualTo("rev-1");
        assertThat(dto.getProductId()).isEqualTo("prod-1");
        assertThat(dto.getUserId()).isEqualTo("user-1");
        assertThat(dto.getUserName()).isEqualTo("Alice");
        assertThat(dto.getRating()).isEqualTo(5);
        assertThat(dto.getComment()).isEqualTo("Great");
        assertThat(dto.getVerifiedBuyer()).isTrue();
        assertThat(dto.getCreatedAt()).isEqualTo("2026-08-01T10:00:00Z");
    }

    @Test
    void allArgsConstructor_setsFields() {
        ReviewResponseDTO dto = new ReviewResponseDTO("rev-1", "prod-1", "user-1", "Alice", 5, "Great", true, "ts");

        assertThat(dto.getReviewId()).isEqualTo("rev-1");
        assertThat(dto.getProductId()).isEqualTo("prod-1");
        assertThat(dto.getUserId()).isEqualTo("user-1");
        assertThat(dto.getUserName()).isEqualTo("Alice");
        assertThat(dto.getRating()).isEqualTo(5);
        assertThat(dto.getComment()).isEqualTo("Great");
        assertThat(dto.getVerifiedBuyer()).isTrue();
        assertThat(dto.getCreatedAt()).isEqualTo("ts");
    }

    @Test
    void builder_buildsDto() {
        ReviewResponseDTO dto = ReviewResponseDTO.builder()
                .reviewId("rev-2")
                .productId("prod-2")
                .userId("user-2")
                .userName("Bob")
                .rating(3)
                .comment("Meh")
                .verifiedBuyer(false)
                .createdAt("ts2")
                .build();

        assertThat(dto.getReviewId()).isEqualTo("rev-2");
        assertThat(dto.getProductId()).isEqualTo("prod-2");
        assertThat(dto.getUserId()).isEqualTo("user-2");
        assertThat(dto.getUserName()).isEqualTo("Bob");
        assertThat(dto.getRating()).isEqualTo(3);
        assertThat(dto.getComment()).isEqualTo("Meh");
        assertThat(dto.getVerifiedBuyer()).isFalse();
        assertThat(dto.getCreatedAt()).isEqualTo("ts2");
    }

    @Test
    void equalsAndHashCode_consistent() {
        ReviewResponseDTO a = ReviewResponseDTO.builder().reviewId("rev-1").rating(5).build();
        ReviewResponseDTO b = ReviewResponseDTO.builder().reviewId("rev-1").rating(5).build();
        ReviewResponseDTO c = ReviewResponseDTO.builder().reviewId("rev-9").rating(5).build();

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    void toString_containsFields() {
        ReviewResponseDTO dto = ReviewResponseDTO.builder().reviewId("rev-1").productId("prod-1").rating(5).build();

        assertThat(dto.toString()).contains("reviewId=rev-1").contains("productId=prod-1").contains("rating=5");
    }
}
