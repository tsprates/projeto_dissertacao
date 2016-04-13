package com.blogspot.tsprates.pso;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
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
        
        if (args[0] != null && Files.exists(Paths.get(args[0]))) {
            Connection conexaoDb = new DbFactory().conecta();
            Properties config = getConfigs(args[0]);
            Pso pso = new Pso(conexaoDb, config);
//            pso.mostraPopulacao();
            pso.carrega();            
        } else {
            System.err.println("É necessário definir um arquivo de configuração.");
        }


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