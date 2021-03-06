package com.github.tsprates.pso;

import net.sourceforge.jdistlib.disttest.DistributionTest;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.exception.InsufficientDataException;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.inference.TestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

/**
 * Particles Swarm Optimization (PSO).
 *
 * @author thiago
 */
public class App
{

    // Número total de execuções
    private final static int EXECS = 50;

    // Algoritmos analisados
    private final static String[] ALGOS = { "mDPSO", "J48", "SMO", "RBF" };

    // K-fold
    private final static int K = 10;

    // Formata casas decimais
    private final static Formatador FORMAT = new Formatador();

    // Gerador de números aleatórios
    private final static Random RANDOM = new Random( 1L );

    /**
     * Main.
     *
     * @param args Lista de argumentos do programa.
     */
    public static void main( String[] args )
    {
        System.out.println( "--------------------------------------------" );
        System.out.println( " Projeto de Dissertação do Mestrado         " );
        System.out.println( " Implementação mDPSO                        " );
        System.out.println( "--------------------------------------------" );

        if ( args.length > 0 && Files.exists( Paths.get( args[0] ) ) )
        {
            Connection db = new DB().conectar();
            Properties config = carregarArquivoDeConfig( args[0] );

            Pso pso = new Pso( db, config, RANDOM, FORMAT, K );

            Weka weka = new Weka( config );

            final List<Double> efetPSO = new ArrayList<>();
            final List<Double> efetJ48 = new ArrayList<>();
            final List<Double> efetSMO = new ArrayList<>();
            final List<Double> efetRBF = new ArrayList<>();

            final List<Double> acurPSO = new ArrayList<>();
            final List<Double> acurJ48 = new ArrayList<>();
            final List<Double> acurSMO = new ArrayList<>();
            final List<Double> acurRBF = new ArrayList<>();

            System.out.printf( "\nTabela: %s\n", config.getProperty( "tabela" ) );

            final Map<String, Map<String, List<Double>>> efetCls = new HashMap<>();
            final Map<String, Map<String, List<Double>>> acurCls = new HashMap<>();

            for ( int iter = 0; iter < EXECS; iter++ )
            {
                System.out.printf( "\n\nExecução: %d\n\n", iter + 1 );

                pso.carregar();

                // valor médio global da efetividade e acurácia
                final double[] resultado = pso.valorMedioGlobal();
                efetPSO.add( resultado[0] );
                acurPSO.add( resultado[1] );

                // weka
                weka.classificar( K, pso.getKPasta() );
                final double[][] efetWeka = weka.efetividade();
                final double[][] acurWeka = weka.acuracia();
                final int numClasses = weka.numClasses();

                // valor médio da efetividade global (weka)
                double medEfetJ48 = 0.0;
                double medEfetSMO = 0.0;
                double medEfetRBF = 0.0;

                // valor médio da acurácia global (weka)
                double medAcurJ48 = 0.0;
                double medAcurSMO = 0.0;
                double medAcurRBF = 0.0;

                for ( int i = 0; i < numClasses; i++ )
                {
                    medEfetJ48 += efetWeka[0][i];
                    medEfetSMO += efetWeka[1][i];
                    medEfetRBF += efetWeka[2][i];

                    medAcurJ48 += acurWeka[0][i];
                    medAcurSMO += acurWeka[1][i];
                    medAcurRBF += acurWeka[2][i];
                }

                // efetividade global
                efetJ48.add( medEfetJ48 / numClasses );
                efetSMO.add( medEfetSMO / numClasses );
                efetRBF.add( medEfetRBF / numClasses );

                // acurácia global
                acurJ48.add( medAcurJ48 / numClasses );
                acurSMO.add( medAcurSMO / numClasses );
                acurRBF.add( medAcurRBF / numClasses );

                // média de cada algoritmo para cada classe
                if ( iter == 0 )
                {
                    for ( String algo : ALGOS )
                    {
                        efetCls.put( algo, new HashMap<String, List<Double>>() );
                        acurCls.put( algo, new HashMap<String, List<Double>>() );

                        for ( String cl : pso.classes() )
                        {
                            efetCls.get( algo ).put( cl, new ArrayList<Double>() );
                            acurCls.get( algo ).put( cl, new ArrayList<Double>() );
                        }
                    }
                }

                // efetividade e acurácia de cada algoritmo em cada classe
                List<String> cls = new ArrayList<>( pso.classes() );
                for ( int i = 0; i < cls.size(); i++ )
                {
                    String cl = cls.get( i );

                    efetCls.get( "J48" ).get( cl ).add( efetWeka[0][i] );
                    efetCls.get( "SMO" ).get( cl ).add( efetWeka[1][i] );
                    efetCls.get( "RBF" ).get( cl ).add( efetWeka[2][i] );

                    acurCls.get( "J48" ).get( cl ).add( acurWeka[0][i] );
                    acurCls.get( "SMO" ).get( cl ).add( acurWeka[1][i] );
                    acurCls.get( "RBF" ).get( cl ).add( acurWeka[2][i] );
                }

                final Map<String, double[]> resultClasses = pso.valorMedioPorClasses();

                for ( Entry<String, double[]> item : resultClasses.entrySet() )
                {
                    final double[] arr = item.getValue();
                    final String classe = item.getKey();

                    efetCls.get( "mDPSO" ).get( classe ).add( arr[0] );
                    acurCls.get( "mDPSO" ).get( classe ).add( arr[1] );
                }
            }

            final Map<String, SummaryStatistics> statsEfet = criarStats( efetPSO, efetJ48, efetSMO, efetRBF );

            final Map<String, SummaryStatistics> statsAcur = criarStats( acurPSO, acurJ48, acurSMO, acurRBF );

            // desempenho médio
            mostrarValorMedioExec( statsEfet, statsAcur );
            mostrarEfetividadePorClasses( "Efetividade", pso.classes(), efetCls );
            mostrarEfetividadePorClasses( "Acurácia", pso.classes(), acurCls );

            // testes estatísticos
            mostrarTesteDeNormalidade( statsEfet, efetPSO, efetJ48, efetSMO, efetRBF );
            mostrarBartlettTeste( efetPSO, efetJ48, efetSMO, efetRBF );
            mostrarTesteOneWayAnova( efetPSO, efetJ48, efetSMO, efetRBF );
            mostrarPostHocTukey( efetPSO, efetJ48, efetSMO, efetRBF );

            // gráfico
            mostrarGraficoDeEfetividadeGlobal( config, efetPSO, efetJ48, efetSMO, efetRBF );

            // salvar efetividade global
            salvarExecsEmCSV( "efetividade", config, efetPSO, efetJ48, efetSMO, efetRBF );

            // salvar acurácia global
            salvarExecsEmCSV( "acuracia", config, acurPSO, acurJ48, acurSMO, acurRBF );
        }
        else
        {
            System.err.println( "Arquivo de configuração não encontrado." );
            System.err.flush();
        }
    }

