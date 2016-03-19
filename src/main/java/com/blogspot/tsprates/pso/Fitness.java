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

    private final Map<String, Set<String>> classeSaidas;

    private List<String> resultado;

    private int total = 0;

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
        this.classeSaidas = classeSaidas;

        for (String k : classeSaidas.keySet())
        {
            total += classeSaidas.get(k).size();
        }
    }

    /**
     * Calcula fitness.
     */
    @Override
    public double[] calcula(Particula p)
    {
        double[] result = new double[2];
        result[0] = calculaEspecifidade(p);
        result[1] = 1.0 / p.numWhere();
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
    private double calculaEspecifidade(Particula p)
    {
        Set<String> lista = classeSaidas.get(p.classe());

        resultado = consultaSql(p.toWhereSql());

        int tp = 0;
        for (String iter : resultado)
        {
            if (lista.contains(iter))
            {
                tp += 1;
            }
        }

        int fp = resultado.size() - tp;

        int tn = total - fp;
        int fn = total - tp;

        double sensibilidade = (double) tp / (tp + fn);
        double especificidade = (double) tn / (tn + fp);

        return sensibilidade * especificidade;
//	return (double) (tp + tn) / total;
    }
}
