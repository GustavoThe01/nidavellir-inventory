package dev.gustavo.nidavellir.application;

import dev.gustavo.nidavellir.application.InventoryTransaction.Stores;
import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.ProductNotFoundException;
import dev.gustavo.nidavellir.domain.Quantity;
import dev.gustavo.nidavellir.domain.Sku;
import dev.gustavo.nidavellir.domain.StockMovement;
import dev.gustavo.nidavellir.domain.StockMovement.MovementType;
import java.time.Clock;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Casos de uso de movimentação de estoque. Saldo e histórico sempre mudam na mesma transação:
 * não existe saldo alterado sem registro, nem registro de algo que não aconteceu.
 */
public final class StockService {

    private final InventoryTransaction transaction;
    private final Clock clock;

    public StockService(InventoryTransaction transaction, Clock clock) {
        this.transaction = Objects.requireNonNull(transaction, "transaction");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public StockMovement receive(Sku sku, Quantity quantity, String reason) {
        return move(sku, MovementType.INBOUND, quantity, reason, product -> product.addStock(quantity));
    }

    public StockMovement dispatch(Sku sku, Quantity quantity, String reason) {
        return move(sku, MovementType.OUTBOUND, quantity, reason, product -> product.removeStock(quantity));
    }

    public StockMovement adjust(Sku sku, Quantity countedStock, String reason) {
        return move(sku, MovementType.ADJUSTMENT, countedStock, reason, product -> product.adjustStock(countedStock));
    }

    private StockMovement move(Sku sku, MovementType type, Quantity quantity, String reason, Consumer<Product> change) {
        return transaction.execute((Stores stores) -> {
            Product product = stores.products().findBySku(sku).orElseThrow(() -> new ProductNotFoundException(sku));
            change.accept(product);

            StockMovement movement = new StockMovement(sku, type, quantity, product.stock(), reason, clock.instant());
            stores.products().save(product);
            stores.movements().append(movement);
            return movement;
        });
    }
}
