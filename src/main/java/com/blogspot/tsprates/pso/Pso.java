package com.blogspot.tsprates.pso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.RandomUtils;

/**
 * Particles Swarm Optimization (Pso).
 *
 * @author thiago
 *
 */
public class Pso {

    private final Connection conexao;

    private final static String[] LISTA_OPERADORES = { "=", "!=", ">", ">=",
	    "<", ">=" };

    private final List<Particula> particulas = new ArrayList<Particula>();

    private final List<String> tipoSaidas = new ArrayList<String>();

    private final Map<String, Set<Integer>> classeSaidas = new HashMap<String, Set<Integer>>();

    private final String tabela;

    private final String colSaida, colId;

    private final int maxIter;

    private final int numPop;

    private String[] colunas;

    private double[] max, min;

    private final Random sorteio = new Random();

    private final Fitness fitness;

    /**
     * Construtor.
     *
     * @param c
     * @param p
     */
    public Pso(Connection c, Properties p) {
	this.conexao = c;
	this.tabela = (String) p.get("tabela");
	this.colSaida = (String) p.get("saida");
	this.colId = (String) p.get("id");
	this.numPop = Integer.valueOf((String) p.get("npop"));
	this.maxIter = Integer.valueOf((String) p.get("maxiter"));

	recuperaColunas();
	recuperaClassesSaida();
	recuperaClasseSaidas();
	recuperaMaxMinDasEntradas();

	this.fitness = new Fitness(c, colId, tabela, classeSaidas);
    }

    /**
    *
    */
    public void carrega() {
	List<Particula> pop = geraPopulacaoInicial();
	
	for (Entry<String, Set<Integer>> c: classeSaidas.entrySet()) {
	    System.out.println(c.getKey() + " " + c.getValue().size() + " " + c.getValue());
	}
	System.out.println();
	
	for (int i = 0; i < maxIter; i++) {
	}

	for (Particula p : pop) {
	    System.out.println(p.getClasse() + " " + p.getWhereSql());
	    double[] resultado = fitness.calcula(p);
	    System.out.println(Arrays.toString(resultado));
	    System.out.println();
	}
	
	
    }

    /**
     *
     */
    private void recuperaClasseSaidas() {
	PreparedStatement ps = null;
	ResultSet rs = null;

	for (String saida : tipoSaidas) {
	    classeSaidas.put(saida, new HashSet<Integer>());
	}

	String sql = "SELECT " + colSaida + ", " + colId + " AS cod FROM "
		+ tabela;
	try {
	    ps = conexao.prepareStatement(sql);
	    rs = ps.executeQuery();

	    while (rs.next()) {
		String col = rs.getString(colSaida);
		classeSaidas.get(col).add(rs.getInt("cod"));
	    }
	} catch (SQLException e) {
	    throw new RuntimeException(
		    "Erro ao mapear nome das colunas com seu código(id).", e);
	} finally {
	    if (ps != null) {
		try {
		    ps.close();
		} catch (SQLException e) {
		}
	    }

	    if (rs != null) {
		try {
		    rs.close();
		} catch (SQLException e) {
		}
	    }
	}
    }

    /**
     *
     */
    private void recuperaColunas() {
	PreparedStatement ps = null;
	ResultSet rs = null;
	ResultSetMetaData metadata = null;
	String sql = "SELECT * FROM " + tabela + " LIMIT 1";
	int numCol;

	try {
	    ps = conexao.prepareStatement(sql);
	    rs = ps.executeQuery();

	    metadata = rs.getMetaData();
	    numCol = metadata.getColumnCount();

	    colunas = new String[numCol - 2];
	    max = new double[numCol - 2];
	    min = new double[numCol - 2];

	    for (int i = 0, j = 0; i < numCol; i++) {
		String coluna = metadata.getColumnName(i + 1);

		if (!colSaida.equalsIgnoreCase(coluna)
			&& !colId.equalsIgnoreCase(coluna)) {
		    colunas[j] = coluna;
		    j++;
		}
	    }
	} catch (SQLException e) {
	    throw new RuntimeException("Erro ao recuperar nome das colunas.", e);
	} finally {
	    if (ps != null) {
		try {
		    ps.close();
		} catch (SQLException e) {
		}
	    }

	    if (rs != null) {
		try {
		    rs.close();
		} catch (SQLException e) {
		}
	    }
	}
    }

