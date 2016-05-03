package com.blogspot.tsprates.pso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private final Map<String, Set<String>> classesSaida;

    private List<String> resultado;

    private int totalSize = 0;
    
    private long numAvaliacao = 0;

    /**
     * Construtor.
     *
     * @param conexao
     * @param colId
     * @param tabela
     * @param classesSaida
     */
    public Fitness(Connection conexao, final String colId, final String tabela,
            final Map<String, Set<String>> classesSaida)
    {
        this.conexao = conexao;
        this.colId = colId;
        this.tabela = tabela;
        this.classesSaida = classesSaida;

        for (String k : classesSaida.keySet())
        {
            totalSize += classesSaida.get(k).size();
        }
    }

    /**
     * Calcula fitness.
     * 
     * @param p Partículas.
     * @return Array contendo complexidade WHERE, efetividade e acurácia.
     */
    public double[] calcular(Particula p)
    {
        final double[] r = realizarCalc(p);
        final double[] result = new double[3];
        result[0] = 1.0 / p.numWhere();
        result[1] = r[0];
        result[2] = r[1];
        return result;
    }

    /**
     * Recupera classe para determinada cláusula SQL WHERE.
     *
     * @param where String de uma cláusula WHERE.
     * @return Retorna lista de String correspondente a uma cláusula WHERE.
     */
    private List<String> consultaSql(String where)
    {
        List<String> l = new ArrayList<>();
        String sql = "SELECT " + colId + " AS id FROM " + tabela + " "
                + "WHERE " + where;

        try (PreparedStatement ps = conexao.prepareStatement(sql);
                ResultSet rs = ps.executeQuery())
        {

            while (rs.next())
            {
                l.add(rs.getString("id"));
            }

            return l;
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
     * @param p Partícula.
     * @return Retorna o valor da acurácia obtido.
     */
    private double[] realizarCalc(Particula p)
    {
        Set<String> listaVerdadeiros = classesSaida.get(p.classe());

        resultado = consultaSql(p.whereSql());

        double tp = 0.0;
        for (String iter : resultado)
        {
            if (listaVerdadeiros.contains(iter))
            {
                tp += 1.0;
            }
        }

        final int resultadoSize = resultado.size();
        final int listaSize = listaVerdadeiros.size();

        double fp = resultadoSize - tp;
        double fn = listaSize - tp;
        double tn = totalSize - fn - fp - tp;

        double sensibilidade = tp / (tp + fn);
        double especificidade = tn / (tn + fp);
        double acuracia = (tp + tn) / (tp + tn + fp + fn);

        double efetividade = especificidade * sensibilidade;
        
        // atualiza o número de avaliação
        numAvaliacao += 1;

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
}
