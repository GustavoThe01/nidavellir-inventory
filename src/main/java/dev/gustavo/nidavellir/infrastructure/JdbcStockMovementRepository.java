package dev.gustavo.nidavellir.infrastructure;

import dev.gustavo.nidavellir.application.StockMovementRepository;
import dev.gustavo.nidavellir.domain.Quantity;
import dev.gustavo.nidavellir.domain.Sku;
import dev.gustavo.nidavellir.domain.StockMovement;
import dev.gustavo.nidavellir.domain.StockMovement.Adjustment;
import dev.gustavo.nidavellir.domain.StockMovement.Inbound;
import dev.gustavo.nidavellir.domain.StockMovement.Outbound;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Histórico de movimentações com JDBC. A ordem vem do {@code id}, porque dois registros podem ter o mesmo horário. */
public final class JdbcStockMovementRepository implements StockMovementRepository {

    private static final String INSERT = """
            INSERT INTO stock_movement (sku, type, quantity, stock_after, reason, occurred_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String SELECT_BY_SKU = """
            SELECT sku, type, quantity, stock_after, reason, occurred_at
            FROM stock_movement WHERE sku = ? ORDER BY id
            """;

    private final ConnectionScope scope;

    public JdbcStockMovementRepository(ConnectionFactory connections) {
        this(ConnectionScope.perOperation(Objects.requireNonNull(connections, "connections")));
    }

    private JdbcStockMovementRepository(ConnectionScope scope) {
        this.scope = scope;
    }

    static JdbcStockMovementRepository boundTo(Connection connection) {
        return new JdbcStockMovementRepository(ConnectionScope.shared(connection));
    }

    @Override
    public void append(StockMovement movement) {
        Objects.requireNonNull(movement, "movement");
        try {
            scope.run(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
                    statement.setString(1, movement.sku().value());
                    statement.setString(2, typeOf(movement));
                    statement.setInt(3, movement.quantity().value());
                    statement.setInt(4, movement.stockAfter().value());
                    statement.setString(5, movement.reason());
                    statement.setString(6, movement.occurredAt().toString());
                    return statement.executeUpdate();
                }
            });
        } catch (SQLException exception) {
            throw new PersistenceException(
                    "Não consegui registrar a movimentação de " + movement.sku() + ".", exception);
        }
    }

    @Override
    public void appendAll(List<StockMovement> movements) {
        Objects.requireNonNull(movements, "movements");
        try {
            scope.run(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
                    for (StockMovement movement : movements) {
                        statement.setString(1, movement.sku().value());
                        statement.setString(2, typeOf(movement));
                        statement.setInt(3, movement.quantity().value());
                        statement.setInt(4, movement.stockAfter().value());
                        statement.setString(5, movement.reason());
                        statement.setString(6, movement.occurredAt().toString());
                        statement.addBatch();
                    }
                    return statement.executeBatch();
                }
            });
        } catch (SQLException exception) {
            throw new PersistenceException(
                    "Não consegui registrar as " + movements.size() + " movimentações.", exception);
        }
    }

    @Override
    public List<StockMovement> findBySku(Sku sku) {
        try {
            return scope.run(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(SELECT_BY_SKU)) {
                    statement.setString(1, sku.value());
                    try (ResultSet row = statement.executeQuery()) {
                        List<StockMovement> movements = new ArrayList<>();
                        while (row.next()) {
                            movements.add(toMovement(row));
                        }
                        return List.copyOf(movements);
                    }
                }
            });
        } catch (SQLException exception) {
            throw new PersistenceException("Não consegui ler o histórico de " + sku + ".", exception);
        }
    }

    /** O código gravado na coluna {@code type}. O switch é exaustivo: um tipo novo sem código não compila. */
    private static String typeOf(StockMovement movement) {
        return switch (movement) {
            case Inbound inbound -> "INBOUND";
            case Outbound outbound -> "OUTBOUND";
            case Adjustment adjustment -> "ADJUSTMENT";
        };
    }

    private static StockMovement toMovement(ResultSet row) throws SQLException {
        Sku sku = Sku.of(row.getString("sku"));
        Quantity quantity = Quantity.of(row.getInt("quantity"));
        Quantity stockAfter = Quantity.of(row.getInt("stock_after"));
        String reason = row.getString("reason");
        Instant occurredAt = Instant.parse(row.getString("occurred_at"));
        return switch (row.getString("type")) {
            case "INBOUND" -> new Inbound(sku, quantity, stockAfter, reason, occurredAt);
            case "OUTBOUND" -> new Outbound(sku, quantity, stockAfter, reason, occurredAt);
            case "ADJUSTMENT" -> new Adjustment(sku, quantity, stockAfter, reason, occurredAt);
            default -> throw new SQLException("Tipo de movimentação desconhecido no banco: " + row.getString("type"));
        };
    }
}
