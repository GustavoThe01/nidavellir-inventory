package dev.gustavo.nidavellir.application;

import dev.gustavo.nidavellir.domain.Sku;
import dev.gustavo.nidavellir.domain.StockMovement;
import java.util.List;

/** Porta do histórico de movimentações. O histórico só cresce: não existe alterar nem apagar. */
public interface StockMovementRepository {

    void append(StockMovement movement);

    /** Histórico do produto, do mais antigo para o mais recente. */
    List<StockMovement> findBySku(Sku sku);
}
