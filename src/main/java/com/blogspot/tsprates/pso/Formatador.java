package com.blogspot.tsprates.pso;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

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
        this.fmt = new DecimalFormat("0.0000", symbols);
    }

    /**
     * Formata valor numérico.
     *
     * @param d
     * @return
     */
    public String formatar(double d)
    {
        return fmt.format(d);
    }
}
