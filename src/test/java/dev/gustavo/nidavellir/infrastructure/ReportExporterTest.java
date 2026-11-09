package dev.gustavo.nidavellir.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.gustavo.nidavellir.application.InventoryReports.CategoryReport;
import dev.gustavo.nidavellir.domain.InvalidValueException;
import dev.gustavo.nidavellir.domain.Money;
import dev.gustavo.nidavellir.domain.StockMovement.Adjustment;
import dev.gustavo.nidavellir.domain.Quantity;
import dev.gustavo.nidavellir.domain.Sku;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Exportação do relatório")
class ReportExporterTest {

    private static final List<CategoryReport> REPORT = List.of(
            new CategoryReport("Forja", 2, 11, 2, 9, 5.5, Money.of("20.90"), 1),
            new CategoryReport("Runas, \"antigas\"", 1, 3, 3, 3, 3.0, Money.of("99.99"), 0));

    private final ReportExporter exporter = new ReportExporter();

    @TempDir
    private Path folder;

    @Test
    @DisplayName("CSV com cabeçalho, ponto decimal e campo com vírgula e aspas entre aspas")
    void csv() throws IOException {
        Path file = exporter.exportCsv(REPORT, folder.resolve("sub/estoque.csv"));

        assertThat(Files.readAllLines(file, StandardCharsets.UTF_8)).containsExactly(
                "categoria,produtos,unidades,estoque_minimo,estoque_maximo,estoque_medio,valor_em_estoque,abaixo_do_minimo",
                "Forja,2,11,2,9,5.50,20.90,1",
                "\"Runas, \"\"antigas\"\"\",1,3,3,3,3.00,99.99,0");
    }

    @Test
    @DisplayName("JSON com os campos e as aspas escapadas")
    void json() throws IOException {
        String json = Files.readString(exporter.exportJson(REPORT, folder.resolve("estoque.json")), StandardCharsets.UTF_8);

        assertThat(json)
                .startsWith("[")
                .contains("\"categoria\": \"Forja\"")
                .contains("\"categoria\": \"Runas, \\\"antigas\\\"\"")
                .contains("\"valorEmEstoque\": 20.90")
                .contains("\"estoqueMedio\": 5.50")
                .endsWith("]\n");
    }

    @Test
    @DisplayName("relatório vazio vira um CSV só com cabeçalho e um JSON com lista vazia")
    void empty() throws IOException {
        assertThat(Files.readAllLines(exporter.exportCsv(List.of(), folder.resolve("v.csv")))).hasSize(1);
        assertThat(Files.readString(exporter.exportJson(List.of(), folder.resolve("v.json")))).isEqualTo("[]\n");
    }

    @Test
    @DisplayName("exportar de novo substitui o arquivo e não deixa temporários para trás")
    void replacesAtomically() throws IOException {
        Path file = folder.resolve("estoque.csv");
        exporter.exportCsv(REPORT, file);
        exporter.exportCsv(REPORT.subList(0, 1), file);

        assertThat(Files.readAllLines(file)).hasSize(2);
        try (var files = Files.list(folder)) {
            assertThat(files).containsExactly(file);
        }
    }

    @Test
    @DisplayName("caracteres de controle viram escape no JSON")
    void escapesControlCharacters() {
        assertThat(ReportExporter.json("a\tb\nc\u0001")).isEqualTo("\"a\\tb\\nc\\u0001\"");
    }

    @Test
    @DisplayName("ajuste com saldo diferente da contagem é recusado")
    void adjustmentInvariant() {
        assertThatThrownBy(() -> new Adjustment(Sku.of("MJ-001"), Quantity.of(5), Quantity.of(4), "Contagem",
                Instant.parse("2026-11-09T12:00:00Z")))
                .isInstanceOf(InvalidValueException.class);
    }
}
