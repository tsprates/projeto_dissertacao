package com.blogspot.tsprates.pso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Classe Fitness.
 *
 * @author thiago
 */
public class Fitness
{

    private final Connection conexao;

    private final String tabela;

    private final String colId;

    private final Map<String, List<String>> classesSaida;

    private List<String> resultado;

    private int totalSize = 0;

    private long numAvaliacao = 0;

    private List<List<String>> kpastas;

    private int k = 0;

    private String notId;

    /**
     * Construtor.
     *
     * @param conexao
     * @param colId
     * @param tabela
     * @param classesSaida
     * @param kpastas
     */
    public Fitness(Connection conexao,
            final String colId,
            final String tabela,
            final Map<String, List<String>> classesSaida)
    {
        this.conexao = conexao;
        this.colId = colId;
        this.tabela = tabela;
        this.classesSaida = classesSaida;

        for (String saida : classesSaida.keySet())
        {
            totalSize += classesSaida.get(saida).size();
        }
    }

    /**
     * Seta k-pasta.
     *
     * @param k
     */
    public void setK(int k)
    {
        this.k = k;
        notId = StringUtils.join(kpastas.get(k), ", ");
    }

    /**
     * Seta k-pasta.
     *
     * @param kpastas
     */
    public void setKPastas(List<List<String>> kpastas)
    {
        this.kpastas = kpastas;
    }

    /**
     * Calcula fitness, para treinamento.
     *
     * @param part Partícula.
     * @return Array contendo a complexidade WHERE, efetividade e acurácia.
     */
    public double[] calcular(Particula part)
    {
        // atualiza o número de avaliação
        numAvaliacao += 1;

        final double[] r = realizarCalculo(part, true);

        final double[] arr = new double[3];
        arr[0] = 1.0 / part.numWhere();
        arr[1] = r[0];
        arr[2] = r[1];

        return arr;
    }

    /**
     * Calcula fitness.
     *
     * @param part Partícula.
     * @param treinamento Treinamento ou validação.
     * @return Array contendo a complexidade WHERE, efetividade e acurácia.
     */
    public double[] calcular(Particula part, boolean treinamento)
    {
        // atualiza o número de avaliação
        numAvaliacao += 1;

        final double[] r = realizarCalculo(part, treinamento);

        final double[] arr = new double[3];
        arr[0] = 1.0 / part.numWhere();
        arr[1] = r[0];
        arr[2] = r[1];

        return arr;
    }

    /**
     * Avalia determinada partícula pela cláusula SQL WHERE.
     *
     * @param where String de uma cláusula WHERE.
     * @param treinamento Treinamento.
     * @return Retorna lista de String correspondente a uma cláusula WHERE.
     */
    private List<String> consultaSql(String where, boolean treinamento)
    {
        List<String> result = new ArrayList<>();

        String sql;

        if (treinamento == true)
        {
            sql = "SELECT " + colId + " AS id "
                    + "FROM " + tabela + " "
                    + "WHERE " + colId + " NOT IN (" + notId + ") "
                    + "AND " + where;
        }
        else
        {
            sql = "SELECT " + colId + " AS id "
                    + "FROM " + tabela + " "
                    + "WHERE " + colId + " IN (" + notId + ") "
                    + "AND " + where;
        }

        try (PreparedStatement ps = conexao.prepareStatement(sql);
                ResultSet rs = ps.executeQuery())
        {

            while (rs.next())
            {
                result.add(rs.getString("id"));
            }

            return result;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(
                    "Erro ao recupera as classes no banco de dados.", e);
        }
    }

    /**
     * Calcula a especificidade de cada partícula.
     *
     * @param part Partícula.
     * @param treinamento Treinamento.
     * @return Retorna o valor da acurácia obtido.
     */
    private double[] realizarCalculo(Particula part, boolean treinamento)
    {
        List<String> listaVerdadeiros = new ArrayList<>(classesSaida.get(part.classe()));

        if (treinamento)
        {
            listaVerdadeiros.removeAll(kpastas.get(k));
        }
        else
        {
            listaVerdadeiros.retainAll(kpastas.get(k));
        }

        resultado = consultaSql(part.whereSql(), treinamento);

        double tp = 0.0;
        for (String iter : resultado)
        {
            if (listaVerdadeiros.contains(iter))
            {
                tp += 1.0;
            }
        }

        final int resultadoSize = resultado.size();
        final int listaVerdadeirosSize = listaVerdadeiros.size();

        double fp = resultadoSize - tp;
        double fn = listaVerdadeirosSize - tp;
        double tn = totalSize - fn - fp - tp;

        double sensibilidade = tp / (tp + fn);
        double especificidade = tn / (tn + fp);
        double acuracia = (tp + tn) / (tp + tn + fp + fn);

        double efetividade = especificidade * sensibilidade;

        return new double[]
        {
            efetividade, acuracia
        };
    }

    /**
     * Retorna o número de avaliação do fitness.
     *
     * @return Número de avaliação do fitness.
     */
    public long getNumAvaliacao()
    {
        return numAvaliacao;
    }

    /**
     * Reset o número de avaliação do fitness.
     *
     * @param n Número de avaliações.
     */
    public void setNumAvaliacao(int n)
    {
        this.numAvaliacao = n;
    }

    /**
     * Fase de validação.
     *
     * @param repositorio
     * @return
     */
    public Map<String, List<Double[]>> validar(Map<String, List<Particula>> repositorio)
    {
        Map<String, List<Double[]>> map = new HashMap<>();

        for (Entry<String, List<Particula>> ent : repositorio.entrySet())
        {
            String saida = ent.getKey();

            map.put(saida, new ArrayList<Double[]>());

            List<Particula> parts = ent.getValue();
            for (Particula part : parts)
            {
                double[] calcularValidacao = calcular(part, false);
                map.get(saida).add(ArrayUtils.toObject(calcularValidacao));
            }
        }

        return map;
    }
}
