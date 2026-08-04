package com.deva.reviewservice.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewRequestDTOTest {

    @Test
    void noArgsConstructor_andSetters() {
        ReviewRequestDTO dto = new ReviewRequestDTO();
        dto.setProductId("prod-1");
        dto.setRating(3);
        dto.setComment("Okay");
        dto.setUserName("Bob");

        assertThat(dto.getProductId()).isEqualTo("prod-1");
        assertThat(dto.getRating()).isEqualTo(3);
        assertThat(dto.getComment()).isEqualTo("Okay");
        assertThat(dto.getUserName()).isEqualTo("Bob");
    }

    @Test
    void allArgsConstructor_setsFields() {
        ReviewRequestDTO dto = new ReviewRequestDTO("prod-1", 5, "Great", "Alice");

        assertThat(dto.getProductId()).isEqualTo("prod-1");
        assertThat(dto.getRating()).isEqualTo(5);
        assertThat(dto.getComment()).isEqualTo("Great");
        assertThat(dto.getUserName()).isEqualTo("Alice");
    }

    @Test
    void builder_buildsDto() {
        ReviewRequestDTO dto = ReviewRequestDTO.builder()
                .productId("prod-1")
                .rating(4)
                .comment("Nice")
                .userName("Carol")
                .build();

        assertThat(dto.getProductId()).isEqualTo("prod-1");
        assertThat(dto.getRating()).isEqualTo(4);
        assertThat(dto.getComment()).isEqualTo("Nice");
        assertThat(dto.getUserName()).isEqualTo("Carol");
    }

    @Test
    void equalsAndHashCode_consistent() {
        ReviewRequestDTO a = ReviewRequestDTO.builder().productId("prod-1").rating(5).comment("A").build();
        ReviewRequestDTO b = ReviewRequestDTO.builder().productId("prod-1").rating(5).comment("A").build();
        ReviewRequestDTO c = ReviewRequestDTO.builder().productId("prod-2").rating(5).comment("A").build();

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not a dto");
    }

    @Test
    void toString_containsFields() {
        ReviewRequestDTO dto = ReviewRequestDTO.builder().productId("prod-1").rating(5).comment("A").userName("U").build();

        assertThat(dto.toString())
                .contains("productId=prod-1")
                .contains("rating=5")
                .contains("comment=A")
                .contains("userName=U");
    }
}
