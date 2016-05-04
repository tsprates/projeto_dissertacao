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
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.FastMath;

// ALTER TABLE wine ADD COLUMN id SERIAL;
// UPDATE wine SET id = nextval(pg_get_serial_sequence('wine','id'));
// ALTER TABLE wine ADD PRIMARY KEY (id);
/**
 * PSO (Particles Swarm Optimization).
 *
 * @author thiago
 */
public class Pso
{

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

    private final Map<String, List<Particula>> repositorio = new HashMap<>();

    private final FronteiraPareto fronteiraPareto = new FronteiraPareto();

    private final String tabela;

    private final String colSaida, colId;

    private final int maxIter;

    private final int numParts;

    private String[] colunas;

    private int[] tipoColunas;

    private final Map<String, Double> max = new HashMap<>();

    private final Map<String, Double> min = new HashMap<>();

    private final double w, c1, c2;

    private final double cr, mut;

    private final Fitness fitness;

    private final Formatador format;

    private final Map<String, List<Double>> efetividade = new HashMap<>();

    private final Map<String, List<Double>> acuracia = new HashMap<>();

    private final Map<String, Integer> popNicho = new HashMap<>();

    private final DistanciaDeMultidao distMultComparator = new DistanciaDeMultidao();

    private final SolucoesNaoDominadas solucoesNaoDominadas;

    private final double turbulencia = 3; // 30% 

    /**
     * Construtor.
     *
     * @param conexao Conexão com banco de dados PostgreSQL.
     * @param props Propriedades de configuração.
     * @param formatador Formatador de casas decimais.
     */
    public Pso(Connection conexao, Properties props, Formatador formatador)
    {
        this.conexao = conexao;
        this.tabela = (props.getProperty("tabela"));
        this.colSaida = props.getProperty("saida");
        this.colId = props.getProperty("id");

        this.w = Double.valueOf(props.getProperty("w"));
        this.c1 = Double.valueOf(props.getProperty("c1"));
        this.c2 = Double.valueOf(props.getProperty("c2"));

        this.cr = Double.valueOf(props.getProperty("cr"));
        this.mut = Double.valueOf(props.getProperty("mut"));
        this.numParts = Integer.valueOf(props.getProperty("npop"));
        this.maxIter = Integer.valueOf(props.getProperty("maxiter"));

        carregarColunas();
        carregarClassesDeSaida();
        criarMapaSaidaId();
        carregarMaxMinEntradas();
        carregarTotalNichoEnxame();

        this.fitness = new Fitness(conexao, colId, tabela, saidas);

        this.solucoesNaoDominadas = new SolucoesNaoDominadas(conexao, tabela, tipoSaidas);

        format = formatador;

        criaRepositorio();
    }

    /**
     * Carrega PSO.
     */
    public void carregar()
    {
        resetRepositorio();

        fitness.setNumAvaliacao(0);

        this.particulas = getEnxameInicial();

        long tempoInicial = System.nanoTime();
        int i = 0;
//        for (int i = 0; i < maxIter; i++)
        while (fitness.getNumAvaliacao() < maxIter)
        {

            for (int j = 0; j < numParts; j++)
            {
                // gbest
                Particula particula = particulas.get(j);
                String classe = particula.classe();
                List<Particula> gbestLista = repositorio.get(classe);

                fronteiraPareto.atualizarParticulas(gbestLista, particula);
                // remove partículas não dominadas
                repositorio.put(classe, new ArrayList<>(
                        FronteiraPareto.getParticulasNaoDominadas(gbestLista)));

                // operador de turbulência
                turbulencia(j, particula);

                // pbest
                particula.atualizaPbest();

                // atualiza posição da partícula
                atualizaPosicao(particula);
            }

            System.out.println("Iteração: " + (i + 1));
            i++;
        }

        for (String saida : tipoSaidas)
        {
            efetividade.put(saida, new ArrayList<Double>());
            acuracia.put(saida, new ArrayList<Double>());
        }

        for (Particula part : particulas)
        {
            double[] fit = part.fitness();
            efetividade.get(part.classe()).add(fit[1]);
            acuracia.get(part.classe()).add(fit[2]);
        }

        long tempoFinal = System.nanoTime();
        double tempoDecorrido = (tempoFinal - tempoInicial) / 1000000000.0;

        System.out.println();
        System.out.println("Tempo decorrido: " + tempoDecorrido);
        System.out.println();

        solucoesNaoDominadas.salvar(repositorio);
    }

