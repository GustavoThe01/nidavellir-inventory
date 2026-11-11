package dev.gustavo.nidavellir.application;

import dev.gustavo.nidavellir.application.InventoryTransaction.Stores;
import dev.gustavo.nidavellir.domain.DomainException;
import dev.gustavo.nidavellir.domain.Product;
import dev.gustavo.nidavellir.domain.Quantity;
import dev.gustavo.nidavellir.domain.Sku;
import dev.gustavo.nidavellir.domain.StockMovement;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Importa produtos em lote, validando linha a linha.
 *
 * <p>São três etapas, cada uma com o modelo de execução que faz sentido para ela:
 * <ol>
 *   <li><b>Validar em paralelo.</b> As linhas são divididas em blocos, e cada bloco vira um produto do domínio
 *       numa thread do pool. Cada tarefa só lê o próprio bloco e devolve um resultado novo: não há estado
 *       compartilhado entre threads, e por isso não há condição de corrida.</li>
 *   <li><b>Conferir duplicados em sequência.</b> Saber qual linha veio "primeiro" depende da ordem; em paralelo,
 *       o resultado mudaria a cada execução. Por isso esta etapa roda numa thread só, na ordem do arquivo.</li>
 *   <li><b>Gravar numa transação só, em batch.</b> Ou o arquivo inteiro de linhas válidas entra, ou nada entra.
 *       O estoque inicial gera uma movimentação de entrada, para o saldo continuar igual ao histórico.</li>
 * </ol>
 */
