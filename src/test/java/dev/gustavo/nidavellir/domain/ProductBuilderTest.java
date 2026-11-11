package dev.gustavo.nidavellir.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProductBuilder")
class ProductBuilderTest {

    @Test
    @DisplayName("monta o produto com todos os campos")
    void buildsProduct() {
        Product product = Product.builder()
                .sku("bg-001")
                .name("Bigorna")
                .category("Forja")
                .price("1249.90")
                .minimumStock(3)
                .build();

        assertThat(product.sku()).isEqualTo(Sku.of("BG-001"));
        assertThat(product.price()).isEqualTo(Money.of("1249.90"));
        assertThat(product.minimumStock()).isEqualTo(Quantity.of(3));
        assertThat(product.stock()).isEqualTo(Quantity.ZERO);
    }

    @Test
    @DisplayName("o estoque mínimo é opcional e começa em zero")
    void minimumStockDefaultsToZero() {
        Product product = Product.builder()
                .sku("BG-001")
                .name("Bigorna")
                .category("Forja")
                .price("10")
                .build();

        assertThat(product.minimumStock()).isEqualTo(Quantity.ZERO);
    }

    @Test
    @DisplayName("campo obrigatório ausente é recusado com o nome do campo")
    void rejectsMissingField() {
        assertThatThrownBy(() -> Product.builder()
                        .sku("BG-001")
                        .name("Bigorna")
                        .price("10")
                        .build())
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("categoria");
    }

    @Test
    @DisplayName("as regras do produto continuam valendo pelo builder")
    void keepsProductRules() {
        assertThatThrownBy(() -> Product.builder()
                        .sku("BG-001")
                        .name("Bigorna")
                        .category("Forja")
                        .price("0")
                        .build())
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("maior que zero");
    }
}
