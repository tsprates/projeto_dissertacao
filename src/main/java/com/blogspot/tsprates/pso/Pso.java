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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Classe PSO (Particles Swarm Optimization).
 *
 * @author thiago
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

    private List<Particula> particulas = new ArrayList<>();

    private final Set<String> tipoSaidas = new TreeSet<>();

    private final Map<String, Set<String>> saidas = new HashMap<>();

    private final Map<String, Set<Particula>> gbest = new HashMap<>();

    private final FronteiraPareto fronteiraPareto = new FronteiraPareto();

    private final String tabela;

    private final String colSaida, colId;

    private final int maxIter;

    private final int numPop;

    private String[] colunas;

    private int[] tipoColunas;

    private double[] max, min;

    private final double w, c1, c2;

    private final double cr, mutAdd, mutOper, prefAtribNum;

    private final Fitness fitness;

    private final Formatador format;

    private final Map<String, List<Double>> efetividade = new HashMap<>();

    /**
     * Construtor.
     *
     * @param c Conexão com banco de dados PostgreSQL.
     * @param props Propriedades de configuração.
     * @param f Formatador.
     */
    public Pso(Connection c, Properties props, Formatador f)
    {
        this.conexao = c;
        this.tabela = (props.getProperty("tabela"));
        this.colSaida = props.getProperty("saida");
        this.colId = props.getProperty("id");

        this.w = Double.valueOf(props.getProperty("w"));
        this.c1 = Double.valueOf(props.getProperty("c1"));
        this.c2 = Double.valueOf(props.getProperty("c2"));

        this.cr = Double.valueOf(props.getProperty("cr"));
        this.mutOper = Double.valueOf(props.getProperty("mutoper"));
        this.mutAdd = Double.valueOf(props.getProperty("mutadd"));
        this.prefAtribNum = Double.valueOf(props.getProperty("prefatribnum"));
        this.numPop = Integer.valueOf(props.getProperty("npop"));
        this.maxIter = Integer.valueOf(props.getProperty("maxiter"));

        carregaColunas();
        carregaTiposSaida();
        mapaSaidaCadaId();
        carregaMaxMinDasEntradas();

        criaGBest();

        // calcula fitness
        this.fitness = new Fitness(c, colId, tabela, saidas);

        // formata valor numérico
        format = f;

        this.particulas = geraPopulacaoInicial();
    }
    
    /**
     * Retorna mapa das classes com a efetividade de cada partícula.
     *
     * @return Mapa das classes com a efetividade de cada partícula.
     */
    public Map<String, List<Double>> getEfetividade()
    {
        return efetividade;
    }

    /**
     * Cria GBest.
     */
    private void criaGBest()
    {
        // Lista não dominados (gbest)
        for (String cl : saidas.keySet())
        {
            gbest.put(cl, new HashSet<Particula>());
        }
    }

    /**
     * Carrega PSO.
     */
    public void carrega()
    {
        long tempoInicial = System.nanoTime();
        for (int i = 0; i < maxIter; i++)
        {
            for (Particula part : particulas)
            {
                // gbest
                String classe = part.classe();
                Set<Particula> gbestParts = gbest.get(classe);
                fronteiraPareto
                        .atualizaParticulasNaoDominadas(gbestParts, part);

                // pbest
                part.atualizaPbest();

                atualizaPosicao(part);
            }

            System.out.println("Iteração: " + (i + 1));
        }

        long tempoFinal = System.nanoTime();

        mostraResultados();

        double tempoDecorrido = (tempoFinal - tempoInicial) / 1000000000.0;
        System.out.println("Tempo decorrido: " + tempoDecorrido);
        System.out.println();
    }

    /**
     * Solução encontrada.
     *
     */
    private void mostraResultados()
    {
        System.out.println();

        StringBuilder builder = new StringBuilder(
                "Classe \tCompl. \tEfet. \tAcur. \tRegra \n\n");

        for (Entry<String, Set<Particula>> parts : gbest.entrySet())
        {

            String classe = parts.getKey();

            for (Particula part : parts.getValue())
            {
                builder.append(classe);

                double[] d = part.fitness();
                for (int i = 0, len = d.length; i < len; i++)
                {
                    builder.append("\t").append(format.format(d[i]));
                }

                builder.append("\t").append(part.whereSql()).append("\n");
            }
        }

        for (String saida : tipoSaidas)
        {
            efetividade.put(saida, new ArrayList<Double>());
        }

        for (Particula part : particulas)
        {
            double[] fit = part.fitness();
            efetividade.get(part.classe()).add(fit[1]);
        }
        
        builder.append("\n");
        
        System.out.println(builder.toString());
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
        final int operLen = LISTA_OPERADORES.length;

        if (w > Math.random())
        {
            int index = rand.nextInt(pos.size());
            String[] clausula = pos.get(index).split(" ");

            if (StringUtils.isNumeric(clausula[2]))
            {
                if (Math.random() < mutOper)
                {
                    clausula[1] = LISTA_OPERADORES[rand.nextInt(operLen)];
                }

                double novoValor = Double.parseDouble(clausula[1])
                        + RandomUtils.nextDouble(-1, 1);

                pos.add(String.format(Locale.ROOT, "%s %s %.2f",
                        clausula[0], clausula[1], novoValor));
            }
            else
            {
                clausula[1] = LISTA_OPERADORES[rand.nextInt(operLen)];
                pos.add(String.format(Locale.ROOT, "%s %s %s",
                        clausula[0], clausula[1], clausula[2]));
            }

            if (Math.random() < mutAdd)
            {
                pos.add(criaCond());
            }

            p.setPosicao(pos);
        }

        // pbest
        final Set<Particula> pBest = p.getPbest();
        final int pBestSize = pBest.size();
        final int pBestIndex = rand.nextInt(pBestSize);

        final Particula pBestPart = getRandomElement(pBest);
        final List<String> pBestPos = new ArrayList<>(pBestPart.posicao());

        if (c1 > Math.random())
        {
            List<String> nPosP = new ArrayList<>();
            int i = 0;
            int pBestPosSize = pBestPos.size();
            while (i < pBestPosSize)
            {
                if (cr > Math.random() || pBestIndex == i)
                {
                    nPosP.add(pBestPos.get(i));
                }
                else if (i < posSize)
                {
                    nPosP.add(pos.get(i));
                }
                i++;
            }
            p.setPosicao(new HashSet<>(nPosP));
        }

        // gbest
        final Set<Particula> gBest = gbest.get(p.classe());
        final int gBestSize = gBest.size();
        final int gBestIndex = rand.nextInt(gBestSize);

        final Particula gBestPart = getRandomElement(gBest);
        final List<String> gBestPos = new ArrayList<>(gBestPart.posicao());

        if (c2 > Math.random())
        {
            List<String> nPosG = new ArrayList<>();
            int i = 0;
            int gBestPosSize = gBestPos.size();
            while (i < gBestPosSize)
            {
                if (cr > Math.random() || gBestIndex == i)
                {
                    nPosG.add(gBestPos.get(i));
                }
                else if (i < posSize)
                {
                    nPosG.add(pos.get(i));
                }
                i++;
            }
            p.setPosicao(new HashSet<>(nPosG));
        }

    }

    /**
     * Retorna um elemento dentro conjunto das partículas.
     * 
     * @param s
     * @return
     */
    private Particula getRandomElement(Set<Particula> s)
    {
        Iterator<Particula> it = s.iterator();
        int i = 0, index = rand.nextInt(s.size());
        while (it.hasNext())
        {
            if (i == index)
            {
                return it.next();
            }
            i++;
        }
        return null;
    }

    /**
     * Mapa de todas as saídas (classes) possíves.
     */
    private void mapaSaidaCadaId()
    {
        for (String s : tipoSaidas)
        {
            saidas.put(s, new HashSet<String>());
        }

        String sql = "SELECT " + colSaida + ", " + colId + " AS col_id FROM " + tabela;

        try (PreparedStatement ps = conexao.prepareStatement(sql);
                ResultSet rs = ps.executeQuery())
        {

            while (rs.next())
            {
                String coluna = rs.getString(colSaida);
                saidas.get(coluna).add(rs.getString("col_id"));
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Erro ao mapear nome das colunas que "
                    + "correspondem as saídas.", e);
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
    private void carregaTiposSaida()
    {
        String sql = "SELECT DISTINCT " + colSaida + " FROM " + tabela
                + " ORDER BY " + colSaida + " ASC";

        try (PreparedStatement ps = conexao.prepareStatement(sql);
                ResultSet rs = ps.executeQuery())
        {
            while (rs.next())
            {
                tipoSaidas.add(rs.getString(colSaida));
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
        Map<String, Integer> contPopNicho = new HashMap<>();
        final int numSaidas = tipoSaidas.size();
        final int numPopNicho = numPop / numSaidas;
        int resto = numPop % numSaidas;

        for (String i : tipoSaidas)
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

        for (String tipo : contPopNicho.keySet())
        {
            for (int i = 0, len = contPopNicho.get(tipo); i < len; i++)
            {
                Set<String> pos = criaWhere();
                String classe = tipo;
                Particula particula = new Particula(pos, classe, fitness,
                        fronteiraPareto);
                particulas.add(particula);
            }

            carregaIniGbest(tipo);
        }

        return particulas;
    }

    /**
     *
     * @param tipo
     */
    private void carregaIniGbest(String tipo)
    {
        Particula partobj1 = null;
        Particula partobj2 = null;

        double[] pfitobj1 = null;
        double[] pfitobj2 = null;

        for (Particula particula : particulas)
        {
            double[] pfit = particula.fitness();
            String cl = particula.classe();

            if (tipo.equals(cl))
            {
                if (partobj1 == null || pfit[0] > pfitobj1[0])
                {
                    partobj1 = particula;
                    pfitobj1 = particula.fitness();
                }

                if (partobj2 == null || pfit[1] > pfitobj2[1])
                {
                    partobj2 = particula;
                    pfitobj2 = particula.fitness();
                }
            }
        }

        // adiciona os melhores indíduos
        // a partir de cada objetivo
        gbest.get(tipo).add(new Particula(partobj1));
        gbest.get(tipo).add(new Particula(partobj2));
    }

    /**
     * Retorna uma lista com clásulas WHERE.
     *
     * @return Lista com clásulas WHERE.
     */
    private Set<String> criaWhere()
    {
        int numCols = colunas.length;
        double probCond = prefAtribNum;
        Set<String> listaWhere = new HashSet<>();

        int maxWhere = (int) RandomUtils.nextDouble(1, numCols);
//        int maxWhere = (int) Math.log(RandomUtils.nextDouble(1, numCols)) + 1;
        double decProb = (probCond - 0.3) / maxWhere;

        for (int i = 0; i < maxWhere; i++)
        {
            String cond = criaCondicao(probCond);
            listaWhere.add(cond);
            probCond -= decProb;
        }

        return listaWhere;
    }

    /**
     * Cria condição para cláusula WHERE.
     *
     * @return
     */
    private String criaCond()
    {
        return criaCondicao(prefAtribNum);
    }

    /**
     * Cria condição para cláusula WHERE.
     *
     * @param prob
     * @return
     */
    private String criaCondicao(double prob)
    {
        int numOper = LISTA_OPERADORES.length;
        int numCols = colunas.length;

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

        String cond = String.format(Locale.ROOT, "%s %s %s", col, oper, valor);
        return cond;
    }

    /**
     * Lista toda a população.
     */
    public void mostraPopulacao()
    {
        System.out.println("Classe:");
        for (String classe : saidas.keySet())
        {
            Set<String> c = saidas.get(classe);
            System.out.println(classe + ") " + c.size());
        }
        System.out.println();
        
        System.out.println("População:");
        for (Particula p : particulas)
        {
            System.out.println(Arrays.toString(p.fitness()) + " : "
                    + p.whereSql());
        }
        System.out.println();
    }
}
