package dev.gustavo.nidavellir.application;

import dev.gustavo.nidavellir.application.InventoryTransaction.Stores;
import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.ProductNotFoundException;
import dev.gustavo.nidavellir.domain.Quantity;
import dev.gustavo.nidavellir.domain.Sku;
import dev.gustavo.nidavellir.domain.StockMovement;
import dev.gustavo.nidavellir.domain.StockMovement.MovementType;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

/**
 * Casos de uso de movimentação de estoque. Saldo e histórico sempre mudam na mesma transação:
 * não existe saldo alterado sem registro, nem registro de algo que não aconteceu.
 */
public final class StockService {

    private final InventoryTransaction transaction;
    private final StockMovementFactory movements;
    private final List<StockLevelListener> listeners;

    public StockService(InventoryTransaction transaction, Clock clock) {
        this(transaction, clock, List.of());
    }

    public StockService(InventoryTransaction transaction, Clock clock, List<StockLevelListener> listeners) {
        this.transaction = Objects.requireNonNull(transaction, "transaction");
        this.movements = new StockMovementFactory(clock);
        this.listeners = List.copyOf(listeners);
    }

    public StockMovement receive(Sku sku, Quantity quantity, String reason) {
        return move(sku, MovementType.INBOUND, quantity, reason);
    }

    public StockMovement dispatch(Sku sku, Quantity quantity, String reason) {
        return move(sku, MovementType.OUTBOUND, quantity, reason);
    }

    public StockMovement adjust(Sku sku, Quantity countedStock, String reason) {
        return move(sku, MovementType.ADJUSTMENT, countedStock, reason);
    }

    /** Histórico do produto, do mais antigo para o mais recente. */
    public List<StockMovement> history(Sku sku) {
        return transaction.execute((Stores stores) -> {
            stores.products().findBySku(sku).orElseThrow(() -> new ProductNotFoundException(sku));
            return stores.movements().findBySku(sku);
        });
    }

    private StockMovement move(Sku sku, MovementType type, Quantity quantity, String reason) {
        Outcome outcome = transaction.execute((Stores stores) -> {
            Product product = stores.products().findBySku(sku).orElseThrow(() -> new ProductNotFoundException(sku));
            boolean wasBelowMinimum = product.isBelowMinimum();

            StockMovement movement = movements.apply(type, product, quantity, reason);
            stores.products().save(product);
            stores.movements().append(movement);
            return new Outcome(movement, product, !wasBelowMinimum && product.isBelowMinimum());
        });

        // Avisa só depois do commit: um alerta de algo que foi desfeito seria um alarme falso.
        if (outcome.crossedMinimum()) {
            listeners.forEach(listener -> listener.onBelowMinimum(outcome.product()));
        }
        return outcome.movement();
    }

    private record Outcome(StockMovement movement, Product product, boolean crossedMinimum) {
    }
}
