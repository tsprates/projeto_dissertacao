package com.blogspot.tsprates.pso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.RandomUtils;

/**
 * Classe PSO (Particles Swarm Optimization).
 * 
 * @author thiago
 *
 */
public class Pso {

    private final Connection conexao;

    private final static String[] LISTA_OPERADORES = { "=", "!=", ">", ">=",
	    "<", ">=" };

    private final Random sorteio = new Random();

    private final List<Particula> particulas = new ArrayList<>();

    private final List<String> tipoSaidas = new ArrayList<>();

    private final Map<String, Set<Integer>> classeSaidas = new HashMap<>();

    private Map<String, List<Particula>> pbest = new HashMap<>();
    
    private Map<String, List<Particula>> gbest = new HashMap<>();

    private final String tabela;

    private final String colSaida, colId;

    private final int maxIter;

    private final int numPop;

    private String[] colunas;

    private double[] max, min;
    
    private double c1, c2;

    private final Fitness fitness;


    /**
     * Construtor.
     *
     * @param c	Conexão com banco de dados PostgreSQL.
     * @param p Propriedades de configuração.
     */
    public Pso(Connection c, Properties p) {
	this.conexao = c;
	this.tabela = (String) p.get("tabela");
	this.colSaida = (String) p.get("saida");
	this.colId = (String) p.get("id");
	this.numPop = Integer.valueOf((String) p.get("npop"));
	this.maxIter = Integer.valueOf((String) p.get("maxiter"));
	this.c1 = Double.valueOf((String) p.get("c1"));
	this.c2 = Double.valueOf((String) p.get("c2"));

	recuperaColunas();
	recuperaClassesSaida();
	recuperaClasseSaidas();
	recuperaMaxMinDasEntradas();
	
	// Lista não dominados (pbest e gbest)
	for (String cl : classeSaidas.keySet()) {
	    pbest.put(cl, new ArrayList<Particula>());
	    gbest.put(cl, new ArrayList<Particula>());
	}
	
	
	this.fitness = new Fitness(c, colId, tabela, classeSaidas);
    }

    /**
     * Carrega PSO.
     */
    public void carrega() {
	List<Particula> pop = geraPopulacaoInicial();

	for (int i = 0; i < maxIter; i++) {
	    for (Particula p : pop) {
		atualizaParticulasNaoDominadas(pbest, p);
		atualizaParticulasNaoDominadas(gbest, p);
		atualizaVelocidade(p);
		atualizaPosicao(p);
	    }
	}
	
	// Mostra resultado
	for (List<Particula> parts : gbest.values()) {
	    for (Particula part : parts) {
		System.out.println(part.getClasse() + ") " + part.getWhereSql() + " : " + Arrays.toString(part.getFitness()));
	    }
	}
    }

    /**
     * Atualiza velocidade.
     * 
     * @param p
     */
    private void atualizaVelocidade(Particula p) {
	List<String> vel = new ArrayList<>(p.getVelocidade());
	List<String> pos = new ArrayList<>(p.getPosicao());
	
	// v(i) = v(i - 1) + c1 * rand() * (pbest(i) - x(i)) + c2 * Rand() * (gbest(i) - x(i))
	List<Particula> pbestList = pbest.get(p.getClasse());
	List<Particula> gbestList = gbest.get(p.getClasse());
	
	
	// c1 * rand() * (pbest(i) - x(i))
	int randPBestIndex = sorteio.nextInt(pbestList.size());
	List<String> p1 = new ArrayList<>(pbestList.get(randPBestIndex).getPosicao());
	p1.removeAll(pos);
	Collections.shuffle(p1);
	p1 = p1.subList(0, (int) Math.ceil(c1 * Math.random() * p1.size()));
	
	// c2 * Rand() * (gbest(i) - x(i))
	int randGBestIndex = sorteio.nextInt(gbestList.size());
	List<String> p2 = new ArrayList<>(gbestList.get(randGBestIndex).getPosicao());
	p2.removeAll(pos);
	Collections.shuffle(p1);
	p2 = p2.subList(0, (int) Math.ceil(c2 * Math.random() * p2.size()));
	
	p1.removeAll(p2);
	p1.addAll(p2);
	
	vel.removeAll(p1);
	vel.addAll(p1);
	
	p.setVelocidade(vel);
    }
    
    /**
     * Atualiza velocidade.
     * 
     * @param p Partícula.
     */
    private void atualizaPosicao(Particula p) {
	List<String> vel = new ArrayList<String>(p.getVelocidade());
	List<String> pos = new ArrayList<String>(p.getPosicao());
	
	vel.removeAll(pos);
	vel.addAll(pos);
	
	p.setPosicao(pos);
    }