    /**
     * Carrega arquivo de configurações.
     *
     * @param arquivo Arquivo de configurações.
     * @return Retorna um objeto Properties contendo as configurações dos algoritmos.
     */
    private static Properties carregarArquivoDeConfig( String arquivo )
    {
        try ( FileInputStream fis = new FileInputStream( arquivo ) )
        {
            Properties props = new Properties();
            props.load( fis );
            return props;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Arquivo de configurações não encontrado.", e );
        }
    }

    /**
     * Gráfico de Efetividade Global por execuções.
     *
     * @param config  Configurações de execução dos algoritmos.
     * @param efetPSO Efetividade obtida durante as execuções pelo PSO.
     * @param efetJ48 Efetividade obtida durante as execuções pelo J48.
     * @param efetSMO Efetividade obtida durante as execuções pelo SMO.
     * @param efetRBF Efetividade obtida durante as execuções pelo RBF.
     */
    private static void mostrarGraficoDeEfetividadeGlobal( Properties config, List<Double> efetPSO,
                                                           List<Double> efetJ48, List<Double> efetSMO,
                                                           List<Double> efetRBF )
    {
        final List<Double> tempEfetPSO = new ArrayList<>( efetPSO );
        final List<Double> tempEfetJ48 = new ArrayList<>( efetJ48 );
        final List<Double> tempEfetSMO = new ArrayList<>( efetSMO );
        final List<Double> tempEfetRBF = new ArrayList<>( efetRBF );

        Collections.sort( tempEfetPSO );
        Collections.sort( tempEfetJ48 );
        Collections.sort( tempEfetSMO );
        Collections.sort( tempEfetRBF );

        // formatação do gráfico
        final String tituloGrafico = StringUtils.capitalize( config.getProperty( "tabela" ) );
        final String eixoX = "Execução";
        final String eixoY = "Sensibilidade x Especificidade";

        // geração gráfico de resultados
        Grafico grafico = new Grafico( tituloGrafico, eixoX, eixoY );
        grafico.adicionarSerie( ALGOS[0], tempEfetPSO );
        grafico.adicionarSerie( ALGOS[1], tempEfetJ48 );
        grafico.adicionarSerie( ALGOS[2], tempEfetSMO );
        grafico.adicionarSerie( ALGOS[3], tempEfetRBF );
        grafico.mostrar();
    }

