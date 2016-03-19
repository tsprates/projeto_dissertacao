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
import java.util.Locale;
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
public class Pso
{

    // ALTER TABLE wine ADD COLUMN id SERIAL;
    // UPDATE wine SET id = nextval(pg_get_serial_sequence('wine','id'));
    // ALTER TABLE wine ADD PRIMARY KEY (id);
    
    
    private final Connection conexao;

    private final static String[] LISTA_OPERADORES =
    {
        "=", "!=", ">", ">=",
        "<", ">="
    };

    private final Random rand = new Random();

    private final List<Particula> particulas = new ArrayList<>();

    private final List<String> saida = new ArrayList<>();

    private final Map<String, Set<String>> classeSaidas = new HashMap<>();

    private Map<String, List<Particula>> pbest = new HashMap<>();

    private Map<String, List<Particula>> gbest = new HashMap<>();

    private final String tabela;

    private final String colSaida, colId;

    private final int maxIter;

    private final int numPop;

    private String[] colunas;

    private double[] max, min;

    private final double c1, c2, w;

    private final Fitness fitness;

    /**
     * Construtor.
     *
     * @param c Conexão com banco de dados PostgreSQL.
     * @param p Propriedades de configuração.
     */
    public Pso(Connection c, Properties p)
    {
        this.conexao = c;
        this.tabela = (String) p.get("tabela");
        this.colSaida = (String) p.get("saida");
        this.colId = (String) p.get("id");
        this.c1 = Double.valueOf((String) p.get("c1"));
        this.c2 = Double.valueOf((String) p.get("c2"));
        this.w = Double.valueOf((String) p.get("w"));
        this.numPop = Integer.valueOf((String) p.get("npop"));
        this.maxIter = Integer.valueOf((String) p.get("maxiter"));

        carregaColunas();
        carregaTiposClassesSaida();
        carregaIdParaSaida();
        carregaMaxMinDasEntradas();

        // Lista não dominados (pbest e gbest)
        for (String cl : classeSaidas.keySet())
        {
            pbest.put(cl, new ArrayList<Particula>());
            gbest.put(cl, new ArrayList<Particula>());
        }

        this.fitness = new Fitness(c, colId, tabela, classeSaidas);
    }

    /**
     * Carrega PSO.
     */
    public void carrega()
    {
        List<Particula> pop = geraPopulacaoInicial();
        mostraPopulacao();

        for (int i = 0; i < maxIter; i++)
        {
            for (Particula p : pop)
            {
                atualizaParticulasNaoDominadas(pbest, p);
                atualizaParticulasNaoDominadas(gbest, p);
                atualizaVelocidade(p);
                atualizaPosicao(p);
            }
        }

        // Mostra resultado
        for (List<Particula> parts : gbest.values())
        {
            for (Particula part : parts)
            {
                System.out.print(part.classe() + ") ");
                for (double d : part.fitness())
                {
                    System.out.print(d);
                    System.out.print(" ");
                }
                System.out.println(part.toWhereSql());
            }
        }
    }

    /**
     * Atualiza velocidade.
     *
     * @param p
     */
    private void atualizaVelocidade(Particula p)
    {
        List<Particula> pBest = pbest.get(p.classe());
        List<Particula> gBest = gbest.get(p.classe());

        List<String[]> vel = new ArrayList<>(p.velocidade());
        List<String[]> pos = new ArrayList<>(p.posicao());

        if (w > Math.random())
        {
            int index = RandomUtils.nextInt(0, pos.size());

            String[] clausula = pos.get(index).clone();
            clausula[0] = LISTA_OPERADORES[RandomUtils.nextInt(0,
                    LISTA_OPERADORES.length)];

            pos.remove(index);
            pos.add(clausula);
        }

        Particula pb = pBest.get(RandomUtils.nextInt(0, pBest.size()));
        if (c1 > Math.random())
        {
            List<String[]> v = new ArrayList<>(pb.velocidade());

            vel.removeAll(v);
            vel.addAll(v);
            Collections.shuffle(vel);
            p.setVelocidade(vel.subList(0, v.size()));
        }

        Particula pg = gBest.get(RandomUtils.nextInt(0, pBest.size()));
        if (c2 > Math.random())
        {
            List<String[]> v = new ArrayList<>(pg.velocidade());

            vel.removeAll(v);
            vel.addAll(v);
            Collections.shuffle(vel);
            p.setVelocidade(vel.subList(0, v.size()));
        }
    }

