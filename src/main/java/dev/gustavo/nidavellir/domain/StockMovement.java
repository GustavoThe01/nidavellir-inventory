package dev.gustavo.nidavellir.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Registro imutável de uma mudança de estoque. Nada entra ou sai sem deixar uma destas linhas.
 *
 * <p>É um tipo algébrico: uma {@code sealed interface} com exatamente três variantes. Um {@code switch} sobre
 * {@code StockMovement} que trate as três não precisa de {@code default}, e o compilador acusa erro se um tipo
 * novo for criado e algum {@code switch} esquecer de tratá-lo. Com o {@code enum} de antes, esse esquecimento
 * só aparecia em tempo de execução.
 */
public sealed interface StockMovement {

    Sku sku();

    /** Quantos itens a movimentação envolveu; no ajuste, é o saldo contado. */
    Quantity quantity();

    /** Saldo do produto logo depois da movimentação. */
    Quantity stockAfter();

    String reason();

    Instant occurredAt();

    /** Mercadoria que entrou no estoque. */
    record Inbound(Sku sku, Quantity quantity, Quantity stockAfter, String reason, Instant occurredAt)
            implements StockMovement {
        public Inbound {
            reason = validate(sku, quantity, stockAfter, reason, occurredAt);
        }
    }

    /** Mercadoria que saiu do estoque. */
    record Outbound(Sku sku, Quantity quantity, Quantity stockAfter, String reason, Instant occurredAt)
            implements StockMovement {
        public Outbound {
            reason = validate(sku, quantity, stockAfter, reason, occurredAt);
        }
    }

    /** Correção do saldo depois de uma contagem física. {@code quantity} é o que foi contado. */
    record Adjustment(Sku sku, Quantity quantity, Quantity stockAfter, String reason, Instant occurredAt)
            implements StockMovement {
        public Adjustment {
            reason = validate(sku, quantity, stockAfter, reason, occurredAt);
            if (!quantity.equals(stockAfter)) {
                throw new InvalidValueException("No ajuste, o saldo final é a quantidade contada.");
            }
        }
    }

    private static String validate(Sku sku, Quantity quantity, Quantity stockAfter, String reason, Instant occurredAt) {
        Objects.requireNonNull(sku, "sku");
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(stockAfter, "stockAfter");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (reason == null || reason.isBlank()) {
            throw new InvalidValueException("O motivo da movimentação é obrigatório.");
        }
        return reason.strip();
    }
}
