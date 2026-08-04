package com.deva.reviewservice.repository;

import com.deva.reviewservice.entity.Review;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewRepositoryTest {

    @Mock
    private DynamoDbEnhancedClient enhancedClient;

    @Mock
    private DynamoDbTable<Review> table;

    private ReviewRepository repository;

    private Review review;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = new ReviewRepository(enhancedClient);
        ReflectionTestUtils.setField(repository, "tableName", "reviews");
        when(enhancedClient.table(anyString(), any(TableSchema.class))).thenReturn(table);

        review = Review.builder()
                .reviewId("rev-1")
                .productId("prod-1")
                .userId("user-1")
                .rating(5)
                .build();
    }

    @Test
    void save_putsItemOnTable() {
        repository.save(review);

        verify(table, times(1)).putItem(review);
    }

    @Test
    void findById_found_returnsReview() {
        when(table.getItem(any(Consumer.class))).thenReturn(review);

        Optional<Review> result = repository.findById("rev-1");

        assertThat(result).isPresent();
        assertThat(result.get().getReviewId()).isEqualTo("rev-1");
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(table.getItem(any(Consumer.class))).thenReturn(null);

        Optional<Review> result = repository.findById("rev-1");

        assertThat(result).isEmpty();
    }

    @Test
    void findByProductId_returnsOnlyMatchingReviews() {
        Review other = Review.builder().reviewId("rev-2").productId("prod-2").build();
        PageIterable<Review> pageIterable = mockPageIterable(List.of(review, other));
        when(table.scan()).thenReturn(pageIterable);

        List<Review> result = repository.findByProductId("prod-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReviewId()).isEqualTo("rev-1");
    }

    @Test
    void findByUserId_returnsOnlyMatchingReviews() {
        Review other = Review.builder().reviewId("rev-2").userId("user-2").build();
        PageIterable<Review> pageIterable = mockPageIterable(List.of(review, other));
        when(table.scan()).thenReturn(pageIterable);

        List<Review> result = repository.findByUserId("user-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReviewId()).isEqualTo("rev-1");
    }

    @Test
    void deleteById_deletesOnTable() {
        when(table.deleteItem(any(Consumer.class))).thenReturn(null);

        repository.deleteById("rev-1");

        verify(table, times(1)).deleteItem(any(Consumer.class));
    }

    @SuppressWarnings("unchecked")
    private PageIterable<Review> mockPageIterable(List<Review> items) {
        SdkIterable<Review> sdkIterable = mock(SdkIterable.class);
        when(sdkIterable.stream()).thenReturn(items.stream());
        PageIterable<Review> pageIterable = mock(PageIterable.class);
        when(pageIterable.items()).thenReturn(sdkIterable);
        return pageIterable;
    }
}
