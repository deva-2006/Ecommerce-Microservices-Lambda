package com.deva.reviewservice.controller;

import com.deva.reviewservice.dto.ReviewRequestDTO;
import com.deva.reviewservice.dto.ReviewResponseDTO;
import com.deva.reviewservice.dto.ReviewSummaryDTO;
import com.deva.reviewservice.security.AuthUserId;
import com.deva.reviewservice.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponseDTO> createReview(
            @AuthUserId String userId,
            HttpServletRequest httpServletRequest,
            @Valid @RequestBody ReviewRequestDTO request) {
        String authHeader = httpServletRequest.getHeader("Authorization");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(userId, authHeader, request));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewResponseDTO>> getReviewsByProductId(@PathVariable String productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProductId(productId));
    }

    @GetMapping("/product/{productId}/summary")
    public ResponseEntity<ReviewSummaryDTO> getReviewSummaryByProductId(@PathVariable String productId) {
        return ResponseEntity.ok(reviewService.getReviewSummaryByProductId(productId));
    }

    @PostMapping("/summaries")
    public ResponseEntity<java.util.Map<String, ReviewSummaryDTO>> getBatchReviewSummaries(
            @RequestBody com.deva.reviewservice.dto.BatchReviewSummaryRequestDTO request) {
        List<String> pids = request != null ? request.getProductIds() : java.util.Collections.emptyList();
        return ResponseEntity.ok(reviewService.getBatchReviewSummaries(pids));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @AuthUserId String userId,
            @PathVariable String reviewId) {
        reviewService.deleteReview(userId, reviewId);
        return ResponseEntity.noContent().build();
    }
}
