package com.welf.estraigruppiutenti.util;

/**
 * Utility per l'esportazione CSV.
 */
public class CsvUtils {
    /**
     * Escapa un valore per CSV usando separatore virgola, con doppie virgolette.
     *
     * @param value valore.
     * @return valore escapato.
     */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean mustQuote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        if (!mustQuote) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
