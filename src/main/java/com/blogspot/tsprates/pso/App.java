package com.blogspot.tsprates.pso;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.util.Properties;

/**
 * Particles Swarm Optimization (Pso).
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
	System.out.println(" Projeto de Dissertação Mestrado        ");
	System.out.println(" Implementação PSO                      ");
	System.out.println("----------------------------------------");
	System.out.println();

	Connection conexaoDb = new DbFactory().conecta();
	Properties config = getConfigs();
	Pso pso = new Pso(conexaoDb, config);
	pso.carrega();
	// pso.mostraPopulacao();

    }

    /**
     * Retorna arquivo de configurações.
     * 
     * @return Properties
     */
    private static Properties getConfigs() {
	FileInputStream fis = null;
	try {
	    fis = new FileInputStream("configs.txt");
	    Properties prop = new Properties();
	    prop.load(fis);
	    return prop;
	} catch (IOException e) {
	    throw new RuntimeException(
		    "Arquivo de configurações não encontrado.", e);
	} finally {
	    if (fis != null) {
		try {
		    fis.close();
		} catch (IOException e) {
		    throw new RuntimeException(
			    "Erro fechar o arquivo de configurações.", e);
		}
	    }
	}
    }
}
