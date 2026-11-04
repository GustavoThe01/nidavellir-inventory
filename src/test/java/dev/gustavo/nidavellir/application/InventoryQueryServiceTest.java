package dev.gustavo.nidavellir.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.gustavo.nidavellir.domain.Category;
import dev.gustavo.nidavellir.domain.Money;
import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.Quantity;
import dev.gustavo.nidavellir.domain.Sku;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InventoryQueryService")
class InventoryQueryServiceTest {

    private static final Category FORGE = Category.of("Forja");
    private static final Category JEWELRY = Category.of("Joias");

    private final ProductRepository repository = mock(ProductRepository.class);
    private final InventoryQueryService service = new InventoryQueryService(repository);

    private Product hammer;
    private Product anvil;
    private Product ring;

    private static Product product(String sku, Category category, String price, int stock, int minimum) {
        Product product = Product.create(Sku.of(sku), "Produto " + sku, category, Money.of(price), Quantity.of(minimum));
        if (stock > 0) {
            product.addStock(Quantity.of(stock));
        }
        return product;
    }

    @BeforeEach
    void setUp() {
        hammer = product("MJ-001", FORGE, "100.00", 2, 5);
        anvil = product("BG-001", FORGE, "500.00", 1, 10);
        ring = product("DR-001", JEWELRY, "0.10", 3, 1);
        when(repository.findAll()).thenReturn(List.of(hammer, anvil, ring));
    }

    @Test
    @DisplayName("filtra por categoria, ignorando maiúsculas")
    void filtersByCategory() {
        assertThat(service.findByCategory(Category.of("FORJA"))).containsExactly(hammer, anvil);
    }

    @Test
    @DisplayName("soma o valor total do estoque sem erro de arredondamento")
    void sumsTotalValue() {
        assertThat(service.totalStockValue()).isEqualTo(Money.of("700.30"));
    }

    @Test
    @DisplayName("estoque vazio vale zero")
    void emptyStockIsZero() {
        when(repository.findAll()).thenReturn(List.of());
        assertThat(service.totalStockValue()).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("lista quem está abaixo do mínimo, do maior déficit para o menor")
    void listsBelowMinimum() {
        assertThat(service.findBelowMinimum()).containsExactly(anvil, hammer);
    }

    @Test
    @DisplayName("agrupa o valor em estoque por categoria")
    void groupsValueByCategory() {
        assertThat(service.stockValueByCategory())
                .containsEntry("Forja", Money.of("700.00"))
                .containsEntry("Joias", Money.of("0.30"))
                .hasSize(2);
    }
}