    /**
     * Imprime a média e desvio padrão das execuções dos algoritmos.
     *
     * @param mapEfetStats Cada efetividade.
     */
    private static void mostrarValorMedioExec( Map<String, SummaryStatistics> mapEfetStats,
                                               Map<String, SummaryStatistics> mapAcurStats )
    {
        System.out.printf( "\n\nMédias Globais por Execuções (%d)\n", EXECS );

        System.out.printf( "\n\n%-10s %-10s %-10s %-10s %-10s\n\n", "Algo.", "Ef. Méd.", "Ef. Desv.", "Ac. Méd.",
                           "Ac. Desv." );

        final String lineFmt = "%-10s %-10s %-10s %-10s %-10s\n";

        final SummaryStatistics efetPSO = mapEfetStats.get( "mDPSO" );
        final SummaryStatistics efetJ48 = mapEfetStats.get( "J48" );
        final SummaryStatistics efetSMO = mapEfetStats.get( "SMO" );
        final SummaryStatistics efetRBF = mapEfetStats.get( "RBF" );

        final SummaryStatistics acurPSO = mapAcurStats.get( "mDPSO" );
        final SummaryStatistics acurJ48 = mapAcurStats.get( "J48" );
        final SummaryStatistics acurSMO = mapAcurStats.get( "SMO" );
        final SummaryStatistics acurRBF = mapAcurStats.get( "RBF" );

        System.out.printf( lineFmt, "mDPSO", FORMAT.formatar( efetPSO.getMean() ),
                           FORMAT.formatar( efetPSO.getStandardDeviation() ), FORMAT.formatar( acurPSO.getMean() ),
                           FORMAT.formatar( acurPSO.getStandardDeviation() ) );

        System.out.printf( lineFmt, "J48", FORMAT.formatar( efetJ48.getMean() ),
                           FORMAT.formatar( efetJ48.getStandardDeviation() ), FORMAT.formatar( acurJ48.getMean() ),
                           FORMAT.formatar( acurJ48.getStandardDeviation() ) );

        System.out.printf( lineFmt, "SMO", FORMAT.formatar( efetSMO.getMean() ),
                           FORMAT.formatar( efetSMO.getStandardDeviation() ), FORMAT.formatar( acurSMO.getMean() ),
                           FORMAT.formatar( acurSMO.getStandardDeviation() ) );

        System.out.printf( lineFmt, "RBF", FORMAT.formatar( efetRBF.getMean() ),
                           FORMAT.formatar( efetRBF.getStandardDeviation() ), FORMAT.formatar( acurRBF.getMean() ),
                           FORMAT.formatar( acurRBF.getStandardDeviation() ) );

    }

    /**
     * Cria mapa de estatísticas para Efetividade.
     *
     * @param listaPSO Lista de efetividade do PSO.
     * @param listaJ48 Lista de efetividade do J48.
     * @param listaSMO Lista de efetividade do SMO.
     * @param listaRBF Lista de efetividade do RBF.
     * @return Retorna um mapa de SummaryStatistics.
     */
    private static Map<String, SummaryStatistics> criarStats( List<Double> listaPSO, List<Double> listaJ48,
                                                              List<Double> listaSMO, List<Double> listaRBF )
    {
        final Map<String, SummaryStatistics> map = new HashMap<>();

        final SummaryStatistics statsPSO = new SummaryStatistics();
        final SummaryStatistics statsJ48 = new SummaryStatistics();
        final SummaryStatistics statsSMO = new SummaryStatistics();
        final SummaryStatistics statsRBF = new SummaryStatistics();

        for ( Double aDouble : listaPSO )
        {
            statsPSO.addValue( aDouble );
        }

        map.put( "mDPSO", statsPSO );

        for ( Double aDouble : listaJ48 )
        {
            statsJ48.addValue( aDouble );
        }

        map.put( "J48", statsJ48 );

        for ( Double aDouble : listaSMO )
        {
            statsSMO.addValue( aDouble );
        }

        map.put( "SMO", statsSMO );

        for ( Double aDouble : listaRBF )
        {
            statsRBF.addValue( aDouble );
        }

        map.put( "RBF", statsRBF );

        return map;
    }

