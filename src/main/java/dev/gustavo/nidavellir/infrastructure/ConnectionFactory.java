package dev.gustavo.nidavellir.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Abre conexões com o arquivo SQLite. Cada operação abre e fecha a própria conexão: em uma aplicação
 * de console, com um único usuário, um pool de conexões seria complexidade sem ganho.
 */
public final class ConnectionFactory {

    private static final String SCHEMA_RESOURCE = "/db/schema.sql";

    private final String url;

    public ConnectionFactory(Path databaseFile) {
        this.url = "jdbc:sqlite:" + Objects.requireNonNull(databaseFile, "databaseFile").toAbsolutePath();
    }

    public Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            // Vale por conexão: o SQLite vem com as chaves estrangeiras desligadas.
            statement.execute("PRAGMA foreign_keys = ON");
            return connection;
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
    }

    /** Cria as tabelas que ainda não existem. Pode ser chamado a cada inicialização. */
    public void initializeSchema() {
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            for (String command : readSchema().split(";")) {
                if (!command.isBlank()) {
                    statement.executeUpdate(command);
                }
            }
        } catch (SQLException | IOException exception) {
            throw new PersistenceException("Não consegui criar o schema do banco.", exception);
        }
    }

    private static String readSchema() throws IOException {
        try (InputStream stream = ConnectionFactory.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (stream == null) {
                throw new IOException("Recurso não encontrado: " + SCHEMA_RESOURCE);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
