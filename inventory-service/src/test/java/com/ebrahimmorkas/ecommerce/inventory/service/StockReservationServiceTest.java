package com.ebrahimmorkas.ecommerce.inventory.service;

import com.ebrahimmorkas.ecommerce.events.InventoryReservationFailedEvent;
import com.ebrahimmorkas.ecommerce.events.InventoryReservedEvent;
import com.ebrahimmorkas.ecommerce.events.OrderCreatedEvent;
import com.ebrahimmorkas.ecommerce.inventory.domain.Product;
import com.ebrahimmorkas.ecommerce.inventory.domain.ReservationItem;
import com.ebrahimmorkas.ecommerce.inventory.domain.ReservationStatus;
import com.ebrahimmorkas.ecommerce.inventory.domain.StockReservation;
import com.ebrahimmorkas.ecommerce.inventory.repository.ProductRepository;
import com.ebrahimmorkas.ecommerce.inventory.repository.StockReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class StockReservationServiceTest {

    private static final UUID ORDER = UUID.randomUUID();

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockReservationRepository reservationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private StockReservationService service;

    @Test
    void reservesAllItemsAndPublishesReservedEvent() {
        Product phone = product("PHONE", 10);
        Product watch = product("WATCH", 5);
        given(productRepository.findBySkuCodeIn(anyCollection())).willReturn(List.of(phone, watch));

        service.reserve(orderCreated(new OrderCreatedEvent.Item("PHONE", 2), new OrderCreatedEvent.Item("WATCH", 5)));

        assertThat(phone.getQuantityAvailable()).isEqualTo(8);
        assertThat(watch.getQuantityAvailable()).isZero();
        ArgumentCaptor<StockReservation> saved = ArgumentCaptor.forClass(StockReservation.class);
        then(reservationRepository).should().save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(ReservationStatus.RESERVED);
        then(eventPublisher).should().publishEvent(any(InventoryReservedEvent.class));
    }

    @Test
    void reservationIsAllOrNothing() {
        Product phone = product("PHONE", 10);
        Product watch = product("WATCH", 1);
        given(productRepository.findBySkuCodeIn(anyCollection())).willReturn(List.of(phone, watch));

        service.reserve(orderCreated(new OrderCreatedEvent.Item("PHONE", 2), new OrderCreatedEvent.Item("WATCH", 3)));

        assertThat(phone.getQuantityAvailable()).isEqualTo(10);
        assertThat(watch.getQuantityAvailable()).isEqualTo(1);
        then(eventPublisher).should().publishEvent(any(InventoryReservationFailedEvent.class));
    }

    @Test
    void duplicateEventIsIgnored() {
        given(reservationRepository.existsByOrderNumber(ORDER)).willReturn(true);

        service.reserve(orderCreated(new OrderCreatedEvent.Item("PHONE", 1)));

        then(productRepository).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void releaseReturnsStockOnlyOnce() {
        Product phone = product("PHONE", 8);
        StockReservation reservation = StockReservation.reserved(ORDER, List.of(new ReservationItem("PHONE", 2)));
        given(reservationRepository.findByOrderNumber(ORDER)).willReturn(Optional.of(reservation));
        given(productRepository.findBySkuCodeIn(anyCollection())).willReturn(List.of(phone));

        service.release(ORDER);
        service.release(ORDER);

        assertThat(phone.getQuantityAvailable()).isEqualTo(10);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void releaseWithoutReservationDoesNothing() {
        given(reservationRepository.findByOrderNumber(ORDER)).willReturn(Optional.empty());

        service.release(ORDER);

        then(productRepository).should(never()).findBySkuCodeIn(anyCollection());
    }

    private static OrderCreatedEvent orderCreated(OrderCreatedEvent.Item... items) {
        return new OrderCreatedEvent(ORDER, "jane@example.com", List.of(items), new BigDecimal("100.00"), Instant.now());
    }

    private static Product product(String sku, int stock) {
        return new Product(sku, sku, null, new BigDecimal("10.00"), stock);
    }
}
