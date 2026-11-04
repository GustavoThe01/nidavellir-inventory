package dev.gustavo.nidavellir.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import dev.gustavo.nidavellir.domain.Category;
import dev.gustavo.nidavellir.domain.Money;
import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.Quantity;
import dev.gustavo.nidavellir.domain.Sku;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InMemoryProductRepository")
class InMemoryProductRepositoryTest {

    private final InMemoryProductRepository repository = new InMemoryProductRepository();

    private static Product product(String sku, String name) {
        return Product.create(Sku.of(sku), name, Category.of("Forja"), Money.of("10"), Quantity.ZERO);
    }

    @Test
    @DisplayName("encontra pelo SKU, mesmo digitado em minúsculas")
    void findsBySku() {
        repository.save(product("MJ-001", "Martelo"));
        assertThat(repository.findBySku(Sku.of("mj-001"))).isPresent();
    }

    @Test
    @DisplayName("devolve vazio quando o SKU não existe")
    void returnsEmptyWhenMissing() {
        assertThat(repository.findBySku(Sku.of("NADA-1"))).isEmpty();
    }

    @Test
    @DisplayName("salvar o mesmo SKU atualiza em vez de duplicar")
    void saveReplaces() {
        repository.save(product("MJ-001", "Martelo"));
        repository.save(product("MJ-001", "Martelo novo"));

        assertThat(repository.findAll()).singleElement()
                .satisfies(found -> assertThat(found.name()).isEqualTo("Martelo novo"));
    }

    @Test
    @DisplayName("lista em ordem de SKU")
    void listsSorted() {
        repository.save(product("ZZ-001", "Zarabatana"));
        repository.save(product("AA-001", "Anel"));

        assertThat(repository.findAll()).extracting(found -> found.sku().value())
                .containsExactly("AA-001", "ZZ-001");
    }

    @Test
    @DisplayName("informa se removeu de fato")
    void deleteReportsResult() {
        repository.save(product("MJ-001", "Martelo"));

        assertThat(repository.deleteBySku(Sku.of("MJ-001"))).isTrue();
        assertThat(repository.deleteBySku(Sku.of("MJ-001"))).isFalse();
    }
}
