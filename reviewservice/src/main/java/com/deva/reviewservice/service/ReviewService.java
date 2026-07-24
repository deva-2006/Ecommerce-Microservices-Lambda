package com.deva.reviewservice.service;

import com.deva.reviewservice.dto.ReviewRequestDTO;
import com.deva.reviewservice.dto.ReviewResponseDTO;
import com.deva.reviewservice.dto.ReviewSummaryDTO;

import java.util.List;

public interface ReviewService {
    ReviewResponseDTO createReview(String userId, String authHeader, ReviewRequestDTO request);
    List<ReviewResponseDTO> getReviewsByProductId(String productId);
    ReviewSummaryDTO getReviewSummaryByProductId(String productId);
    void deleteReview(String userId, String reviewId);
}