public final class ProductImportService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductImportService.class);
    private static final int BLOCK_SIZE = 2_000;
    private static final int COLUMNS = 6;

    /** Uma linha do arquivo, já sem o cabeçalho, com o número original. */
    public record ImportLine(int number, String content) {}

    public record ImportError(int line, String message) {}

    public record ImportReport(int read, int imported, List<ImportError> errors, Duration elapsed) {

        public ImportReport {
            errors = List.copyOf(errors);
        }
    }

    private record Parsed(int line, Product product, int initialStock, String error) {}

    private final InventoryTransaction transaction;
    private final StockMovementFactory movements;
    private final Clock clock;
    private final int parallelism;

    /**
     * @param parallelism quantas threads validam ao mesmo tempo. Com 1, tudo roda na thread atual.
     */
    public ProductImportService(InventoryTransaction transaction, Clock clock, int parallelism) {
        this.transaction = Objects.requireNonNull(transaction, "transaction");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.movements = new StockMovementFactory(clock);
        if (parallelism < 1) {
            throw new IllegalArgumentException("O paralelismo mínimo é 1.");
        }
        this.parallelism = parallelism;
    }

    public ImportReport importLines(List<ImportLine> lines) {
        long start = System.nanoTime();
        LOG.info("Importação iniciada: {} linhas, {} thread(s)", lines.size(), parallelism);

        List<Parsed> parsed = parse(lines);
        List<ImportError> errors = new ArrayList<>();
        List<Parsed> accepted = acceptOnce(parsed, errors);

        int imported = transaction.execute((Stores stores) -> {
            List<Parsed> fresh = rejectExisting(stores, accepted, errors);
            List<Product> products = new ArrayList<>(fresh.size());
            List<StockMovement> entries = new ArrayList<>();
            for (Parsed item : fresh) {
                products.add(item.product());
                if (item.initialStock() > 0) {
                    entries.add(
                            movements.inbound(item.product(), Quantity.of(item.initialStock()), "Importação inicial"));
                }
            }
            stores.products().saveAll(products);
            stores.movements().appendAll(entries);
            return products.size();
        });

        errors.sort((left, right) -> Integer.compare(left.line(), right.line()));
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
        if (!errors.isEmpty()) {
            LOG.warn(
                    "Importação com {} linha(s) recusada(s); a primeira na linha {}",
                    errors.size(),
                    errors.getFirst().line());
        }
        LOG.info("Importação concluída: {} de {} linhas em {} ms", imported, lines.size(), elapsed.toMillis());
        return new ImportReport(lines.size(), imported, errors, elapsed);
    }

    /** Etapa 1: valida em blocos. Os resultados voltam na ordem dos blocos, então a ordem das linhas se mantém. */
    private List<Parsed> parse(List<ImportLine> lines) {
        List<List<ImportLine>> blocks = new ArrayList<>();
        for (int from = 0; from < lines.size(); from += BLOCK_SIZE) {
            blocks.add(lines.subList(from, Math.min(from + BLOCK_SIZE, lines.size())));
        }

        if (parallelism == 1 || blocks.size() == 1) {
            return blocks.stream().flatMap(block -> parseBlock(block).stream()).toList();
        }

        // Validar é trabalho de CPU: um pool do tamanho do paralelismo pedido. Virtual threads ajudariam se cada
        // linha esperasse por I/O (consultar uma API, por exemplo), o que não é o caso aqui.
        try (ExecutorService pool = Executors.newFixedThreadPool(parallelism)) {
            List<Future<List<Parsed>>> futures = blocks.stream()
                    .map(block -> pool.submit(() -> parseBlock(block)))
                    .toList();
            List<Parsed> result = new ArrayList<>(lines.size());
            for (Future<List<Parsed>> future : futures) {
                result.addAll(future.get());
            }
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("A importação foi interrompida.", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Falha inesperada ao validar as linhas.", exception.getCause());
        }
    }

    private List<Parsed> parseBlock(List<ImportLine> block) {
        List<Parsed> result = new ArrayList<>(block.size());
        for (ImportLine line : block) {
            result.add(parseLine(line));
        }
        return result;
    }

    /** sku,nome,categoria,preco,estoque_minimo,estoque_inicial */
    private Parsed parseLine(ImportLine line) {
        List<String> fields = CsvLine.split(line.content());
        if (fields.size() != COLUMNS) {
            return failure(line, "esperava %d colunas, encontrei %d".formatted(COLUMNS, fields.size()));
        }
        try {
            Product product = Product.builder()
                    .sku(fields.get(0))
                    .name(fields.get(1))
                    .category(fields.get(2))
                    .price(fields.get(3).replace(',', '.'))
                    .minimumStock(number(fields.get(4), "estoque mínimo"))
                    .build();
            int initialStock = number(fields.get(5), "estoque inicial");
            if (initialStock < 0) {
                return failure(line, "o estoque inicial não pode ser negativo");
            }
            return new Parsed(line.number(), product, initialStock, null);
        } catch (DomainException exception) {
            return failure(line, exception.getMessage());
        } catch (NumberFormatException exception) {
            return failure(line, exception.getMessage());
        }
    }

    private static int number(String value, String field) {
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException exception) {
            throw new NumberFormatException("%s deve ser um número inteiro: '%s'".formatted(field, value));
        }
    }

    private static Parsed failure(ImportLine line, String message) {
        return new Parsed(line.number(), null, 0, message);
    }

    /** Etapa 2: em sequência, na ordem do arquivo, para o resultado ser sempre o mesmo. */
    private static List<Parsed> acceptOnce(List<Parsed> parsed, List<ImportError> errors) {
        Map<Sku, Integer> firstLine = new HashMap<>();
        List<Parsed> accepted = new ArrayList<>();
        for (Parsed item : parsed) {
            if (item.error() != null) {
                errors.add(new ImportError(item.line(), item.error()));
                continue;
            }
            Integer first = firstLine.putIfAbsent(item.product().sku(), item.line());
            if (first != null) {
                errors.add(new ImportError(
                        item.line(),
                        "SKU %s repetido no arquivo (primeiro na linha %d)"
                                .formatted(item.product().sku(), first)));
                continue;
            }
            accepted.add(item);
        }
        return accepted;
    }

    /** SKU que já existe no banco não é sobrescrito: a importação só cadastra produtos novos. */
    private static List<Parsed> rejectExisting(Stores stores, List<Parsed> accepted, List<ImportError> errors) {
        var existing = stores.products().findAll().stream().map(Product::sku).collect(Collectors.toSet());
        List<Parsed> fresh = new ArrayList<>(accepted.size());
        for (Parsed item : accepted) {
            if (existing.contains(item.product().sku())) {
                errors.add(new ImportError(
                        item.line(),
                        "SKU %s já está cadastrado".formatted(item.product().sku())));
            } else {
                fresh.add(item);
            }
        }
        return fresh;
    }
}
