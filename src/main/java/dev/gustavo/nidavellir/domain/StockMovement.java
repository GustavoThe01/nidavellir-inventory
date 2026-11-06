package dev.gustavo.nidavellir.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Registro imutável de uma mudança de estoque. Nada entra ou sai sem deixar uma destas linhas.
 *
 * @param quantity quantos itens a movimentação envolveu; em um ajuste, é o novo saldo contado
 * @param stockAfter saldo do produto logo depois da movimentação
 */
public record StockMovement(
        Sku sku, MovementType type, Quantity quantity, Quantity stockAfter, String reason, Instant occurredAt) {

    public enum MovementType {
        /** Mercadoria que entrou no estoque. */
        INBOUND,
        /** Mercadoria que saiu do estoque. */
        OUTBOUND,
        /** Correção do saldo depois de uma contagem física. */
        ADJUSTMENT
    }

    public StockMovement {
        Objects.requireNonNull(sku, "sku");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(stockAfter, "stockAfter");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (reason == null || reason.isBlank()) {
            throw new InvalidValueException("O motivo da movimentação é obrigatório.");
        }
        reason = reason.strip();
    }
}
