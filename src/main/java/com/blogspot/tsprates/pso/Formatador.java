package com.blogspot.tsprates.pso;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Classe para formatação de valores numéricos.
 *
 * @author thiago
 */
public class Formatador
{

    private final DecimalFormat fmt;

    /**
     * Construtor.
     */
    public Formatador()
    {
        // Decimal formatter
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator('.');
        this.fmt = new DecimalFormat("0.000000", symbols);
    }

    /**
     * Formata valor numérico.
     *
     * @param d Valor numérico.
     * @return String formatada do valor numérico.
     */
    public String formatar(double d)
    {
        return fmt.format(d);
    }
}
