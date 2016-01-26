package com.blogspot.tsprates.pso;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.util.Properties;

public class App {
    public static void main(String[] args) {
	System.out.println("Projeto de Dissertação Mestrado");
	System.out.println("Implementação PSO");

	Connection conexaoDb = new DbFactory().conecta();
	Properties config = getConfigProperties();	
	Pso pso = new Pso(conexaoDb, config);
	pso.carrega();
	pso.mostraPopulacao();
    }

    
    /**
     * Retorna arquivo de configurações. 
     * 
     * @return Properties
     */
    private static Properties getConfigProperties() {
	try {
	    FileInputStream fis = new FileInputStream("configs.txt");
	    Properties prop = new Properties();
	    prop.load(fis);
	    fis.close();
	    return prop;
	} catch (IOException e) {
	    throw new RuntimeException(
		    "Arquivo de configurações não encontrado.", e);
	}
    }
}