    /**
     * Atualiza velocidade.
     *
     * @param p Partícula.
     */
    private void atualizaPosicao(Particula p)
    {
        List<String[]> vel = new ArrayList<>(p.velocidade());
        List<String[]> pos = new ArrayList<>(p.posicao());

        vel.removeAll(pos);
        vel.addAll(pos);

        p.setPosicao(pos);
    }

    /**
     * Adiciona partículas não dominadas
     *
     * @param melhorCadaClasse pbest ou gbest
     * @param p Partícula.
     */
    private void atualizaParticulasNaoDominadas(
            Map<String, List<Particula>> melhorCadaClasse, Particula p)
    {
        double[] pfit = p.fitness();
        String pcl = p.classe();

        List<Particula> parts = melhorCadaClasse.get(pcl);

        if (parts.isEmpty())
        {
            parts.add(p);
        }
        else
        {
            List<Particula> removeItens = new ArrayList<>();

            for (Particula part : parts)
            {
                double[] fit = part.fitness();

//		System.out.println(Arrays.toString(pfit) + " " + Arrays.toString(fit));
                if (pfit[0] >= fit[0] && pfit[1] >= fit[1]
                        && (pfit[0] > fit[0] || pfit[1] > fit[1]))
                {
//		    System.out.println("entrou aqui");
                    removeItens.add(part);
                }

            }

//	    System.out.println(parts);
//	    System.out.print("Remove: ");
//	    System.out.println(removeItens);
            if (removeItens.size() > 0)
            {
                parts.removeAll(removeItens);
                parts.add(p);
            }
        }
    }

