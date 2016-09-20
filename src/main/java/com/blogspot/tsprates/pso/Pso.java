package com.blogspot.tsprates.pso;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.util.FastMath;

import java.sql.*;
import java.util.*;
import java.util.Map.Entry;

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
        ">", ">=", "<", ">=",
        "!=", "="
    };

    private final static double[] PROB_OPERADORES =
    {
        0.0, 0.22, 0.44, 0.66, 0.88,
        0.94, 1.0
    };

    private static final String TAB_FORMAT = "%-15s %-10s %-10s %-10s %s\n";

    private static final String TAB_CABECALHO;

    static
    {
        TAB_CABECALHO = String.format(TAB_FORMAT, "Classe", "Compl.",
                "Efet.", "Acur.", "Regra");
    }

    private final static double TURBULENCIA = 3;

    private final int NUM_K;

    private final String tabela;

    private List<Particula> particulas = new ArrayList<>();

    private final Map<String, Integer> enxameNicho;

    private final int numParts;

    private final double w, c1, c2;

    private final double crossover;

    private final Fitness fitness;

    private List<List<String>> kpastas;

    private final Formatador fmt;

    private final int maxIter;

    private final String colSaida, colId;

    private final List<String> colunas = new ArrayList<>();

    private Map<String, Integer> tipoColunas = new HashMap<>();

    private final Map<String, Double> max = new HashMap<>();

    private final Map<String, Double> min = new HashMap<>();

    private final Map<String, List<Double>> efetividade = new HashMap<>();

    private final Map<String, List<Double>> acuracia = new HashMap<>();

    private final Map<String, List<String>> mapaSaida = new HashMap<>();

    private final Set<String> tipoSaidas = new TreeSet<>();

    private final Map<String, List<Particula>> repositorio = new HashMap<>();

    private double[] valorMedioGlobal;

    private Map<String, double[]> valorMedioPorClasses;

    /**
     * Construtor.
     *
     * @param conexao Conexão com banco de dados.
     * @param config Configurações.
     * @param formatador Formatador de casas decimais.
     * @param numKpastas Validação K-Pastas.
     */
    public Pso(Connection conexao, final Properties config,
            final Formatador formatador, final int numKpastas)
    {
        this.conexao = conexao;
        this.tabela = config.getProperty("tabela");
        this.colSaida = config.getProperty("saida");
        this.colId = config.getProperty("id");

        this.w = Double.valueOf(config.getProperty("w"));
        this.c1 = Double.valueOf(config.getProperty("c1"));
        this.c2 = Double.valueOf(config.getProperty("c2"));

        this.crossover = Double.valueOf(config.getProperty("cr"));
        this.numParts = Integer.valueOf(config.getProperty("npop"));
        this.maxIter = Integer.valueOf(config.getProperty("maxiter"));

        this.fmt = formatador;

        this.NUM_K = numKpastas;

        carregarColunasDaTabela();
        carregarTiposDeSaida();
        carregarSaidaPorId();
        carregarMaxMinColunasDaTabela();

        this.enxameNicho = dividirNichoEnxame();

        criarRepositorio();

        this.fitness = new Fitness(conexao, colId, tabela, mapaSaida);
    }

    /**
     * Carrega PSO.
     */
    public void carregar()
    {
        // tempo inicial
        long tempoInicial = System.nanoTime();

        // validação cruzada
        kpastas = criarKpastas();

        fitness.setKPastas(kpastas);

        Map<String, double[]> kpastasClasses = criarValorMedioKpastas();

        for (int i = 0; i < NUM_K; i++)
        {
            fitness.setK(i);

            this.particulas = getEnxameInicial();

            resetRepositorio();

            // reseta contaddor de número de avaliações
            fitness.setNumAvaliacao(0);

            System.out.printf("\nPartição: %d \n", i + 1);
            System.out.printf("\nValidação: %s \n", kpastas.get(i));

            while (fitness.getNumAvaliacao() < maxIter)
            {
                for (int part = 0; part < numParts; part++)
                {
                    Particula particula = particulas.get(part);

                    // gbest
                    atualizarRepositorio(particula);

                    // operador de turbulência
                    aplicarTurbulencia(part);

                    // pbest
                    particula.atualizaPbest();

                    // atualiza posição da partícula
                    atualizaPosicao(particula);
                }
            } // fim: iterações

            mostrarTreinamento();

            // Fase de validação
            Map<String, List<double[]>> validacao = fitness.validar(repositorio);
            mostrarValidacao(validacao);

            // seleciona as melhores efetividade
            selecionarEfetividadeValidacao(validacao, kpastasClasses);
        }

        calcularValorMedio(kpastasClasses);

        // efetividade por classes (nichos)
        this.valorMedioPorClasses = new TreeMap<>(kpastasClasses);

        // média das melhores efetividades
        this.valorMedioGlobal = valorMedioGlobalKpastas(kpastasClasses);

        long tempoFinal = System.nanoTime();
        double tempoDecorrido = (tempoFinal - tempoInicial) / 1000000000.0;

        System.out.println("\nTempo decorrido: " + tempoDecorrido);
    }

    /**
     * Calcula o valor médio para as k partições.
     *
     * @param kpastasClasses
     */
    private void calcularValorMedio(Map<String, double[]> kpastasClasses)
    {
        // Atualiza média das K partições
        for (Entry<String, double[]> it : kpastasClasses.entrySet())
        {
            double[] arr = it.getValue();
            arr[0] /= NUM_K;
            arr[1] /= NUM_K;
            kpastasClasses.put(it.getKey(), arr);
        }
    }

    /**
     * Calcula o valor médio global.
     *
     * @param kpastasClasses
     * @return
     */
    private double[] valorMedioGlobalKpastas(
            Map<String, double[]> kpastasClasses)
    {
        double[] total = new double[2];

        for (double d[] : kpastasClasses.values())
        {
            total[0] += d[0];
            total[1] += d[1];
        }

        final int size = tipoSaidas.size();

        total[0] /= size;
        total[1] /= size;

        return total;
    }

    /**
     * Inicializa mapa de resultados para cada classe de saída.
     *
     * @return
     */
    private Map<String, double[]> criarValorMedioKpastas()
    {
        Map<String, double[]> kpastasClasses = new HashMap<>();
        for (String saida : tipoSaidas)
        {
            kpastasClasses.put(saida, new double[]
            {
                0.0, 0.0
            });
        }
        return kpastasClasses;
    }

    /**
     * Seleciona a melhor efetividade para cada classe de saída.
     *
     * @param validacao Mapa de fitness encontrados por saída.
     * @param kpastasClasses
     */
    private void selecionarEfetividadeValidacao(
            Map<String, List<double[]>> validacao,
            Map<String, double[]> kpastasClasses)
    {
        for (Entry<String, List<double[]>> entrada : validacao.entrySet())
        {
            String saida = entrada.getKey();
            List<double[]> fits = entrada.getValue();

            double[] f = fits.get(0);

            double maiorEfet = f[1];
            double acur = f[2];

            for (double[] fit : fits)
            {
                if (fit[1] > maiorEfet)
                {
                    maiorEfet = fit[1];
                    acur = fit[2];
                }
            }

            double[] arr = kpastasClasses.get(saida);
            arr[0] += maiorEfet;
            arr[1] += acur;
            kpastasClasses.put(saida, arr);
        }
    }

    /**
     * Mostra validação.
     *
     * @param validacao Lista de fitness encontrados por saída.
     */
    private void mostrarValidacao(Map<String, List<double[]>> validacao)
    {
        System.out.println("\n\nFase de validação:");
        System.out.println();

        // tabela de validação
        System.out.print(TAB_CABECALHO);
        System.out.println();

        for (String saida : tipoSaidas)
        {
            List<double[]> r = validacao.get(saida);
            List<Particula> rep = repositorio.get(saida);

            for (int i = 0, l = r.size(); i < l; i++)
            {
                final double[] fit = r.get(i);
                mostrarFmtSaida(saida, fit, rep.get(i).whereSql());
            }
        }

        System.out.println();
    }

    /**
     * Mostra tabela de saída dos resultados.
     */
    public void mostrarTreinamento()
    {
        Map<String, List<Particula>> solucoes = new TreeMap<>(repositorio);

        System.out.println("\n\nFase de treinamento:\n");

        System.out.println(TAB_CABECALHO);

        for (Entry<String, List<Particula>> parts : solucoes.entrySet())
        {

            String classe = parts.getKey();

            List<Particula> listaParts = parts.getValue();

            Collections.sort(listaParts);

            // tabela de treinamento
            for (Particula part : listaParts)
            {
                double[] f = part.fitness();
                mostrarFmtSaida(classe, f, part.whereSql());
            }
        }
    }

    /**
     * Monta uma linha da tabela de resultado do algoritmo.
     *
     * @param classe Nicho do enxame.
     * @param fso Funções objetivo.
     * @param whereSql Cláusula WHERE.
     */
    private void mostrarFmtSaida(String classe, double[] fso, String whereSql)
    {
        String compl = fmt.formatar(fso[0]);
        String efet = fmt.formatar(fso[1]);
        String acur = fmt.formatar(fso[2]);

        String cl; // classe
        if (classe.length() > 10)
        {
            cl = classe.substring(0, 10);
        }
        else
        {
            cl = classe;
        }

        System.out.printf(TAB_FORMAT, cl, compl, efet, acur, whereSql);
    }

    /**
     * Operador de turbulência.
     *
     * @param indexPart Índice da partícula.
     */
    private void aplicarTurbulencia(int indexPart)
    {
        if ((indexPart % TURBULENCIA) == 0)
        {
            perturbar(this.particulas.get(indexPart), true);
        }
        else if ((indexPart % TURBULENCIA) == 1)
        {
            perturbar(this.particulas.get(indexPart), false);
        }
    }

    /**
     * Atualiza posição.
     *
     * @param part Partícula.
     */
    private void atualizaPosicao(Particula part)
    {
        List<String> partPos = new ArrayList<>(part.posicao());
        final int partPosSize = partPos.size();

        perturbar(part, w, true);

        // pbest
        if (c1 > FastMath.random())
        {
            List<Particula> pbest = new ArrayList<>(part.getPbest());
            aplicarRecomb(pbest, part, partPos, partPosSize);
        }

        // gbest
        if (c2 > FastMath.random())
        {
            List<Particula> gbest = repositorio.get(part.classe());
            aplicarRecomb(gbest, part, partPos, partPosSize);
        }

    }

    /**
     * Aplicar operação de recombinação (crossover).
     *
     * @param best
     * @param part
     * @param partPos
     * @param partPosSize
     */
    private void aplicarRecomb(List<Particula> best, Particula part,
            List<String> partPos, final int partPosSize)
    {
        Particula partProx = Distancia.retornarParticulaMaisProxima(best, part);
        recombinar(partProx, part, partPos, partPosSize);
    }

    /**
     * Operador de crossover.
     *
     * @param partBest Melhor partícula.
     * @param part Partícula.
     * @param partPos Posição da partícula.
     * @param partPosSize Tamanho do vetor posição da partícula.
     */
    private void recombinar(Particula partBest, Particula part,
            List<String> partPos, final int partPosSize)
    {
        final List<String> bestPos = new ArrayList<>(partBest.posicao());

        List<String> newPos = new ArrayList<>();

        int bestPosSize = bestPos.size();

        int i = 0;
        while (i < bestPosSize)
        {
            if (crossover > FastMath.random())
            {
                newPos.add(bestPos.get(RandomUtils.nextInt(0, bestPosSize)));
                i++;
            }

            if (i < partPosSize)
            {
                newPos.add(partPos.get(RandomUtils.nextInt(0, partPosSize)));
                i++;
            }
        }

        part.setPosicao(new HashSet<>(newPos));
    }

    /**
     * Perturbação.
     *
     * @param p Partícula.
     * @param distnorm Mutação Gaussiana.
     */
    private void perturbar(Particula p, boolean distnorm)
    {
        perturbar(p, 1.0, distnorm);
    }

    /**
     * Mutação.
     *
     * @param p Partícula.
     * @param pm Probabilidade de mutação.
     * @param distnorm Mutação Gaussiana.
     */
    private void perturbar(Particula p, double pm, boolean distnorm)
    {
        if (pm > FastMath.random())
        {
            final List<String> pos = new ArrayList<>(p.posicao());

            final int index = RandomUtils.nextInt(0, pos.size());
            final String[] clausula = pos.get(index).split(" ");

            if (0.5 > FastMath.random())
            {
                pos.add(criarCondicao());
            }
            else
            {
                String oper = clausula[1];
                String val = clausula[2];

                if (NumberUtils.isNumber(clausula[2]) && 0.5 > FastMath.random())
                {
                    // Artigo: Empirical Study of Particle Swarm Optimization Mutation Operators
                    final double valor = Double.parseDouble(clausula[2]);
                    double newVal;

                    if (distnorm)
                    {
                        // Proposta de Higashi et al. (2003)
                        // Mutação não uniforme
                        final double alfa = 0.1 * (max.get(clausula[0]) - min.get(clausula[0])) + Double.MIN_VALUE;
                        final double r = new NormalDistribution(0, alfa).sample();
                        newVal = valor + r;
                    }
                    else
                    {
                        // Proposta de Michalewitz (1996)
                        // Mutação uniforme
                        if (FastMath.random() < 0.5)
                        {
                            newVal = valor + (max.get(clausula[0]) - valor) * FastMath.random();
                        }
                        else
                        {
                            newVal = valor - (valor - min.get(clausula[0])) * FastMath.random();
                        }
                    }

                    val = String.format(Locale.ROOT, "%.3f", newVal);
                }
                else
                {
                    final double r = FastMath.random();
                    int indexOper = 0;

                    for (int k = 1, len = LISTA_OPERADORES.length; k < len; k++)
                    {
                        if (PROB_OPERADORES[k - 1] >= r && PROB_OPERADORES[k] < r)
                        {
                            indexOper = k - 1;
                        }
                    }

                    oper = LISTA_OPERADORES[indexOper];

                }

                pos.set(index, String.format(Locale.ROOT, "%s %s %s", clausula[0], oper, val));
            }

            p.setPosicao(pos);
        }
    }

    /**
     * Carrega um mapa de todas as saídas (classes) para cada ID de cada
     * registro da tabela do banco de dados.
     *
     */
    private void carregarSaidaPorId()
    {
        for (String saida : tipoSaidas)
        {
            mapaSaida.put(saida, new ArrayList<String>());
        }

        String sql = "SELECT " + colSaida + ", " + colId + " AS col_id "
                + "FROM " + tabela;

        try (PreparedStatement ps = conexao.prepareStatement(sql);
                ResultSet rs = ps.executeQuery())
        {
            while (rs.next())
            {
                String coluna = rs.getString(colSaida);
                mapaSaida.get(coluna).add(rs.getString("col_id"));
            }

            if (mapaSaida.size() > numParts)
            {
                throw new RuntimeException("Tamanho do enxame é insuficiente.");
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
    private void carregarColunasDaTabela()
    {
        ResultSetMetaData metadata;

        String sql = "SELECT * "
                + "FROM " + tabela + " "
                + "LIMIT 1";

        int numCol;

        try (PreparedStatement ps = conexao.prepareStatement(sql);
                ResultSet rs = ps.executeQuery())
        {
            metadata = rs.getMetaData();
            numCol = metadata.getColumnCount();

            for (int i = 0; i < numCol; i++)
            {
                String coluna = metadata.getColumnName(i + 1);
                int tipoColuna = metadata.getColumnType(i + 1);

                if (!colSaida.equalsIgnoreCase(coluna)
                        && !colId.equalsIgnoreCase(coluna))
                {
                    colunas.add(coluna);
                    tipoColunas.put(coluna, tipoColuna);
                }
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Erro ao recuperar nome das colunas.", e);
        }
    }

    /**
     * Recupera os valores da coluna de saída (classes).
     */
    private void carregarTiposDeSaida()
    {
        String sql = "SELECT DISTINCT " + colSaida + " "
                + "FROM " + tabela + " "
                + "ORDER BY " + colSaida + " ASC";

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
     * Recupera os máximos e mínimos das colunas.
     */
    private void carregarMaxMinColunasDaTabela()
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
     * Faz a divisão do total da população em nichos similares para cada classe
     * do problema.
     *
     * @return População de cada nicho.
     */
    private Map<String, Integer> dividirNichoEnxame()
    {
        final int numSaidas = tipoSaidas.size();
        final int numPopNicho = numParts / numSaidas;
        int resto = numParts % numSaidas;
        Map<String, Integer> nicho = new HashMap<>();

        for (String i : tipoSaidas)
        {
            if (resto > 0)
            {
                nicho.put(i, numPopNicho + 1);
                resto -= 1;
            }
            else
            {
                nicho.put(i, numPopNicho);
            }
        }

        return nicho;
    }

    /**
     * Gera população inicial.
     *
     * @return Lista contendo a população de partículas.
     */
    private List<Particula> getEnxameInicial()
    {
        List<Particula> newParts = new ArrayList<>();

        for (String cls : enxameNicho.keySet())
        {
            List<Particula> nichoParticulas = new ArrayList<>();

            for (int i = 0, len = enxameNicho.get(cls); i < len; i++)
            {
                Particula particula = criarParticula(cls);
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
     * Cria uma partícula da classe definida.
     *
     * @param cls Classe.
     * @return
     */
    private Particula criarParticula(final String cls)
    {
        Set<String> pos = criarWhere();
        return new Particula(pos, cls, fitness);
    }

    /**
     * Carrega soluções iniciais para cada objetivo.
     *
     * @param classe Nicho do enxame.
     * @param particulas
     */
    private void inicializaRepositorio(String classe, List<Particula> particulas)
    {
        for (Particula p : particulas)
        {
            FronteiraPareto.atualizarParticulas(repositorio.get(classe), p);
        }

    }

    /**
     * Lista toda a população.
     */
    public void mostrarEnxame()
    {
        System.out.println("Classe:");
        for (String classe : mapaSaida.keySet())
        {
            List<String> c = mapaSaida.get(classe);
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
     * Retorna o conjunto que compõe a clásula WHERE.
     *
     * @return Conjunto de condições da cláusula WHERE.
     */
    private Set<String> criarWhere()
    {
        int numCols = colunas.size();
        Set<String> listaWhere = new HashSet<>();

        double R = RandomUtils.nextDouble(1, numCols);
        int maxWhere = (int) FastMath.ceil(FastMath.log(2.0, R)) + 1;
//        int maxWhere = (int) RandomUtils.nextDouble(1, numCols);

        for (int i = 0; i < maxWhere; i++)
        {
            String cond = criarCondicao();
            listaWhere.add(cond);
        }

        return listaWhere;
    }

    /**
     * Cria condição da cláusula WHERE.
     *
     * @return String da cláusula WHERE.
     */
    private String criarCondicao()
    {
        final int numOper = LISTA_OPERADORES.length;
        final int numCols = colunas.size();

        final int colIndex = RandomUtils.nextInt(0, numCols);
        final int operIndex = RandomUtils.nextInt(0, numOper);

        final double prob = 0.9;

        String valor;

        // verifica se a condição ocorrerá com o 
        // campo constante ou valor numérico
        if (prob > FastMath.random())
        {
            final String coluna = colunas.get(colIndex);
            final Double minCol = min.get(coluna);
            final Double maxCol = max.get(coluna);
            final double newVal = (maxCol - minCol) * FastMath.random() + minCol;

            valor = String.format(Locale.ROOT, "%.3f", newVal);
        }
        else
        {
            int index;
            do
            {
                index = RandomUtils.nextInt(0, numCols);
            }
            while (index == colIndex); // diferentes colunas

            valor = colunas.get(index);
        }

        String col = colunas.get(colIndex);
        String oper = LISTA_OPERADORES[operIndex];

        return String.format(Locale.ROOT, "%s %s %s", col, oper, valor);
    }

    /**
     * Retorna a população de partículas.
     *
     * @return Lista de partículas.
     */
    public List<Particula> getEnxame()
    {
        return particulas;
    }

    /**
     * Retorna tipos de saída (classes).
     *
     * @return Tipos de saída (classes ou nichos do enxame).
     */
    public Set<String> getTiposSaidas()
    {
        return tipoSaidas;
    }

    /**
     * Mapa de classes (saídas) e os respectivos IDs de cada registro da tabela
     * no banco de dados.
     *
     * @return Mapa de saídas (classes) por IDs.
     */
    public Map<String, List<String>> getSaidasPorId()
    {
        return mapaSaida;
    }

    /**
     * Retorna mapa das classes (saídas) com a efetividade de cada partícula.
     *
     * @return Mapa das classes com a efetividade de cada partícula.
     */
    public Map<String, List<Double>> getEfetividade()
    {
        return efetividade;
    }

    /**
     * Retorna mapa das classes (saídas) com a acuracia de cada partícula.
     *
     * @return Mapa das classes (saídas) com a efetividade de cada partícula.
     */
    public Map<String, List<Double>> getAcuracia()
    {
        return acuracia;
    }

    /**
     * Cria GBest.
     */
    private void criarRepositorio()
    {
        // Lista não dominados (gbest)
        for (String saida : tipoSaidas)
        {
            repositorio.put(saida, new ArrayList<Particula>());
        }
    }

    /**
     * Reset GBest.
     */
    private void resetRepositorio()
    {
        // Lista não dominados (gbest)
        for (String saida : tipoSaidas)
        {
            repositorio.get(saida).clear();
        }
    }

    /**
     * Atualiza repositório de partículas não dominadas.
     *
     * @param particula
     */
    private void atualizarRepositorio(Particula particula)
    {
        String classe = particula.classe();
        List<Particula> gbestLista = repositorio.get(classe);

        FronteiraPareto.atualizarParticulas(gbestLista, particula);

        List<Particula> rep = new ArrayList<>(gbestLista);

        repositorio.put(classe, rep);

        // Verifica tamanho do repositório
        FronteiraPareto.verificarTamanhoDoRepositorio(rep);
    }

    /**
     * Validação Cruzada K-Pastas.
     *
     */
    private List<List<String>> criarKpastas()
    {
        List<List<String>> kpastasTemp = new ArrayList<>();

        for (int i = 0; i < NUM_K; i++)
        {
            kpastasTemp.add(new ArrayList<String>());
        }

        int total = 0;
        Map<String, List<String>> temp = new HashMap<>();

        // Deep cloning
        for (String saida : tipoSaidas)
        {
            temp.put(saida, new ArrayList<String>());

            List<String> mapaSaidaTemp = mapaSaida.get(saida);

            for (int i = 0, size = mapaSaidaTemp.size(); i < size; i++)
            {
                temp.get(saida).add(mapaSaidaTemp.get(i));
                total++;
            }

            Collections.shuffle(temp.get(saida));
        }

        int k = 0;
        List<String> listaTipoSaidas = new ArrayList<>(tipoSaidas);

        for (int i = 0; i < total;)
        {
            for (int j = 0, size = listaTipoSaidas.size(); j < size;)
            {
                String index = listaTipoSaidas.get(j);
                List<String> ids = temp.get(index);

                while (k < NUM_K)
                {
                    if (!ids.isEmpty())
                    {
                        String id = ids.remove(0);
                        kpastasTemp.get(k).add(id);
                        i++;
                        k++;
                    }
                    else
                    {
                        break;
                    }
                }

                if (k == NUM_K)
                {
                    k = 0;
                }
                else
                {
                    j++;
                }
            }
        }

        return kpastasTemp;
    }

    /**
     * Retorna K-Pastas.
     *
     * @return Lista de K-Pastas estratificada.
     */
    public List<List<String>> getKPasta()
    {
        return kpastas;
    }

    /**
     * Retorna o valor médio global (média da classes) das k partições.
     *
     * @return Retorna um array com o valor médio global da efetividade e
     * acurácia.
     */
    public double[] valorMedioGlobal()
    {
        return valorMedioGlobal;
    }

    /**
     * Retorna o valor médio por classes das k partições.
     *
     * @return Retorna um mapa da efetividade e acurácia.
     */
    public Map<String, double[]> valorMedioPorClasses()
    {
        return valorMedioPorClasses;
    }
}
