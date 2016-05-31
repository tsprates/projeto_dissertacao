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
            Connection conexaoDb = new DbFactory().conectar();
            Properties config = carregarConfigArquivo(args[0]);

            final Formatador format = new Formatador();

            final int exec = 50;

            final int kpastas = 10;
            Pso pso = new Pso(conexaoDb, config, format, kpastas);

            List<Double> efetPsoExec = new ArrayList<>();
            List<Double> efetJ48Exec = new ArrayList<>();
            List<Double> efetSMOExec = new ArrayList<>();
            List<Double> efetRBFExec = new ArrayList<>();
            
            String tabela = config.getProperty("tabela");
            String colSaida = config.getProperty("saida");
            String colId = config.getProperty("id");
            

            for (int iter = 0; iter < exec; iter++)
            {
                System.out.println();
                System.out.println("Execução: " + (iter + 1));
                System.out.println();

                pso.carregar();
                pso.mostrarResultados();

                efetPsoExec.add(pso.getResultado());
                
                Weka ad = new Weka(pso.getKPasta(), kpastas, tabela, colId, colSaida);
                double[] efetArray = ad.getEfetividadeArray();
                efetJ48Exec.add(efetArray[0]);
                efetSMOExec.add(efetArray[1]);
                efetRBFExec.add(efetArray[2]);
            }

            // mostra o gráfico
            final String tituloGrafico = StringUtils
                    .capitalize(config.getProperty("tabela"));
            final String eixoX = "Execução";
            final String eixoY = "Sensibilidade x Especificidade";
            Grafico g = new Grafico(tituloGrafico, eixoX, eixoY);
            g.adicionaSerie("MOPSO", efetPsoExec);
            g.adicionaSerie("J48", efetJ48Exec);
            g.adicionaSerie("SMO", efetSMOExec);
            g.adicionaSerie("RBF", efetRBFExec);
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
