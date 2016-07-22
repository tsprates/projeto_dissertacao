package com.blogspot.tsprates.pso;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.*;
import java.util.Map.Entry;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.exception.InsufficientDataException;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.stat.inference.TestUtils;

/**
 * Particles Swarm Optimization (PSO).
 *
 * @author thiago
 */
public class App
{

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
            Connection db = new DbFactory().conectar();
            Properties config = carregarArquivoConfig(args[0]);

            final Formatador f = new Formatador();

            final int K = 10;
            Pso pso = new Pso(db, config, f, K);

            List<Double> efetPSO = new ArrayList<>();
            List<Double> efetJ48 = new ArrayList<>();
            List<Double> efetSMO = new ArrayList<>();
            List<Double> efetRBF = new ArrayList<>();

            System.out.printf("\nTabela: %s\n", config.getProperty("tabela"));

            // matriz de algoritmos x classes
            double[][] matCls = null;

            final int EXEC = 30;
            for (int iter = 0; iter < EXEC; iter++)
            {
                System.out.printf("\nExecução: %d\n\n", iter + 1);

                pso.carregar();

                efetPSO.add(pso.getResultado());

                Weka weka = new Weka(pso.getKPasta(), K, config);
                double[][] efetWeka = weka.getEfetividadeArray();
                int numCls = weka.numClasses();

                // valor médio efetividade global
                double medEfetJ48 = 0.0;
                double medEfetSMO = 0.0;
                double medEfetRBF = 0.0;

                for (int i = 0; i < numCls; i++)
                {
                    medEfetJ48 += efetWeka[0][i];
                    medEfetSMO += efetWeka[1][i];
                    medEfetRBF += efetWeka[2][i];
                }

                // Adiciona Efetividade Global
                efetJ48.add(medEfetJ48 / (double) numCls);
                efetSMO.add(medEfetSMO / (double) numCls);
                efetRBF.add(medEfetRBF / (double) numCls);

                // Verifica se matriz de classes foi instanciada
                if (matCls == null)
                {
                    matCls = new double[4][numCls];

                    for (int i = 0; i < numCls; i++)
                    {
                        matCls[0][i] = 0.0; // MOPSO
                        matCls[1][i] = 0.0; // J48
                        matCls[2][i] = 0.0; // SMO
                        matCls[3][i] = 0.0; // RBF
                    }
                }

                for (int i = 0; i < numCls; i++)
                {
                    matCls[1][i] += efetWeka[0][i];  // J48
                    matCls[2][i] += efetWeka[1][i];  // SMO
                    matCls[3][i] += efetWeka[2][i];  // RBF
                }

                int j = 0;
                final Map<String, Double> resultadoPorClasses = pso.getResultadoPorClasses();
                for (Entry<String, Double> classe : resultadoPorClasses.entrySet())
                {
                    matCls[0][j] += classe.getValue(); // MOPSO
                    j++;
                }
            }

            if (matCls != null)
            {
                for (int i = 0; i < pso.getClasses().size(); i++)
                {
                    matCls[0][i] /= EXEC;  // MOPSO
                    matCls[1][i] /= EXEC;  // J48
                    matCls[2][i] /= EXEC;  // SMO
                    matCls[3][i] /= EXEC;  // RBF
                }

                System.out.println("\nAlgoritmos por Classes:\n");
                String[] alg =
                {
                    "MOPSO", "J48", "SMO", "RBF"
                };
                for (int i = 0; i < matCls.length; i++)
                {
                    System.out.printf("%-10s", alg[i]);
                    for (int j = 0; j < matCls[i].length; j++)
                    {
                        String valor = f.formatar(matCls[i][j]);
                        System.out.printf(" %-10s", valor);
                    }
                    System.out.println();
                }
            }

            Map<String, SummaryStatistics> statsEfet = criarMapStats(efetPSO, efetJ48, efetSMO, efetRBF);

            mostrarValorMedioExec(f, statsEfet);

//            mostrarTesteWilcoxon(f, efetPSO, efetJ48, efetSMO, efetRBF);
            mostrarTesteDeNormalidade(f, statsEfet, efetPSO, efetJ48, efetSMO, efetRBF);
            mostrarTesteOneWayAnova(f, efetPSO, efetJ48, efetSMO, efetRBF);

            mostrarGraficoDeEfetividadeGlobal(config, efetPSO, efetJ48, efetSMO, efetRBF);
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
     * @return Properties Retorna um objeto Properties contendas as
     * configurações dos algoritmos.
     */
    private static Properties carregarArquivoConfig(String arquivo)
    {
        try (FileInputStream fis = new FileInputStream(arquivo))
        {
            Properties prop = new Properties();
            prop.load(fis);
            return prop;
        }
        catch (IOException e)
        {
            throw new RuntimeException(
                    "Arquivo de configurações não encontrado.", e);
        }
    }

