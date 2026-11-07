package dev.gustavo.nidavellir.domain.pricing;

import dev.gustavo.nidavellir.domain.InvalidValueException;
import dev.gustavo.nidavellir.domain.Money;
import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.Quantity;
import java.util.Comparator;
import java.util.List;

/** Implementações de {@link PricingPolicy}. */
public final class Policies {

    private Policies() {
    }

    /** Desconto para compras a partir de uma quantidade mínima (atacado). */
    public record BulkDiscount(int minimumQuantity, int percent) implements PricingPolicy {

        public BulkDiscount {
            if (minimumQuantity < 2) {
                throw new InvalidValueException("O atacado começa em pelo menos 2 unidades.");
            }
            requireValidPercent(percent);
        }

        @Override
        public Money totalFor(Product product, Quantity quantity) {
            Money total = REGULAR.totalFor(product, quantity);
            return quantity.value() >= minimumQuantity ? PricingPolicy.discount(total, percent) : total;
        }
    }

    /** Desconto para girar produtos com estoque acima de um limite (queima de estoque). */
    public record ClearanceDiscount(int stockAbove, int percent) implements PricingPolicy {

        public ClearanceDiscount {
            if (stockAbove < 0) {
                throw new InvalidValueException("O limite de estoque não pode ser negativo.");
            }
            requireValidPercent(percent);
        }

        @Override
        public Money totalFor(Product product, Quantity quantity) {
            Money total = REGULAR.totalFor(product, quantity);
            return product.stock().value() > stockAbove ? PricingPolicy.discount(total, percent) : total;
        }
    }

    /**
     * Composite: aplica a política que der o menor preço ao cliente. Os descontos não se acumulam,
     * que é a regra mais comum no varejo.
     */
    public record BestPrice(List<PricingPolicy> policies) implements PricingPolicy {

        public BestPrice {
            policies = List.copyOf(policies);
            if (policies.isEmpty()) {
                throw new InvalidValueException("Informe pelo menos uma política de preço.");
            }
        }

        @Override
        public Money totalFor(Product product, Quantity quantity) {
            return policies.stream()
                    .map(policy -> policy.totalFor(product, quantity))
                    .min(Comparator.naturalOrder())
                    .orElseThrow();
        }
    }

    private static void requireValidPercent(int percent) {
        if (percent <= 0 || percent >= 100) {
            throw new InvalidValueException("O desconto deve ficar entre 1% e 99%: " + percent);
        }
    }
}