    /**
     * Operador de turbulência.
     *
     * @param iter
     * @param particula
     */
    private void turbulencia(double iter, Particula particula)
    {
        if ((iter % turbulencia) == 0)
        {
            perturbar(particula, mut);
        }
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

        perturbar(p, w);

        // pbest
        final List<Particula> pbest = new ArrayList<>(p.getPbest());
        if (c1 > Math.random())
        {
            final int index = rand.nextInt(pbest.size());
            Particula pBestPart = pbest.get(index);
            recombinar(pBestPart, posSize, pos, p);
        }

        // gbest
        if (c2 > Math.random())
        {
            final List<Particula> gbest = repositorio.get(p.classe());

            final int size = gbest.size();
            Particula p1 = gbest.get(rand.nextInt(size));
            Particula p2 = gbest.get(rand.nextInt(size));

            distMultComparator.atualizar(gbest);
            if (distMultComparator.compare(p1, p2) > 0)
            {
                recombinar(p1, posSize, pos, p);
            }
            else
            {
                recombinar(p2, posSize, pos, p);
            }
        }

    }

    /**
     * Operador de crossover.
     *
     * @param best
     * @param posSize
     * @param pos
     * @param part
     */
    private void recombinar(Particula best, int posSize,
            List<String> pos, Particula part)
    {
        final List<String> bestPos = new ArrayList<>(best.posicao());

        List<String> newPos = new ArrayList<>();

        int bestPosSize = bestPos.size();

        int i = 0;
        while (i < bestPosSize)
        {
            if (cr > Math.random())
            {
                newPos.add(bestPos.get(rand.nextInt(bestPosSize)));
                i++;
            }

            if (i < posSize)
            {
                newPos.add(pos.get(rand.nextInt(posSize)));
                i++;
            }
        }

        part.setPosicao(new HashSet<>(newPos));
    }

    /**
     * Busca local.
     *
     * @param p
     * @param pm
     */
    private void perturbar(Particula p, double pm)
    {
        final int operLen = LISTA_OPERADORES.length;

        final double sorteio = Math.random();

        if (sorteio < pm)
        {
            List<String> pos = new ArrayList<>(p.posicao());
            final int i = rand.nextInt(pos.size());
            String[] clausula = pos.get(i).split(" ");

            if (StringUtils.isNumeric(clausula[2]))
            {
                double novoValor = Double.parseDouble(clausula[1])
                        + RandomUtils.nextDouble(-1, 1);

                pos.add(String.format(Locale.ROOT, "%s %s %.3f", clausula[0],
                        clausula[1], novoValor));
            }
            else
            {
                if (Math.random() < 0.5)
                {
                    pos.add(criarCondicao());
                }
                else
                {
                    clausula[1] = LISTA_OPERADORES[rand.nextInt(operLen)];
                    pos.add(String.format(Locale.ROOT, "%s %s %s", clausula[0],
                            clausula[1], clausula[2]));

                }
            }

            p.setPosicao(pos);
        }

    }