    /**
     * Teste OneWay Anova.
     *
     * @param efetPSO Efetividade obtida durante as execuções pelo PSO.
     * @param efetJ48 Efetividade obtida durante as execuções pelo J48.
     * @param efetSMO Efetividade obtida durante as execuções pelo SMO.
     * @param efetRBF Efetividade obtida durante as execuções pelo RBF.
     * @link https://en.wikipedia.org/wiki/One-way_analysis_of_variance
     */
    private static void mostrarTesteOneWayAnova( List<Double> efetPSO, List<Double> efetJ48, List<Double> efetSMO,
                                                 List<Double> efetRBF )
    {
        final Double[] objArrPSO = efetPSO.toArray( new Double[0] );
        final Double[] objArrJ48 = efetJ48.toArray( new Double[0] );
        final Double[] objArrSMO = efetSMO.toArray( new Double[0] );
        final Double[] objArrRBF = efetRBF.toArray( new Double[0] );

        final double[] arrPSO = ArrayUtils.toPrimitive( objArrPSO );
        final double[] arrJ48 = ArrayUtils.toPrimitive( objArrJ48 );
        final double[] arrSMO = ArrayUtils.toPrimitive( objArrSMO );
        final double[] arrRBF = ArrayUtils.toPrimitive( objArrRBF );

        final List<double[]> algs = new ArrayList<>();
        algs.add( arrPSO );
        algs.add( arrJ48 );
        algs.add( arrSMO );
        algs.add( arrRBF );

        final String pvalor = FORMAT.formatar( TestUtils.oneWayAnovaPValue( algs ) );
        System.out.printf( "\n\nTeste OneWay Anova (p-valor=0.05) Efetividade : %s \n", pvalor );
    }

    /**
     * Teste de normalidade Kolmogorov-Smirnov.
     *
     * @param efetPSO Efetividade obtida durante execuções pelo PSO.
     * @param efetJ48 Efetividade obtida durante execuções pelo J48.
     * @param efetSMO Efetividade obtida durante execuções pelo SMO.
     * @param efetRBF Efetividade obtida durante execuções pelo RBF.
     * @link https://en.wikipedia.org/wiki/Kolmogorov%E2%80%93Smirnov_test
     */
    private static void mostrarTesteDeNormalidade( Map<String, SummaryStatistics> mapStats, List<Double> efetPSO,
                                                   List<Double> efetJ48, List<Double> efetSMO, List<Double> efetRBF )
    {
        final SummaryStatistics statsPSO = mapStats.get( "mDPSO" );
        final SummaryStatistics statsJ48 = mapStats.get( "J48" );
        final SummaryStatistics statsSMO = mapStats.get( "SMO" );
        final SummaryStatistics statsRBF = mapStats.get( "RBF" );

        final double medPSO = statsPSO.getMean();
        final double desvPSO = statsPSO.getStandardDeviation();

        final double medJ48 = statsJ48.getMean();
        final double desvJ48 = statsJ48.getStandardDeviation();

        final double medSMO = statsSMO.getMean();
        final double desvSMO = statsSMO.getStandardDeviation();

        final double medRBF = statsRBF.getMean();
        final double desvRBF = statsRBF.getStandardDeviation();

        System.out.println( "\n\nTeste de Normalidade (Kolmogorov-Smirnov) Efetividade:\n" );

        final double ksPSO = kolmogorovSmirnov( medPSO, desvPSO, efetPSO );
        System.out.printf( "mDPSO : %s\n", FORMAT.formatar( ksPSO ) );

        final double ksJ48 = kolmogorovSmirnov( medJ48, desvJ48, efetJ48 );
        System.out.printf( "  J48 : %s\n", FORMAT.formatar( ksJ48 ) );

        final double ksSMO = kolmogorovSmirnov( medSMO, desvSMO, efetSMO );
        System.out.printf( "  SMO : %s\n", FORMAT.formatar( ksSMO ) );

        final double ksRBF = kolmogorovSmirnov( medRBF, desvRBF, efetRBF );
        System.out.printf( "  RBF : %s\n", FORMAT.formatar( ksRBF ) );
    }

