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
public class Fitness implements InterfaceFitness {

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
     * Calcula fitness.
     */
    @Override
    public double[] calcula(Particula p) {
	double[] result = new double[2];
	result[0] = (double) calculaAcurarcia(p);
	result[1] = (double) p.getNumWhere();
	return result;
    }

    /**
     * Recupera classe para determinada cláusula SQL WHERE.
     * 
     * @param whereSql String de uma cláusula WHERE.
     * @return Retorna lista de String correspondente a uma cláusula WHERE.
     */
    private List<String> avaliaSql(String whereSql) {
	List<String> result = new ArrayList<>();
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
     * @param p Partícula.
     * @return Retorna o valor da acurácia obtido.
     */
    private double calculaAcurarcia(Particula p) {
	Set<Integer> lista = classeSaidas.get(p.getClasse());
	
	resultado = avaliaSql(p.asWhereSql());
	
	int vp = 0; // verdadeiro positivo
	for (int id : lista) {
	    if (resultado.contains(String.valueOf(id))) {
		vp += 1;
	    }
	}
	
	int nv = 0; // verdadeiro negativo
	for (String i : resultado) {
	    if (!lista.contains(Integer.valueOf(i))) {
		vp += 1;
	    }
	}
	
	int total = 0;
	for (String chave : classeSaidas.keySet()) {
	    total += classeSaidas.get(chave).size();
	}
	
	return (nv + vp) / total;
    }
}