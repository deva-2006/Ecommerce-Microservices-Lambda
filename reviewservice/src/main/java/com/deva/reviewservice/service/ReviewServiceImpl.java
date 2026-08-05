package com.deva.reviewservice.service;

import com.deva.reviewservice.client.OrderClient;
import com.deva.reviewservice.dto.ReviewRequestDTO;
import com.deva.reviewservice.dto.ReviewResponseDTO;
import com.deva.reviewservice.dto.ReviewSummaryDTO;
import com.deva.reviewservice.entity.Review;
import com.deva.reviewservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderClient orderClient;

    @Override
    public ReviewResponseDTO createReview(String userId, String authHeader, ReviewRequestDTO request) {
        boolean purchased = false;
        try {
            if (authHeader != null && !authHeader.isBlank()) {
                Map<String, Boolean> res = orderClient.verifyPurchase(authHeader, request.getProductId());
                if (res != null && Boolean.TRUE.equals(res.get("purchased"))) {
                    purchased = true;
                }
            }
        } catch (Exception e) {
            // Fallback: If Feign client fails or in local testing, allow if requested or enforce purchase
        }

        if (!purchased) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only verified buyers who purchased this item can leave a review.");
        }

        Review review = Review.builder()
                .reviewId(UUID.randomUUID().toString())
                .productId(request.getProductId())
                .userId(userId)
                .userName(request.getUserName() != null && !request.getUserName().isBlank() ? request.getUserName() : "Customer")
                .rating(request.getRating())
                .comment(request.getComment())
                .verifiedBuyer(true)
                .createdAt(Instant.now().toString())
                .build();

        reviewRepository.save(review);
        return toResponse(review);
    }

    @Override
    public List<ReviewResponseDTO> getReviewsByProductId(String productId) {
        return reviewRepository.findByProductId(productId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ReviewSummaryDTO getReviewSummaryByProductId(String productId) {
        List<Review> reviews = reviewRepository.findByProductId(productId);
        if (reviews.isEmpty()) {
            return ReviewSummaryDTO.builder()
                    .productId(productId)
                    .averageRating(0.0)
                    .totalReviews(0)
                    .ratingBreakdown(Map.of(5, 0, 4, 0, 3, 0, 2, 0, 1, 0))
                    .build();
        }

        double totalScore = reviews.stream().mapToInt(Review::getRating).sum();
        double avg = Math.round((totalScore / reviews.size()) * 10.0) / 10.0;

        Map<Integer, Integer> breakdown = new HashMap<>();
        for (int i = 1; i <= 5; i++) breakdown.put(i, 0);
        for (Review r : reviews) {
            int rating = r.getRating() != null ? r.getRating() : 5;
            breakdown.put(rating, breakdown.getOrDefault(rating, 0) + 1);
        }

        return ReviewSummaryDTO.builder()
                .productId(productId)
                .averageRating(avg)
                .totalReviews(reviews.size())
                .ratingBreakdown(breakdown)
                .build();
    }

    @Override
    public Map<String, ReviewSummaryDTO> getBatchReviewSummaries(List<String> productIds) {
        Map<String, ReviewSummaryDTO> result = new HashMap<>();
        if (productIds == null || productIds.isEmpty()) {
            return result;
        }
        for (String pid : productIds) {
            if (pid != null && !pid.isBlank()) {
                result.put(pid, getReviewSummaryByProductId(pid));
            }
        }
        return result;
    }

    @Override
    public void deleteReview(String userId, String reviewId) {
        Review r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        if (!userId.equals(r.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this review");
        }
        reviewRepository.deleteById(reviewId);
    }

    private ReviewResponseDTO toResponse(Review r) {
        return ReviewResponseDTO.builder()
                .reviewId(r.getReviewId())
                .productId(r.getProductId())
                .userId(r.getUserId())
                .userName(r.getUserName())
                .rating(r.getRating())
                .comment(r.getComment())
                .verifiedBuyer(r.getVerifiedBuyer() != null ? r.getVerifiedBuyer() : true)
                .createdAt(r.getCreatedAt())
                .build();
    }
}
