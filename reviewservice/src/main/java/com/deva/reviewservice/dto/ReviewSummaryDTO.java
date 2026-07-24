package com.deva.reviewservice.dto;

import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummaryDTO {

    private String productId;
    private Double averageRating;
    private Integer totalReviews;
    private Map<Integer, Integer> ratingBreakdown; // 5 -> count, 4 -> count, etc.
}
