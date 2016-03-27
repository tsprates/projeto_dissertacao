package com.blogspot.tsprates.pso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.apache.commons.lang3.StringUtils;

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

    private final Map<String, List<Particula>> pbest = new HashMap<>();

    private final Map<String, List<Particula>> gbest = new HashMap<>();

    private final String tabela;

    private final String colSaida, colId;

    private final int maxIter;

    private final int numPop;

    private String[] colunas;
    
    private int[] tipoColunas;

    private double[] max, min;

    private final double c1, c2, w;

    private final Fitness fitness;
    
    private final DecimalFormat formatter;

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
        
        
        // Decimal formatter
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator('.');
        formatter = new DecimalFormat("##.##", symbols);
    }

    /**
     * Carrega PSO.
     */
    public void carrega()
    {
        List<Particula> pop = geraPopulacaoInicial();

        long tempoInicial = System.nanoTime();
        for (int i = 0; i < maxIter; i++)
        {
            for (Particula part : pop)
            {
                atualizaParticulasNaoDominadas(pbest, part);
                atualizaParticulasNaoDominadas(gbest, part);
                atualizaPosicao(part);
            }
        }

        long tempoFinal = System.nanoTime();
        
        // Mostra resultado
        StringBuilder builder = new StringBuilder("Classe\tCompl.\tEfet.\tAcur.\tRegra\n\n");

        for (List<Particula> parts : gbest.values())
        {
            for (Particula part : parts)
            {
                builder.append(part.classe());
                for (double d : part.fitness())
                {
                    builder.append("\t").append(formatter.format(d));
                }
                builder.append("\t").append(part.toWhereSql());
            }
            builder.append("\n");
        }
        
        System.out.println(builder.toString());

        double tempoDecorrido = (tempoFinal - tempoInicial) / 1000000000.0;
        System.out.println("\nTempo decorrido: " + tempoDecorrido);
    }


    /**
     * Atualiza posição.
     *
     * @param p Partícula.
     */
    private void atualizaPosicao(Particula p)
    {
        List<String> pos = new ArrayList<>(p.posicao());
        final int posSize = pos.size();
        
        List<Particula> pBest = pbest.get(p.classe());
        List<Particula> gBest = gbest.get(p.classe());
        
        if (w > Math.random())
        {
            int index = rand.nextInt(pos.size());
            String[] clausula = pos.get(index).split(" ");
            
            if (StringUtils.isNumeric(clausula[2])) {
                if (Math.random() < 0.1) {
                    clausula[1] = LISTA_OPERADORES[rand.nextInt(LISTA_OPERADORES.length)];
                }
                double novoValor = Double.parseDouble(clausula[1]) + RandomUtils.nextDouble(-1, 1);
                pos.add(String.format(Locale.ROOT, "%s %s %.2f", clausula[0], clausula[1], novoValor));
                p.setPosicao(pos);
            } else {
                clausula[1] = LISTA_OPERADORES[rand.nextInt(LISTA_OPERADORES.length)];
                pos.add(String.format(Locale.ROOT, "%s %s %s", clausula[0], clausula[1], clausula[2]));
                p.setPosicao(pos);
            }
        }

        final int pBestSize = pBest.size();
        Particula pBestPart = pBest.get(rand.nextInt(pBestSize));
        if (c1 > Math.random())
        {
            List<String> posb = new ArrayList<>(pBestPart.posicao());
            posb.addAll(pos);
            Collections.shuffle(pos);
            p.setPosicao(new HashSet<>(posb.subList(0, posSize)));
        }
        
        
        final int gBestSize = gBest.size();
        Particula gBestPart = gBest.get(rand.nextInt(gBestSize));
        if (c2 > Math.random())
        {
            List<String> posg = new ArrayList<>(gBestPart.posicao());
            posg.addAll(pos);
            Collections.shuffle(pos);
            p.setPosicao(new HashSet<>(posg.subList(0, posSize)));
        }

    }

    /**
     * Adiciona partículas não dominadas
     *
     * @param melhoresDaClasse pbest ou gbest
     * @param p Partícula.
     */
    private void atualizaParticulasNaoDominadas(
            Map<String, List<Particula>> melhoresDaClasse, Particula p)
    {
        double[] pfit = p.fitness();
        String pcl = p.classe();

        List<Particula> parts = melhoresDaClasse.get(pcl);

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
                if (pfit[0] >= fit[0] && pfit[1] >= fit[1]
                        && (pfit[0] > fit[0] || pfit[1] > fit[1]))
                {
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
        for (String s : saida)
        {
            classeSaidas.put(s, new HashSet<String>());
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
            tipoColunas = new int[numCol - 2];
            
            max = new double[numCol - 2];
            min = new double[numCol - 2];

            for (int i = 0, j = 0; i < numCol; i++)
            {
                String coluna = metadata.getColumnName(i + 1);
                int tipoColuna = metadata.getColumnType(i + 1);

                if (!colSaida.equalsIgnoreCase(coluna)
                        && !colId.equalsIgnoreCase(coluna))
                {
                    colunas[j] = coluna;
                    tipoColunas[j] = tipoColuna;
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
                Collection<String> pos = criaWhere();
                String classe = tipo;
                Particula particula = new Particula(pos, classe, fitness);
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
    private Collection<String> criaWhere()
    {
        List<String> listaWhere = new ArrayList<>();

        int numOper = LISTA_OPERADORES.length;
        int numCols = colunas.length;
        double prob = 0.8;

//        int maxWhere = (int) RandomUtils.nextDouble(1, numCols);
        int maxWhere = (int) (Math.log(RandomUtils.nextDouble(1, numCols)) / Math.log(Math.E)) + 1;
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

            
            listaWhere.add(String.format(Locale.ROOT, 
                    "%s %s %s", col, oper, valor));

            prob -= decProb;
        }

        return listaWhere;
    }

    /**
     *
     */
    public void mostraPopulacao()
    {
//        System.out.println(Arrays.toString(tipoColunas));
        
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
