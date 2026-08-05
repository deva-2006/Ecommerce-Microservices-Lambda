package com.deva.reviewservice.service;

import com.deva.reviewservice.dto.ReviewRequestDTO;
import com.deva.reviewservice.dto.ReviewResponseDTO;
import com.deva.reviewservice.dto.ReviewSummaryDTO;

import java.util.List;
import java.util.Map;

public interface ReviewService {
    ReviewResponseDTO createReview(String userId, String authHeader, ReviewRequestDTO request);
    List<ReviewResponseDTO> getReviewsByProductId(String productId);
    ReviewSummaryDTO getReviewSummaryByProductId(String productId);
    Map<String, ReviewSummaryDTO> getBatchReviewSummaries(List<String> productIds);
    void deleteReview(String userId, String reviewId);
}