    /**
     * Mapeia de todas as saídas (classes) possíves para cada id da tupla.
     *
     */
    private void criarMapaSaidaId()
    {
        for (String s : tipoSaidas)
        {
            saidas.put(s, new HashSet<String>());
        }

        String sql = "SELECT " + colSaida + ", " + colId + " AS col_id FROM "
                + tabela;

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
    private void carregarColunas()
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
    private void carregarClassesDeSaida()
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
     * Recupera os máximos e mínimos de cada entradas.
     */
    private void carregarMaxMinEntradas()
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

            while (rs.next())
            {
                int i = 0;
                for (String entrada : colunas)
                {
                    max.put(entrada, rs.getDouble(i + 1));
                    min.put(entrada, rs.getDouble(i + 2));
                    i += 2;
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
     * Faz a divisão númerica de cada nicho (dependendo de cada classe).
     */
    private void carregarTotalNichoEnxame()
    {
        final int numSaidas = tipoSaidas.size();
        final int numPopNicho = numParts / numSaidas;
        int resto = numParts % numSaidas;

        for (String i : tipoSaidas)
        {
            if (resto > 0)
            {
                popNicho.put(i, numPopNicho + 1);
                resto -= 1;
            }
            else
            {
                popNicho.put(i, numPopNicho);
            }
        }
    }

    /**
     * Gera população inicial.
     *
     * @return Lista contendo a população de partículas.
     */
    private List<Particula> getEnxameInicial()
    {
        List<Particula> newParts = new ArrayList<>();

        for (String cls : popNicho.keySet())
        {
            List<Particula> nichoParticulas = new ArrayList<>();

            for (int i = 0, len = popNicho.get(cls); i < len; i++)
            {
                Set<String> pos = criarWhere();

                Particula particula = new Particula(pos, cls, fitness,
                        fronteiraPareto);

                nichoParticulas.add(particula);

            }

            // seta o gbest para cada nicho
            inicializaRepositorio(cls, nichoParticulas);

            // adiciona novas particulas com o novo nicho
            newParts.addAll(nichoParticulas);
        }

        return newParts;
    }

    /**
     * Carrega soluções iniciais para cada objetivo.
     *
     * @param classe
     * @param particulas
     */
    private void inicializaRepositorio(String classe, List<Particula> particulas)
    {
        for (Particula p : particulas)
        {
            fronteiraPareto.atualizarParticulas(repositorio.get(classe), p);
        }

    }

    /**
     * Solução encontrada.
     *
     */
    public void mostrarResultados()
    {
        System.out.println();

        Map<String, List<Particula>> solucoes = repositorio;

        StringBuilder builder = new StringBuilder(
                "Classe \tCompl. \tEfet. \tAcur. \tRegra \n\n");

        for (Entry<String, List<Particula>> parts
                : solucoes.entrySet())
        {

            String classe = parts.getKey();

            List<Particula> resultado = parts.getValue();

            Collections.sort(resultado);

            for (Particula part : resultado)
            {
                builder.append(classe);

                double[] d = part.fitness();
                for (int i = 0, len = d.length; i < len; i++)
                {
                    builder.append("\t").append(format.formatar(d[i]));
                }

                builder.append("\t").append(part.whereSql()).append("\n");
            }
        }

        builder.append("\n");

        System.out.println(builder.toString());
    }

    /**
     * Lista toda a população.
     */
    public void mostrarEnxame()
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

    /**
     * Retorna uma lista com clásulas WHERE.
     *
     * @return Lista com clásulas WHERE.
     */
    private Set<String> criarWhere()
    {
        int numCols = colunas.length;
        Set<String> listaWhere = new HashSet<>();

        int maxWhere = (int) Math.ceil(FastMath.log(2.0,
                RandomUtils.nextDouble(2, numCols))) + 1;
//        int maxWhere = (int) RandomUtils.nextDouble(1, numCols);

        for (int i = 0; i < maxWhere; i++)
        {
            String cond = criarCondicao();
            listaWhere.add(cond);
        }

        return listaWhere;
    }

    /**
     * Cria condição para cláusula WHERE.
     *
     * @return
     */
    private String criarCondicao()
    {
        final int numOper = LISTA_OPERADORES.length;
        final int numCols = colunas.length;

        final int colIndex = rand.nextInt(numCols);
        final int operIndex = rand.nextInt(numOper);

        final double prob = 0.6;

        String valor;

        // verifica se a condição ocorrerá com o campo constante ou valor numérico
        if (rand.nextDouble() > prob)
        {
            valor = String.format(
                    Locale.ROOT,
                    "%.3f",
                    RandomUtils.nextDouble(min.get(colunas[colIndex]),
                            max.get(colunas[colIndex])));
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
     * Retorna a população de partículas
     *
     * @return
     */
    public List<Particula> getEnxame()
    {
        return particulas;
    }

    /**
     * Retorna tipos de saída.
     *
     * @return Tipos de saída.
     */
    public Set<String> getClasses()
    {
        return tipoSaidas;
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
     * Retorna mapa das classes com a acuracia de cada partícula.
     *
     * @return Mapa das classes com a efetividade de cada partícula.
     */
    public Map<String, List<Double>> getAcuracia()
    {
        return acuracia;
    }

    /**
     * Cria GBest.
     */
    private void criaRepositorio()
    {
        // Lista não dominados (gbest)
        for (String classe : tipoSaidas)
        {
            repositorio.put(classe, new ArrayList<Particula>());
        }
    }

    /**
     * Cria GBest.
     */
    private void resetRepositorio()
    {
        // Lista não dominados (gbest)
        for (String classe : tipoSaidas)
        {
            repositorio.get(classe).clear();
        }
    }

}
