package com.github.tsprates.pso;

import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

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

    private final Map<String, List<String>> particulasPorClasse;

    private int totalSize = 0;

    private long numAvaliacao = 0;

    private List<List<String>> kpastas;

    private int k = 0;

    private String notId;

    /**
     * Construtor.
     *
     * @param conexao              Conexão DB.
     * @param colId                Campo ID.
     * @param tabela               Nome da tabela no banco de dados.
     * @param particulasPorClasses Lista de partículas organizado por classes.
     */
    public Fitness( Connection conexao, String colId, String tabela, Map<String, List<String>> particulasPorClasses )
    {
        this.conexao = conexao;
        this.colId = colId;
        this.tabela = tabela;
        this.particulasPorClasse = particulasPorClasses;

        for ( String saida : particulasPorClasses.keySet() )
        {
            totalSize += particulasPorClasses.get( saida ).size();
        }
    }

    /**
     * Seta k-pasta.
     *
     * @param k
     */
    public void setK( int k )
    {
        this.k = k;
        notId = StringUtils.join( kpastas.get( k ), ", " );
    }

    /**
     * Seta k-pasta.
     *
     * @param kpastas
     */
    public void setKPastas( List<List<String>> kpastas )
    {
        this.kpastas = kpastas;
    }

    /**
     * Calcula fitness.
     *
     * @param part Partícula.
     * @return Array contendo a complexidade, efetividade e acurácia.
     */
    public double[] calcular( Particula part )
    {
        // atualiza o número de avaliação
        numAvaliacao += 1;

        final double[] r = realizarCalculo( part, true );

        final double[] arr = new double[3];
        arr[0] = 1.0 / part.numWhere();
        arr[1] = r[0];
        arr[2] = r[1];

        return arr;
    }

    /**
     * Calcula fitness.
     *
     * @param part        Partícula.
     * @param treinamento Se verdadeiro então fase de treinamento senão fase de teste.
     * @return Array contendo a complexidade WHERE, efetividade e acurácia.
     */
    public double[] calcular( Particula part, boolean treinamento )
    {
        // atualiza o número de avaliação
        numAvaliacao += 1;

        final double[] r = realizarCalculo( part, treinamento );

        final double[] arr = new double[3];
        arr[0] = 1.0 / part.numWhere();
        arr[1] = r[0];
        arr[2] = r[1];

        return arr;
    }

    /**
     * Avalia determinada partícula pela cláusula SQL WHERE.
     *
     * @param where       String de uma cláusula WHERE.
     * @param treinamento Treinamento.
     * @return Retorna lista de String correspondente a uma cláusula WHERE.
     */
    private List<String> consultaSql( String where, boolean treinamento )
    {
        List<String> result = new ArrayList<>();

        String sql;

        if ( treinamento )
        {
            sql = "SELECT " + colId + " AS id " + "FROM " + tabela + " " + "WHERE " + colId + " NOT IN (" + notId + ") "
                            + "AND " + where;
        }
        else
        {
            sql = "SELECT " + colId + " AS id " + "FROM " + tabela + " " + "WHERE " + colId + " IN (" + notId + ") "
                            + "AND " + where;
        }

        try ( PreparedStatement ps = conexao.prepareStatement( sql ); ResultSet rs = ps.executeQuery() )
        {

            while ( rs.next() )
            {
                result.add( rs.getString( "id" ) );
            }

            return result;
        }
        catch ( SQLException e )
        {
            throw new RuntimeException( "Erro ao recupera as classes no banco de dados.", e );
        }
    }

    /**
     * Calcula a especificidade e acurácia da partícula.
     *
     * @param p           Partícula.
     * @param treinamento Treinamento.
     * @return Retorna a efetividade e acurácia calculada.
     */
    private double[] realizarCalculo( Particula p, boolean treinamento )
    {
        final String classe = p.classe();
        final List<String> verdadeiros = new ArrayList<>( particulasPorClasse.get( classe ) );

        final List<String> kpastaAtual = kpastas.get( k );
        int total;

        if ( treinamento )
        {
            verdadeiros.removeAll( kpastaAtual );
            total = totalSize - kpastaAtual.size();
        }
        else
        {
            verdadeiros.retainAll( kpastaAtual );
            total = kpastaAtual.size();
        }

        final List<String> consultaSql = consultaSql( p.whereSql(), treinamento );
        final int consultaSqlSize = consultaSql.size();

        final int verdadeirosSize = verdadeiros.size();

        double tp = 0.0;
        for ( String id : consultaSql )
        {
            if ( verdadeiros.contains( id ) )
            {
                tp += 1.0;
            }
        }

        double fp = consultaSqlSize - tp;
        double fn = verdadeirosSize - tp;
        double tn = total - fn - fp - tp;

        double sensibilidade = tp / ( tp + fn );
        double especificidade = tn / ( tn + fp );
        double acuracia = ( tp + tn ) / ( tp + tn + fp + fn );

        double efetividade = especificidade * sensibilidade;

        return new double[] { efetividade, acuracia };
    }

    /**
     * Retorna o número de avaliação da função fitness.
     *
     * @return Número de avaliação da função fitness.
     */
    public long numAvaliacao()
    {
        return numAvaliacao;
    }

    /**
     * Seta o número de avaliação do fitness.
     *
     * @param num Número de avaliações.
     */
    public void setNumAvaliacao( int num )
    {
        this.numAvaliacao = num;
    }

    /**
     * Reseta o número de avaliação do fitness.
     */
    public void resetNumAvaliacao()
    {
        this.numAvaliacao = 0;
    }

    /**
     * Fase de teste.
     *
     * @param repositorio Partículas não dominadas, divididas por classes.
     * @return Mapa de fitness das partículas.
     */
    public Map<String, List<double[]>> testar( Map<String, List<Particula>> repositorio )
    {
        Map<String, List<double[]>> mapFit = new TreeMap<>();

        for ( Entry<String, List<Particula>> classePart : repositorio.entrySet() )
        {
            String saida = classePart.getKey();

            mapFit.put( saida, new ArrayList<double[]>() );

            List<Particula> parts = classePart.getValue();
            for ( Particula part : parts )
            {
                double[] arr = calcular( part, false );
                mapFit.get( saida ).add( arr );
            }
        }

        return mapFit;
    }
}
