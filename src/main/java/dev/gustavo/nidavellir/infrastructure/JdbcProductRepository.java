package dev.gustavo.nidavellir.infrastructure;

import dev.gustavo.nidavellir.application.ProductRepository;
import dev.gustavo.nidavellir.domain.Category;
import dev.gustavo.nidavellir.domain.Money;
import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.Quantity;
import dev.gustavo.nidavellir.domain.Sku;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementação da porta {@link ProductRepository} com JDBC puro.
 *
 * <p>Todo valor vem de {@link PreparedStatement}, nunca de texto concatenado: é o que impede SQL injection.
 * Os comandos e resultados ficam em try-with-resources, que os fecha mesmo em caso de erro.
 */
public final class JdbcProductRepository implements ProductRepository {

    private static final int CENTS_SCALE = 2;
    private static final String COLUMNS = "sku, name, category, price_cents, stock, minimum_stock";

    private static final String UPSERT = """
            INSERT INTO product (%s) VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (sku) DO UPDATE SET
                name = excluded.name,
                category = excluded.category,
                price_cents = excluded.price_cents,
                stock = excluded.stock,
                minimum_stock = excluded.minimum_stock
            """.formatted(COLUMNS);
    private static final String SELECT_BY_SKU = "SELECT %s FROM product WHERE sku = ?".formatted(COLUMNS);
    private static final String SELECT_ALL = "SELECT %s FROM product ORDER BY sku".formatted(COLUMNS);
    private static final String DELETE_BY_SKU = "DELETE FROM product WHERE sku = ?";

    private final ConnectionScope scope;

    public JdbcProductRepository(ConnectionFactory connections) {
        this(ConnectionScope.perOperation(Objects.requireNonNull(connections, "connections")));
    }

    private JdbcProductRepository(ConnectionScope scope) {
        this.scope = scope;
    }

    /** Repositório que participa de uma transação em andamento. */
    static JdbcProductRepository boundTo(Connection connection) {
        return new JdbcProductRepository(ConnectionScope.shared(connection));
    }

    @Override
    public void save(Product product) {
        Objects.requireNonNull(product, "product");
        try {
            scope.run(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(UPSERT)) {
                    statement.setString(1, product.sku().value());
                    statement.setString(2, product.name());
                    statement.setString(3, product.category().name());
                    statement.setLong(4, toCents(product.price()));
                    statement.setInt(5, product.stock().value());
                    statement.setInt(6, product.minimumStock().value());
                    return statement.executeUpdate();
                }
            });
        } catch (SQLException exception) {
            throw new PersistenceException("Não consegui salvar o produto " + product.sku() + ".", exception);
        }
    }

    @Override
    public Optional<Product> findBySku(Sku sku) {
        try {
            return scope.run(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(SELECT_BY_SKU)) {
                    statement.setString(1, sku.value());
                    try (ResultSet row = statement.executeQuery()) {
                        return row.next() ? Optional.of(toProduct(row)) : Optional.<Product>empty();
                    }
                }
            });
        } catch (SQLException exception) {
            throw new PersistenceException("Não consegui buscar o produto " + sku + ".", exception);
        }
    }

    @Override
    public List<Product> findAll() {
        try {
            return scope.run(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
                     ResultSet row = statement.executeQuery()) {
                    List<Product> products = new ArrayList<>();
                    while (row.next()) {
                        products.add(toProduct(row));
                    }
                    return List.copyOf(products);
                }
            });
        } catch (SQLException exception) {
            throw new PersistenceException("Não consegui listar os produtos.", exception);
        }
    }

    @Override
    public boolean deleteBySku(Sku sku) {
        try {
            return scope.run(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(DELETE_BY_SKU)) {
                    statement.setString(1, sku.value());
                    return statement.executeUpdate() > 0;
                }
            });
        } catch (SQLException exception) {
            throw new PersistenceException("Não consegui remover o produto " + sku + ".", exception);
        }
    }

    private static Product toProduct(ResultSet row) throws SQLException {
        return Product.restore(
                Sku.of(row.getString("sku")),
                row.getString("name"),
                Category.of(row.getString("category")),
                new Money(BigDecimal.valueOf(row.getLong("price_cents"), CENTS_SCALE)),
                Quantity.of(row.getInt("stock")),
                Quantity.of(row.getInt("minimum_stock")));
    }

    private static long toCents(Money money) {
        return money.amount().movePointRight(CENTS_SCALE).longValueExact();
    }
}
