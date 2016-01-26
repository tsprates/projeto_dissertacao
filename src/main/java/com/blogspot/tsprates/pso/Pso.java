package com.blogspot.tsprates.pso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.RandomUtils;

/**
 * Particles Swarm Optimization
 * 
 * @author thiago
 *
 */
public class Pso {

    private final Connection conexao;

    private final static String[] OPERADORES = { "=", "!=", ">", ">=", "<",
	    ">=" };

    private final List<Particula> particulas = new ArrayList<Particula>();

    private final List<String> tipoSaidas = new ArrayList<String>();

    private final Map<String, Set<Integer>> mapaSaidas = new HashMap<String, Set<Integer>>();

    private String[] colunas;

    private double[] max;

    private double[] min;

    private String colSaida, colCod;

    private int numPop;

    private final Random sorteio = new Random();

    private final static String TABELA = "wine_class";

    /**
     * Construtor.
     *
     * @param c
     * @param p
     */
    public Pso(Connection c, Properties p) {
	this.conexao = c;
	// this.props = p;

	this.colSaida = (String) p.get("saida");
	this.colCod = (String) p.get("id");
	this.numPop = Integer.valueOf((String) p.get("npop"));

	recuperaColunas();

	recuperaClassesSaida();
	controiMapaSaidas();

	recuperaMaxMinDasEntradas();
    }

    private void controiMapaSaidas() {
	for (String saida : tipoSaidas) {
	    mapaSaidas.put(saida, new HashSet<Integer>());
	}

	String sql = "select " + colSaida + ", " + colCod + " as cod from "
		+ TABELA;
	try {
	    PreparedStatement ps = conexao.prepareStatement(sql);
	    ResultSet rs = ps.executeQuery();

	    while (rs.next()) {
		String col = rs.getString(colSaida);
		mapaSaidas.get(col).add(rs.getInt("cod"));
	    }

	    ps.close();
	    rs.close();
	} catch (SQLException e) {
	    throw new RuntimeException(
		    "Erro ao mapear nome das colunas com seu código(id).", e);
	}
    }

    /**
     * 
     */
    public void carrega() {

	geraPopulacaoInicial();
    }

    /**
     *
     */
    private void recuperaColunas() {
	String sql = "select * from " + TABELA + " limit 1";

	try {
	    PreparedStatement ps = conexao.prepareStatement(sql);
	    ResultSet rs = ps.executeQuery();
	    ResultSetMetaData metadata = rs.getMetaData();

	    int numCol = metadata.getColumnCount();
	    colunas = new String[numCol - 2];
	    max = new double[numCol - 2];
	    min = new double[numCol - 2];

	    for (int i = 0, j = 0; i < numCol; i++) {
		String coluna = metadata.getColumnName(i + 1);

		if (!colSaida.equalsIgnoreCase(coluna)
			&& !colCod.equalsIgnoreCase(coluna)) {
		    colunas[j] = coluna;
		    j++;
		}
	    }

	    // System.out.println(Arrays.toString(colunas));

	    ps.close();
	    rs.close();
	} catch (SQLException e) {
	    throw new RuntimeException("Erro ao recuperar nome das colunas.", e);
	} finally {

	}
    }

    /**
     * 
     */
    private void recuperaClassesSaida() {
	String sql = "select distinct " + colSaida + " from " + TABELA;

	try {
	    PreparedStatement ps = conexao.prepareStatement(sql);
	    ResultSet rs = ps.executeQuery();

	    while (rs.next()) {
		tipoSaidas.add(rs.getString(colSaida));
	    }

	    ps.close();
	    rs.close();
	} catch (SQLException e) {
	    throw new RuntimeException(
		    "Erro ao classe saída no banco de dados.", e);
	}
    }

    /**
     * 
     */
    private void recuperaMaxMinDasEntradas() {

	// faixa de valores de cada coluna
	StringBuilder sb = new StringBuilder();
	for (String entrada : colunas) {
	    sb.append(", ").append("max(").append(entrada).append(")")
		    .append(", ").append("min(").append(entrada).append(")");
	}
	String sql = "select " + sb.toString().substring(1) + " from " + TABELA;

	try {
	    PreparedStatement ps = conexao.prepareStatement(sql);
	    ResultSet rs = ps.executeQuery();

	    int numCol = colunas.length * 2;
	    while (rs.next()) {
		for (int i = 0, j = 0; i < numCol; i += 2, j++) {
		    max[j] = rs.getDouble(i + 1);
		    min[j] = rs.getDouble(i + 2);
		}
	    }

	    ps.close();
	    rs.close();
	} catch (SQLException e) {
	    throw new RuntimeException(
		    "Erro ao buscar (min, max) no banco de dados.", e);
	}
    }

    private void geraPopulacaoInicial() {
	for (int i = 0; i < numPop; i++) {
	    List<String> vel = criaListaWhere();
	    List<String> pos = criaListaWhere();

	    int size = tipoSaidas.size();
	    String classe = tipoSaidas.get(i % size);
	    Particula particula = new Particula(vel, pos, classe);
	    
	    particulas.add(particula);
	}
    }

    private List<String> criaListaWhere() {
	int numCol = colunas.length;
	int numOper = OPERADORES.length;
	int maxWhere = sorteio.nextInt(numCol) + 1;

	List<String> listaWhere = new ArrayList<>();

	for (int i = 0; i < maxWhere; i++) {
	    int col = sorteio.nextInt(numCol);
	    int oper = sorteio.nextInt(numOper);

	    String cond2;

	    if (sorteio.nextDouble() > 0.5) {
		cond2 = String.valueOf(RandomUtils.nextDouble(min[col],
			max[col]));
	    } else {
		int index = sorteio.nextInt(numCol);
		cond2 = colunas[index];
	    }

	    String whereSql = String.format("%s %s %s", colunas[col],
		    OPERADORES[oper], cond2);

	    listaWhere.add(whereSql);
	}

	return listaWhere;
    }

    /**
     * 
     */
    public void mostraPopulacao() {
	for (Particula p : particulas) {
	    System.out.println(p.getVelocidadeSql());
	}

	System.out.println("Classes");

	for (String classe : mapaSaidas.keySet()) {
	    Set<Integer> conj = mapaSaidas.get(classe);
	    System.out.println(classe + ") " + conj.size());
	}
    }

}
