package dev.gustavo.nidavellir.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import dev.gustavo.nidavellir.application.StockService;
import dev.gustavo.nidavellir.domain.Category;
import dev.gustavo.nidavellir.domain.InsufficientStockException;
import dev.gustavo.nidavellir.domain.InvalidValueException;
import dev.gustavo.nidavellir.domain.Money;
import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.ProductNotFoundException;
import dev.gustavo.nidavellir.domain.Quantity;
import dev.gustavo.nidavellir.domain.Sku;
import dev.gustavo.nidavellir.domain.StockMovement;
import dev.gustavo.nidavellir.domain.StockMovement.MovementType;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Roda contra um SQLite de verdade em arquivo temporário: é o único jeito de provar um rollback. */
@DisplayName("StockService com JDBC")
class StockServiceIntegrationTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-06T12:00:00Z"), ZoneOffset.UTC);
    private static final Sku HAMMER = Sku.of("MJ-001");

    @TempDir
    Path directory;

    private ConnectionFactory connections;
    private JdbcProductRepository products;
    private JdbcStockMovementRepository movements;
    private StockService service;

    @BeforeEach
    void setUp() {
        connections = new ConnectionFactory(directory.resolve("teste.db"));
        connections.initializeSchema();
        products = new JdbcProductRepository(connections);
        movements = new JdbcStockMovementRepository(connections);
        service = new StockService(new JdbcInventoryTransaction(connections), CLOCK);
        products.save(Product.create(HAMMER, "Martelo", Category.of("Forja"), Money.of("100"), Quantity.of(2)));
    }

    private Quantity currentStock() {
        return products.findBySku(HAMMER).orElseThrow().stock();
    }

    @Test
    @DisplayName("entrada e saída mudam o saldo e ficam no histórico, em ordem")
    void recordsHistory() {
        service.receive(HAMMER, Quantity.of(10), "Compra do fornecedor");
        service.dispatch(HAMMER, Quantity.of(4), "Venda");

        assertThat(currentStock()).isEqualTo(Quantity.of(6));
        assertThat(movements.findBySku(HAMMER))
                .extracting(StockMovement::type, StockMovement::stockAfter, StockMovement::reason)
                .containsExactly(
                        tuple(MovementType.INBOUND, Quantity.of(10), "Compra do fornecedor"),
                        tuple(MovementType.OUTBOUND, Quantity.of(6), "Venda"));
    }

    @Test
    @DisplayName("o histórico guarda o horário do relógio")
    void usesClock() {
        service.receive(HAMMER, Quantity.of(1), "Compra");

        assertThat(movements.findBySku(HAMMER).getFirst().occurredAt()).isEqualTo(CLOCK.instant());
    }

    @Test
    @DisplayName("o ajuste define o saldo contado, mesmo que seja zero")
    void adjustsToCountedStock() {
        service.receive(HAMMER, Quantity.of(5), "Compra");

        service.adjust(HAMMER, Quantity.ZERO, "Contagem física");

        assertThat(currentStock()).isEqualTo(Quantity.ZERO);
    }

    @Test
    @DisplayName("saída maior que o saldo não altera nada e não deixa histórico")
    void insufficientStockChangesNothing() {
        service.receive(HAMMER, Quantity.of(1), "Compra");

        assertThatThrownBy(() -> service.dispatch(HAMMER, Quantity.of(2), "Venda"))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(currentStock()).isEqualTo(Quantity.of(1));
        assertThat(movements.findBySku(HAMMER)).hasSize(1);
    }

    @Test
    @DisplayName("produto inexistente é recusado")
    void unknownProduct() {
        assertThatThrownBy(() -> service.receive(Sku.of("NADA-1"), Quantity.of(1), "Compra"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("motivo em branco é recusado e desfaz a alteração do saldo")
    void blankReasonRollsBack() {
        assertThatThrownBy(() -> service.receive(HAMMER, Quantity.of(1), " "))
                .isInstanceOf(InvalidValueException.class);

        assertThat(currentStock()).isEqualTo(Quantity.ZERO);
    }

    @Test
    @DisplayName("ROLLBACK: se o histórico falhar depois do saldo gravado, o saldo também volta")
    void historyFailureRollsBackStock() throws SQLException {
        service.receive(HAMMER, Quantity.of(5), "Compra");
        try (Connection connection = connections.open(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE stock_movement");
        }

        assertThatThrownBy(() -> service.dispatch(HAMMER, Quantity.of(2), "Venda"))
                .isInstanceOf(PersistenceException.class);

        assertThat(currentStock()).isEqualTo(Quantity.of(5));
    }

    @Test
    @DisplayName("produto com histórico não pode ser apagado")
    void productWithHistoryCannotBeDeleted() {
        service.receive(HAMMER, Quantity.of(1), "Compra");

        assertThatThrownBy(() -> products.deleteBySku(HAMMER)).isInstanceOf(PersistenceException.class);
    }
}
