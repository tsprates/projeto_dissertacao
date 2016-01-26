package com.blogspot.tsprates.pso;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Banco de dados.
 * 
 * @author thiago
 *
 */
public class DbFactory {
    /**
     * Nome do banco de dados.
     */
    private final String DB_NAME = "geominas";

    /**
     * Usuário do banco de dados.
     */
    private final String USERNAME = "postgres";

    /**
     * Senha do banco de dados.
     */
    private final String PASSWORD = "admin";

    /**
     * Conexão banco de dados.
     * 
     * @return Connection
     */
    public Connection conecta() {
	try {
	    return DriverManager.getConnection("jdbc:postgresql://localhost/"
		    + DB_NAME, USERNAME, PASSWORD);
	} catch (SQLException e) {
	    throw new RuntimeException(e);
	}
    }

}
