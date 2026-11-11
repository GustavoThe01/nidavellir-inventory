package dev.gustavo.nidavellir.infrastructure;

import dev.gustavo.nidavellir.application.ProductImportService.ImportError;
import dev.gustavo.nidavellir.application.ProductImportService.ImportLine;
import dev.gustavo.nidavellir.domain.InvalidValueException;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Lê o arquivo de importação e grava o relatório de linhas recusadas, sempre em UTF-8. */
public final class CsvProductFile {

    public static final String HEADER = "sku,nome,categoria,preco,estoque_minimo,estoque_inicial";

    private CsvProductFile() {}

    /** Linhas do arquivo, sem o cabeçalho e sem as linhas em branco. O número é o da linha no arquivo. */
    public static List<ImportLine> read(Path file) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null || !header.strip().replace("\uFEFF", "").equalsIgnoreCase(HEADER)) {
                throw new InvalidValueException("Cabeçalho inválido. A primeira linha deve ser: " + HEADER);
            }
            List<ImportLine> lines = new ArrayList<>();
            int number = 1;
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                number++;
                if (!line.isBlank()) {
                    lines.add(new ImportLine(number, line));
                }
            }
            return lines;
        } catch (IOException exception) {
            throw new PersistenceException("Não consegui ler o arquivo " + file + ".", exception);
        }
    }

    /** Grava ao lado do arquivo importado um CSV com a linha e o motivo de cada recusa. */
    public static Path writeErrors(Path importedFile, List<ImportError> errors) {
        String name = importedFile.getFileName().toString().replaceFirst("\\.csv$", "");
        Path report = importedFile.resolveSibling(name + ".erros.csv");
        String content = "linha,motivo\n"
                + errors.stream()
                        .map(error -> error.line() + "," + ReportExporter.csv(error.message()))
                        .collect(Collectors.joining("\n"))
                + "\n";
        try {
            return Files.writeString(report, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new PersistenceException("Não consegui gravar o relatório de erros em " + report + ".", exception);
        }
    }
}