    /**
     * Adiciona partículas não dominadas
     * 
     * @param melhoresDeCadaClasse pbest ou gbest
     * @param p	Partícula.
     */
    private void atualizaParticulasNaoDominadas(Map<String, List<Particula>> melhoresDeCadaClasse,
	    Particula p) {
	String cl = p.getClasse();
	double[] fit = p.getFitness();
	
	// Lista dos melhores de acordo com nicho
	List<Particula> listaParticulas = melhoresDeCadaClasse.get(cl); 

	if (listaParticulas.size() == 0) {
	    listaParticulas.add(p);
	} else {
	    List<Particula> aSerRemovido = new ArrayList<>();

	    for (Particula partAtual : listaParticulas) {
		double[] pfit = partAtual.getFitness();

		if (fit[0] >= pfit[0] && fit[1] <= pfit[1]
			&& (fit[0] > pfit[0] || fit[1] < pfit[1])) {
		    aSerRemovido.add(partAtual);
		}
	    }
	   
	    if (aSerRemovido.size() > 0) {		
		listaParticulas.removeAll(aSerRemovido);
		listaParticulas.add(p);
	    }
	}
    }

    /**
     *
     */
    private void recuperaClasseSaidas() {
	for (String saida : tipoSaidas) {
	    classeSaidas.put(saida, new HashSet<Integer>());
	}

	String sql = "SELECT " + colSaida + ", " + colId + " AS col_id FROM " + tabela;
	try (PreparedStatement ps = conexao.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
	    while (rs.next()) {
		String coluna = rs.getString(colSaida);
		classeSaidas.get(coluna).add(rs.getInt("col_id"));
	    }
	} catch (SQLException e) {
	    throw new RuntimeException(
		    "Erro ao mapear nome das colunas que correspondem as saídas.", e);
	}
    }

    /**
     *	Converte colunas da tabela.
     */
    private void recuperaColunas() {
	ResultSetMetaData metadata = null;
	String sql = "SELECT * FROM " + tabela + " LIMIT 1";
	int numCol;

	try (PreparedStatement ps = conexao.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
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
	}
    }

    /**
     *	Recupera os valores referentes a coluna de saída.
     */
    private void recuperaClassesSaida() {
	String sql = "SELECT DISTINCT " + colSaida + " FROM " + tabela;

	try (PreparedStatement ps = conexao.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
	    while (rs.next()) {
		tipoSaidas.add(rs.getString(colSaida));
	    }
	} catch (SQLException e) {
	    throw new RuntimeException(
		    "Erro ao classe saída no banco de dados.", e);
	} 
    }

    /**
     *	Recupera máximos e mínimos para entradas.
     */
    private void recuperaMaxMinDasEntradas() {
	// faixa de valores de cada coluna
	StringBuilder sb = new StringBuilder();

	for (String entrada : colunas) {
	    sb.append(", ").append("max(").append(entrada).append(")")
		    .append(", ").append("min(").append(entrada).append(")");
	}

	String sql = "SELECT " + sb.toString().substring(1) + " FROM " + tabela;

	try (PreparedStatement ps = conexao.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
	    int numCols = colunas.length * 2;
	    while (rs.next()) {
		for (int i = 0, j = 0; i < numCols; i += 2, j++) {
		    max[j] = rs.getDouble(i + 1);
		    min[j] = rs.getDouble(i + 2);
		}
	    }

	} catch (SQLException e) {
	    throw new RuntimeException(
		    "Erro ao buscar (min, max) no banco de dados.", e);
	}
    }

    /**
     * Gera população inicial.
     * 
     * @return Lista contendo a população de partículas.
     */
    private List<Particula> geraPopulacaoInicial() {
	int size = tipoSaidas.size();

	for (int i = 0; i < numPop; i++) {
	    List<String> vel = criaWhereAleatorio();
	    List<String> pos = criaWhereAleatorio();

	    String classe = tipoSaidas.get(i % size);
	    Particula particula = new Particula(vel, pos, classe, fitness);

	    particulas.add(particula);
	}

	return particulas;
    }

    /**
     * Retorna uma lista com clásulas WHERE.
     * 
     * @return Lista com clásulas WHERE.
     */
    private List<String> criaWhereAleatorio() {
	int numOper = LISTA_OPERADORES.length;
	int numCols = colunas.length;

	int maxClausulasWhere = sorteio.nextInt(numCols) + 1;

	List<String> listaDeClausulasWhere = new ArrayList<>();

	for (int i = 0; i < maxClausulasWhere; i++) {
	    int colIndex = sorteio.nextInt(numCols);
	    int operIndex = sorteio.nextInt(numOper);

	    // Verifica se comparação ocorrerá 
	    // com outra coluna ou numericamente
	    String valor;
	    if (sorteio.nextDouble() > 0.5) {
		valor = String.valueOf(RandomUtils.nextDouble(min[colIndex],
			max[colIndex]));
	    } else {
		// diferentes colunas
		int index;
		do {
		    index = sorteio.nextInt(numCols);
		} while (index == colIndex);

		valor = colunas[index];
	    }

	    String col = colunas[colIndex];
	    String oper = LISTA_OPERADORES[operIndex];

	    String whereSql = String.format("%s %s %s", col, oper, valor);
	    
//	    if (sorteio.nextDouble() > 0.5) {
//		whereSql = "NOT " + whereSql;
//	    }

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
