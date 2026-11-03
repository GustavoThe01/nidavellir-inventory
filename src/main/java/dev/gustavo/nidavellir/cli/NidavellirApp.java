package dev.gustavo.nidavellir.cli;

/**
 * Ponto de entrada da aplicação de linha de comando.
 *
 * <p>Por enquanto apenas exibe o banner; o menu interativo chega no Dia 07.
 */
public final class NidavellirApp {

    static final String NAME = "Nidavellir Inventory";

    private NidavellirApp() {
    }

    public static void main(String[] args) {
        System.out.println(banner());
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
