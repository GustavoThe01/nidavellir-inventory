package dev.gustavo.nidavellir.infrastructure;

import dev.gustavo.nidavellir.application.InventoryTransaction;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Function;

/**
 * Transação manual com JDBC: desliga o auto-commit, confirma no fim e desfaz em qualquer falha.
 * A conexão é a mesma para todos os repositórios, porque é a conexão que define a transação.
 */
public final class JdbcInventoryTransaction implements InventoryTransaction {

    private final ConnectionFactory connections;

    public JdbcInventoryTransaction(ConnectionFactory connections) {
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    @Override
    public <T> T execute(Function<Stores, T> work) {
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try {
                T result = work.apply(new Stores(
                        JdbcProductRepository.boundTo(connection),
                        JdbcStockMovementRepository.boundTo(connection)));
                connection.commit();
                return result;
            } catch (RuntimeException | SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não consegui concluir a transação.", exception);
        }
    }

    private static void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            // A falha original é a que importa; a do rollback fica registrada junto dela.
            original.addSuppressed(rollbackFailure);
        }
    }
}
