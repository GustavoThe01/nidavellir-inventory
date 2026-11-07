package dev.gustavo.nidavellir.application;

import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.Quantity;
import dev.gustavo.nidavellir.domain.StockMovement;
import dev.gustavo.nidavellir.domain.StockMovement.MovementType;
import java.time.Clock;
import java.util.Objects;

/**
 * Factory das movimentações: concentra em um só lugar a decisão de como cada tipo altera o produto
 * e como o registro é montado (horário, saldo depois da operação).
 */
public final class StockMovementFactory {

    private final Clock clock;

    public StockMovementFactory(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Aplica a movimentação ao produto e devolve o registro correspondente. */
    public StockMovement apply(MovementType type, Product product, Quantity quantity, String reason) {
        switch (type) {
            case INBOUND -> product.addStock(quantity);
            case OUTBOUND -> product.removeStock(quantity);
            case ADJUSTMENT -> product.adjustStock(quantity);
        }
        return new StockMovement(product.sku(), type, quantity, product.stock(), reason, clock.instant());
    }
}
