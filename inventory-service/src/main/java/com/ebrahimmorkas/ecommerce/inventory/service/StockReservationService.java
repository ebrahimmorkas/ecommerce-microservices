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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Inventory's part of the order saga. Reservation is all-or-nothing: either every line item is
 * deducted or none is. Outcome events are published only after the transaction commits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockReservationService {

    private final ProductRepository productRepository;
    private final StockReservationRepository reservationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void reserve(OrderCreatedEvent event) {
        UUID orderNumber = event.orderNumber();
        if (reservationRepository.existsByOrderNumber(orderNumber)) {
            log.info("Ignoring duplicate OrderCreatedEvent for order {}", orderNumber);
            return;
        }

        Map<String, Integer> requested = event.items().stream()
                .collect(Collectors.toMap(OrderCreatedEvent.Item::skuCode, OrderCreatedEvent.Item::quantity,
                        Integer::sum, LinkedHashMap::new));
        Map<String, Product> products = productRepository.findBySkuCodeIn(requested.keySet()).stream()
                .collect(Collectors.toMap(Product::getSkuCode, Function.identity()));

        Optional<String> shortage = findShortage(requested, products);
        if (shortage.isPresent()) {
            log.info("Rejecting reservation for order {}: {}", orderNumber, shortage.get());
            reservationRepository.save(StockReservation.rejected(orderNumber));
            eventPublisher.publishEvent(new InventoryReservationFailedEvent(orderNumber, shortage.get(), Instant.now()));
            return;
        }

        requested.forEach((sku, quantity) -> products.get(sku).decreaseStock(quantity));
        List<ReservationItem> items = requested.entrySet().stream()
                .map(e -> new ReservationItem(e.getKey(), e.getValue()))
                .toList();
        reservationRepository.save(StockReservation.reserved(orderNumber, items));
        log.info("Reserved stock for order {}", orderNumber);
        eventPublisher.publishEvent(new InventoryReservedEvent(orderNumber, event.customerEmail(),
                event.totalAmount(), Instant.now()));
    }

    /** Compensating action: returns reserved stock when the order fails later in the saga. */
    @Transactional
    public void release(UUID orderNumber) {
        reservationRepository.findByOrderNumber(orderNumber)
                .filter(reservation -> reservation.getStatus() == ReservationStatus.RESERVED)
                .ifPresent(reservation -> {
                    List<String> skus = reservation.getItems().stream().map(ReservationItem::skuCode).toList();
                    Map<String, Product> products = productRepository.findBySkuCodeIn(skus).stream()
                            .collect(Collectors.toMap(Product::getSkuCode, Function.identity()));
                    reservation.getItems().forEach(item -> products.get(item.skuCode()).increaseStock(item.quantity()));
                    reservation.release();
                    log.info("Released stock for order {}", orderNumber);
                });
    }

    private static Optional<String> findShortage(Map<String, Integer> requested, Map<String, Product> products) {
        return requested.entrySet().stream()
                .map(entry -> {
                    Product product = products.get(entry.getKey());
                    if (product == null) {
                        return "Unknown SKU " + entry.getKey();
                    }
                    if (product.getQuantityAvailable() < entry.getValue()) {
                        return "Insufficient stock for %s (requested %d, available %d)"
                                .formatted(entry.getKey(), entry.getValue(), product.getQuantityAvailable());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst();
    }
}
