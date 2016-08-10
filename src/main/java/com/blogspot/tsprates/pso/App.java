package com.blogspot.tsprates.pso;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.exception.InsufficientDataException;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
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
    private final static int EXECS = 30; 

    // Algoritmos analisados
    private final static String[] ALGOS = 
    {
        "MOPSO", "J48", "SMO", "RBF"
    };
    
    // Kfold
    private final static int K = 10; 
    
    // Formata casas decimais
    private final static Formatador FORMAT = new Formatador();
    

    /**
     * Main.
     *
     * @param args
     */
    public static void main(String[] args)
    {
        System.out.println("--------------------------------------------");
        System.out.println(" Projeto de Dissertação Mestrado            ");
        System.out.println(" Implementação PSO                          ");
        System.out.println("--------------------------------------------");

        if (args.length > 0 && Files.exists(Paths.get(args[0])))
        {
            Connection db = new DB().conectar();
            Properties config = carregarArquivoConfig(args[0]);

            Pso pso = new Pso(db, config, FORMAT, K);

            List<Double> efetPSO = new ArrayList<>();
            List<Double> efetJ48 = new ArrayList<>();
            List<Double> efetSMO = new ArrayList<>();
            List<Double> efetRBF = new ArrayList<>();
            
            List<Double> acurPSO = new ArrayList<>();
            List<Double> acurJ48 = new ArrayList<>();
            List<Double> acurSMO = new ArrayList<>();
            List<Double> acurRBF = new ArrayList<>();

            System.out.printf("\nTabela: %s\n", config.getProperty("tabela"));

            // Matriz Efetividade
            double[][] matEfetCls = null;
            
            // Matriz Acurácia
            double[][] matAcurCls = null;

            for (int iter = 0; iter < EXECS; iter++)
            {
                System.out.printf("\n\nExecução: %d\n\n", iter + 1);

                pso.carregar();
                
                // Valor médio global da efetividade e acurácia
                final double[] resultado = pso.valorMedioGlobal();
                efetPSO.add(resultado[0]); 
                acurPSO.add(resultado[1]); 

                Weka weka = new Weka(config, K, pso.getKPasta());
                double[][] efetWeka = weka.efetividade();
                double[][] acurWeka = weka.acuracia();
                int numClasses = weka.numClasses();

                // Valor médio global (WEKA)
                double medEfetJ48 = 0.0;
                double medEfetSMO = 0.0;
                double medEfetRBF = 0.0;
                
                double medAcurJ48 = 0.0;
                double medAcurSMO = 0.0;
                double medAcurRBF = 0.0;

                for (int i = 0; i < numClasses; i++)
                {
                    medEfetJ48 += efetWeka[0][i];
                    medEfetSMO += efetWeka[1][i];
                    medEfetRBF += efetWeka[2][i];
                    
                    medAcurJ48 += acurWeka[0][i];
                    medAcurSMO += acurWeka[1][i];
                    medAcurRBF += acurWeka[2][i];
                }

                // Efetividade
                efetJ48.add(medEfetJ48 / (double) numClasses);
                efetSMO.add(medEfetSMO / (double) numClasses);
                efetRBF.add(medEfetRBF / (double) numClasses);
                
                // Acurácia
                acurJ48.add(medAcurJ48 / (double) numClasses);
                acurSMO.add(medAcurSMO / (double) numClasses);
                acurRBF.add(medAcurRBF / (double) numClasses);

                // Matriz para Efetividade algoritmo x classe.
                if (matEfetCls == null) 
                {
                    matEfetCls = new double[4][numClasses];
                }
                
                // Matriz para Efetividade algoritmo x classe.
                if (matAcurCls == null) 
                {
                    matAcurCls = new double[4][numClasses];
                }

                for (int i = 0; i < numClasses; i++)
                {
                    matEfetCls[1][i] += efetWeka[0][i];  // J48
                    matEfetCls[2][i] += efetWeka[1][i];  // SMO
                    matEfetCls[3][i] += efetWeka[2][i];  // RBF
                    
                    matAcurCls[1][i] += efetWeka[0][i];  // J48
                    matAcurCls[2][i] += efetWeka[1][i];  // SMO
                    matAcurCls[3][i] += efetWeka[2][i];  // RBF
                }

                final Map<String, double[]> resultadoPorClasses = pso
                        .valorMedioPorClasses();
                
                int j = 0;
                for (Entry<String, double[]> classe : resultadoPorClasses.entrySet())
                {
                    final double[] arr = classe.getValue();
                    matEfetCls[0][j] += arr[0];
                    matAcurCls[0][j] += arr[1];
                    j++;
                }
            }

            final Map<String, SummaryStatistics> statsEfet = criarStats(
                    efetPSO, efetJ48, efetSMO, efetRBF);
            
            final Map<String, SummaryStatistics> statsAcur = criarStats(
                    acurPSO, acurJ48, acurSMO, acurRBF);

            mostrarValorMedioExec(FORMAT, statsEfet, statsAcur);

            // Média por execução de cada classe
            for (int i = 0, size = pso.getTiposSaidas().size(); i < size; i++)
            {
                matEfetCls[0][i] /= EXECS;  // MOPSO
                matEfetCls[1][i] /= EXECS;  // J48
                matEfetCls[2][i] /= EXECS;  // SMO
                matEfetCls[3][i] /= EXECS;  // RBF
            }
            
            mostrarEfetividadePorClasses(pso.getTiposSaidas(), matEfetCls);

            // Testes estatísticos
            mostrarTesteDeNormalidade(statsEfet, efetPSO, efetJ48, efetSMO, efetRBF);
            mostrarTesteOneWayAnova(efetPSO, efetJ48, efetSMO, efetRBF);
            mostrarPostHocTukey(efetPSO, efetJ48, efetSMO, efetRBF);

            mostrarGraficoDeEfetividadeGlobal(config, efetPSO, efetJ48, efetSMO, efetRBF);
            
            // Salva Efetividade Global em CSV
            salvarExecsEmCSV(config, efetPSO, efetJ48, efetSMO, efetRBF);
        }
        else
        {
            System.err.println("É necessário definir um arquivo de configuração.");
        }
    }

    /**
     * Carrega arquivo de configurações.
     *
     * @param arquivo Arquivo de configurações.
     * @return Retorna um objeto Properties contendas as configurações dos algoritmos.
     */
    private static Properties carregarArquivoConfig(String arquivo)
    {
        try (FileInputStream fis = new FileInputStream(arquivo))
        {
            Properties props = new Properties();
            props.load(fis);
            return props;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Arquivo de configurações não encontrado.", e);
        }
    }

    /**
     * Gráfico de Efetividade Global por execuções.
     *
     * @param config Configurações de execução dos algoritmos.
     * @param efetPSO Efetividade obtida durante as execuções pelo PSO.
     * @param efetJ48 Efetividade obtida durante as execuções pelo J48.
     * @param efetSMO Efetividade obtida durante as execuções pelo SMO.
     * @param efetRBF Efetividade obtida durante as execuções pelo RBF.
     */
    private static void mostrarGraficoDeEfetividadeGlobal(Properties config,
            List<Double> efetPSO, List<Double> efetJ48, List<Double> efetSMO,
            List<Double> efetRBF)
    {
        List<Double> tempEfetPSO = new ArrayList<>(efetPSO);
        List<Double> tempEfetJ48 = new ArrayList<>(efetJ48);
        List<Double> tempEfetSMO = new ArrayList<>(efetSMO);
        List<Double> tempEfetRBF = new ArrayList<>(efetRBF);

        // Ordena efetividade
        Collections.sort(tempEfetPSO);
        Collections.sort(tempEfetJ48);
        Collections.sort(tempEfetSMO);
        Collections.sort(tempEfetRBF);

        // Gráfico Efetividade Média
        final String tituloGrafico = StringUtils.capitalize(config.getProperty("tabela"));
        final String eixoX = "Execução";
        final String eixoY = "Sensibilidade x Especificidade";

        Grafico g = new Grafico(tituloGrafico, eixoX, eixoY);
        g.adicionarSerie("MOPSO", tempEfetPSO);
        g.adicionarSerie("J48", tempEfetJ48);
        g.adicionarSerie("SMO", tempEfetSMO);
        g.adicionarSerie("RBF", tempEfetRBF);
        g.mostra();
    }

    /**
     * Imprime a média e desvio padrão das execuções dos algoritmos.
     *
     * @param f Formatador para casas decimais.
     * @param mapEfetStats Efetividade.
     */
    private static void mostrarValorMedioExec(final Formatador f,
            Map<String, SummaryStatistics> mapEfetStats, 
            Map<String, SummaryStatistics> mapAcurStats)
    {
        // cabeçalho
        System.out.printf("\n\n%-10s %-10s %-10s %-10s %-10s\n\n", 
                "Algo.", "Ef. Méd.", "Ef. Desv.", "Ac. Méd.", "Ac. Desv.");
        
        final String lineFmt = "%-10s %-10s %-10s %-10s %-10s\n";

        final SummaryStatistics efetPSO = mapEfetStats.get("PSO");
        final SummaryStatistics efetJ48 = mapEfetStats.get("J48");
        final SummaryStatistics efetSMO = mapEfetStats.get("SMO");
        final SummaryStatistics efetRBF = mapEfetStats.get("RBF");
        
        final SummaryStatistics acurPSO = mapAcurStats.get("PSO");
        final SummaryStatistics acurJ48 = mapAcurStats.get("J48");
        final SummaryStatistics acurSMO = mapAcurStats.get("SMO");
        final SummaryStatistics acurRBF = mapAcurStats.get("RBF");

        System.out.printf(lineFmt, "MOPSO",
                f.formatar(efetPSO.getMean()),
                f.formatar(efetPSO.getStandardDeviation()),
                f.formatar(acurPSO.getMean()),
                f.formatar(acurPSO.getStandardDeviation())
        );

        System.out.printf(lineFmt, "J48",
                f.formatar(efetJ48.getMean()),
                f.formatar(efetJ48.getStandardDeviation()),
                f.formatar(acurJ48.getMean()),
                f.formatar(acurJ48.getStandardDeviation())
        );

        System.out.printf(lineFmt, "SMO",
                f.formatar(efetSMO.getMean()),
                f.formatar(efetSMO.getStandardDeviation()),
                f.formatar(acurSMO.getMean()),
                f.formatar(acurSMO.getStandardDeviation())
        );

        System.out.printf(lineFmt, "RBF",
                f.formatar(efetRBF.getMean()),
                f.formatar(efetRBF.getStandardDeviation()),
                f.formatar(acurRBF.getMean()),
                f.formatar(acurRBF.getStandardDeviation())
        );

    }
    
    /**
     * Cria mapa de estatísticas para Efetividade.
     *
     * @param listaPSO
     * @param listaJ48
     * @param listaSMO
     * @param listaRBF
     * @return Retorna um mapa de algoritmos por SummaryStatistics.
     */
    private static Map<String, SummaryStatistics> criarStats(
            List<Double> listaPSO, List<Double> listaJ48,
            List<Double> listaSMO, List<Double> listaRBF)
    {
        Map<String, SummaryStatistics> map = new HashMap<>();

        SummaryStatistics statsPSO = new SummaryStatistics();
        for (int i = 0, size = listaPSO.size(); i < size; i++)
        {
            statsPSO.addValue(listaPSO.get(i));
        }
        map.put("PSO", statsPSO);

        SummaryStatistics statsJ48 = new SummaryStatistics();
        for (int i = 0, size = listaJ48.size(); i < size; i++)
        {
            statsJ48.addValue(listaJ48.get(i));
        }
        map.put("J48", statsJ48);

        SummaryStatistics statsSMO = new SummaryStatistics();
        for (int i = 0, size = listaSMO.size(); i < size; i++)
        {
            statsSMO.addValue(listaSMO.get(i));
        }
        map.put("SMO", statsSMO);

        SummaryStatistics statsRBF = new SummaryStatistics();
        for (int i = 0, size = listaRBF.size(); i < size; i++)
        {
            statsRBF.addValue(listaRBF.get(i));
        }
        map.put("RBF", statsRBF);

        return map;
    }

    /**
     * Teste OneWay Anova.
     *
     * @link https://en.wikipedia.org/wiki/One-way_analysis_of_variance
     * @param efetPSO Efetividade obtida durante as execuções pelo MOPSO.
     * @param efetJ48 Efetividade obtida durante as execuções pelo J48.
     * @param efetSMO Efetividade obtida durante as execuções pelo SMO.
     * @param efetRBF Efetividade obtida durante as execuções pelo RBF.
     */
    private static void mostrarTesteOneWayAnova(List<Double> efetPSO, 
            List<Double> efetJ48, List<Double> efetSMO, List<Double> efetRBF)
    {
        Double[] objArrPSO = efetPSO.toArray(new Double[0]);
        Double[] objArrJ48 = efetJ48.toArray(new Double[0]);
        Double[] objArrSMO = efetSMO.toArray(new Double[0]);
        Double[] objArrRBF = efetRBF.toArray(new Double[0]);

        double[] arrPSO = ArrayUtils.toPrimitive(objArrPSO);
        double[] arrJ48 = ArrayUtils.toPrimitive(objArrJ48);
        double[] arrSMO = ArrayUtils.toPrimitive(objArrSMO);
        double[] arrRBF = ArrayUtils.toPrimitive(objArrRBF);

        List<double[]> algs = new ArrayList<>();
        algs.add(arrPSO);
        algs.add(arrJ48);
        algs.add(arrSMO);
        algs.add(arrRBF);

        final String pvalor = FORMAT.formatar(TestUtils.oneWayAnovaPValue(algs));
        System.out.printf("\n\nTeste OneWay Anova (p-valor=0.05) : %s\n", pvalor);
    }

    /**
     * Teste de normalidade Kolmogorov-Smirnov.
     *
     * @link https://en.wikipedia.org/wiki/Kolmogorov%E2%80%93Smirnov_test
     * @param efetPSO Efetividade obtida durante execuções pelo PSO.
     * @param efetJ48 Efetividade obtida durante execuções pelo J48.
     * @param efetSMO Efetividade obtida durante execuções pelo SMO.
     * @param efetRBF Efetividade obtida durante execuções pelo RBF.
     */
    private static void mostrarTesteDeNormalidade(
            Map<String, SummaryStatistics> mapStats,
            List<Double> efetPSO, List<Double> efetJ48, List<Double> efetSMO,
            List<Double> efetRBF)
    {
        final SummaryStatistics PSO = mapStats.get("PSO");
        final SummaryStatistics J48 = mapStats.get("J48");
        final SummaryStatistics SMO = mapStats.get("SMO");
        final SummaryStatistics RBF = mapStats.get("RBF");

        final double medPSO = PSO.getMean();
        final double desvPSO = PSO.getStandardDeviation();

        final double medJ48 = J48.getMean();
        final double desvJ48 = J48.getStandardDeviation();

        final double medSMO = SMO.getMean();
        final double desvSMO = SMO.getStandardDeviation();

        final double medRBF = RBF.getMean();
        final double desvRBF = RBF.getStandardDeviation();

        System.out.println("\n\nTeste de Normalidade (Kolmogorov-Smirnov):\n");

        double ksPSO = kolmogorovSmirnov(medPSO, desvPSO, efetPSO);
        System.out.printf("MOPSO : %s\n", FORMAT.formatar(ksPSO));

        double ksJ48 = kolmogorovSmirnov(medJ48, desvJ48, efetJ48);
        System.out.printf("J48   : %s\n", FORMAT.formatar(ksJ48));

        double ksSMO = kolmogorovSmirnov(medSMO, desvSMO, efetSMO);
        System.out.printf("SMO   : %s\n", FORMAT.formatar(ksSMO));

        double ksRBF = kolmogorovSmirnov(medRBF, desvRBF, efetRBF);
        System.out.printf("RBF   : %s\n", FORMAT.formatar(ksRBF));
    }

    /**
     * Teste de Normalidade (Kolmogorov-Smirnov Test).
     *
     * @param mediaAlg Média do algoritmo.
     * @param desvAlg Desvio padrão do algoritmo.
     * @param efetAlg Efetividade Efetividade obtida durante as execuções pelo algoritmo.
     * @throws InsufficientDataException
     * @throws NullArgumentException
     * @throws NotStrictlyPositiveException
     * @return Resultado do KS-test.
     */
    private static double kolmogorovSmirnov(final double mediaAlg,
            final double desvAlg, List<Double> efetAlg) 
            throws InsufficientDataException, NullArgumentException, 
                NotStrictlyPositiveException
    {
        final NormalDistribution normdist = new NormalDistribution(mediaAlg, desvAlg);
        
        Double[] arrObjEfet = efetAlg.toArray(new Double[0]);
        
        return TestUtils.kolmogorovSmirnovTest(normdist, 
                ArrayUtils.toPrimitive(arrObjEfet), false);
    }

    /**
     * Teste Tukey.
     * 
     * @param efetPSO Efetividade obtida durante as execuções pelo PSO.
     * @param efetJ48 Efetividade obtida durante as execuções pelo J48.
     * @param efetSMO Efetividade obtida durante as execuções pelo SMO.
     * @param efetRBF Efetividade obtida durante as execuções pelo RBF.
     */
    private static void mostrarPostHocTukey(List<Double> efetPSO, 
            List<Double> efetJ48, List<Double> efetSMO, List<Double> efetRBF)
    {
        int k = 4;
        int n = EXECS;
        
        System.out.println("\n\nTeste Post-Hoc Tukey:\n");
        
        double[] groupTotals = new double[k];
        for (int i = 0; i < n; ++i)
        {
            groupTotals[0] += efetPSO.get(i);
            groupTotals[1] += efetJ48.get(i);
            groupTotals[2] += efetSMO.get(i);
            groupTotals[3] += efetRBF.get(i);
        }

        double[] groupMeans = new double[k];
        groupMeans[0] = groupTotals[0] / n;
        groupMeans[1] = groupTotals[1] / n;
        groupMeans[2] = groupTotals[2] / n;
        groupMeans[3] = groupTotals[3] / n;

        int dfWG = (n * k) - k;

        double[] ssGroup = new double[k];
        for (int i = 0; i < n; ++i)
        {
            ssGroup[0] += efetPSO.get(i) * efetPSO.get(i);
            ssGroup[1] += efetJ48.get(i) * efetJ48.get(i);
            ssGroup[2] += efetSMO.get(i) * efetSMO.get(i);
            ssGroup[3] += efetRBF.get(i) * efetRBF.get(i);
        }

        double ssWG = 0.0;
        for (int i = 0; i < k; ++i)
        {
            ssWG += ssGroup[i] - (groupTotals[i] * groupTotals[i]) / n;
        }

        double msWG = ssWG / dfWG;

        double cdHSD = getQ(k, dfWG) * Math.sqrt(msWG / n); // Sheskin, p. 910

        for (int i = 0; i < groupMeans.length - 1; ++i)
        {
            for (int j = i + 1; j < groupMeans.length; ++j)
            {
                double dif = Math.abs(groupMeans[i] - groupMeans[j]);
                
                String signif = dif > cdHSD 
                        ? "Há diferenças significativas" 
                        : "NÃO há diferenças significativas";
                
                String fmtDif = FORMAT.formatar(dif);
                String fmtCdHSD = FORMAT.formatar(cdHSD);
                
                System.out.printf("%5s <-> %-5s : (%s > %s) %s\n", ALGOS[i], 
                        ALGOS[j], fmtDif, fmtCdHSD, signif);
            }
        }

    }

    /**
     * Get the Studentized range statistic, <i>q</i> for <i>k</i> groups and
     * <i>df</i> degrees of freedom.
     * <p>
     *
     * The value is looked up in a table for &alpha; = 0.05 values of the
     * Studentized range statistic. The value for <i>k</i> is the number of
     * groups being compared. Only <i>k</i> = 2 to 10 is supported.
     * <p>
     *
     * The table values were transcribed from
     * <p>
     *
     * <blockquote> <a
     * href="http://cse.niaes.affrc.go.jp/miwa/probcalc/s-range/srng_tbl.html#fivepercent"
     * >http://cse.niaes.affrc.go.jp/miwa/probcalc/s-range/srng_tbl.html#fivepercent</a>
     * </blockquote>
     * <p>
     *
     * @param k the number of groups (<i>k</i> = 2 to 10)
     * @param df the df-error for the omnibus <i>F</i> test
     * @return q (Studentized range statistic) or -1 if 1 > <i>k</i> > 10.
     */
    private static double getQ(int k, int df)
    {
        final double[][] TAB = {
            {1, 17.969, 26.976, 32.819, 37.082, 40.408, 43.119, 45.397, 47.357, 49.071},
            {2, 6.085, 8.331, 9.798, 10.881, 11.734, 12.435, 13.027, 13.539, 13.988},
            {3, 4.501, 5.910, 6.825, 7.502, 8.037, 8.478, 8.852, 9.177, 9.462},
            {4, 3.926, 5.040, 5.757, 6.287, 6.706, 7.053, 7.347, 7.602, 7.826},
            {5, 3.635, 4.602, 5.218, 5.673, 6.033, 6.330, 6.582, 6.801, 6.995},
            {6, 3.460, 4.339, 4.896, 5.305, 5.628, 5.895, 6.122, 6.319, 6.493},
            {7, 3.344, 4.165, 4.681, 5.060, 5.359, 5.606, 5.815, 5.997, 6.158},
            {8, 3.261, 4.041, 4.529, 4.886, 5.167, 5.399, 5.596, 5.767, 5.918},
            {9, 3.199, 3.948, 4.415, 4.755, 5.024, 5.244, 5.432, 5.595, 5.738},
            {10, 3.151, 3.877, 4.327, 4.654, 4.912, 5.124, 5.304, 5.460, 5.598},
            {11, 3.113, 3.820, 4.256, 4.574, 4.823, 5.028, 5.202, 5.353, 5.486},
            {12, 3.081, 3.773, 4.199, 4.508, 4.750, 4.950, 5.119, 5.265, 5.395},
            {13, 3.055, 3.734, 4.151, 4.453, 4.690, 4.884, 5.049, 5.192, 5.318},
            {14, 3.033, 3.701, 4.111, 4.407, 4.639, 4.829, 4.990, 5.130, 5.253},
            {15, 3.014, 3.673, 4.076, 4.367, 4.595, 4.782, 4.940, 5.077, 5.198},
            {16, 2.998, 3.649, 4.046, 4.333, 4.557, 4.741, 4.896, 5.031, 5.150},
            {17, 2.984, 3.628, 4.020, 4.303, 4.524, 4.705, 4.858, 4.991, 5.108},
            {18, 2.971, 3.609, 3.997, 4.276, 4.494, 4.673, 4.824, 4.955, 5.071},
            {19, 2.960, 3.593, 3.977, 4.253, 4.468, 4.645, 4.794, 4.924, 5.037},
            {20, 2.950, 3.578, 3.958, 4.232, 4.445, 4.620, 4.768, 4.895, 5.008},
            {21, 2.941, 3.565, 3.942, 4.213, 4.424, 4.597, 4.743, 4.870, 4.981},
            {22, 2.933, 3.553, 3.927, 4.196, 4.405, 4.577, 4.722, 4.847, 4.957},
            {23, 2.926, 3.542, 3.914, 4.180, 4.388, 4.558, 4.702, 4.826, 4.935},
            {24, 2.919, 3.532, 3.901, 4.166, 4.373, 4.541, 4.684, 4.807, 4.915},
            {25, 2.913, 3.523, 3.890, 4.153, 4.358, 4.526, 4.667, 4.789, 4.897},
            {26, 2.907, 3.514, 3.880, 4.141, 4.345, 4.511, 4.652, 4.773, 4.880},
            {27, 2.902, 3.506, 3.870, 4.130, 4.333, 4.498, 4.638, 4.758, 4.864},
            {28, 2.897, 3.499, 3.861, 4.120, 4.322, 4.486, 4.625, 4.745, 4.850},
            {29, 2.892, 3.493, 3.853, 4.111, 4.311, 4.475, 4.613, 4.732, 4.837},
            {30, 2.888, 3.486, 3.845, 4.102, 4.301, 4.464, 4.601, 4.720, 4.824},
            {31, 2.884, 3.481, 3.838, 4.094, 4.292, 4.454, 4.591, 4.709, 4.812},
            {32, 2.881, 3.475, 3.832, 4.086, 4.284, 4.445, 4.581, 4.698, 4.802},
            {33, 2.877, 3.470, 3.825, 4.079, 4.276, 4.436, 4.572, 4.689, 4.791},
            {34, 2.874, 3.465, 3.820, 4.072, 4.268, 4.428, 4.563, 4.680, 4.782},
            {35, 2.871, 3.461, 3.814, 4.066, 4.261, 4.421, 4.555, 4.671, 4.773},
            {36, 2.868, 3.457, 3.809, 4.060, 4.255, 4.414, 4.547, 4.663, 4.764},
            {37, 2.865, 3.453, 3.804, 4.054, 4.249, 4.407, 4.540, 4.655, 4.756},
            {38, 2.863, 3.449, 3.799, 4.049, 4.243, 4.400, 4.533, 4.648, 4.749},
            {39, 2.861, 3.445, 3.795, 4.044, 4.237, 4.394, 4.527, 4.641, 4.741},
            {40, 2.858, 3.442, 3.791, 4.039, 4.232, 4.388, 4.521, 4.634, 4.735},
            {48, 2.843, 3.420, 3.764, 4.008, 4.197, 4.351, 4.481, 4.592, 4.690},
            {60, 2.829, 3.399, 3.737, 3.977, 4.163, 4.314, 4.441, 4.550, 4.646},
            {80, 2.814, 3.377, 3.711, 3.947, 4.129, 4.277, 4.402, 4.509, 4.603},
            {120, 2.800, 3.356, 3.685, 3.917, 4.096, 4.241, 4.363, 4.468, 4.560},
            {240, 2.786, 3.335, 3.659, 3.887, 4.063, 4.205, 4.324, 4.427, 4.517},
            {999, 2.772, 3.314, 3.633, 3.858, 4.030, 4.170, 4.286, 4.387, 4.474}};

        if (k < 2 || k > 10)
        {
            return -1.0; // not supported
        }
        int columnIndex = k - 1; // index for correct column (e.g., k = 3 is column 2)

        // find pertinent row in table
        int i = 0;
        while (i < TAB.length && df > TAB[i][0])
        {
            ++i;
        }

        // don't allow i to go past end of table
        if (i == TAB.length)
        {
            --i;
        }

        return TAB[i][columnIndex];
    }
    
    /**
     * Salva a Efetividade Global durante as execuções em um arquivo CSV.
     * 
     * @param config Configurações.
     * @param efetPSO Efetividade obtida durante as execuções pelo PSO.
     * @param efetJ48 Efetividade obtida durante as execuções pelo J48.
     * @param efetSMO Efetividade obtida durante as execuções pelo SMO.
     * @param efetRBF Efetividade obtida durante as execuções pelo RBF.
     */
    public static void salvarExecsEmCSV(Properties config, List<Double> efetPSO, 
            List<Double> efetJ48, List<Double> efetSMO, List<Double> efetRBF)
    {
        CSVFormat format = CSVFormat.DEFAULT.withRecordSeparator("\n");
        
        final DateFormat datefmt = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
        final String dataAtual = datefmt.format(new Date());
                    
        final Path pathExecs = Paths.get("execs");
        if (!Files.exists(pathExecs))
        {
            try
            {
                Files.createDirectory(pathExecs);
            }
            catch (IOException ex)
            {
                throw new RuntimeException("Não foi possível criar o diretório "
                        + "para salvar as execuções da Efetividade Global.", ex);
            }
        }
        
        String arqCSV = String.format("%s_%s_%s_%s_%s.csv", 
                config.getProperty("tabela"), 
                config.getProperty("npop"), 
                config.getProperty("maxiter"), 
                EXECS, 
                dataAtual);
        
        String caminhoArqCSV = pathExecs.toString() + File.separator + arqCSV;
        
        try (FileWriter fw = new FileWriter(caminhoArqCSV); 
                CSVPrinter printer = new CSVPrinter(fw, format))
        {
            printer.printRecord((Object[]) ALGOS);
            
            for (int i = 0; i < EXECS; i++) 
            {
                Object[] rec = new Object[ALGOS.length];
                rec[0] = efetPSO.get(i);
                rec[1] = efetJ48.get(i);
                rec[2] = efetSMO.get(i);
                rec[3] = efetRBF.get(i);
                printer.printRecord(rec);
            }
            
            System.out.printf("\n\nGravação da Efetividade Global em CSV "
                    + "realizada com sucesso (%s).\n\n", arqCSV);
        }
        catch (IOException ex)
        {
            throw new RuntimeException("Erro ao salvar resultados no CSV.", ex);
        }
        
    }
    
    /**
     * Mostra a tabela de cada algoritmo por classe.
     * 
     * @param saidas Tipos classes (saídas).
     * @param matEfetCls Matriz de algoritmos por classes (saídas).
     */
    private static void mostrarEfetividadePorClasses(
            Collection<String> saidas, double[][] matEfetCls)
    {
        System.out.println("\n\nAlgoritmos por Classes:\n");
        
        System.out.printf("%-10s", " ");
        
        for (String saida : saidas)
        {
            String s = (saida.length() > 10) ? saida.substring(0, 10) : saida;
            System.out.printf(" %-10s", s);
        }
        
        System.out.println("\n");
        
        for (int i = 0; i < matEfetCls.length; i++)
        {
            System.out.printf("%-10s", ALGOS[i]);
            
            for (int j = 0; j < matEfetCls[i].length; j++)
            {
                String valor = FORMAT.formatar(matEfetCls[i][j]);
                System.out.printf(" %-10s", valor);
            }
            
            System.out.println();
        }
    }
}