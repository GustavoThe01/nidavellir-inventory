package dev.gustavo.nidavellir.domain.pricing;

import dev.gustavo.nidavellir.domain.Money;
import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.Quantity;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Strategy de precificação: cada política calcula o preço total de uma venda de um jeito.
 *
 * <p>Uma promoção nova é uma classe nova, sem mexer nas que já existem nem em quem as usa (OCP).
 */
@FunctionalInterface
public interface PricingPolicy {

    /** Preço total para vender {@code quantity} unidades do produto. */
    Money totalFor(Product product, Quantity quantity);

    /** Preço cheio, sem desconto. */
    PricingPolicy REGULAR = (product, quantity) -> product.price().multiply(quantity.value());

    /**
     * Aplica um percentual de desconto ao total, arredondando para o centavo mais próximo
     * (com empate indo para o par, o arredondamento bancário).
     */
    static Money discount(Money total, int percent) {
        BigDecimal factor = BigDecimal.valueOf(100L - percent).movePointLeft(2);
        return new Money(total.amount().multiply(factor).setScale(2, RoundingMode.HALF_EVEN));
    }
}
