package com.blogspot.tsprates.pso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Classe Fitness.
 *
 * @author thiago
 */
public class Fitness implements FitnessInterface {

    private final Connection conexao;

    private final String tabela;

    private final String colId;

    private final Map<String, Set<Integer>> classeSaidas;

    private List<String> resultado;

    /**
     * Construtor.
     * 
     * @param conexao
     * @param colId
     * @param tabela
     * @param classeSaidas
     */
    public Fitness(Connection conexao, final String colId, final String tabela,
	    final Map<String, Set<Integer>> classeSaidas) {
	this.conexao = conexao;
	this.colId = colId;
	this.tabela = tabela;
	this.classeSaidas = classeSaidas;
    }

    /**
     * 
     */
    public double[] calcula(Particula p) {
	double[] result = new double[2];
	result[0] = (double) calculaAcurarcia(p);
	result[1] = (double) p.getNumWhereSql();
	return result;
    }

    /**
     * Recupera classe para determinada cláusula SQL WHERE.
     * 
     * @param whereSql
     *            String de uma cláusula WHERE.
     * @return Retorna lista resultante da WHERE.
     */
    private List<String> avaliaSql(String whereSql) {
	List<String> result = new ArrayList<String>();
	String sql = "SELECT " + colId + " AS id FROM " + tabela + " WHERE "
		+ whereSql;

	try (PreparedStatement ps = conexao.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
	    while (rs.next()) {
		result.add(rs.getString("id"));
	    }

	    return result;
	} catch (SQLException e) {
	    throw new RuntimeException(
		    "Erro ao recupera as classes no banco de dados.", e);
	}
    }

    /**
     * Calcula a acurácia de cada partícula.
     * 
     * @param p
     *            Partícula.
     * @return
     */
    private double calculaAcurarcia(Particula p) {
	Set<Integer> listaId = classeSaidas.get(p.getClasse());
	
	resultado = avaliaSql(p.getWhereSql());
	
	int totalEncontrado = 0;
	for (int id : listaId) {
	    if (resultado.contains(String.valueOf(id))) {
		totalEncontrado += 1;
	    }
	}
	
	int somaTotal = 0;
	for (String chave : classeSaidas.keySet()) {
	    somaTotal += classeSaidas.get(chave).size();
	}
	
	return ((listaId.size() - totalEncontrado) + totalEncontrado) / somaTotal;
    }
}