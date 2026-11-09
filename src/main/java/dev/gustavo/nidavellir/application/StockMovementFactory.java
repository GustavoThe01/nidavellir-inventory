package dev.gustavo.nidavellir.application;

import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.Quantity;
import dev.gustavo.nidavellir.domain.StockMovement;
import dev.gustavo.nidavellir.domain.StockMovement.Adjustment;
import dev.gustavo.nidavellir.domain.StockMovement.Inbound;
import dev.gustavo.nidavellir.domain.StockMovement.Outbound;
import java.time.Clock;
import java.util.Objects;

/**
 * Factory das movimentações: concentra em um só lugar como cada tipo altera o produto
 * e como o registro é montado (horário, saldo depois da operação).
 */
public final class StockMovementFactory {

    private final Clock clock;

    public StockMovementFactory(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public StockMovement inbound(Product product, Quantity quantity, String reason) {
        product.addStock(quantity);
        return new Inbound(product.sku(), quantity, product.stock(), reason, clock.instant());
    }

    public StockMovement outbound(Product product, Quantity quantity, String reason) {
        product.removeStock(quantity);
        return new Outbound(product.sku(), quantity, product.stock(), reason, clock.instant());
    }

    public StockMovement adjustment(Product product, Quantity countedStock, String reason) {
        product.adjustStock(countedStock);
        return new Adjustment(product.sku(), countedStock, product.stock(), reason, clock.instant());
    }
}