    /**
     * Gráfico de Efetividade Global por execuções.
     *
     * @param config Configurações de execução dos algoritmos.
     * @param efetPSO Efetividade obtida durante as execuções pelo MOPSO.
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
     * @param efetPSO Efetividade obtida durante as execuções pelo MOPSO.
     * @param efetJ48 Efetividade obtida durante as execuções pelo J48.
     * @param efetSMO Efetividade obtida durante as execuções pelo SMO.
     * @param efetRBF Efetividade obtida durante as execuções pelo RBF.
     */
    private static void mostrarValorMedioExec(final Formatador f,
            Map<String, SummaryStatistics> mapStats)
    {

        System.out.println("\n");
        System.out.printf("%-10s %-10s %-10s\n\n", "Alg.", "Méd.", "Desv.");
        String strFormat = "%-10s %-10s %-10s\n";

        final SummaryStatistics MOPSO = mapStats.get("MOPSO");
        final SummaryStatistics J48 = mapStats.get("J48");
        final SummaryStatistics SMO = mapStats.get("SMO");
        final SummaryStatistics RBF = mapStats.get("RBF");

        System.out.printf(strFormat,
                "MOPSO",
                f.formatar(MOPSO.getMean()),
                f.formatar(MOPSO.getStandardDeviation()));

        System.out.printf(strFormat,
                "J48",
                f.formatar(J48.getMean()),
                f.formatar(J48.getStandardDeviation()));

        System.out.printf(strFormat,
                "SMO",
                f.formatar(SMO.getMean()),
                f.formatar(SMO.getStandardDeviation()));

        System.out.printf(strFormat,
                "RBF",
                f.formatar(RBF.getMean()),
                f.formatar(RBF.getStandardDeviation()));

    }

    /**
     * Teste de Postos Sinalizados de Wilcoxon.
     *
     * @see https://en.wikipedia.org/wiki/Wilcoxon_signed-rank_test
     * @param f Formatador para casas decimais.
     * @param efetPSO Efetividade obtida durante as execuções pelo MOPSO.
     * @param efetJ48 Efetividade obtida durante as execuções pelo J48.
     * @param efetSMO Efetividade obtida durante as execuções pelo SMO.
     * @param efetRBF Efetividade obtida durante as execuções pelo RBF.
     */
    private static void mostrarTesteWilcoxon(final Formatador f,
            List<Double> efetPSO, List<Double> efetJ48, List<Double> efetSMO,
            List<Double> efetRBF)
    {
        // Wilcoxon Test
        WilcoxonSignedRankTest w = new WilcoxonSignedRankTest();

        Double[] tempArrPSO = efetPSO.toArray(new Double[0]);
        Double[] tempArrJ48 = efetJ48.toArray(new Double[0]);
        Double[] tempArrSMO = efetSMO.toArray(new Double[0]);
        Double[] tempArrRBF = efetRBF.toArray(new Double[0]);

        double[] arrPSO = ArrayUtils.toPrimitive(tempArrPSO);
        double[] arrJ48 = ArrayUtils.toPrimitive(tempArrJ48);
        double[] arrSMO = ArrayUtils.toPrimitive(tempArrSMO);
        double[] arrRBF = ArrayUtils.toPrimitive(tempArrRBF);

        String pvaluePSO_J48 = f.formatar(w.wilcoxonSignedRankTest(arrPSO, arrJ48, false));
        String pvaluePSO_SMO = f.formatar(w.wilcoxonSignedRankTest(arrPSO, arrSMO, false));
        String pvaluePSO_RBF = f.formatar(w.wilcoxonSignedRankTest(arrPSO, arrRBF, false));
        String pvalueJ48_SMO = f.formatar(w.wilcoxonSignedRankTest(arrJ48, arrSMO, false));
        String pvalueJ48_RBF = f.formatar(w.wilcoxonSignedRankTest(arrJ48, arrRBF, false));
        String pvalueSMO_RBF = f.formatar(w.wilcoxonSignedRankTest(arrSMO, arrRBF, false));

        System.out.println("\n\nTeste Wilcoxon MOPSO:\n");

        String strFormat = "%-10s %-10s %-10s %-10s %-10s\n";

        System.out.printf(strFormat, "", "MOPSO", "J48", "SMO", "RBF");
        System.out.printf(strFormat, "MOPSO", "-", pvaluePSO_J48, pvaluePSO_SMO, pvaluePSO_RBF);
        System.out.printf(strFormat, "J48", pvaluePSO_J48, "-", pvalueJ48_SMO, pvalueJ48_RBF);
        System.out.printf(strFormat, "SMO", pvaluePSO_SMO, pvalueJ48_SMO, "-", pvalueSMO_RBF);
        System.out.printf(strFormat, "RBF", pvaluePSO_RBF, pvalueJ48_RBF, pvalueSMO_RBF, "-");
    }

