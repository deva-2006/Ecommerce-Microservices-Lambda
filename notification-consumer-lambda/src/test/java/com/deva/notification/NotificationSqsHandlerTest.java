package com.deva.notification;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.SesClientBuilder;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationSqsHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final SesClient SES_CLIENT = mock(SesClient.class);
    private static final DynamoDbClient DYNAMO_DB = mock(DynamoDbClient.class);

    private static NotificationSqsHandler handler;

    private Context context;

    @BeforeAll
    static void injectMockClients() throws Exception {
        // The handler builds its clients eagerly in a static initializer, so the
        // builders must be intercepted while that initializer runs.
        try (MockedStatic<SesClient> sesMock = mockStatic(SesClient.class);
             MockedStatic<DynamoDbClient> dynamoMock = mockStatic(DynamoDbClient.class)) {

            SesClientBuilder sesBuilder = mock(SesClientBuilder.class);
            when(sesBuilder.region(any(Region.class))).thenReturn(sesBuilder);
            when(sesBuilder.overrideConfiguration(any(ClientOverrideConfiguration.class))).thenReturn(sesBuilder);
            when(sesBuilder.build()).thenReturn(SES_CLIENT);
            sesMock.when(SesClient::builder).thenReturn(sesBuilder);

            DynamoDbClientBuilder dynamoBuilder = mock(DynamoDbClientBuilder.class);
            when(dynamoBuilder.region(any(Region.class))).thenReturn(dynamoBuilder);
            when(dynamoBuilder.overrideConfiguration(any(ClientOverrideConfiguration.class))).thenReturn(dynamoBuilder);
            when(dynamoBuilder.build()).thenReturn(DYNAMO_DB);
            dynamoMock.when(DynamoDbClient::builder).thenReturn(dynamoBuilder);

            handler = new NotificationSqsHandler();

            assertThat(readStaticField("sesClient")).isSameAs(SES_CLIENT);
            assertThat(readStaticField("dynamoDb")).isSameAs(DYNAMO_DB);
        }
    }

    @BeforeEach
    void setUp() {
        reset(SES_CLIENT, DYNAMO_DB);
        context = mock(Context.class);
    }

    // ------------------------------------------------------------------
    // handleRequest
    // ------------------------------------------------------------------

    @Test
    void handleRequest_validMessageWithFullItem_processesAndSendsEmail() throws Exception {
        when(DYNAMO_DB.getItem(any(GetItemRequest.class))).thenReturn(fullItemResponse());

        String body = snsBody(payload(Map.of(
                "orderId", "order-1234567890",
                "email", "buyer@example.com",
                "amount", 199.99,
                "paymentMethod", "UPI",
                "paymentId", "pay_123",
                "timestamp", "2026-08-04T10:30:00Z")));

        Void result = handler.handleRequest(eventWithBodies(body), context);

        assertThat(result).isNull();
        verify(DYNAMO_DB, times(1)).getItem(any(GetItemRequest.class));
        verify(SES_CLIENT, times(1)).sendEmail(any(SendEmailRequest.class));

        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(SES_CLIENT).sendEmail(captor.capture());
        SendEmailRequest request = captor.getValue();
        assertThat(request.source()).contains("ShopVibe");
        assertThat(request.destination().toAddresses()).containsExactly("buyer@example.com");
        assertThat(request.message().subject().data()).contains("order-12");
        assertThat(request.message().body().html().data())
                .contains("123 Main St")
                .contains("Laptop")
                .contains("199.99");
        assertThat(request.message().body().text().data())
                .contains("order-1234567890")
                .contains("199.99");
    }

    @Test
    void handleRequest_validMessage_usesPayloadAndFallbacks() throws Exception {
        when(DYNAMO_DB.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());

        String body = snsBody(payload(Map.of(
                "orderId", "ORD1",
                "email", "buyer@example.com")));

        handler.handleRequest(eventWithBodies(body), context);

        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(SES_CLIENT, times(1)).sendEmail(captor.capture());
        SendEmailRequest request = captor.getValue();
        assertThat(request.source()).contains("ShopVibe");
        assertThat(request.destination().toAddresses()).containsExactly("buyer@example.com");
        assertThat(request.message().subject().data()).contains("ORD1");
        assertThat(request.message().body().html().data()).contains("Address not available");
        assertThat(request.message().body().text().data())
                .contains("ORD1")
                .contains("0.00");
    }

    @Test
    void handleRequest_payloadMissingOrderId_throwsRuntimeException() throws Exception {
        String body = snsBody(payload(Map.of("email", "buyer@example.com")));

        assertThatThrownBy(() -> handler.handleRequest(eventWithBodies(body), context))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed processing SQS message");
    }

    @Test
    void handleRequest_invalidTimestamp_usesRawTimestampInEmail() throws Exception {
        when(DYNAMO_DB.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());

        String body = snsBody(payload(Map.of(
                "orderId", "ORD-T",
                "email", "buyer@example.com",
                "timestamp", "not-a-date")));

        handler.handleRequest(eventWithBodies(body), context);

        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(SES_CLIENT, times(1)).sendEmail(captor.capture());
        assertThat(captor.getValue().message().body().text().data()).contains("not-a-date");
    }

    @Test
    void handleRequest_dynamoThrows_stillSendsEmailWithFallbacks() throws Exception {
        when(DYNAMO_DB.getItem(any(GetItemRequest.class))).thenThrow(new RuntimeException("dynamo down"));

        String body = snsBody(payload(Map.of(
                "orderId", "ORD-9",
                "email", "buyer@example.com")));

        Void result = handler.handleRequest(eventWithBodies(body), context);

        assertThat(result).isNull();
        verify(SES_CLIENT, times(1)).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void handleRequest_emptyRecords_returnsNullWithoutCallingClients() {
        Void result = handler.handleRequest(eventWithBodies(), context);

        assertThat(result).isNull();
        verifyNoInteractions(DYNAMO_DB, SES_CLIENT);
    }

    @Test
    void handleRequest_nullEvent_throwsNullPointerException() {
        assertThatThrownBy(() -> handler.handleRequest(null, context))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void handleRequest_malformedBody_throwsRuntimeException() {
        assertThatThrownBy(() -> handler.handleRequest(eventWithBodies("this is not json"), context))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed processing SQS message");
    }

    @Test
    void handleRequest_envelopeWithoutMessageField_throwsRuntimeException() throws Exception {
        String body = MAPPER.writeValueAsString(Map.of("Type", "Notification"));

        assertThatThrownBy(() -> handler.handleRequest(eventWithBodies(body), context))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed processing SQS message");
    }

    @Test
    void handleRequest_innerMessageNotJson_throwsRuntimeException() throws Exception {
        String body = snsBody("plain text not json");

        assertThatThrownBy(() -> handler.handleRequest(eventWithBodies(body), context))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed processing SQS message");
    }

    @Test
    void handleRequest_sesClientFails_throwsRuntimeException() throws Exception {
        when(DYNAMO_DB.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());
        doThrow(new RuntimeException("SES down")).when(SES_CLIENT).sendEmail(any(SendEmailRequest.class));

        String body = snsBody(payload(Map.of("orderId", "ORD-1", "email", "buyer@example.com")));

        assertThatThrownBy(() -> handler.handleRequest(eventWithBodies(body), context))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed processing SQS message")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void handleRequest_multipleMessages_sendsEmailPerMessage() throws Exception {
        when(DYNAMO_DB.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());

        String b1 = snsBody(payload(Map.of("orderId", "A-1", "email", "a@b.c")));
        String b2 = snsBody(payload(Map.of("orderId", "B-2", "email", "c@d.e")));

        handler.handleRequest(eventWithBodies(b1, b2), context);

        verify(SES_CLIENT, times(2)).sendEmail(any(SendEmailRequest.class));
    }

    // ------------------------------------------------------------------
    // fetchOrderDetails
    // ------------------------------------------------------------------

    @Test
    void fetchOrderDetails_itemPresent_returnsMappedFields() throws Exception {
        when(DYNAMO_DB.getItem(any(GetItemRequest.class))).thenReturn(fullItemResponse());

        @SuppressWarnings("unchecked")
        Map<String, Object> result = invoke("fetchOrderDetails", new Class[]{String.class}, "order-1");

        assertThat(result)
                .containsEntry("shippingAddress", "123 Main St")
                .containsEntry("totalAmount", 199.99);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0))
                .containsEntry("productName", "Laptop")
                .containsEntry("quantity", 2)
                .containsEntry("price", 99.99)
                .containsEntry("subtotal", 199.98);
    }

    @Test
    void fetchOrderDetails_itemMissingOptionalFields_partialMap() throws Exception {
        Map<String, AttributeValue> item = Map.of(
                "shippingAddress", AttributeValue.builder().s("Only Street").build());
        when(DYNAMO_DB.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().item(item).build());

        @SuppressWarnings("unchecked")
        Map<String, Object> result = invoke("fetchOrderDetails", new Class[]{String.class}, "order-2");

        assertThat(result).containsOnlyKeys("shippingAddress");
    }

    @Test
    void fetchOrderDetails_itemWithSparseItemMaps_keepsOnlyPresentKeys() throws Exception {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("items", AttributeValue.builder().l(
                AttributeValue.builder().m(Map.of("other", AttributeValue.builder().s("x").build())).build()).build());
        when(DYNAMO_DB.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().item(item).build());

        @SuppressWarnings("unchecked")
        Map<String, Object> result = invoke("fetchOrderDetails", new Class[]{String.class}, "order-5");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0)).isEmpty();
    }

    @Test
    void fetchOrderDetails_noItem_returnsEmptyMap() throws Exception {
        when(DYNAMO_DB.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());

        @SuppressWarnings("unchecked")
        Map<String, Object> result = invoke("fetchOrderDetails", new Class[]{String.class}, "missing-order");

        assertThat(result).isEmpty();
    }

    @Test
    void fetchOrderDetails_dynamoThrows_returnsEmptyMap() throws Exception {
        when(DYNAMO_DB.getItem(any(GetItemRequest.class))).thenThrow(new RuntimeException("dynamo down"));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = invoke("fetchOrderDetails", new Class[]{String.class}, "order-3");

        assertThat(result).isEmpty();
    }

    @Test
    void fetchOrderDetails_itemsElementWithoutMapKeys_keepsEmptyItemMap() throws Exception {
        Map<String, AttributeValue> item = Map.of(
                "items", AttributeValue.builder().l(AttributeValue.builder().n("5").build()).build());
        when(DYNAMO_DB.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().item(item).build());

        @SuppressWarnings("unchecked")
        Map<String, Object> result = invoke("fetchOrderDetails", new Class[]{String.class}, "order-4");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0)).isEmpty();
    }

    // ------------------------------------------------------------------
    // parseItems
    // ------------------------------------------------------------------

    @Test
    void parseItems_withValidMaps_returnsRows() {
        @SuppressWarnings("unchecked")
        List<String[]> rows = invoke("parseItems", new Class[]{Map.class},
                Map.of("items", List.of(Map.of("productName", "Pen", "quantity", 3, "price", 1.5))));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsExactly("Pen", "3", "1.50");
    }

    @Test
    void parseItems_mapMissingFields_usesDefaults() {
        @SuppressWarnings("unchecked")
        List<String[]> rows = invoke("parseItems", new Class[]{Map.class},
                Map.of("items", List.of(Map.of("subtotal", "9.0"))));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsExactly("Item", "1", "0.00");
    }

    @Test
    void parseItems_nonMapElement_skipped() {
        @SuppressWarnings("unchecked")
        List<String[]> rows = invoke("parseItems", new Class[]{Map.class},
                Map.of("items", List.of("not-a-map")));

        assertThat(rows).isEmpty();
    }

    @Test
    void parseItems_noItemsKey_returnsEmpty() {
        @SuppressWarnings("unchecked")
        List<String[]> rows = invoke("parseItems", new Class[]{Map.class}, Map.of());

        assertThat(rows).isEmpty();
    }

    @Test
    void parseItems_itemsNotAList_returnsEmpty() {
        @SuppressWarnings("unchecked")
        List<String[]> rows = invoke("parseItems", new Class[]{Map.class}, Map.of("items", "nope"));

        assertThat(rows).isEmpty();
    }

    // ------------------------------------------------------------------
    // formatTimestamp
    // ------------------------------------------------------------------

    @Test
    void formatTimestamp_validIso_formatsInKolkataZone() {
        String result = invoke("formatTimestamp", new Class[]{String.class}, "2026-08-04T10:30:00Z");

        assertThat(result).contains("Aug 2026").isNotEqualTo("2026-08-04T10:30:00Z");
    }

    @Test
    void formatTimestamp_invalid_returnsInputUnchanged() {
        String result = invoke("formatTimestamp", new Class[]{String.class}, "not-a-date");

        assertThat(result).isEqualTo("not-a-date");
    }

    // ------------------------------------------------------------------
    // buildItemRowsHtml / buildItemRowsPlain
    // ------------------------------------------------------------------

    @Test
    void buildItemRowsHtml_formatsRows() {
        String html = invoke("buildItemRowsHtml", new Class[]{List.class},
                Collections.singletonList(new String[]{"Pen", "2", "5.00"}));

        assertThat(html).contains("Pen").contains("5.00");
    }

    @Test
    void buildItemRowsHtml_empty_returnsEmpty() {
        String html = invoke("buildItemRowsHtml", new Class[]{List.class}, List.of());

        assertThat(html).isEmpty();
    }

    @Test
    void buildItemRowsPlain_formatsRows() {
        String plain = invoke("buildItemRowsPlain", new Class[]{List.class},
                Collections.singletonList(new String[]{"Pen", "2", "5.00"}));

        assertThat(plain).contains("Pen").contains("5.00");
    }

    @Test
    void buildItemRowsPlain_empty_returnsEmpty() {
        String plain = invoke("buildItemRowsPlain", new Class[]{List.class}, List.of());

        assertThat(plain).isEmpty();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private SQSEvent eventWithBodies(String... bodies) {
        SQSEvent event = new SQSEvent();
        List<SQSEvent.SQSMessage> records = new ArrayList<>();
        for (String body : bodies) {
            SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
            message.setBody(body);
            records.add(message);
        }
        event.setRecords(records);
        return event;
    }

    private static String snsBody(String innerJson) throws Exception {
        return MAPPER.writeValueAsString(Map.of("Message", innerJson));
    }

    private static String payload(Map<String, Object> data) throws Exception {
        return MAPPER.writeValueAsString(data);
    }

    private static GetItemResponse fullItemResponse() {
        Map<String, AttributeValue> item = Map.of(
                "shippingAddress", AttributeValue.builder().s("123 Main St").build(),
                "totalAmount", AttributeValue.builder().n("199.99").build(),
                "items", AttributeValue.builder().l(
                        AttributeValue.builder().m(Map.of(
                                "productName", AttributeValue.builder().s("Laptop").build(),
                                "quantity", AttributeValue.builder().n("2").build(),
                                "price", AttributeValue.builder().n("99.99").build(),
                                "subtotal", AttributeValue.builder().n("199.98").build()
                        )).build()
                ).build());
        return GetItemResponse.builder().item(item).build();
    }

    @SuppressWarnings("unchecked")
    private <T> T invoke(String name, Class<?>[] paramTypes, Object... args) {
        try {
            Method method = NotificationSqsHandler.class.getDeclaredMethod(name, paramTypes);
            method.setAccessible(true);
            return (T) method.invoke(handler, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object readStaticField(String name) throws Exception {
        Field field = NotificationSqsHandler.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }
}
