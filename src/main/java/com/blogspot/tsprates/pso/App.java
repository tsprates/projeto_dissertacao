package com.blogspot.tsprates.pso;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.util.Properties;

/**
 * Programa.
 * 
 * @author thiago
 *
 */
public class App {
    
    /**
     * Main.
     * 
     * @param args
     */
    public static void main(String[] args) {
	System.out.println("----------------------------------------");
	System.out.println("    Projeto de Dissertação Mestrado     ");
	System.out.println("    Implementação PSO                   ");
	System.out.println("----------------------------------------");
	System.out.println();

	Connection conexaoDb = new DbFactory().conecta();
	Properties config = getConfigs();	
	PSO pso = new PSO(conexaoDb, config);
	pso.carrega();
	pso.mostraPopulacao();
    }

    
    /**
     * Retorna arquivo de configurações. 
     * 
     * @return Properties
     */
    private static Properties getConfigs() {
	try {
	    FileInputStream fis = new FileInputStream("configs.txt");
	    Properties prop = new Properties();
	    prop.load(fis);
	    fis.close();
	    return prop;
	} catch (IOException e) {
	    throw new RuntimeException("Arquivo de configurações não encontrado.", e);
	}
    }
}
