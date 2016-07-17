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

            final Formatador formatador = new Formatador();

            final int K = 10;
            Pso pso = new Pso(db, config, formatador, K);

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
                efetJ48.add(medEfetJ48 / numCls);
                efetSMO.add(medEfetSMO / numCls);
                efetRBF.add(medEfetRBF / numCls);

                // Verifica se matriz de classes foi instanciada
                if (matCls == null)
                {
                    matCls = new double[4][numCls];

                    for (int i = 0; i < numCls; i++)
                    {
                        matCls[0][i] = 0.0; // MOPSO
                        matCls[1][i] = 0.0; // SMO
                        matCls[2][i] = 0.0; // RBF
                        matCls[3][i] = 0.0; // J48
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
                    matCls[1][i] /= EXEC;  // SMO
                    matCls[2][i] /= EXEC;  // RBF
                    matCls[3][i] /= EXEC;  // J48
                }
            }

            if (matCls != null)
            {
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
                        String v = formatador.formatar(matCls[i][j]);
                        System.out.printf(" %-10s", v);
                    }
                    System.out.println();
                }
            }

            mostrarValorMedioExec(formatador, efetPSO, efetJ48, efetSMO, efetRBF);

            wilcoxonTeste(formatador, efetPSO, efetJ48, efetSMO, efetRBF);

            mostrarGrafico(config, efetPSO, efetJ48, efetSMO, efetRBF);
        }
        else
        {
            System.err.println(
                    "É necessário definir um arquivo de configuração.");
        }
    }

    /**
     * Carrega arquivo de configurações.
     *
     * @param configFile Configurations
     * @return Properties
     */
    private static Properties carregarArquivoConfig(String configFile)
    {
        try (FileInputStream fis = new FileInputStream(configFile))
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
     * Gráfico valor médio execuções.
     *
     * @param config
     * @param efetPSO
     * @param efetJ48
     * @param efetSMO
     * @param efetRBF
     */
    private static void mostrarGrafico(Properties config, List<Double> efetPSO,
            List<Double> efetJ48, List<Double> efetSMO, List<Double> efetRBF)
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
     * Teste não-paramétrico para verificar se os resultados médios pertencem a
     * mesma distribuição estatística.
     *
     * @param fmt
     * @param efetPSO
     * @param efetJ48
     * @param efetSMO
     * @param efetRBF
     */
    private static void wilcoxonTeste(final Formatador fmt,
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

        String pvaluePSO_J48 = fmt.formatar(w.wilcoxonSignedRankTest(arrPSO, arrJ48, false));
        String pvaluePSO_SMO = fmt.formatar(w.wilcoxonSignedRankTest(arrPSO, arrSMO, false));
        String pvaluePSO_RBF = fmt.formatar(w.wilcoxonSignedRankTest(arrPSO, arrRBF, false));
        String pvalueJ48_SMO = fmt.formatar(w.wilcoxonSignedRankTest(arrJ48, arrSMO, false));
        String pvalueJ48_RBF = fmt.formatar(w.wilcoxonSignedRankTest(arrJ48, arrRBF, false));
        String pvalueSMO_RBF = fmt.formatar(w.wilcoxonSignedRankTest(arrSMO, arrRBF, false));

        System.out.println("\n\nTeste Wilcoxon MOPSO:\n");

        String strFormat = "%-10s %-10s %-10s %-10s %-10s\n";

        System.out.printf(strFormat, "", "MOPSO", "J48", "SMO", "RBF");
        System.out.printf(strFormat, "MOPSO", "-", pvaluePSO_J48, pvaluePSO_SMO, pvaluePSO_RBF);
        System.out.printf(strFormat, "J48", pvaluePSO_J48, "-", pvalueJ48_SMO, pvalueJ48_RBF);
        System.out.printf(strFormat, "SMO", pvaluePSO_SMO, pvalueJ48_SMO, "-", pvalueSMO_RBF);
        System.out.printf(strFormat, "RBF", pvaluePSO_RBF, pvalueJ48_RBF, pvalueSMO_RBF, "-");
    }

    /**
     * Imprime a média e desvio padrão das execuções dos algoritmos.
     *
     * @param fmt
     * @param efetPSO
     * @param efetJ48
     * @param efetSMO
     * @param efetRBF
     */
    private static void mostrarValorMedioExec(final Formatador fmt,
            List<Double> efetPSO, List<Double> efetJ48, List<Double> efetSMO,
            List<Double> efetRBF)
    {
        SummaryStatistics statsPSO = new SummaryStatistics();
        for (int i = 0, size = efetPSO.size(); i < size; i++)
        {
            statsPSO.addValue(efetPSO.get(i));
        }

        SummaryStatistics statsJ48 = new SummaryStatistics();
        for (int i = 0, size = efetJ48.size(); i < size; i++)
        {
            statsJ48.addValue(efetJ48.get(i));
        }

        SummaryStatistics statsSMO = new SummaryStatistics();
        for (int i = 0, size = efetSMO.size(); i < size; i++)
        {
            statsSMO.addValue(efetSMO.get(i));
        }

        SummaryStatistics statsRBF = new SummaryStatistics();
        for (int i = 0, size = efetRBF.size(); i < size; i++)
        {
            statsRBF.addValue(efetRBF.get(i));
        }

        System.out.println("\n");
        System.out.printf("%-10s %-10s %-10s\n\n", "Alg.", "Méd.", "Desv.");

        String strFormat = "%-10s %-10s %-10s\n";

        System.out.printf(strFormat,
                "MOPSO",
                fmt.formatar(statsPSO.getMean()),
                fmt.formatar(statsPSO.getStandardDeviation()));

        System.out.printf(strFormat,
                "J48",
                fmt.formatar(statsJ48.getMean()),
                fmt.formatar(statsJ48.getStandardDeviation()));

        System.out.printf(strFormat,
                "SMO",
                fmt.formatar(statsSMO.getMean()),
                fmt.formatar(statsSMO.getStandardDeviation()));

        System.out.printf(strFormat,
                "RBF",
                fmt.formatar(statsRBF.getMean()),
                fmt.formatar(statsRBF.getStandardDeviation()));
    }
}