    /**
     *
     */
    private void recuperaClassesSaida() {
	PreparedStatement ps = null;
	ResultSet rs = null;
	String sql = "SELECT DISTINCT " + colSaida + " FROM " + tabela;

	try {
	    ps = conexao.prepareStatement(sql);
	    rs = ps.executeQuery();

	    while (rs.next()) {
		tipoSaidas.add(rs.getString(colSaida));
	    }
	} catch (SQLException e) {
	    throw new RuntimeException(
		    "Erro ao classe saída no banco de dados.", e);
	} finally {
	    if (ps != null) {
		try {
		    ps.close();
		} catch (SQLException e) {
		}
	    }

	    if (rs != null) {
		try {
		    rs.close();
		} catch (SQLException e) {
		}
	    }
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

	String sql = "SELECT " + sb.toString().substring(1) + " FROM " + tabela;

	PreparedStatement ps = null;
	ResultSet rs = null;

	try {
	    ps = conexao.prepareStatement(sql);
	    rs = ps.executeQuery();

	    int numCol = colunas.length * 2;
	    while (rs.next()) {
		for (int i = 0, j = 0; i < numCol; i += 2, j++) {
		    max[j] = rs.getDouble(i + 1);
		    min[j] = rs.getDouble(i + 2);
		}
	    }

	} catch (SQLException e) {
	    throw new RuntimeException(
		    "Erro ao buscar (min, max) no banco de dados.", e);
	} finally {
	    if (ps != null) {
		try {
		    ps.close();
		} catch (SQLException e) {
		}
	    }

	    if (rs != null) {
		try {
		    rs.close();
		} catch (SQLException e) {
		}
	    }
	}
    }

    /**
     * Gera população inicial.
     * 
     * @return Lista contendo a população de partículas.
     */
    private List<Particula> geraPopulacaoInicial() {
	for (int i = 0; i < numPop; i++) {
	    List<String> vel = criaListaWhere();
	    List<String> pos = criaListaWhere();

	    int size = tipoSaidas.size();
	    String classe = tipoSaidas.get(i % size);
	    Particula particula = new Particula(vel, pos, classe);

	    particulas.add(particula);
	}

	return particulas;
    }

    /**
     * Retorna uma lista com clásulas WHERE.
     * 
     * @return Lista com clásulas WHERE.
     */
    private List<String> criaListaWhere() {
	int numCol = colunas.length;
	int numOper = LISTA_OPERADORES.length;

	int maxClausulasWhere = sorteio.nextInt(5) + 1;

	List<String> listaDeClausulasWhere = new ArrayList<String>();

	for (int i = 0; i < maxClausulasWhere; i++) {
	    int colIndex = sorteio.nextInt(numCol);
	    int operIndex = sorteio.nextInt(numOper);

	    // verifica se comparação ocorrerá com outra coluna ou numericamente
	    String valor;
	    if (sorteio.nextDouble() > 0.5) {
		valor = String.valueOf(RandomUtils.nextDouble(min[colIndex],
			max[colIndex]));
	    } else {
		// diferente da coluna
		int index;
		do {
		    index = sorteio.nextInt(numCol);
		} while (index == colIndex);

		valor = colunas[index];
	    }

	    String col = colunas[colIndex];
	    String oper = LISTA_OPERADORES[operIndex];

	    String whereSql = String.format("%s %s %s", col, oper, valor);
	    if (sorteio.nextDouble() > 0.5) {
		whereSql = "NOT " + whereSql;
	    }

	    listaDeClausulasWhere.add(whereSql);
	}

	return listaDeClausulasWhere;
    }

    /**
     *  
     */
    // public void mostraPopulacao()
    // {
    // for (Particula p : particulas)
    // {
    // System.out.println(p.getWhereSql());
    // }
    //
    // System.out.println("Classes");
    //
    // for (String classe : classeSaidas.keySet())
    // {
    // Set<Integer> conj = classeSaidas.get(classe);
    // System.out.println(classe + ") " + conj.size());
    // }
    // }

}
