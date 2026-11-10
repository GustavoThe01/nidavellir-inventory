package dev.gustavo.nidavellir.application;

import java.util.ArrayList;
import java.util.List;

/** Divide uma linha de CSV (RFC 4180): vírgula separa campos, e um campo entre aspas pode conter vírgula e aspas duplicadas. */
final class CsvLine {

    private CsvLine() {
    }

    static List<String> split(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                fields.add(field.toString().strip());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString().strip());
        return fields;
    }
}