    /**
     * Teste de Normalidade (Kolmogorov-Smirnov Test).
     *
     * @param mediaAlg Média do algoritmo.
     * @param desvAlg  Desvio padrão do algoritmo.
     * @param efetAlg  Efetividade obtida durante as execuções pelo algoritmo.
     * @return Resultado do KS-test.
     * @throws InsufficientDataException
     * @throws NullArgumentException
     * @throws NotStrictlyPositiveException
     */
    private static double kolmogorovSmirnov( double mediaAlg, double desvAlg, List<Double> efetAlg )
                    throws InsufficientDataException, NullArgumentException, NotStrictlyPositiveException
    {
        final double desvio = desvAlg + Double.MIN_VALUE;

        final NormalDistribution normdist = new NormalDistribution( mediaAlg, desvio );

        final Double[] arrObjEfet = efetAlg.toArray( new Double[0] );

        return TestUtils.kolmogorovSmirnovTest( normdist, ArrayUtils.toPrimitive( arrObjEfet ), false );
    }

    /**
     * Teste Bartlett.
     *
     * @param efetPSO Efetividade obtida durante execuções pelo PSO.
     * @param efetJ48 Efetividade obtida durante execuções pelo J48.
     * @param efetSMO Efetividade obtida durante execuções pelo SMO.
     * @param efetRBF Efetividade obtida durante execuções pelo RBF.
     * @link https://en.wikipedia.org/wiki/Bartlett%27s_test
     */
    private static void mostrarBartlettTeste( List<Double> efetPSO, List<Double> efetJ48, List<Double> efetSMO,
                                              List<Double> efetRBF )
    {
        final Double[] objArrPSO = efetPSO.toArray( new Double[0] );
        final Double[] objArrJ48 = efetJ48.toArray( new Double[0] );
        final Double[] objArrSMO = efetSMO.toArray( new Double[0] );
        final Double[] objArrRBF = efetRBF.toArray( new Double[0] );

        final double[] arrPSO = ArrayUtils.toPrimitive( objArrPSO );
        final double[] arrJ48 = ArrayUtils.toPrimitive( objArrJ48 );
        final double[] arrSMO = ArrayUtils.toPrimitive( objArrSMO );
        final double[] arrRBF = ArrayUtils.toPrimitive( objArrRBF );

        final double[] x = new double[4 * EXECS];
        final int[] group = new int[4 * EXECS];
        final int ex = EXECS;

        System.arraycopy( arrPSO, 0, x, 0, ex );
        System.arraycopy( arrJ48, 0, x, ex, ex );
        System.arraycopy( arrSMO, 0, x, ex * 2, ex );
        System.arraycopy( arrRBF, 0, x, ex * 3, ex );

        Arrays.fill( group, 0, ex, 1 );
        Arrays.fill( group, ex, ex * 2, 2 );
        Arrays.fill( group, ex * 2, ex * 3, 3 );
        Arrays.fill( group, ex * 4, ex * 4, 4 );

        final double[] result = DistributionTest.bartlett_test( x, group );
        final String pvalor = FORMAT.formatar( result[1] );

        System.out.printf( "\n\nTeste Bartlett (p-valor=0.05) Efetividade : %s \n", pvalor );
    }