    /**
     * Cria mapa de estatísticas.
     *
     * @param f Formatador para casas decimais.
     * @param efetPSO Efetividade obtida durante as execuções pelo MOPSO.
     * @param efetJ48 Efetividade obtida durante as execuções pelo J48.
     * @param efetSMO Efetividade obtida durante as execuções pelo SMO.
     * @param efetRBF Efetividade obtida durante as execuções pelo RBF.
     * @return
     */
    private static Map<String, SummaryStatistics> criarMapStats(
            List<Double> efetPSO, List<Double> efetJ48,
            List<Double> efetSMO, List<Double> efetRBF)
    {
        Map<String, SummaryStatistics> map = new HashMap<>();

        SummaryStatistics statsPSO = new SummaryStatistics();
        for (int i = 0, size = efetPSO.size(); i < size; i++)
        {
            statsPSO.addValue(efetPSO.get(i));
        }
        map.put("MOPSO", statsPSO);

        SummaryStatistics statsJ48 = new SummaryStatistics();
        for (int i = 0, size = efetJ48.size(); i < size; i++)
        {
            statsJ48.addValue(efetJ48.get(i));
        }
        map.put("J48", statsJ48);

        SummaryStatistics statsSMO = new SummaryStatistics();
        for (int i = 0, size = efetSMO.size(); i < size; i++)
        {
            statsSMO.addValue(efetSMO.get(i));
        }
        map.put("SMO", statsSMO);

        SummaryStatistics statsRBF = new SummaryStatistics();
        for (int i = 0, size = efetRBF.size(); i < size; i++)
        {
            statsRBF.addValue(efetRBF.get(i));
        }
        map.put("RBF", statsRBF);

        return map;
    }

    /**
     * Teste OneWay Anova.
     *
     * @see https://en.wikipedia.org/wiki/One-way_analysis_of_variance
     * @param f Formatador para casas decimais.
     * @param efetPSO Efetividade obtida durante as execuções pelo MOPSO.
     * @param efetJ48 Efetividade obtida durante as execuções pelo J48.
     * @param efetSMO Efetividade obtida durante as execuções pelo SMO.
     * @param efetRBF Efetividade obtida durante as execuções pelo RBF.
     */
    private static void mostrarTesteOneWayAnova(final Formatador f,
            List<Double> efetPSO, List<Double> efetJ48, List<Double> efetSMO,
            List<Double> efetRBF)
    {
        Double[] objArrPSO = efetPSO.toArray(new Double[0]);
        Double[] objArrJ48 = efetJ48.toArray(new Double[0]);
        Double[] objArrSMO = efetSMO.toArray(new Double[0]);
        Double[] objArrRBF = efetRBF.toArray(new Double[0]);

        double[] arrPSO = ArrayUtils.toPrimitive(objArrPSO);
        double[] arrJ48 = ArrayUtils.toPrimitive(objArrJ48);
        double[] arrSMO = ArrayUtils.toPrimitive(objArrSMO);
        double[] arrRBF = ArrayUtils.toPrimitive(objArrRBF);

        List<double[]> PSO_J48 = new ArrayList<>();
        PSO_J48.add(arrPSO);
        PSO_J48.add(arrJ48);

        List<double[]> PSO_SMO = new ArrayList<>();
        PSO_SMO.add(arrPSO);
        PSO_SMO.add(arrSMO);

        List<double[]> PSO_RBF = new ArrayList<>();
        PSO_RBF.add(arrPSO);
        PSO_RBF.add(arrRBF);

        List<double[]> J48_SMO = new ArrayList<>();
        J48_SMO.add(arrJ48);
        J48_SMO.add(arrSMO);

        List<double[]> J48_RBF = new ArrayList<>();
        J48_RBF.add(arrJ48);
        J48_RBF.add(arrRBF);

        List<double[]> SMO_RBF = new ArrayList<>();
        SMO_RBF.add(arrSMO);
        SMO_RBF.add(arrRBF);

        String pvaluePSO_J48 = f.formatar(TestUtils.oneWayAnovaPValue(PSO_J48));
        String pvaluePSO_SMO = f.formatar(TestUtils.oneWayAnovaPValue(PSO_SMO));
        String pvaluePSO_RBF = f.formatar(TestUtils.oneWayAnovaPValue(PSO_RBF));
        String pvalueJ48_SMO = f.formatar(TestUtils.oneWayAnovaPValue(J48_SMO));
        String pvalueJ48_RBF = f.formatar(TestUtils.oneWayAnovaPValue(J48_RBF));
        String pvalueSMO_RBF = f.formatar(TestUtils.oneWayAnovaPValue(SMO_RBF));

        System.out.println("\n\nTeste OneWay Anova:\n");

        String strFormat = "%-10s %-10s %-10s %-10s %-10s\n";

        System.out.printf(strFormat, "", "MOPSO", "J48", "SMO", "RBF");
        System.out.printf(strFormat, "MOPSO", "-", pvaluePSO_J48, pvaluePSO_SMO, pvaluePSO_RBF);
        System.out.printf(strFormat, "J48", pvaluePSO_J48, "-", pvalueJ48_SMO, pvalueJ48_RBF);
        System.out.printf(strFormat, "SMO", pvaluePSO_SMO, pvalueJ48_SMO, "-", pvalueSMO_RBF);
        System.out.printf(strFormat, "RBF", pvaluePSO_RBF, pvalueJ48_RBF, pvalueSMO_RBF, "-");
    }

