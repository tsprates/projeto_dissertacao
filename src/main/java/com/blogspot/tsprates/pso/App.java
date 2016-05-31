package com.blogspot.tsprates.pso;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 * Particles Swarm Optimization (PSO).
 *
 * @author thiago
 *
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
            Properties config = carregarConfigArquivo(args[0]);

            final Formatador fmt = new Formatador();

            final int K = 10;
            Pso pso = new Pso(db, config, fmt, K);

            List<Double> efetPSO = new ArrayList<>();
            List<Double> efetJ48 = new ArrayList<>();
            List<Double> efetSMO = new ArrayList<>();
            List<Double> efetRBF = new ArrayList<>();

            String tabela = config.getProperty("tabela");
            String colSaida = config.getProperty("saida");
            String colId = config.getProperty("id");

            final int EXEC = 50;
            for (int iter = 0; iter < EXEC; iter++)
            {
                System.out.println();
                System.out.println("Execução: " + (iter + 1));
                System.out.println();

                pso.carregar();
                pso.mostrarResultados();

                efetPSO.add(pso.getResultado());

                Weka ad = new Weka(pso.getKPasta(), K, tabela, colId, colSaida);
                double[] efetArray = ad.getEfetividadeArray();
                efetJ48.add(efetArray[0]);
                efetSMO.add(efetArray[1]);
                efetRBF.add(efetArray[2]);
            }

            SummaryStatistics statsPso = new SummaryStatistics();
            for (int i = 0, size = efetPSO.size(); i < size; i++)
            {
                statsPso.addValue(efetPSO.get(i));
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

            System.out.println("\nAlg. \tMéd. \tDesv.\n");
            
            System.out.printf("PSO \t%s \t%s \n", 
                    fmt.formatar(statsPso.getMean()), 
                    fmt.formatar(statsPso.getStandardDeviation()));
            
            System.out.printf("J48 \t%s \t%s\n", 
                    fmt.formatar(statsJ48.getMean()), 
                    fmt.formatar(statsJ48.getStandardDeviation()));
            
            System.out.printf("SMO \t%s \t%s\n", 
                    fmt.formatar(statsSMO.getMean()), 
                    fmt.formatar(statsSMO.getStandardDeviation()));
            
            System.out.printf("RBF \t%s \t%s\n", 
                    fmt.formatar(statsRBF.getMean()), 
                    fmt.formatar(statsRBF.getStandardDeviation()));

            // mostra o gráfico
            final String tituloGrafico = StringUtils
                    .capitalize(config.getProperty("tabela"));
            final String eixoX = "Execução";
            final String eixoY = "Sensibilidade x Especificidade";
            Grafico g = new Grafico(tituloGrafico, eixoX, eixoY);
            g.adicionaSerie("MOPSO", efetPSO);
            g.adicionaSerie("J48", efetJ48);
            g.adicionaSerie("SMO", efetSMO);
            g.adicionaSerie("RBF", efetRBF);
            g.mostra();
        }
        else
        {
            System.err.println(
                    "É necessário definir um arquivo de configuração.");
        }

    }

    /**
     * Retorna arquivo de configurações.
     *
     * @param configFile Configurations
     * @return Properties
     */
    private static Properties carregarConfigArquivo(String configFile)
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
}
