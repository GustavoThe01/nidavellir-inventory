package dev.gustavo.nidavellir.cli;

import dev.gustavo.nidavellir.application.CatalogService;
import dev.gustavo.nidavellir.application.InventoryQueryService;
import dev.gustavo.nidavellir.application.InventoryReports;
import dev.gustavo.nidavellir.application.InventoryTransaction;
import dev.gustavo.nidavellir.application.ProductImportService;
import dev.gustavo.nidavellir.application.ProductRepository;
import dev.gustavo.nidavellir.application.StockLevelListener;
import dev.gustavo.nidavellir.application.StockService;
import dev.gustavo.nidavellir.infrastructure.ConnectionFactory;
import dev.gustavo.nidavellir.infrastructure.JdbcInventoryTransaction;
import dev.gustavo.nidavellir.infrastructure.JdbcProductRepository;
import dev.gustavo.nidavellir.infrastructure.ReportExporter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ponto de entrada: monta as dependências (composition root) e abre o menu.
 *
 * <p>Uso: {@code java -cp ... NidavellirApp [arquivo.db]}. Sem argumento, usa {@code nidavellir.db}
 * na pasta atual.
 */
public final class NidavellirApp {

    static final String NAME = "Nidavellir Inventory";

    private static final Logger LOG = LoggerFactory.getLogger(NidavellirApp.class);

    private NidavellirApp() {
    }

    public static void main(String[] args) {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        ConnectionFactory connections = new ConnectionFactory(Path.of(args.length > 0 ? args[0] : "nidavellir.db"));
        connections.initializeSchema();

        out.println(banner());
        menu(new Terminal(in, out), new JdbcProductRepository(connections), new JdbcInventoryTransaction(connections),
                Clock.systemDefaultZone(), exception -> LOG.error("Erro na operação do menu", exception)).run();
    }

    /** Monta o menu com as dependências recebidas. Os testes chamam este método com repositórios em memória. */
    static Menu menu(Terminal terminal, ProductRepository products, InventoryTransaction transaction, Clock clock,
                     java.util.function.Consumer<Exception> errorLog) {
        StockLevelListener alert = product -> terminal.println(
                "⚠ %s (%s) ficou abaixo do mínimo: %s de %s.".formatted(
                        product.name(), product.sku(), product.stock(), product.minimumStock()));
        StockService stock = new StockService(transaction, clock, List.of(alert));
        InventoryQueryService queries = new InventoryQueryService(products);

        return new Menu(terminal, errorLog)
                .register("1", new Commands.ListProducts(products))
                .register("2", new Commands.RegisterProduct(new CatalogService(products)))
                .register("3", new Commands.ReceiveStock(stock))
                .register("4", new Commands.DispatchStock(stock))
                .register("5", new Commands.AdjustStock(stock))
                .register("6", new Commands.ShowHistory(stock))
                .register("7", new Commands.BelowMinimum(queries))
                .register("8", new Commands.StockValue(queries))
                .register("9", new Commands.CategoryReportCommand(new InventoryReports(products)))
                .register("10", new Commands.ExportReport(new InventoryReports(products), new ReportExporter()))
                .register("11", new Commands.ImportProducts(
                        new ProductImportService(transaction, clock, Runtime.getRuntime().availableProcessors())))
                .register("0", new Commands.Exit());
    }

    static String banner() {
        return """
                ==========================================
                  %s
                  A forja dos anões guarda o seu estoque
                ==========================================
                """.formatted(NAME);
    }
}
