package dev.gustavo.nidavellir.cli;

import dev.gustavo.nidavellir.application.CatalogService;
import dev.gustavo.nidavellir.application.InventoryQueryService;
import dev.gustavo.nidavellir.application.InventoryReports;
import dev.gustavo.nidavellir.application.InventoryReports.CategoryReport;
import dev.gustavo.nidavellir.application.InventoryReports.MovementSummary;
import dev.gustavo.nidavellir.application.ProductImportService;
import dev.gustavo.nidavellir.application.ProductImportService.ImportReport;
import dev.gustavo.nidavellir.application.ProductRepository;
import dev.gustavo.nidavellir.application.StockService;
import dev.gustavo.nidavellir.domain.Category;
import dev.gustavo.nidavellir.domain.Money;
import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.Quantity;
import dev.gustavo.nidavellir.domain.Sku;
import dev.gustavo.nidavellir.domain.StockMovement;
import dev.gustavo.nidavellir.infrastructure.CsvProductFile;
import dev.gustavo.nidavellir.infrastructure.ReportExporter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * As opções do menu. Cada uma lê o que precisa pelo {@link Terminal}, chama a camada de aplicação e mostra
 * o resultado. Nenhuma regra de negócio mora aqui: quem decide se a saída é válida é o domínio.
 */
