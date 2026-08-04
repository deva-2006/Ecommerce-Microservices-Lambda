package com.deva.reviewservice.controller;

import com.deva.reviewservice.dto.ReviewRequestDTO;
import com.deva.reviewservice.dto.ReviewResponseDTO;
import com.deva.reviewservice.dto.ReviewSummaryDTO;
import com.deva.reviewservice.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    private ReviewRequestDTO requestDTO;
    private ReviewResponseDTO responseDTO;
    private ReviewSummaryDTO summaryDTO;

    @BeforeEach
    void setUp() {
        requestDTO = new ReviewRequestDTO();
        requestDTO.setProductId("prod-1");
        requestDTO.setRating(5);
        requestDTO.setComment("Great product");

        responseDTO = ReviewResponseDTO.builder()
                .reviewId("rev-1")
                .productId("prod-1")
                .userId("user-1")
                .userName("Alice")
                .rating(5)
                .comment("Great product")
                .verifiedBuyer(true)
                .createdAt("2026-08-01T10:00:00Z")
                .build();

        summaryDTO = ReviewSummaryDTO.builder()
                .productId("prod-1")
                .averageRating(4.5)
                .totalReviews(2)
                .ratingBreakdown(Map.of(5, 1, 4, 1, 3, 0, 2, 0, 1, 0))
                .build();
    }

    @Test
    void createReview_returnsCreatedWithServiceResult() {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer token");
        when(reviewService.createReview("user-1", "Bearer token", requestDTO)).thenReturn(responseDTO);

        ResponseEntity<ReviewResponseDTO> response = reviewController.createReview("user-1", servletRequest, requestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getReviewId()).isEqualTo("rev-1");
        assertThat(response.getBody().getRating()).isEqualTo(5);
        verify(reviewService, times(1)).createReview("user-1", "Bearer token", requestDTO);
    }

    @Test
    void createReview_propagatesServiceException() {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer token");
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only verified buyers can leave a review"))
                .when(reviewService).createReview(eq("user-1"), eq("Bearer token"), any(ReviewRequestDTO.class));

        assertThatThrownBy(() -> reviewController.createReview("user-1", servletRequest, requestDTO))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only verified buyers");
    }

    @Test
    void getReviewsByProductId_returnsOkWithList() {
        when(reviewService.getReviewsByProductId("prod-1")).thenReturn(List.of(responseDTO));

        ResponseEntity<List<ReviewResponseDTO>> response = reviewController.getReviewsByProductId("prod-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getProductId()).isEqualTo("prod-1");
    }

    @Test
    void getReviewsByProductId_emptyListReturnsOk() {
        when(reviewService.getReviewsByProductId("prod-1")).thenReturn(List.of());

        ResponseEntity<List<ReviewResponseDTO>> response = reviewController.getReviewsByProductId("prod-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getReviewSummaryByProductId_returnsOkWithSummary() {
        when(reviewService.getReviewSummaryByProductId("prod-1")).thenReturn(summaryDTO);

        ResponseEntity<ReviewSummaryDTO> response = reviewController.getReviewSummaryByProductId("prod-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAverageRating()).isEqualTo(4.5);
        assertThat(response.getBody().getTotalReviews()).isEqualTo(2);
    }

    @Test
    void deleteReview_returnsNoContentAndDelegates() {
        reviewController.deleteReview("user-1", "rev-1");

        verify(reviewService, times(1)).deleteReview("user-1", "rev-1");
    }

    @Test
    void deleteReview_propagatesServiceException() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"))
                .when(reviewService).deleteReview("user-1", "rev-1");

        assertThatThrownBy(() -> reviewController.deleteReview("user-1", "rev-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Review not found");

        verify(reviewService, never()).deleteReview("user-1", "unknown");
    }
}