    /**
     * Teste Tukey.
     *
     * @param efetPSO Efetividade obtida durante as execuções pelo PSO.
     * @param efetJ48 Efetividade obtida durante as execuções pelo J48.
     * @param efetSMO Efetividade obtida durante as execuções pelo SMO.
     * @param efetRBF Efetividade obtida durante as execuções pelo RBF.
     */
    private static void mostrarPostHocTukey( List<Double> efetPSO, List<Double> efetJ48, List<Double> efetSMO,
                                             List<Double> efetRBF )
    {
        int k = 4;
        int n = EXECS;

        System.out.println( "\n\nTeste Post-Hoc Tukey - Efetividade:\n" );

        double[] groupTotals = new double[k];
        for ( int i = 0; i < n; ++i )
        {
            groupTotals[0] += efetPSO.get( i );
            groupTotals[1] += efetJ48.get( i );
            groupTotals[2] += efetSMO.get( i );
            groupTotals[3] += efetRBF.get( i );
        }

        double[] groupMeans = new double[k];
        groupMeans[0] = groupTotals[0] / n;
        groupMeans[1] = groupTotals[1] / n;
        groupMeans[2] = groupTotals[2] / n;
        groupMeans[3] = groupTotals[3] / n;

        int dfWG = ( n * k ) - k;

        double[] ssGroup = new double[k];
        for ( int i = 0; i < n; ++i )
        {
            ssGroup[0] += efetPSO.get( i ) * efetPSO.get( i );
            ssGroup[1] += efetJ48.get( i ) * efetJ48.get( i );
            ssGroup[2] += efetSMO.get( i ) * efetSMO.get( i );
            ssGroup[3] += efetRBF.get( i ) * efetRBF.get( i );
        }

        double ssWG = 0.0;
        for ( int i = 0; i < k; ++i )
        {
            ssWG += ssGroup[i] - ( groupTotals[i] * groupTotals[i] ) / n;
        }

        double msWG = ssWG / dfWG;

        double cdHSD = getQ( k, dfWG ) * Math.sqrt( msWG / n ); // Sheskin, p. 910

        for ( int i = 0; i < groupMeans.length - 1; ++i )
        {
            for ( int j = i + 1; j < groupMeans.length; ++j )
            {
                double dif = Math.abs( groupMeans[i] - groupMeans[j] );

                String signif = ( dif > cdHSD ) ? "Significativa" : "NÃO significativa";

                String fmtDif = FORMAT.formatar( dif );
                String fmtCdHSD = FORMAT.formatar( cdHSD );

                System.out.printf( "%5s <-> %-5s : (%s > %s) %s\n", ALGOS[i], ALGOS[j], fmtDif, fmtCdHSD, signif );
            }
        }

    }

