package com.blogspot.tsprates.pso;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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

            final int exec = 30;

            Pso pso = new Pso(conexaoDb, config, format);
            final Set<String> classes = pso.getClasses();

            Map<String, List<Double>> efetividade;

            List<Double> efetividadeGlobal = new ArrayList<>();

            for (int iter = 0; iter < exec; iter++)
            {
                System.out.println();
                System.out.println("Execução: " + (iter + 1));
                System.out.println();

                pso.carregar();

                efetividade = pso.getEfetividade();

                double soma = 0;
                for (String c : classes)
                {
                    soma += Collections.max(efetividade.get(c));
                }
                efetividadeGlobal.add(soma / (double) classes.size());

            }

            final String tituloGrafico = StringUtils.capitalize(config
                    .getProperty("tabela"));
            final String eixoX = "Execução";
            final String eixoY = "Sensibilidade x Especificidade";
            Grafico g = new Grafico(tituloGrafico, eixoX, eixoY);
            g.adicionaSerie("MOPSO", efetividadeGlobal);
            g.mostra();
        }
        else
        {
            System.err
                    .println("É necessário definir um arquivo de configuração.");
        }

    }

    /**
     * Retorna arquivo de configurações.
     *
     * @param configFile
     *            Configurations
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
