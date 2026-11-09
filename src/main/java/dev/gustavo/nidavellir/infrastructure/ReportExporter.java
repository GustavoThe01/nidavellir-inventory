package dev.gustavo.nidavellir.infrastructure;

import dev.gustavo.nidavellir.application.InventoryReports.CategoryReport;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Exporta o relatório por categoria em CSV e JSON, sem biblioteca externa.
 *
 * <p>O arquivo é escrito primeiro num temporário e só depois movido para o nome final: quem abrir o arquivo
 * nunca vê um relatório pela metade. Números usam ponto decimal ({@link Locale#ROOT}), para o arquivo ser
 * lido por qualquer ferramenta, independente do idioma da máquina.
 */
public final class ReportExporter {

    private static final String CSV_HEADER =
            "categoria,produtos,unidades,estoque_minimo,estoque_maximo,estoque_medio,valor_em_estoque,abaixo_do_minimo";

    public Path exportCsv(List<CategoryReport> reports, Path file) {
        String rows = reports.stream()
                .map(report -> String.join(",",
                        csv(report.category()),
                        Long.toString(report.products()),
                        Long.toString(report.units()),
                        Integer.toString(report.minimumStock()),
                        Integer.toString(report.maximumStock()),
                        String.format(Locale.ROOT, "%.2f", report.averageStock()),
                        report.stockValue().amount().toPlainString(),
                        Long.toString(report.belowMinimum())))
                .collect(Collectors.joining("\n"));
        return write(file, CSV_HEADER + "\n" + rows + (rows.isEmpty() ? "" : "\n"));
    }

    public Path exportJson(List<CategoryReport> reports, Path file) {
        String items = reports.stream()
                .map(report -> """
                          {
                            "categoria": %s,
                            "produtos": %d,
                            "unidades": %d,
                            "estoqueMinimo": %d,
                            "estoqueMaximo": %d,
                            "estoqueMedio": %s,
                            "valorEmEstoque": %s,
                            "abaixoDoMinimo": %d
                          }""".formatted(
                        json(report.category()), report.products(), report.units(), report.minimumStock(),
                        report.maximumStock(), String.format(Locale.ROOT, "%.2f", report.averageStock()),
                        report.stockValue().amount().toPlainString(), report.belowMinimum()))
                .collect(Collectors.joining(",\n"));
        return write(file, reports.isEmpty() ? "[]\n" : "[\n" + items + "\n]\n");
    }

    /** RFC 4180: campo com vírgula, aspas ou quebra de linha vai entre aspas, e aspas internas são duplicadas. */
    static String csv(String value) {
        return value.contains(",") || value.contains("\"") || value.contains("\n")
                ? "\"" + value.replace("\"", "\"\"") + "\""
                : value;
    }

    static String json(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.append('"').toString();
    }

    private static Path write(Path file, String content) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            Path temporary = Files.createTempFile(parent, ".relatorio-", ".tmp");
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                return Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                return Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new PersistenceException("Não consegui gravar o relatório em " + file + ".", exception);
        }
    }
}
