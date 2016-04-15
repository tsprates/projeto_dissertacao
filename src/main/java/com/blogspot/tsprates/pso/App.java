package com.blogspot.tsprates.pso;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

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
            Connection conexaoDb = new DbFactory().conecta();
            Properties config = getConfigs(args[0]);
            Pso pso = new Pso(conexaoDb, config);
//            pso.mostraPopulacao();
            pso.carrega();

            final String tituloGrafico = config.getProperty("tabela");
            final Map<String, List<Double>> efetividade = pso.getEfetividade();
            mostraGrafico(tituloGrafico, efetividade);
        }
        else
        {
            System.err.println("É necessário definir um arquivo de configuração.");
        }

    }

    /**
     * Gráfico de resultado
     *
     * @param tituloGrafico
     * @param pso
     */
    private static void mostraGrafico(final String tituloGrafico,
            Map<String, List<Double>> mapa)
    {
        Grafico g = new Grafico(tituloGrafico);
        for (Entry<String, List<Double>> ent : mapa.entrySet())
        {
            g.adicionaSerie(ent.getKey(), ent.getValue());
        }
        g.mostra();
    }

    /**
     * Retorna arquivo de configurações.
     *
     * @param configFile Configurations
     * @return Properties
     */
    private static Properties getConfigs(String configFile)
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
