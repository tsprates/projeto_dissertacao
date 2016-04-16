package com.blogspot.tsprates.pso;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Formatador
{
    private DecimalFormat format;
    
    /**
     * 
     */
    public Formatador()
    {
        // Decimal formatter
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator(',');
        this.format = new DecimalFormat("0.000", symbols);
    }
    
    /**
     * Formata valor numérico.
     *
     * @param d
     * @return
     */
    public String format(double d)
    {
        return format.format(d);
    }
}
