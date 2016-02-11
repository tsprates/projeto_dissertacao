package com.blogspot.tsprates.pso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.RandomUtils;

/**
 * Particles Swarm Optimization (PSO).
 *
 * @author thiago
 *
 */
public class PSO
{

    private final Connection conexao;

    private final static String[] LISTA_OPERADORES = { "=", "!=", ">", ">=",
            "<", ">=" };

    private final List<Particula> particulas = new ArrayList<Particula>();

    private final List<String> tipoSaidas = new ArrayList<String>();

    private final Map<String, Set<Integer>> classeSaidas = new HashMap<String, Set<Integer>>();

    private final String tabela;

    private final String colSaida, colCod;

    private final int numPop;

    private String[] colunas;

    private double[] max, min;

    private final Random sorteio = new Random();

    /**
     * Construtor.
     *
     * @param c
     * @param p
     */
    public PSO(Connection c, Properties p)
    {
        this.conexao = c;
        // this.props = p;

        this.tabela = (String) p.get("tabela");
        this.colSaida = (String) p.get("saida");
        this.colCod = (String) p.get("id");
        this.numPop = Integer.valueOf((String) p.get("npop"));

        init();
    }

    /**
     * 
     */
    private void init()
    {
        recuperaColunas();
        recuperaClassesSaida();
        recuperaClasseSaidas();
        recuperaMaxMinDasEntradas();
    }

    /**
     *
     */
    private void recuperaClasseSaidas()
    {
        for (String saida : tipoSaidas)
        {
            classeSaidas.put(saida, new HashSet<Integer>());
        }

        String sql = "SELECT " + colSaida + ", " + colCod + " AS cod FROM "
                + tabela;
        try
        {
            PreparedStatement ps = conexao.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next())
            {
                String col = rs.getString(colSaida);
                classeSaidas.get(col).add(rs.getInt("cod"));
            }

            ps.close();
            rs.close();
        }
        catch (SQLException e)
        {
            throw new RuntimeException(
                    "Erro ao mapear nome das colunas com seu código(id).", e);
        }
    }

    /**
     *
     */
    public void carrega()
    {
        geraPopulacaoInicial();
    }

    /**
     *
     */
    private void recuperaColunas()
    {
        String sql = "SELECT * FROM " + tabela + " LIMIT 1";

        try
        {
            PreparedStatement ps = conexao.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData metadata = rs.getMetaData();

            int numCol = metadata.getColumnCount();

            colunas = new String[numCol - 2];
            max = new double[numCol - 2];
            min = new double[numCol - 2];

            for (int i = 0, j = 0; i < numCol; i++)
            {
                String coluna = metadata.getColumnName(i + 1);

                if (!colSaida.equalsIgnoreCase(coluna)
                        && !colCod.equalsIgnoreCase(coluna))
                {
                    colunas[j] = coluna;
                    j++;
                }
            }

            // System.out.println(Arrays.toString(colunas));
            ps.close();
            rs.close();
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Erro ao recuperar nome das colunas.", e);
        }
        finally
        {

        }
    }

    /**
     *
     */
    private void recuperaClassesSaida()
    {
        String sql = "SELECT DISTINCT " + colSaida + " FROM " + tabela;

        try
        {
            PreparedStatement ps = conexao.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next())
            {
                tipoSaidas.add(rs.getString(colSaida));
            }

            ps.close();
            rs.close();
        }
        catch (SQLException e)
        {
            throw new RuntimeException(
                    "Erro ao classe saída no banco de dados.", e);
        }
    }

    /**
     *
     */
    private void recuperaMaxMinDasEntradas()
    {
        // faixa de valores de cada coluna
        StringBuilder sb = new StringBuilder();
        for (String entrada : colunas)
        {
            sb.append(", ").append("max(").append(entrada).append(")")
                    .append(", ").append("min(").append(entrada).append(")");
        }
        String sql = "SELECT " + sb.toString().substring(1) + " FROM " + tabela;

        try
        {
            PreparedStatement ps = conexao.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            int numCol = colunas.length * 2;
            while (rs.next())
            {
                for (int i = 0, j = 0; i < numCol; i += 2, j++)
                {
                    max[j] = rs.getDouble(i + 1);
                    min[j] = rs.getDouble(i + 2);
                }
            }

            ps.close();
            rs.close();
        }
        catch (SQLException e)
        {
            throw new RuntimeException(
                    "Erro ao buscar (min, max) no banco de dados.", e);
        }
    }

    private void geraPopulacaoInicial()
    {
        for (int i = 0; i < numPop; i++)
        {
            List<String> vel = criaListaWhere();
            List<String> pos = criaListaWhere();

            int size = tipoSaidas.size();
            String classe = tipoSaidas.get(i % size);
            Particula particula = new Particula(vel, pos, classe);

            particulas.add(particula);
        }
    }

    private List<String> criaListaWhere()
    {
        int numCol = colunas.length;
        int numOper = LISTA_OPERADORES.length;

        int maxWhere = sorteio.nextInt(numCol) + 1;

        List<String> listaWhere = new ArrayList<String>();

        for (int i = 0; i < maxWhere; i++)
        {
            int colIndex = sorteio.nextInt(numCol);
            int operIndex = sorteio.nextInt(numOper);

            String val;
            if (sorteio.nextDouble() > 0.5)
            {
                val = String.valueOf(RandomUtils.nextDouble(min[colIndex],
                        max[colIndex]));
            }
            else
            {
                int index = sorteio.nextInt(numCol);
                val = colunas[index];
            }

            String col = (sorteio.nextDouble() > 0.5) ? colunas[colIndex]
                    : "NOT " + colunas[colIndex];

            String oper = LISTA_OPERADORES[operIndex];

            String whereSql = String.format("%s %s %s", col, oper, val);

            listaWhere.add(whereSql);
        }

        return listaWhere;
    }

    /**
     *
     */
    public void mostraPopulacao()
    {
        for (Particula p : particulas)
        {
            System.out.println(p.getWhereSql());
        }

        System.out.println("Classes");

        for (String classe : classeSaidas.keySet())
        {
            Set<Integer> conj = classeSaidas.get(classe);
            System.out.println(classe + ") " + conj.size());
        }
    }

}