    /**
     * Get the Studentized range statistic, <i>q</i> for <i>k</i> groups and <i>df</i> degrees of freedom.
     * <p>
     * The value is looked up in a table for &alpha; = 0.05 values of the Studentized range statistic. The value for
     * <i>k</i> is the number of groups being compared. Only <i>k</i> = 2 to 10 is supported.
     * <p>
     * The table values were transcribed from
     * <p>
     * <blockquote> <a href= "http://cse.niaes.affrc.go.jp/miwa/probcalc/s-range/srng_tbl.html#fivepercent"
     * >http://cse.niaes.affrc.go.jp/miwa/probcalc/s-range/srng_tbl.html#fivepercent</a> </blockquote>
     * <p>
     *
     * @param k  the number of groups (<i>k</i> = 2 to 10)
     * @param df the df-error for the omnibus <i>F</i> test
     * @return q (Studentized range statistic) or -1 if 1 > <i>k</i> > 10.
     */
    private static double getQ( int k, int df )
    {
        final double[][] TAB = {
                        { 1, 17.969, 26.976, 32.819, 37.082, 40.408, 43.119, 45.397, 47.357, 49.071 },
                        { 2, 6.085, 8.331, 9.798, 10.881, 11.734, 12.435, 13.027, 13.539, 13.988 },
                        { 3, 4.501, 5.910, 6.825, 7.502, 8.037, 8.478, 8.852, 9.177, 9.462 },
                        { 4, 3.926, 5.040, 5.757, 6.287, 6.706, 7.053, 7.347, 7.602, 7.826 },
                        { 5, 3.635, 4.602, 5.218, 5.673, 6.033, 6.330, 6.582, 6.801, 6.995 },
                        { 6, 3.460, 4.339, 4.896, 5.305, 5.628, 5.895, 6.122, 6.319, 6.493 },
                        { 7, 3.344, 4.165, 4.681, 5.060, 5.359, 5.606, 5.815, 5.997, 6.158 },
                        { 8, 3.261, 4.041, 4.529, 4.886, 5.167, 5.399, 5.596, 5.767, 5.918 },
                        { 9, 3.199, 3.948, 4.415, 4.755, 5.024, 5.244, 5.432, 5.595, 5.738 },
                        { 10, 3.151, 3.877, 4.327, 4.654, 4.912, 5.124, 5.304, 5.460, 5.598 },
                        { 11, 3.113, 3.820, 4.256, 4.574, 4.823, 5.028, 5.202, 5.353, 5.486 },
                        { 12, 3.081, 3.773, 4.199, 4.508, 4.750, 4.950, 5.119, 5.265, 5.395 },
                        { 13, 3.055, 3.734, 4.151, 4.453, 4.690, 4.884, 5.049, 5.192, 5.318 },
                        { 14, 3.033, 3.701, 4.111, 4.407, 4.639, 4.829, 4.990, 5.130, 5.253 },
                        { 15, 3.014, 3.673, 4.076, 4.367, 4.595, 4.782, 4.940, 5.077, 5.198 },
                        { 16, 2.998, 3.649, 4.046, 4.333, 4.557, 4.741, 4.896, 5.031, 5.150 },
                        { 17, 2.984, 3.628, 4.020, 4.303, 4.524, 4.705, 4.858, 4.991, 5.108 },
                        { 18, 2.971, 3.609, 3.997, 4.276, 4.494, 4.673, 4.824, 4.955, 5.071 },
                        { 19, 2.960, 3.593, 3.977, 4.253, 4.468, 4.645, 4.794, 4.924, 5.037 },
                        { 20, 2.950, 3.578, 3.958, 4.232, 4.445, 4.620, 4.768, 4.895, 5.008 },
                        { 21, 2.941, 3.565, 3.942, 4.213, 4.424, 4.597, 4.743, 4.870, 4.981 },
                        { 22, 2.933, 3.553, 3.927, 4.196, 4.405, 4.577, 4.722, 4.847, 4.957 },
                        { 23, 2.926, 3.542, 3.914, 4.180, 4.388, 4.558, 4.702, 4.826, 4.935 },
                        { 24, 2.919, 3.532, 3.901, 4.166, 4.373, 4.541, 4.684, 4.807, 4.915 },
                        { 25, 2.913, 3.523, 3.890, 4.153, 4.358, 4.526, 4.667, 4.789, 4.897 },
                        { 26, 2.907, 3.514, 3.880, 4.141, 4.345, 4.511, 4.652, 4.773, 4.880 },
                        { 27, 2.902, 3.506, 3.870, 4.130, 4.333, 4.498, 4.638, 4.758, 4.864 },
                        { 28, 2.897, 3.499, 3.861, 4.120, 4.322, 4.486, 4.625, 4.745, 4.850 },
                        { 29, 2.892, 3.493, 3.853, 4.111, 4.311, 4.475, 4.613, 4.732, 4.837 },
                        { 30, 2.888, 3.486, 3.845, 4.102, 4.301, 4.464, 4.601, 4.720, 4.824 },
                        { 31, 2.884, 3.481, 3.838, 4.094, 4.292, 4.454, 4.591, 4.709, 4.812 },
                        { 32, 2.881, 3.475, 3.832, 4.086, 4.284, 4.445, 4.581, 4.698, 4.802 },
                        { 33, 2.877, 3.470, 3.825, 4.079, 4.276, 4.436, 4.572, 4.689, 4.791 },
                        { 34, 2.874, 3.465, 3.820, 4.072, 4.268, 4.428, 4.563, 4.680, 4.782 },
                        { 35, 2.871, 3.461, 3.814, 4.066, 4.261, 4.421, 4.555, 4.671, 4.773 },
                        { 36, 2.868, 3.457, 3.809, 4.060, 4.255, 4.414, 4.547, 4.663, 4.764 },
                        { 37, 2.865, 3.453, 3.804, 4.054, 4.249, 4.407, 4.540, 4.655, 4.756 },
                        { 38, 2.863, 3.449, 3.799, 4.049, 4.243, 4.400, 4.533, 4.648, 4.749 },
                        { 39, 2.861, 3.445, 3.795, 4.044, 4.237, 4.394, 4.527, 4.641, 4.741 },
                        { 40, 2.858, 3.442, 3.791, 4.039, 4.232, 4.388, 4.521, 4.634, 4.735 },
                        { 48, 2.843, 3.420, 3.764, 4.008, 4.197, 4.351, 4.481, 4.592, 4.690 },
                        { 60, 2.829, 3.399, 3.737, 3.977, 4.163, 4.314, 4.441, 4.550, 4.646 },
                        { 80, 2.814, 3.377, 3.711, 3.947, 4.129, 4.277, 4.402, 4.509, 4.603 },
                        { 120, 2.800, 3.356, 3.685, 3.917, 4.096, 4.241, 4.363, 4.468, 4.560 },
                        { 240, 2.786, 3.335, 3.659, 3.887, 4.063, 4.205, 4.324, 4.427, 4.517 },
                        { 999, 2.772, 3.314, 3.633, 3.858, 4.030, 4.170, 4.286, 4.387, 4.474 }
        };

        if ( k < 2 || k > 10 )
        {
            return -1.0; // not supported
        }

        int j = k - 1; // index for correct column (e.g., k = 3 is column 2)

        // find pertinent row in table
        int i = 0;
        while ( i < TAB.length && df > TAB[i][0] )
        {
            ++i;
        }

        // don't allow i to go past end of table
        if ( i == TAB.length )
        {
            --i;
        }

        return TAB[i][j];
    }

