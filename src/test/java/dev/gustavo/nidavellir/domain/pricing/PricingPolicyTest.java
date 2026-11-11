package dev.gustavo.nidavellir.domain.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.gustavo.nidavellir.domain.InvalidValueException;
import dev.gustavo.nidavellir.domain.Money;
import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.Quantity;
import dev.gustavo.nidavellir.domain.pricing.Policies.BestPrice;
import dev.gustavo.nidavellir.domain.pricing.Policies.BulkDiscount;
import dev.gustavo.nidavellir.domain.pricing.Policies.ClearanceDiscount;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Políticas de preço (Strategy)")
class PricingPolicyTest {

    private static Product hammer(int stock) {
        Product product = Product.builder()
                .sku("MJ-001")
                .name("Martelo")
                .category("Forja")
                .price("19.99")
                .build();
        if (stock > 0) {
            product.addStock(Quantity.of(stock));
        }
        return product;
    }

    @Test
    @DisplayName("preço cheio é preço × quantidade")
    void regular() {
        assertThat(PricingPolicy.REGULAR.totalFor(hammer(0), Quantity.of(3))).isEqualTo(Money.of("59.97"));
    }

    @Test
    @DisplayName("atacado só vale a partir da quantidade mínima")
    void bulk() {
        PricingPolicy bulk = new BulkDiscount(5, 10);

        assertThat(bulk.totalFor(hammer(0), Quantity.of(4))).isEqualTo(Money.of("79.96"));
        assertThat(bulk.totalFor(hammer(0), Quantity.of(5))).isEqualTo(Money.of("89.96"));
    }

    @Test
    @DisplayName("o desconto arredonda para o centavo, com empate indo para o par")
    void roundsToCents() {
        assertThat(PricingPolicy.discount(Money.of("0.25"), 50)).isEqualTo(Money.of("0.12"));
        assertThat(PricingPolicy.discount(Money.of("0.35"), 50)).isEqualTo(Money.of("0.18"));
    }

    @Test
    @DisplayName("queima de estoque só vale com estoque acima do limite")
    void clearance() {
        PricingPolicy clearance = new ClearanceDiscount(50, 20);

        assertThat(clearance.totalFor(hammer(50), Quantity.of(1))).isEqualTo(Money.of("19.99"));
        assertThat(clearance.totalFor(hammer(51), Quantity.of(1))).isEqualTo(Money.of("15.99"));
    }

    @Test
    @DisplayName("melhor preço escolhe a política mais barata, sem acumular descontos")
    void bestPrice() {
        PricingPolicy best =
                new BestPrice(List.of(PricingPolicy.REGULAR, new BulkDiscount(2, 5), new ClearanceDiscount(10, 15)));

        assertThat(best.totalFor(hammer(0), Quantity.of(2))).isEqualTo(Money.of("37.98"));
        assertThat(best.totalFor(hammer(20), Quantity.of(2))).isEqualTo(Money.of("33.98"));
    }

    @Test
    @DisplayName("uma política nova entra sem alterar as existentes")
    void openForExtension() {
        PricingPolicy fixedPromo = (product, quantity) -> Money.of("1.00").multiply(quantity.value());
        PricingPolicy best = new BestPrice(List.of(PricingPolicy.REGULAR, fixedPromo));

        assertThat(best.totalFor(hammer(0), Quantity.of(3))).isEqualTo(Money.of("3.00"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 100, -5})
    @DisplayName("percentual fora de 1 a 99 é recusado")
    void rejectsInvalidPercent(int percent) {
        assertThatThrownBy(() -> new BulkDiscount(10, percent)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new ClearanceDiscount(10, percent)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("melhor preço sem nenhuma política é recusado")
    void rejectsEmptyBestPrice() {
        assertThatThrownBy(() -> new BestPrice(List.of())).isInstanceOf(InvalidValueException.class);
    }
}
