package dev.gustavo.nidavellir.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.gustavo.nidavellir.domain.InsufficientStockException;
import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.Quantity;
import dev.gustavo.nidavellir.domain.Sku;
import dev.gustavo.nidavellir.domain.StockMovement;
import dev.gustavo.nidavellir.infrastructure.InMemoryProductRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Alerta de estoque mínimo (Observer)")
class StockAlertTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-07T12:00:00Z"), ZoneOffset.UTC);
    private static final Sku HAMMER = Sku.of("MJ-001");

    private final List<String> alerts = new ArrayList<>();
    private final InMemoryProductRepository products = new InMemoryProductRepository();
    private StockService service;

    /** Transação em memória: suficiente para testar o aviso, que não depende de banco. */
    private final InventoryTransaction inMemory = new InventoryTransaction() {
        private final StockMovementRepository movements = new StockMovementRepository() {
            private final List<StockMovement> history = new ArrayList<>();

            @Override
            public void append(StockMovement movement) {
                history.add(movement);
            }

            @Override
            public List<StockMovement> findBySku(Sku sku) {
                return history.stream()
                        .filter(movement -> movement.sku().equals(sku))
                        .toList();
            }
        };

        @Override
        public <T> T execute(Function<Stores, T> work) {
            return work.apply(new Stores(products, movements));
        }
    };

    @BeforeEach
    void setUp() {
        Product hammer = Product.builder()
                .sku("MJ-001")
                .name("Martelo")
                .category("Forja")
                .price("10")
                .minimumStock(5)
                .build();
        hammer.addStock(Quantity.of(10));
        products.save(hammer);
        service = new StockService(
                inMemory,
                CLOCK,
                List.of(
                        product -> alerts.add("compras:" + product.sku()),
                        product -> alerts.add("log:" + product.stock())));
    }

    @Test
    @DisplayName("avisa todos os listeners quando o estoque cruza o mínimo")
    void notifiesWhenCrossing() {
        service.dispatch(HAMMER, Quantity.of(6), "Venda");

        assertThat(alerts).containsExactly("compras:MJ-001", "log:4");
    }

    @Test
    @DisplayName("não avisa enquanto o estoque continua acima do mínimo")
    void silentAboveMinimum() {
        service.dispatch(HAMMER, Quantity.of(5), "Venda");

        assertThat(alerts).isEmpty();
    }

    @Test
    @DisplayName("não repete o aviso para quem já estava abaixo do mínimo")
    void notifiesOnlyOnce() {
        service.dispatch(HAMMER, Quantity.of(6), "Venda");
        service.dispatch(HAMMER, Quantity.of(1), "Venda");

        assertThat(alerts).hasSize(2);
    }

    @Test
    @DisplayName("avisa de novo depois de o estoque ser reposto e cair outra vez")
    void notifiesAgainAfterRestock() {
        service.dispatch(HAMMER, Quantity.of(6), "Venda");
        service.receive(HAMMER, Quantity.of(10), "Reposição");
        service.dispatch(HAMMER, Quantity.of(10), "Venda");

        assertThat(alerts).hasSize(4);
    }

    @Test
    @DisplayName("não avisa quando a operação falha")
    void silentOnFailure() {
        assertThatThrownBy(() -> service.dispatch(HAMMER, Quantity.of(99), "Venda"))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(alerts).isEmpty();
    }

    @Test
    @DisplayName("o ajuste de inventário também pode disparar o aviso")
    void adjustmentNotifies() {
        service.adjust(HAMMER, Quantity.of(2), "Contagem física");

        assertThat(alerts).hasSize(2);
    }
}
