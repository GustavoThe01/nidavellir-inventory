package dev.gustavo.nidavellir.application;

import java.util.function.Function;

/**
 * Porta da transação: o que for gravado dentro de {@code work} é confirmado junto ou desfeito junto.
 *
 * <p>A camada de aplicação decide o que é atômico, sem saber que por baixo existe um
 * {@code setAutoCommit(false)}.
 */
public interface InventoryTransaction {

    /** Repositórios ligados à mesma transação. */
    record Stores(ProductRepository products, StockMovementRepository movements) {
    }

    <T> T execute(Function<Stores, T> work);
}