public final class Commands {

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT).withZone(ZoneId.of("America/Fortaleza"));
    private static final String ROW = "%-10s %-28s %-14s %12s %8s %8s";

    private Commands() {}

    public record ListProducts(ProductRepository products) implements Command {

        @Override
        public String description() {
            return "Listar produtos";
        }

        @Override
        public void execute(Terminal terminal) {
            printTable(terminal, products.findAll(), "Nenhum produto cadastrado.");
        }
    }

    public record RegisterProduct(CatalogService catalog) implements Command {

        @Override
        public String description() {
            return "Cadastrar produto";
        }

        @Override
        public void execute(Terminal terminal) {
            Sku sku = terminal.sku("SKU (ex.: MJ-001): ");
            String name = terminal.text("Nome: ");
            Category category = terminal.ask("Categoria: ", Category::of);
            Money price = terminal.money("Preço (ex.: 19,90): ");
            int minimum = terminal.integer("Estoque mínimo: ", 0);

            Product product = catalog.register(Product.create(sku, name, category, price, Quantity.of(minimum)));
            terminal.println("✓ Produto %s cadastrado, com estoque zero.".formatted(product.sku()));
        }
    }

    public record ReceiveStock(StockService stock) implements Command {

        @Override
        public String description() {
            return "Registrar entrada";
        }

        @Override
        public void execute(Terminal terminal) {
            StockMovement movement = stock.receive(
                    terminal.sku("SKU: "), Quantity.of(terminal.integer("Quantidade: ", 1)), terminal.text("Motivo: "));
            terminal.println("✓ Entrada registrada. Estoque atual: " + movement.stockAfter());
        }
    }

    public record DispatchStock(StockService stock) implements Command {

        @Override
        public String description() {
            return "Registrar saída";
        }

        @Override
        public void execute(Terminal terminal) {
            StockMovement movement = stock.dispatch(
                    terminal.sku("SKU: "), Quantity.of(terminal.integer("Quantidade: ", 1)), terminal.text("Motivo: "));
            terminal.println("✓ Saída registrada. Estoque atual: " + movement.stockAfter());
        }
    }

    public record AdjustStock(StockService stock) implements Command {

        @Override
        public String description() {
            return "Ajustar após contagem física";
        }

        @Override
        public void execute(Terminal terminal) {
            StockMovement movement = stock.adjust(
                    terminal.sku("SKU: "),
                    Quantity.of(terminal.integer("Quantidade contada: ", 0)),
                    terminal.text("Motivo: "));
            terminal.println("✓ Estoque ajustado para " + movement.stockAfter() + ".");
        }
    }

    public record ShowHistory(StockService stock) implements Command {

        @Override
        public String description() {
            return "Histórico de um produto";
        }

        @Override
        public void execute(Terminal terminal) {
            List<StockMovement> history = stock.history(terminal.sku("SKU: "));
            if (history.isEmpty()) {
                terminal.println("Nenhuma movimentação para este produto.");
                return;
            }
            terminal.println("%-16s %-10s %8s %8s  %s".formatted("Quando", "Tipo", "Qtd.", "Saldo", "Motivo"));
            history.forEach(movement -> terminal.println("%-16s %-10s %8s %8s  %s"
                    .formatted(
                            WHEN.format(movement.occurredAt()),
                            label(movement),
                            movement.quantity(),
                            movement.stockAfter(),
                            movement.reason())));

            MovementSummary summary = InventoryReports.summarize(history);
            terminal.println("Total: %d unidade(s) de entrada, %d de saída e %d ajuste(s)."
                    .formatted(summary.inboundUnits(), summary.outboundUnits(), summary.adjustments()));
        }

        private static String label(StockMovement movement) {
            return switch (movement) {
                case StockMovement.Inbound inbound -> "Entrada";
                case StockMovement.Outbound outbound -> "Saída";
                case StockMovement.Adjustment adjustment -> "Ajuste";
            };
        }
    }

    public record BelowMinimum(InventoryQueryService queries) implements Command {

        @Override
        public String description() {
            return "Produtos abaixo do mínimo";
        }

        @Override
        public void execute(Terminal terminal) {
            printTable(terminal, queries.findBelowMinimum(), "Nenhum produto abaixo do mínimo.");
        }
    }

    public record StockValue(InventoryQueryService queries) implements Command {

        @Override
        public String description() {
            return "Valor do estoque por categoria";
        }

        @Override
        public void execute(Terminal terminal) {
            Map<String, Money> byCategory = queries.stockValueByCategory();
            byCategory.forEach((category, value) -> terminal.println("%-20s %14s".formatted(category, value)));
            terminal.println("%-20s %14s".formatted("TOTAL", queries.totalStockValue()));
        }
    }

    public record CategoryReportCommand(InventoryReports reports) implements Command {

        @Override
        public String description() {
            return "Relatório por categoria";
        }

        @Override
        public void execute(Terminal terminal) {
            List<CategoryReport> lines = reports.byCategory();
            if (lines.isEmpty()) {
                terminal.println("Nenhum produto cadastrado.");
                return;
            }
            String row = "%-16s %8s %8s %6s %6s %8s %14s %8s";
            terminal.println(
                    row.formatted("Categoria", "Produtos", "Unidades", "Mín.", "Máx.", "Média", "Valor", "Repor"));
            lines.forEach(line -> terminal.println(row.formatted(
                    line.category(),
                    line.products(),
                    line.units(),
                    line.minimumStock(),
                    line.maximumStock(),
                    "%.1f".formatted(line.averageStock()),
                    line.stockValue(),
                    line.belowMinimum())));
        }
    }

    public record ExportReport(InventoryReports reports, ReportExporter exporter) implements Command {

        @Override
        public String description() {
            return "Exportar relatório (CSV e JSON)";
        }

        @Override
        public void execute(Terminal terminal) {
            Path folder = terminal.ask(
                    "Pasta de destino (Enter para \"relatorios\"): ",
                    value -> Path.of(value.isEmpty() ? "relatorios" : value));
            List<CategoryReport> lines = reports.byCategory();
            Path csv = exporter.exportCsv(lines, folder.resolve("estoque-por-categoria.csv"));
            Path json = exporter.exportJson(lines, folder.resolve("estoque-por-categoria.json"));
            terminal.println("✓ Relatório exportado:");
            terminal.println("  " + csv.toAbsolutePath());
            terminal.println("  " + json.toAbsolutePath());
        }
    }

    public record ImportProducts(ProductImportService importer) implements Command {

        private static final int ERRORS_ON_SCREEN = 10;

        @Override
        public String description() {
            return "Importar produtos de um CSV";
        }

        @Override
        public void execute(Terminal terminal) {
            terminal.println("Formato: " + CsvProductFile.HEADER);
            Path file = terminal.ask("Arquivo: ", value -> {
                Path path = Path.of(value);
                if (!Files.isRegularFile(path)) {
                    throw new IllegalArgumentException("Arquivo não encontrado: " + path.toAbsolutePath());
                }
                return path;
            });

            ImportReport report = importer.importLines(CsvProductFile.read(file));
            terminal.println("✓ %d de %d linha(s) importada(s) em %d ms."
                    .formatted(
                            report.imported(), report.read(), report.elapsed().toMillis()));
            if (report.errors().isEmpty()) {
                return;
            }

            terminal.println(
                    "✗ %d linha(s) recusada(s):".formatted(report.errors().size()));
            report.errors().stream()
                    .limit(ERRORS_ON_SCREEN)
                    .forEach(error -> terminal.println("  linha %d: %s".formatted(error.line(), error.message())));
            if (report.errors().size() > ERRORS_ON_SCREEN) {
                terminal.println("  ... e mais %d.".formatted(report.errors().size() - ERRORS_ON_SCREEN));
            }
            terminal.println("Relatório completo: "
                    + CsvProductFile.writeErrors(file, report.errors()).toAbsolutePath());
        }
    }

    public record Exit() implements Command {

        @Override
        public String description() {
            return "Sair";
        }

        @Override
        public void execute(Terminal terminal) {
            terminal.println("Até a próxima forja!");
        }

        @Override
        public boolean exits() {
            return true;
        }
    }

    private static void printTable(Terminal terminal, List<Product> products, String empty) {
        if (products.isEmpty()) {
            terminal.println(empty);
            return;
        }
        terminal.println(ROW.formatted("SKU", "Nome", "Categoria", "Preço", "Estoque", "Mínimo"));
        for (Product product : products) {
            String flag = product.isBelowMinimum() ? " ⚠" : "";
            terminal.println(ROW.formatted(
                            product.sku(),
                            product.name(),
                            product.category(),
                            product.price(),
                            product.stock(),
                            product.minimumStock())
                    + flag);
        }
    }
}
