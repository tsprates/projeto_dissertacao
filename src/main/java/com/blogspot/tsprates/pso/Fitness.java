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
public class Fitness implements InterfaceFitness
{

    private final Connection conexao;

    private final String tabela;

    private final String colId;

    private final Map<String, Set<String>> classeSaida;

    private List<String> resultado;

    private int totalSize = 0;

    /**
     * Construtor.
     *
     * @param conexao
     * @param colId
     * @param tabela
     * @param classeSaidas
     */
    public Fitness(Connection conexao, final String colId, final String tabela,
            final Map<String, Set<String>> classeSaidas)
    {
        this.conexao = conexao;
        this.colId = colId;
        this.tabela = tabela;
        this.classeSaida = classeSaidas;

        for (String k : classeSaidas.keySet())
        {
            totalSize += classeSaidas.get(k).size();
        }
    }

    /**
     * Calcula fitness.
     */
    @Override
    public double[] calcula(Particula p)
    {
        final double[] r = calc(p);
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
    private double[] calc(Particula p)
    {
        Set<String> listaVerdadeiros = classeSaida.get(p.classe());

        resultado = consultaSql(p.whereSql());

        int tp = 0;
        for (String iter : resultado)
        {
            if (listaVerdadeiros.contains(iter))
            {
                tp += 1;
            }
        }
        
        final int resultadoSize = resultado.size();
        final int listaSize = listaVerdadeiros.size();

        int fp = resultadoSize - tp;
        int fn = listaSize - tp;
        int tn = totalSize - fn - fp - tp;

        double sensibilidade = (double) tp / (tp + fn);
        double especificidade = (double) tn / (tn + fp);
        double acuracia = (double) (tp + tn) / (tp + tn + fp + fn);

        double efetividade = especificidade * sensibilidade;
        
        return new double[]{efetividade, acuracia};
    }
}
