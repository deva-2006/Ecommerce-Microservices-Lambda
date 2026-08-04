package com.deva.reviewservice.service;

import com.deva.reviewservice.client.OrderClient;
import com.deva.reviewservice.dto.ReviewRequestDTO;
import com.deva.reviewservice.dto.ReviewResponseDTO;
import com.deva.reviewservice.dto.ReviewSummaryDTO;
import com.deva.reviewservice.entity.Review;
import com.deva.reviewservice.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderClient orderClient;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private ReviewRequestDTO requestDTO;
    private Review review;

    @BeforeEach
    void setUp() {
        requestDTO = new ReviewRequestDTO();
        requestDTO.setProductId("prod-1");
        requestDTO.setRating(4);
        requestDTO.setComment("Solid");
        requestDTO.setUserName("Alice");

        review = Review.builder()
                .reviewId("rev-1")
                .productId("prod-1")
                .userId("user-1")
                .userName("Alice")
                .rating(4)
                .comment("Solid")
                .verifiedBuyer(true)
                .createdAt("2026-08-01T10:00:00Z")
                .build();
    }

    // ---------------- createReview ----------------

    @Test
    void createReview_verifiedPurchase_savesAndReturnsDto() {
        when(orderClient.verifyPurchase("Bearer token", "prod-1")).thenReturn(Map.of("purchased", true));
        doNothing().when(reviewRepository).save(any(Review.class));

        ReviewResponseDTO result = reviewService.createReview("user-1", "Bearer token", requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo("prod-1");
        assertThat(result.getUserId()).isEqualTo("user-1");
        assertThat(result.getUserName()).isEqualTo("Alice");
        assertThat(result.getRating()).isEqualTo(4);
        assertThat(result.getComment()).isEqualTo("Solid");
        assertThat(result.getVerifiedBuyer()).isTrue();
        assertThat(result.getCreatedAt()).isNotBlank();
        assertThat(result.getReviewId()).isNotBlank();
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    void createReview_blankUserName_defaultsToCustomer() {
        requestDTO.setUserName("  ");
        when(orderClient.verifyPurchase("Bearer token", "prod-1")).thenReturn(Map.of("purchased", true));

        ReviewResponseDTO result = reviewService.createReview("user-1", "Bearer token", requestDTO);

        assertThat(result.getUserName()).isEqualTo("Customer");
    }

    @Test
    void createReview_noAuthHeader_throwsForbidden() {
        assertThatThrownBy(() -> reviewService.createReview("user-1", null, requestDTO))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);

        verify(reviewRepository, times(0)).save(any(Review.class));
    }

    @Test
    void createReview_blankAuthHeader_throwsForbidden() {
        assertThatThrownBy(() -> reviewService.createReview("user-1", "   ", requestDTO))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }

    @Test
    void createReview_verifyReturnsNull_throwsForbidden() {
        when(orderClient.verifyPurchase("Bearer token", "prod-1")).thenReturn(null);

        assertThatThrownBy(() -> reviewService.createReview("user-1", "Bearer token", requestDTO))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }

    @Test
    void createReview_verifyReturnsNotPurchased_throwsForbidden() {
        when(orderClient.verifyPurchase("Bearer token", "prod-1")).thenReturn(Map.of("purchased", false));

        assertThatThrownBy(() -> reviewService.createReview("user-1", "Bearer token", requestDTO))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }

    @Test
    void createReview_orderClientThrows_fallsBackToForbidden() {
        when(orderClient.verifyPurchase("Bearer token", "prod-1")).thenThrow(new RuntimeException("order service down"));

        assertThatThrownBy(() -> reviewService.createReview("user-1", "Bearer token", requestDTO))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }

    // ---------------- getReviewsByProductId ----------------

    @Test
    void getReviewsByProductId_returnsMappedReviews() {
        Review nullVerified = Review.builder()
                .reviewId("rev-2")
                .productId("prod-1")
                .userId("user-2")
                .userName("Bob")
                .rating(3)
                .comment("Okay")
                .verifiedBuyer(null)
                .createdAt("2026-08-02T10:00:00Z")
                .build();
        when(reviewRepository.findByProductId("prod-1")).thenReturn(List.of(review, nullVerified));

        List<ReviewResponseDTO> result = reviewService.getReviewsByProductId("prod-1");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getReviewId()).isEqualTo("rev-1");
        assertThat(result.get(0).getVerifiedBuyer()).isTrue();
        assertThat(result.get(1).getVerifiedBuyer()).isTrue();
    }

    @Test
    void getReviewsByProductId_empty_returnsEmptyList() {
        when(reviewRepository.findByProductId("prod-1")).thenReturn(List.of());

        List<ReviewResponseDTO> result = reviewService.getReviewsByProductId("prod-1");

        assertThat(result).isEmpty();
    }

    // ---------------- getReviewSummaryByProductId ----------------

    @Test
    void getReviewSummaryByProductId_noReviews_returnsZeroSummary() {
        when(reviewRepository.findByProductId("prod-1")).thenReturn(List.of());

        ReviewSummaryDTO summary = reviewService.getReviewSummaryByProductId("prod-1");

        assertThat(summary.getProductId()).isEqualTo("prod-1");
        assertThat(summary.getAverageRating()).isEqualTo(0.0);
        assertThat(summary.getTotalReviews()).isZero();
        assertThat(summary.getRatingBreakdown()).containsEntry(1, 0)
                .containsEntry(2, 0)
                .containsEntry(3, 0)
                .containsEntry(4, 0)
                .containsEntry(5, 0);
    }

    @Test
    void getReviewSummaryByProductId_withReviews_computesAverageAndBreakdown() {
        Review r1 = Review.builder().productId("prod-1").rating(5).build();
        Review r2 = Review.builder().productId("prod-1").rating(4).build();
        when(reviewRepository.findByProductId("prod-1")).thenReturn(List.of(r1, r2));

        ReviewSummaryDTO summary = reviewService.getReviewSummaryByProductId("prod-1");

        assertThat(summary.getAverageRating()).isEqualTo(4.5);
        assertThat(summary.getTotalReviews()).isEqualTo(2);
        assertThat(summary.getRatingBreakdown()).containsEntry(5, 1)
                .containsEntry(4, 1)
                .containsEntry(3, 0)
                .containsEntry(2, 0)
                .containsEntry(1, 0);
    }

    @Test
    void getReviewSummaryByProductId_roundsAverageToTenths() {
        Review r1 = Review.builder().productId("prod-1").rating(5).build();
        Review r2 = Review.builder().productId("prod-1").rating(5).build();
        Review r3 = Review.builder().productId("prod-1").rating(4).build();
        when(reviewRepository.findByProductId("prod-1")).thenReturn(List.of(r1, r2, r3));

        ReviewSummaryDTO summary = reviewService.getReviewSummaryByProductId("prod-1");

        assertThat(summary.getTotalReviews()).isEqualTo(3);
        assertThat(summary.getAverageRating()).isEqualTo(4.7);
        assertThat(summary.getRatingBreakdown()).containsEntry(5, 2)
                .containsEntry(4, 1)
                .containsEntry(3, 0)
                .containsEntry(2, 0)
                .containsEntry(1, 0);
    }

    // ---------------- deleteReview ----------------

    @Test
    void deleteReview_owner_deletesReview() {
        when(reviewRepository.findById("rev-1")).thenReturn(Optional.of(review));
        doNothing().when(reviewRepository).deleteById("rev-1");

        reviewService.deleteReview("user-1", "rev-1");

        verify(reviewRepository, times(1)).deleteById("rev-1");
    }

    @Test
    void deleteReview_notFound_throwsNotFound() {
        when(reviewRepository.findById("rev-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview("user-1", "rev-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND)
                .hasMessageContaining("Review not found");

        verify(reviewRepository, times(0)).deleteById(any());
    }

    @Test
    void deleteReview_notOwner_throwsForbidden() {
        when(reviewRepository.findById("rev-1")).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview("other-user", "rev-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN)
                .hasMessageContaining("Not authorized to delete this review");

        verify(reviewRepository, times(0)).deleteById(any());
    }
}