    /**
     *
     */
    private void carregaIdParaSaida()
    {
        for (String saida : saida)
        {
            classeSaidas.put(saida, new HashSet<String>());
        }

        String sql = "SELECT " + colSaida + ", " + colId + " AS col_id FROM " + tabela;

        try (PreparedStatement ps = conexao.prepareStatement(sql);
                ResultSet rs = ps.executeQuery())
        {

            while (rs.next())
            {
                String coluna = rs.getString(colSaida);
                classeSaidas.get(coluna).add(rs.getString("col_id"));
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException(
                    "Erro ao mapear nome das colunas que correspondem as saídas.",
                    e);
        }
    }

    /**
     * Recupera colunas da tabela.
     */
    private void carregaColunas()
    {
        ResultSetMetaData metadata;
        String sql = "SELECT * FROM " + tabela + " LIMIT 1";
        int numCol;

        try (PreparedStatement ps = conexao.prepareStatement(sql);
                ResultSet rs = ps.executeQuery())
        {
            metadata = rs.getMetaData();
            numCol = metadata.getColumnCount();

            colunas = new String[numCol - 2];
            max = new double[numCol - 2];
            min = new double[numCol - 2];

            for (int i = 0, j = 0; i < numCol; i++)
            {
                String coluna = metadata.getColumnName(i + 1);

                if (!colSaida.equalsIgnoreCase(coluna)
                        && !colId.equalsIgnoreCase(coluna))
                {
                    colunas[j] = coluna;
                    j++;
                }
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Erro ao recuperar nome das colunas.", e);
        }
    }

    /**
     * Recupera os valores da coluna de saída.
     */
    private void carregaTiposClassesSaida()
    {
        String sql = "SELECT DISTINCT " + colSaida + " FROM " + tabela;

        try (PreparedStatement ps = conexao.prepareStatement(sql);
                ResultSet rs = ps.executeQuery())
        {
            while (rs.next())
            {
                saida.add(rs.getString(colSaida));
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException(
                    "Erro ao classe saída no banco de dados.", e);
        }
    }

    /**
     * Recupera máximos e mínimos para entradas.
     */
    private void carregaMaxMinDasEntradas()
    {
        // faixa de valores de cada coluna
        StringBuilder sb = new StringBuilder();

        for (String entrada : colunas)
        {
            sb.append(", ").append("max(").append(entrada).append(")")
                    .append(", ").append("min(").append(entrada).append(")");
        }

        String sql = "SELECT " + sb.toString().substring(1) + " FROM " + tabela;

        try (PreparedStatement ps = conexao.prepareStatement(sql);
                ResultSet rs = ps.executeQuery())
        {
            int numCols = colunas.length * 2;
            while (rs.next())
            {
                for (int i = 0, j = 0; i < numCols; i += 2, j++)
                {
                    max[j] = rs.getDouble(i + 1);
                    min[j] = rs.getDouble(i + 2);
                }
            }

        }
        catch (SQLException e)
        {
            throw new RuntimeException(
                    "Erro ao buscar (min, max) no banco de dados.", e);
        }
    }

    /**
     * Gera população inicial.
     *
     * @return Lista contendo a população de partículas.
     */
    private List<Particula> geraPopulacaoInicial()
    {
        int numSaidas = saida.size();
        int numPopNicho = numPop / numSaidas;
        int resto = numPop % numSaidas;
        Map<String, Integer> contPopNicho = new HashMap<>();

        for (String i : saida)
        {
            if (resto > 0)
            {
                contPopNicho.put(i, numPopNicho + 1);
                resto -= 1;
            }
            else
            {
                contPopNicho.put(i, numPopNicho);
            }
        }

	// System.out.println(contPopNicho);
        for (String tipo : contPopNicho.keySet())
        {
            for (int i = 0, len = contPopNicho.get(tipo); i < len; i++)
            {
                List<String[]> vel = criaWhere();
                List<String[]> pos = criaWhere();
                String classe = tipo;

                Particula particula = new Particula(vel, pos, classe, fitness);

//		String sql = "SELECT " + colSaida + " AS cl, COUNT(*) AS t "
//			+ "FROM " + tabela + " " + "WHERE "
//			+ particula.toWhereSql() + " " + "GROUP BY " + colSaida
//			+ " " + "ORDER BY t DESC";
//
//		try (PreparedStatement ps = conexao.prepareStatement(sql);
//			ResultSet rs = ps.executeQuery()) {
//
//		    if (rs.next()) {
//			particula.setClasse(rs.getString("cl"));
//		    } else {
//			int index;
//			String cl;
//
//			do {
//			    index = rand.nextInt(saida.size());
//			    cl = saida.get(index);
//			    contPopNicho.put(cl,
//				    contPopNicho.get(cl) - 1);
//			} while (contPopNicho.get(cl) > 0);
//
//			particula.setClasse(cl);
//		    }
//		} catch (SQLException e) {
//		    throw new RuntimeException("Erro ao determinar a classe.",
//			    e);
//		}
                particulas.add(particula);
            }

        }

        return particulas;
    }

    /**
     * Retorna uma lista com clásulas WHERE.
     *
     * @return Lista com clásulas WHERE.
     */
    private List<String[]> criaWhere()
    {
        List<String[]> listaWhere = new ArrayList<>();

        int numOper = LISTA_OPERADORES.length;
        int numCols = colunas.length;
        double prob = 0.9;

        int maxWhere = (int) RandomUtils.nextDouble(1, numCols + 1);
        double decProb = (prob - 0.3) / maxWhere;

        for (int i = 0; i < maxWhere; i++)
        {
            int colIndex = rand.nextInt(numCols);
            int operIndex = rand.nextInt(numOper);

            String valor;

            // Verifica se comparação ocorrerá numericamente ou via coluna
            if (rand.nextDouble() > prob)
            {
                valor = String.format(Locale.ROOT, "%.2f",
                        RandomUtils.nextDouble(min[colIndex], max[colIndex]));
            }
            else
            {
                int index;
                do
                {
                    index = rand.nextInt(numCols);
                }
                while (index == colIndex); // diferentes colunas

                valor = colunas[index];
            }

            String col = colunas[colIndex];
            String oper = LISTA_OPERADORES[operIndex];

            String[] cond = new String[]
            {
                col, oper, valor
            };
            listaWhere.add(cond);

            prob -= decProb;
        }

        return listaWhere;
    }

    /**
     *
     */
    public void mostraPopulacao()
    {
        for (Particula p : particulas)
        {
            System.out.println(Arrays.toString(p.fitness()) + " : "
                    + p.toWhereSql());
        }

        System.out.println("Classes");

        for (String classe : classeSaidas.keySet())
        {
            Set<String> c = classeSaidas.get(classe);
            System.out.println(classe + ") " + c.size());
        }
    }
}
