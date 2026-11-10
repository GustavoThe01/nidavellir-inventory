package dev.gustavo.nidavellir.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.gustavo.nidavellir.application.ProductImportService.ImportError;
import dev.gustavo.nidavellir.application.ProductImportService.ImportLine;
import dev.gustavo.nidavellir.application.ProductImportService.ImportReport;
import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.Quantity;
import dev.gustavo.nidavellir.domain.Sku;
import dev.gustavo.nidavellir.domain.StockMovement;
import dev.gustavo.nidavellir.domain.StockMovement.Inbound;
import dev.gustavo.nidavellir.infrastructure.InMemoryProductRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Importação de produtos em lote")
class ProductImportServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneOffset.UTC);

    private final InMemoryProductRepository products = new InMemoryProductRepository();
    private final List<StockMovement> history = new ArrayList<>();
    private final InventoryTransaction transaction = new InventoryTransaction() {
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
    };

    private ReportAndLines run(int parallelism, String... lines) {
        List<ImportLine> input = IntStream.range(0, lines.length)
                .mapToObj(i -> new ImportLine(i + 2, lines[i]))
                .toList();
        return new ReportAndLines(new ProductImportService(transaction, CLOCK, parallelism).importLines(input));
    }

    private record ReportAndLines(ImportReport report) {
        List<Integer> errorLines() {
            return report.errors().stream().map(ImportError::line).toList();
        }
    }

    @Test
    @DisplayName("importa as linhas válidas e registra o estoque inicial como entrada")
    void importsValidLines() {
        ImportReport report = run(1,
                "MJ-001,Martelo,Forja,\"19,90\",5,12",
                "RN-001,\"Runa de proteção, rara\",Runas,99.00,0,0").report();

        assertThat(report.imported()).isEqualTo(2);
        assertThat(report.errors()).isEmpty();
        assertThat(products.findBySku(Sku.of("MJ-001")).orElseThrow().stock()).isEqualTo(Quantity.of(12));
        assertThat(products.findBySku(Sku.of("RN-001")).orElseThrow().name()).isEqualTo("Runa de proteção, rara");
        assertThat(history).singleElement()
                .isInstanceOfSatisfying(Inbound.class, entry -> assertThat(entry.reason()).isEqualTo("Importação inicial"));
    }

    @Test
    @DisplayName("cada linha inválida vira um erro com o número da linha e o motivo, e as outras entram")
    void reportsEachInvalidLine() {
        ImportReport report = run(1,
                "MJ-001,Martelo,Forja,19.90,5,1",
                "mj 002,Bigorna,Forja,10.00,0,0",
                "MJ-003,Tenaz,Forja,abc,0,0",
                "MJ-004,Fole,Forja,10.00,dois,0",
                "MJ-005,Lima,Forja,10.00,0,-3",
                "MJ-006,Só cinco colunas,Forja,10.00,0").report();

        assertThat(report.imported()).isEqualTo(1);
        assertThat(report.errors()).extracting(ImportError::line).containsExactly(3, 4, 5, 6, 7);
        assertThat(report.errors()).extracting(ImportError::message).satisfiesExactly(
                message -> assertThat(message).contains("SKU inválido"),
                message -> assertThat(message).contains("Valor monetário inválido"),
                message -> assertThat(message).contains("estoque mínimo deve ser um número inteiro"),
                message -> assertThat(message).contains("estoque inicial não pode ser negativo"),
                message -> assertThat(message).contains("esperava 6 colunas, encontrei 5"));
    }

    @Test
    @DisplayName("SKU repetido no arquivo: a primeira linha entra, as outras são recusadas apontando para ela")
    void duplicatesInFile() {
        ImportReport report = run(4,
                "MJ-001,Martelo,Forja,19.90,0,0",
                "MJ-002,Bigorna,Forja,10.00,0,0",
                "MJ-001,Martelo de novo,Forja,25.00,0,0").report();

        assertThat(report.imported()).isEqualTo(2);
        assertThat(report.errors()).containsExactly(new ImportError(4, "SKU MJ-001 repetido no arquivo (primeiro na linha 2)"));
        assertThat(products.findBySku(Sku.of("MJ-001")).orElseThrow().name()).isEqualTo("Martelo");
    }

    @Test
    @DisplayName("SKU já cadastrado não é sobrescrito")
    void existingSku() {
        products.save(Product.builder().sku("MJ-001").name("Martelo original").category("Forja").price("50").build());

        ImportReport report = run(1, "MJ-001,Martelo importado,Forja,19.90,0,0").report();

        assertThat(report.imported()).isZero();
        assertThat(report.errors()).singleElement().extracting(ImportError::message).isEqualTo("SKU MJ-001 já está cadastrado");
        assertThat(products.findBySku(Sku.of("MJ-001")).orElseThrow().name()).isEqualTo("Martelo original");
    }

    @Test
    @DisplayName("com 1 ou 4 threads, o resultado é idêntico: mesmas linhas importadas e mesmos erros, na mesma ordem")
    void parallelEqualsSequential() {
        String[] lines = IntStream.rangeClosed(1, 20_000)
                .mapToObj(i -> i % 97 == 0 ? "X,linha ruim,C,0,0,0"
                        : i % 101 == 0 ? "SKU-%05d,Repetido,Carga,1.00,0,0".formatted(i - 1)
                        : "SKU-%05d,Produto %d,Carga,%d.50,1,%d".formatted(i, i, i % 300, i % 7))
                .toArray(String[]::new);

        ReportAndLines sequential = run(1, lines);
        products.findAll().forEach(product -> products.deleteBySku(product.sku()));
        history.clear();
        ReportAndLines parallel = run(4, lines);

        assertThat(parallel.report().imported()).isEqualTo(sequential.report().imported());
        assertThat(parallel.report().errors()).isEqualTo(sequential.report().errors());
        assertThat(parallel.errorLines()).isSorted();
        assertThat(sequential.report().imported() + sequential.report().errors().size()).isEqualTo(20_000);
    }

    @Test
    @DisplayName("paralelismo menor que 1 é recusado")
    void invalidParallelism() {
        assertThatThrownBy(() -> new ProductImportService(transaction, CLOCK, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CSV: aspas protegem vírgulas, e aspas duplicadas viram uma aspa")
    void csvLine() {
        assertThat(CsvLine.split("a,\"b, c\",\"diz \"\"oi\"\"\", d ")).containsExactly("a", "b, c", "diz \"oi\"", "d");
        assertThat(CsvLine.split("")).containsExactly("");
    }
}
