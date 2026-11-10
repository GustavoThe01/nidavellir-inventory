package dev.gustavo.nidavellir.cli;

import static org.assertj.core.api.Assertions.assertThat;

import dev.gustavo.nidavellir.application.InventoryTransaction;
import dev.gustavo.nidavellir.application.StockMovementRepository;
import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.Sku;
import dev.gustavo.nidavellir.domain.StockMovement;
import dev.gustavo.nidavellir.infrastructure.InMemoryProductRepository;
import dev.gustavo.nidavellir.infrastructure.PersistenceException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Simula sessões inteiras: o que o usuário digita entra como texto, e a saída é conferida como texto. */
@DisplayName("Menu da CLI (Command)")
class MenuTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-08T12:00:00Z"), ZoneOffset.UTC);

    private final InMemoryProductRepository products = new InMemoryProductRepository();
    private final List<Exception> logged = new ArrayList<>();
    private InventoryTransaction transaction = new InMemoryTransaction();

    private final class InMemoryTransaction implements InventoryTransaction {
        private final List<StockMovement> history = new ArrayList<>();
        private final StockMovementRepository movements = new StockMovementRepository() {
            @Override
            public void append(StockMovement movement) {
                history.add(movement);
            }

            @Override
            public List<StockMovement> findBySku(Sku sku) {
                return history.stream().filter(movement -> movement.sku().equals(sku)).toList();
            }
        };

        @Override
        public <T> T execute(Function<Stores, T> work) {
            return work.apply(new Stores(products, movements));
        }
    }

    private String session(String... lines) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        BufferedReader in = new BufferedReader(new StringReader(String.join("\n", lines) + "\n"));
        NidavellirApp.menu(new Terminal(in, out), products, transaction, CLOCK, logged::add).run();
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private void hammer(int stock) {
        Product product = Product.builder().sku("MJ-001").name("Martelo").category("Forja").price("19.90").minimumStock(5).build();
        if (stock > 0) {
            product.addStock(dev.gustavo.nidavellir.domain.Quantity.of(stock));
        }
        products.save(product);
    }

    @Test
    @DisplayName("cadastra um produto pedindo de novo os campos digitados errado")
    void registersWithRetries() {
        String output = session("2", "mj 001", "mj-001", "Martelo", "Forja", "abc", "19,90", "-1", "5", "1", "0");

        assertThat(output)
                .contains("✗ SKU inválido")
                .contains("✗ Valor monetário inválido")
                .contains("✗ O valor mínimo é 0.")
                .contains("✓ Produto MJ-001 cadastrado")
                .contains("Martelo")
                .contains("R$ 19.90")
                .contains("Até a próxima forja!");
        assertThat(products.findBySku(Sku.of("MJ-001"))).isPresent();
    }

    @Test
    @DisplayName("regra de negócio violada vira mensagem, e o menu continua")
    void domainErrorKeepsTheSession() {
        hammer(3);

        String output = session("4", "MJ-001", "10", "Venda", "1", "0");

        assertThat(output).contains("✗ Estoque insuficiente").contains("Martelo");
        assertThat(products.findBySku(Sku.of("MJ-001")).orElseThrow().stock().value()).isEqualTo(3);
        assertThat(logged).isEmpty();
    }

    @Test
    @DisplayName("saída que cruza o mínimo mostra o alerta, e o histórico registra as movimentações")
    void alertAndHistory() {
        hammer(0);

        String output = session("3", "MJ-001", "10", "Compra", "4", "MJ-001", "6", "Venda", "6", "MJ-001", "0");

        assertThat(output)
                .contains("✓ Entrada registrada. Estoque atual: 10")
                .contains("⚠ Martelo (MJ-001) ficou abaixo do mínimo: 4 de 5.")
                .contains("Entrada")
                .contains("Saída");
    }

    @Test
    @DisplayName("SKU duplicado e produto inexistente aparecem como mensagem")
    void friendlyMessages() {
        hammer(1);

        String output = session("2", "MJ-001", "Outro", "Forja", "1", "0", "6", "XX-999", "0");

        assertThat(output).contains("✗ Já existe um produto com o SKU MJ-001.").contains("✗ Produto XX-999 não encontrado.");
    }

    @Test
    @DisplayName("falha de banco não derruba a sessão e vai para o log de erros")
    void persistenceFailure() {
        hammer(5);
        transaction = new InventoryTransaction() {
            @Override
            public <T> T execute(Function<Stores, T> work) {
                throw new PersistenceException("disco cheio", new java.sql.SQLException("SQLITE_FULL"));
            }
        };

        String output = session("3", "MJ-001", "1", "Compra", "1", "0");

        assertThat(output).contains("✗ Não consegui ler ou gravar os dados").contains("Martelo");
        assertThat(logged).hasSize(1);
        assertThat(output).doesNotContain("SQLITE_FULL");
    }

    @Test
    @DisplayName("opção inválida pede de novo, e o fim da entrada encerra sem erro")
    void invalidOptionAndEndOfInput() {
        String output = session("99", "3", "MJ-001");

        assertThat(output).contains("Opção inválida");
        assertThat(logged).isEmpty();
    }

    @Test
    @DisplayName("valor do estoque mostra cada categoria e o total")
    void stockValue() {
        hammer(2);

        assertThat(session("8", "0")).contains("Forja").contains("TOTAL").contains("R$ 39.80");
    }

    @Test
    @DisplayName("o relatório por categoria e a exportação aparecem no menu")
    void reportAndExport(@org.junit.jupiter.api.io.TempDir java.nio.file.Path folder) {
        hammer(2);

        String output = session("9", "10", folder.toString(), "3", "MJ-001", "5", "Compra", "6", "MJ-001", "0");

        assertThat(output)
                .contains("Forja")
                .contains("✓ Relatório exportado:")
                .contains("estoque-por-categoria.csv")
                .contains("Total: 5 unidade(s) de entrada, 0 de saída e 0 ajuste(s).");
        assertThat(folder.resolve("estoque-por-categoria.json")).exists();
    }
}
