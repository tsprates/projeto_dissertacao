package com.blogspot.tsprates.pso;

import weka.core.Instances;
import weka.core.converters.DatabaseSaver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Classe para importação no banco de dados.
 *
 * @author thiago
 */
public class DBUtils
{

    /**
     * Importa arquivo arff (Weka) para o banco de dados.
     *
     * @param arff Caminho do arquivo arff.
     * @param tabela Nome da nova tabela referente ao arquivo arff.
     */
    public static void importarArff(String arff, String tabela)
    {
        String url = "jdbc:postgresql://localhost/geominas";
        String user = "postgres";
        String password = "admin";

        try (BufferedReader reader = new BufferedReader(new FileReader(arff)))
        {
            Instances data = new Instances(reader);
            data.setClassIndex(data.numAttributes() - 1);

            DatabaseSaver save = new DatabaseSaver();
            save.setUrl(url);
            save.setUser(user);
            save.setPassword(password);
            save.setInstances(data);
            save.setRelationForTableName(false);
            save.setTableName(tabela);
            save.connectToDatabase();
            save.writeBatch();
        }
        catch (Exception ex)
        {
            throw new RuntimeException("Erro ao importar arquivo arff.", ex);
        }

        try (Connection con = DriverManager.getConnection(url, user, password))
        {
            PreparedStatement[] ps = new PreparedStatement[2];
            ps[0] = con.prepareStatement("ALTER TABLE " + tabela + " ADD COLUMN id SERIAL;");
            ps[1] = con.prepareStatement("ALTER TABLE " + tabela + " ADD PRIMARY KEY (id);");

            for (PreparedStatement p : ps)
            {
                p.execute();
            }
        }
        catch (SQLException ex)
        {
            throw new RuntimeException("Erro ao coluna ID para arquivo arff importado.", ex);
        }

        System.out.println("Arquivo arff importado com sucesso.");
    }
}
