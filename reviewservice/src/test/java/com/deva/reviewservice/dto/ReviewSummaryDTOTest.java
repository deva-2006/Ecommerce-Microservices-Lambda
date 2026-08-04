package com.deva.reviewservice.dto;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewSummaryDTOTest {

    @Test
    void noArgsConstructor_andSetters() {
        ReviewSummaryDTO dto = new ReviewSummaryDTO();
        dto.setProductId("prod-1");
        dto.setAverageRating(4.5);
        dto.setTotalReviews(2);
        dto.setRatingBreakdown(Map.of(5, 1, 4, 1));

        assertThat(dto.getProductId()).isEqualTo("prod-1");
        assertThat(dto.getAverageRating()).isEqualTo(4.5);
        assertThat(dto.getTotalReviews()).isEqualTo(2);
        assertThat(dto.getRatingBreakdown()).containsEntry(5, 1).containsEntry(4, 1);
    }

    @Test
    void allArgsConstructor_setsFields() {
        Map<Integer, Integer> breakdown = Map.of(5, 2, 4, 0, 3, 0, 2, 0, 1, 0);
        ReviewSummaryDTO dto = new ReviewSummaryDTO("prod-1", 5.0, 2, breakdown);

        assertThat(dto.getProductId()).isEqualTo("prod-1");
        assertThat(dto.getAverageRating()).isEqualTo(5.0);
        assertThat(dto.getTotalReviews()).isEqualTo(2);
        assertThat(dto.getRatingBreakdown()).isSameAs(breakdown);
    }

    @Test
    void builder_buildsDto() {
        ReviewSummaryDTO dto = ReviewSummaryDTO.builder()
                .productId("prod-1")
                .averageRating(3.5)
                .totalReviews(4)
                .ratingBreakdown(Map.of(5, 1, 4, 1, 3, 1, 2, 1, 1, 0))
                .build();

        assertThat(dto.getProductId()).isEqualTo("prod-1");
        assertThat(dto.getAverageRating()).isEqualTo(3.5);
        assertThat(dto.getTotalReviews()).isEqualTo(4);
        assertThat(dto.getRatingBreakdown()).hasSize(5);
    }

    @Test
    void equalsAndHashCode_consistent() {
        ReviewSummaryDTO a = ReviewSummaryDTO.builder().productId("prod-1").averageRating(4.5).totalReviews(2).build();
        ReviewSummaryDTO b = ReviewSummaryDTO.builder().productId("prod-1").averageRating(4.5).totalReviews(2).build();
        ReviewSummaryDTO c = ReviewSummaryDTO.builder().productId("prod-2").averageRating(4.5).totalReviews(2).build();

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    void toString_containsFields() {
        ReviewSummaryDTO dto = ReviewSummaryDTO.builder().productId("prod-1").averageRating(4.5).totalReviews(2).build();

        assertThat(dto.toString())
                .contains("productId=prod-1")
                .contains("averageRating=4.5")
                .contains("totalReviews=2");
    }
}
