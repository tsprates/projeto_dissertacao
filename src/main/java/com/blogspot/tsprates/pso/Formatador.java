package com.blogspot.tsprates.pso;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Classe para formatação de dados.
 *
 * @author thiago
 */
public class Formatador
{

    private final DecimalFormat fmt;

    private static final Locale LOC = Locale.ROOT;

    public static final String TAB_LINHA = "%-15s %-10s %-10s %-10s %s\n";

    public static final String TAB_CABECALHO = String.format(TAB_LINHA,
            "Classe", "Compl.", "Efet.", "Acur.", "Regra");

    /**
     * Construtor.
     */
    public Formatador()
    {
        // Decimal formatter
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(LOC);
        symbols.setDecimalSeparator('.');
        this.fmt = new DecimalFormat("0.000000", symbols);
    }

    /**
     * Formata um valor numérico, para exibição em tabela de resultados.
     *
     * @param valor Valor numérico.
     * @return String formatada referente ao valor numérico.
     */
    public String formatar(final double valor)
    {
        return fmt.format(valor);
    }

    /**
     * Formata o tempo decorrido em segundos.
     *
     * @param tempoInicial Tempo inicial em nanosegundos.
     * @param tempoFinal Tempo final em nanosegundos.
     * @return
     */
    public static String formatarTempoDecorrido(final long tempoInicial,
            final long tempoFinal)
    {
        final double tempoDecorrido = (tempoFinal - tempoInicial) / 1000000000.0;
        return String.format(LOC, "%.2f segs", tempoDecorrido);
    }

    /**
     * Formata um valor numérico para uma cláusula SQL WHERE, sendo composto por
     * 3 casas decimais.
     *
     * @param valor Valor numérico.
     * @return String formatada do valor numérico.
     */
    public static String formatarValorNumericoWhere(final double valor)
    {
        return String.format(LOC, "%.3f", valor);
    }

    /**
     * Devolve uma string referente à condição de uma cláusula SQL WHERE.
     *
     * @param atributo Atributo.
     * @param operador Operador.
     * @param valor Valor númerico ou outro atributo.
     * @return
     */
    public static String formatarCondicaoWhere(final String atributo,
            final String operador, final String valor)
    {
        return String.format(LOC, "%s %s %s", atributo, operador, valor);
    }

    /**
     * Define o tamanho da string referente a classe ao ser mostrado na tabela
     * de resultados.
     *
     * @param classe Nicho do enxame.
     * @return String formatada da classe.
     */
    public static String formatarClasse(final String classe)
    {
        String cl; // classe

        if (classe.length() > 10)
        {
            cl = classe.substring(0, 10);
        }
        else
        {
            cl = classe;
        }

        return cl;
    }
}
