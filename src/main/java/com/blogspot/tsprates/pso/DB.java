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
public class DB
{

    /**
     * Nome do banco de dados.
     */
    public final static String DBNAME = "geominas";

    /**
     * Usuário do banco de dados.
     */
    public final static String USERNAME = "postgres";

    /**
     * Senha do banco de dados.
     */
    public final static String PASSWORD = "admin";

    /**
     * Conexão banco de dados.
     *
     * @return Connection
     */
    public Connection conectar()
    {
        try
        {
            return DriverManager.getConnection("jdbc:postgresql://localhost/"
                    + DBNAME, USERNAME, PASSWORD);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

}
