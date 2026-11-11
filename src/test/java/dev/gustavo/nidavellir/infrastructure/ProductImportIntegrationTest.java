package dev.gustavo.nidavellir.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.gustavo.nidavellir.application.ProductImportService;
import dev.gustavo.nidavellir.application.ProductImportService.ImportError;
import dev.gustavo.nidavellir.application.ProductImportService.ImportReport;
import dev.gustavo.nidavellir.domain.InvalidValueException;
import dev.gustavo.nidavellir.domain.Sku;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Importação com arquivo e SQLite de verdade")
class ProductImportIntegrationTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path directory;

    @Test
    @DisplayName("lê o CSV, grava em batch numa transação e gera o relatório de erros ao lado do arquivo")
    void importsFileIntoSqlite() throws IOException {
        Path file = directory.resolve("produtos.csv");
        String body = IntStream.rangeClosed(1, 5_000)
                .mapToObj(i -> "SKU-%04d,Produto %d,Carga,10.00,0,%d".formatted(i, i, i % 3))
                .collect(Collectors.joining("\n"));
        Files.writeString(
                file,
                "﻿" + CsvProductFile.HEADER + "\n" + body + "\n\nRUIM,x,Carga,1.00,0,0\n",
                StandardCharsets.UTF_8);
        ConnectionFactory connections = new ConnectionFactory(directory.resolve("estoque.db"));
        connections.initializeSchema();
        ProductImportService importer = new ProductImportService(new JdbcInventoryTransaction(connections), CLOCK, 4);

        ImportReport report = importer.importLines(CsvProductFile.read(file));

        JdbcProductRepository products = new JdbcProductRepository(connections);
        assertThat(report.imported()).isEqualTo(5_000);
        assertThat(products.findAll()).hasSize(5_000);
        assertThat(products.findBySku(Sku.of("SKU-0002")).orElseThrow().stock().value())
                .isEqualTo(2);
        assertThat(new JdbcStockMovementRepository(connections).findBySku(Sku.of("SKU-0002")))
                .hasSize(1);
        // A linha em branco conta na numeração: a linha ruim é a 5003 do arquivo.
        assertThat(report.errors())
                .singleElement()
                .extracting(ImportError::line)
                .isEqualTo(5_003);

        Path errors = CsvProductFile.writeErrors(file, report.errors());
        assertThat(errors.getFileName().toString()).isEqualTo("produtos.erros.csv");
        assertThat(Files.readAllLines(errors)).first().isEqualTo("linha,motivo");
    }

    @Test
    @DisplayName("cabeçalho errado é recusado antes de importar qualquer linha")
    void rejectsWrongHeader() throws IOException {
        Path file = Files.writeString(directory.resolve("ruim.csv"), "codigo,nome\nMJ-001,Martelo\n");

        assertThatThrownBy(() -> CsvProductFile.read(file))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining(CsvProductFile.HEADER);
    }

    @Test
    @DisplayName("arquivo sem nenhuma linha válida não grava nada")
    void emptyFile() throws IOException {
        Path file = Files.writeString(directory.resolve("vazio.csv"), CsvProductFile.HEADER + "\n");
        ConnectionFactory connections = new ConnectionFactory(directory.resolve("estoque.db"));
        connections.initializeSchema();

        ImportReport report = new ProductImportService(new JdbcInventoryTransaction(connections), CLOCK, 2)
                .importLines(CsvProductFile.read(file));

        assertThat(report.read()).isZero();
        assertThat(new JdbcProductRepository(connections).findAll()).isEqualTo(List.of());
    }
}
