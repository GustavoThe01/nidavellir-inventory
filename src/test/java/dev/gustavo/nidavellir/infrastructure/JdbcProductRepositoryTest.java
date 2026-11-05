package dev.gustavo.nidavellir.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.gustavo.nidavellir.application.InventoryQueryService;
import dev.gustavo.nidavellir.application.ProductRepository;
import dev.gustavo.nidavellir.application.ProductRepositoryContract;
import dev.gustavo.nidavellir.domain.Category;
import dev.gustavo.nidavellir.domain.Money;
import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.Quantity;
import dev.gustavo.nidavellir.domain.Sku;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("JdbcProductRepository")
class JdbcProductRepositoryTest extends ProductRepositoryContract {

    @TempDir
    Path directory;

    private ConnectionFactory connections;
    private JdbcProductRepository repository;

    @BeforeEach
    void setUp() {
        connections = new ConnectionFactory(directory.resolve("teste.db"));
        connections.initializeSchema();
        repository = new JdbcProductRepository(connections);
    }

    @Override
    protected ProductRepository repository() {
        return repository;
    }

    @Test
    @DisplayName("os dados continuam lá depois de abrir uma nova instância")
    void dataSurvivesNewInstance() {
        repository.save(product("MJ-001", "Martelo"));

        connections.initializeSchema();
        JdbcProductRepository reopened = new JdbcProductRepository(connections);

        assertThat(reopened.findBySku(Sku.of("MJ-001"))).isPresent();
    }

    @Test
    @DisplayName("texto malicioso é salvo como dado e não executa SQL")
    void resistsSqlInjection() {
        String attack = "x'); DROP TABLE product; --";

        repository.save(product("MJ-001", attack));

        assertThat(repository.findBySku(Sku.of("MJ-001")).orElseThrow().name()).isEqualTo(attack);
        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("o preço é gravado em centavos, sem erro de arredondamento")
    void storesPriceInCents() throws SQLException {
        Product product = Product.create(
                Sku.of("MJ-001"), "Martelo", Category.of("Forja"), Money.of("19.99"), Quantity.ZERO);
        repository.save(product);

        try (Connection connection = connections.open();
             Statement statement = connection.createStatement();
             var row = statement.executeQuery("SELECT price_cents, typeof(price_cents) FROM product")) {
            row.next();
            assertThat(row.getLong(1)).isEqualTo(1999L);
            assertThat(row.getString(2)).isEqualTo("integer");
        }
    }

    @Test
    @DisplayName("o próprio banco recusa estoque negativo")
    void databaseRejectsNegativeStock() throws SQLException {
        repository.save(product("MJ-001", "Martelo"));

        try (Connection connection = connections.open();
             Statement statement = connection.createStatement()) {
            assertThatThrownBy(() -> statement.executeUpdate("UPDATE product SET stock = -1"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("CHECK");
        }
    }

    @Test
    @DisplayName("a camada de aplicação funciona com o JDBC sem nenhuma alteração")
    void applicationWorksWithJdbc() {
        Product hammer = product("MJ-001", "Martelo");
        hammer.addStock(Quantity.of(3));
        repository.save(hammer);

        InventoryQueryService queries = new InventoryQueryService(repository);

        assertThat(queries.totalStockValue()).isEqualTo(Money.of("30.00"));
    }

    @Test
    @DisplayName("banco sem schema vira PersistenceException, sem vazar SQLException")
    void wrapsTechnicalFailures() {
        ConnectionFactory empty = new ConnectionFactory(directory.resolve("vazio.db"));
        JdbcProductRepository withoutSchema = new JdbcProductRepository(empty);

        assertThatThrownBy(withoutSchema::findAll)
                .isInstanceOf(PersistenceException.class)
                .hasCauseInstanceOf(SQLException.class);
    }
}
