package dev.gustavo.nidavellir.infrastructure;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * De onde um repositório tira a conexão: uma nova para cada operação, ou a conexão de uma transação em
 * andamento, que não pode ser fechada pelo repositório.
 */
@FunctionalInterface
interface ConnectionScope {

    <T> T run(SqlWork<T> work) throws SQLException;

    @FunctionalInterface
    interface SqlWork<T> {
        T apply(Connection connection) throws SQLException;
    }

    static ConnectionScope perOperation(ConnectionFactory factory) {
        return new ConnectionScope() {
            @Override
            public <T> T run(SqlWork<T> work) throws SQLException {
                try (Connection connection = factory.open()) {
                    return work.apply(connection);
                }
            }
        };
    }

    static ConnectionScope shared(Connection connection) {
        return new ConnectionScope() {
            @Override
            public <T> T run(SqlWork<T> work) throws SQLException {
                return work.apply(connection);
            }
        };
    }
}
