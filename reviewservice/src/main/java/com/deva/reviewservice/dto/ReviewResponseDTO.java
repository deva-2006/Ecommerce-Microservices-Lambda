package com.deva.reviewservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDTO {

    private String reviewId;
    private String productId;
    private String userId;
    private String userName;
    private Integer rating;
    private String comment;
    private Boolean verifiedBuyer;
    private String createdAt;
}