    /**
     * Teste de normalidade Kolmogorov-Smirnov.
     *
     * @see https://en.wikipedia.org/wiki/Kolmogorov%E2%80%93Smirnov_test
     * @param f Formatador para casas decimais.
     * @param efetPSO Efetividade obtida durante execuções pelo MOPSO.
     * @param efetJ48 Efetividade obtida durante execuções pelo J48.
     * @param efetSMO Efetividade obtida durante execuções pelo SMO.
     * @param efetRBF Efetividade obtida durante execuções pelo RBF.
     */
    private static void mostrarTesteDeNormalidade(final Formatador f,
            Map<String, SummaryStatistics> mapStats,
            List<Double> efetPSO, List<Double> efetJ48, List<Double> efetSMO,
            List<Double> efetRBF)
    {
        final SummaryStatistics pso = mapStats.get("MOPSO");
        final SummaryStatistics j48 = mapStats.get("J48");
        final SummaryStatistics smo = mapStats.get("SMO");
        final SummaryStatistics rbf = mapStats.get("RBF");

        final double meanPSO = pso.getMean();
        final double stdPSO = pso.getStandardDeviation();

        final double meanJ48 = j48.getMean();
        final double stdJ48 = j48.getStandardDeviation();

        final double meanSMO = smo.getMean();
        final double stdSMO = smo.getStandardDeviation();

        final double meanRBF = rbf.getMean();
        final double stdRBF = rbf.getStandardDeviation();

        System.out.println("\n\nTeste Kolmogorov-Smirnov:\n");

        double ksPSO = kolmogorovSmirnov(meanPSO, stdPSO, efetPSO);
        System.out.printf("MOPSO : %s\n", f.formatar(ksPSO));

        double ksJ48 = kolmogorovSmirnov(meanJ48, stdJ48, efetJ48);
        System.out.printf("J48   : %s\n", f.formatar(ksJ48));

        double ksSMO = kolmogorovSmirnov(meanSMO, stdSMO, efetSMO);
        System.out.printf("SMO   : %s\n", f.formatar(ksSMO));

        double ksRBF = kolmogorovSmirnov(meanRBF, stdRBF, efetRBF);
        System.out.printf("RBF   : %s\n", f.formatar(ksRBF));
    }

    /**
     * Teste de Normalidade (Kolmogorov-Smirnov Test).
     *
     * @param mediaAlg Média do algoritmo.
     * @param desvioAlg Desvio padrão do algoritmo.
     * @param efetAlg Efetividade Efetividade obtida durante as execuções pelo
     * algoritmo.
     * @return
     * @throws InsufficientDataException
     * @throws NullArgumentException
     * @throws NotStrictlyPositiveException
     */
    private static double kolmogorovSmirnov(final double mediaAlg,
            final double desvioAlg, List<Double> efetAlg) throws InsufficientDataException, NullArgumentException, NotStrictlyPositiveException
    {
        final NormalDistribution normdist = new NormalDistribution(mediaAlg, desvioAlg);
        Double[] arrObj = efetAlg.toArray(new Double[0]);
        final double test = TestUtils.kolmogorovSmirnovTest(normdist, ArrayUtils.toPrimitive(arrObj), false);
        return test;
    }
}
