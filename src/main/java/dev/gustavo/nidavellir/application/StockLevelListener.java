package dev.gustavo.nidavellir.application;

import dev.gustavo.nidavellir.domain.Product;

/**
 * Observer: é avisado quando um produto cruza o estoque mínimo para baixo.
 *
 * <p>Quem avisa ({@link StockService}) não sabe se o aviso vira e-mail, log ou pedido de compra.
 * Um destino novo é um listener novo, sem tocar no serviço.
 */
@FunctionalInterface
public interface StockLevelListener {

    void onBelowMinimum(Product product);
}