    /**
     * Salva os resultados das execuções em arquivo CSV.
     *
     * @param metrica  Métrica.
     * @param config   Configurações do algoritmo.
     * @param listaPSO Lista de resultados do PSO.
     * @param listaJ48 Lista de resultados do J48.
     * @param listaSMO Lista de resultados do SMO.
     * @param listaRBF Lista de resultados do RBF.
     */
    private static void salvarExecsEmCSV( String metrica, Properties config, List<Double> listaPSO,
                                          List<Double> listaJ48, List<Double> listaSMO, List<Double> listaRBF )
    {
        final CSVFormat format = CSVFormat.DEFAULT.withRecordSeparator( "\n" );

        final DateFormat datefmt = new SimpleDateFormat( "yyyy-MM-dd_HH-mm" );
        final String dataAtual = datefmt.format( new Date() );

        final Path pathExecs = Paths.get( "execs" );

        if ( !Files.exists( pathExecs ) )
        {
            try
            {
                Files.createDirectory( pathExecs );
            }
            catch ( IOException ex )
            {
                throw new RuntimeException( "Não foi possível criar o diretório para salvar o CSV.", ex );
            }
        }

        final String arqCSV =
                        String.format( "%s_%s_%s_%s_%s_%s.csv", metrica, config.getProperty( "tabela" ),
                                       config.getProperty( "npop" ), config.getProperty( "maxiter" ), EXECS,
                                       dataAtual );

        final String caminhoArqCSV = pathExecs.toString() + File.separator + arqCSV;

        try ( FileWriter fw = new FileWriter( caminhoArqCSV ); CSVPrinter printer = new CSVPrinter( fw, format ) )
        {
            printer.printRecord( (Object[]) ALGOS );

            for ( int i = 0; i < EXECS; i++ )
            {
                Object[] rec = new Object[ALGOS.length];
                rec[0] = listaPSO.get( i );
                rec[1] = listaJ48.get( i );
                rec[2] = listaSMO.get( i );
                rec[3] = listaRBF.get( i );
                printer.printRecord( rec );
            }

            System.out.printf( "\nGravação em CSV realizada com sucesso (%s).\n", arqCSV );
        }
        catch ( IOException ex )
        {
            throw new RuntimeException( "Erro ao salvar resultados no CSV.", ex );
        }

    }

    /**
     * Mostra a tabela de efetividade de cada algoritmo por classe.
     *
     * @param legendaTabela Legenda tabela (Tipo métrica).
     * @param classes       Classes.
     * @param mapCls        Mapa de algoritmos por classes.
     */
    private static void mostrarEfetividadePorClasses( String legendaTabela, Collection<String> classes,
                                                      Map<String, Map<String, List<Double>>> mapCls )
    {
        final StandardDeviation sd = new StandardDeviation();

        System.out.printf( "\n\nMédia de Algoritmos por Classes (%s):\n\n", legendaTabela );

        System.out.printf( "%-10s", "Algo." );
        for ( String cl : classes )
        {
            String s = ( cl.length() > 10 ) ? cl.substring( 0, 10 ) : cl;
            System.out.printf( " %-10s %-10s", s, "" );
        }

        System.out.println( "\n" );

        for ( String algo : ALGOS )
        {
            System.out.printf( "%-10s", algo );

            for ( Entry<String, List<Double>> ent : mapCls.get( algo ).entrySet() )
            {
                final List<Double> val = ent.getValue();

                final double[] arrVal = ArrayUtils.toPrimitive( val.toArray( new Double[0] ) );

                String media = FORMAT.formatar( StatUtils.mean( arrVal ) );
                String desvio = FORMAT.formatar( sd.evaluate( arrVal ) );

                System.out.printf( " %-10s %-10s", media, desvio );
            }

            System.out.println();
        }
    }
}
