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
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.distribution.NormalDistribution;
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
        ">", ">=", "<", ">=",
        "!=", "="
    };

    private final static double[] PROB_OPERADORES =
    {
        0.0, 0.22, 0.44, 0.66, 0.88,
        0.94, 1.0
    };

    private static final String STR_FORMAT = "%-10s %-10s %-10s %-10s %s\n";

    private static final String CABECALHO_TAB;

    static
    {
        CABECALHO_TAB = String.format(STR_FORMAT, "Classe", "Compl.", "Efet.", "Acur.", "Regra");
    }

    private final Random rand = new Random();

    private List<Particula> particulas = new ArrayList<>();

    private final Set<String> tipoSaidas = new TreeSet<>();

    private final Map<String, List<String>> mapaSaida = new HashMap<>();

    private final Map<String, List<Particula>> repositorio = new HashMap<>();

    private final String tabela;

    private final String colSaida, colId;

    private final int maxIter;

    private final int numParts;

    private String[] colunas;

    private int[] tipoColunas;

    private final Map<String, Double> max = new HashMap<>();

    private final Map<String, Double> min = new HashMap<>();

    private final double w, c1, c2;

    private final double crossover;

    private final Fitness fitness;

    private final Formatador fmt;

    private final Map<String, List<Double>> efetividade = new HashMap<>();

    private final Map<String, List<Double>> acuracia = new HashMap<>();

    private final Map<String, Integer> enxameNicho;

    private final double turbulencia = 3;

    private final int NUM_K;

    private List<List<String>> kpastas;

    private double resultado;

    /**
     * Construtor.
     *
     * @param conexao Conexão com banco de dados PostgreSQL.
     * @param props Propriedades de configuração.
     * @param formatador Formatador de casas decimais.
     * @param numKpastas Número para validação k-pastas.
     */
    public Pso(Connection conexao,
            final Properties props,
            final Formatador formatador,
            final int numKpastas)
    {
        this.conexao = conexao;
        this.tabela = props.getProperty("tabela");
        this.colSaida = props.getProperty("saida");
        this.colId = props.getProperty("id");

        this.w = Double.valueOf(props.getProperty("w"));
        this.c1 = Double.valueOf(props.getProperty("c1"));
        this.c2 = Double.valueOf(props.getProperty("c2"));

        this.crossover = Double.valueOf(props.getProperty("cr"));
        this.numParts = Integer.valueOf(props.getProperty("npop"));
        this.maxIter = Integer.valueOf(props.getProperty("maxiter"));

        carregarColunas();
        carregarClassesDeSaida();
        mapaSaidaId();
        carregarMaxMinEntradas();

        enxameNicho = dividirNichoEnxame();

        fmt = formatador;

        NUM_K = numKpastas;

        criaRepositorio();

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
        criarKpastas();
        fitness.setKPastas(kpastas);

        Map<String, Double> execKpastasClasses = iniciarValorMedioExecKpastas();

        for (int i = 0; i < NUM_K; i++)
        {
            fitness.setK(i);

            // cria enxame
            this.particulas = getEnxameInicial();

            // reseta repositório
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

            System.out.println("\nFase de treinamento:\n");

            mostrarResultados();

            // Fase de validação
            Map<String, List<Double[]>> validacao = fitness.validar(repositorio);

            mostrarResultadoValidacao(validacao);

            // Seleciona melhores efetividade
            selecionarEfetividadeValidacao(validacao, execKpastasClasses);
        }

        // Média das melhores efetividades
        this.resultado = getValorMedioExecKpastas(execKpastasClasses);

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

        System.out.println("Tempo decorrido: " + tempoDecorrido);
        System.out.println();
    }

    /**
     * Calcula o resultado médio.
     *
     * @param execKpastasClasses
     * @return
     */
    private double getValorMedioExecKpastas(Map<String, Double> execKpastasClasses)
    {
        double total = 0.0;
        for (Double d : execKpastasClasses.values())
        {
            total += d;
        }
        return (total / (tipoSaidas.size() * NUM_K));
    }

    /**
     * Retorna o resultado.
     *
     * @return
     */
    public double getResultado()
    {
        return resultado;
    }

    /**
     * Inicializa mapa de resultados para cada classe de saída.
     *
     * @return
     */
    private Map<String, Double> iniciarValorMedioExecKpastas()
    {
        Map<String, Double> execKpastasClasses = new HashMap<>();
        for (String saida : tipoSaidas)
        {
            execKpastasClasses.put(saida, 0.0);
        }
        return execKpastasClasses;
    }

    /**
     * Calcula a melhor efetividade de cada classe de saída.
     *
     * @param validacao Mapa de fitness encontrados por saída.
     * @param execKpastasClasses
     */
    private void selecionarEfetividadeValidacao(
            Map<String, List<Double[]>> validacao,
            Map<String, Double> execKpastasClasses)
    {
        for (Entry<String, List<Double[]>> entrada : validacao.entrySet())
        {
            String saida = entrada.getKey();
            List<Double[]> fits = entrada.getValue();

            // vetor fitness na posição 2 (efetividade)
            Double[] temp = fits.get(0);
            double maior = temp[1];
            for (Double[] fit : fits)
            {
                if (fit[1] > maior)
                {
                    maior = fit[1];
                }
            }

            execKpastasClasses.put(saida, execKpastasClasses.get(saida) + maior);
        }
    }

    /**
     * Mostra validação.
     *
     * @param validacao Lista de fitness encontrados por saída.
     */
    private void mostrarResultadoValidacao(Map<String, List<Double[]>> validacao)
    {
        System.out.println("\nFase de validação:");
        System.out.println();

        // tabela de resultado validação
        System.out.print(CABECALHO_TAB);
        System.out.println();

        for (String saida : tipoSaidas)
        {
            List<Double[]> r = validacao.get(saida);
            List<Particula> rep = repositorio.get(saida);

            for (int i = 0, l = r.size(); i < l; i++)
            {
                double[] f = ArrayUtils.toPrimitive(r.get(i));
                mostrarFmtSaida(saida, f, rep.get(i).whereSql());
            }
        }

        System.out.println();
    }

    /**
     * Mostra tabela de saída dos resultados.
     */
    public void mostrarResultados()
    {
        Map<String, List<Particula>> solucoes = new TreeMap<>(repositorio);

        System.out.println(CABECALHO_TAB);

        for (Entry<String, List<Particula>> parts : solucoes.entrySet())
        {

            String classe = parts.getKey();

            List<Particula> listaParts = parts.getValue();

            Collections.sort(listaParts);

            // tabela de resultado
            for (Particula part : listaParts)
            {
                double[] f = part.fitness();
                mostrarFmtSaida(classe, f, part.whereSql());
            }
        }
    }

    /**
     * Monta uma linha formatada de saída.
     *
     * @param classe Saída.
     * @param fo Vetor de funções objetivo.
     * @param whereSql SQL encontrado.
     */
    private void mostrarFmtSaida(String classe, double[] fo, String whereSql)
    {
        String compl = fmt.formatar(fo[0]);
        String efet = fmt.formatar(fo[1]);
        String acur = fmt.formatar(fo[2]);
        System.out.printf(STR_FORMAT, classe, compl, efet, acur, whereSql);
    }

    /**
     * Operador de turbulência.
     *
     * @param indexPart Índice da partícula.
     */
    private void aplicarTurbulencia(int indexPart)
    {
        if ((indexPart % turbulencia) == 0)
        {
            perturbar(this.particulas.get(indexPart), true);
        }
        else if ((indexPart % turbulencia) == 1)
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
        if (c1 > Math.random())
        {
            List<Particula> pbest = new ArrayList<>(part.getPbest());
            aplicarRecomb(pbest, part, partPos, partPosSize);
        }

        // gbest
        if (c2 > Math.random())
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
            if (crossover > Math.random())
            {
                newPos.add(bestPos.get(rand.nextInt(bestPosSize)));
                i++;
            }

            if (i < partPosSize)
            {
                newPos.add(partPos.get(rand.nextInt(partPosSize)));
                i++;
            }
        }

        part.setPosicao(new HashSet<>(newPos));
    }

    /**
     * Perturbação.
     *
     * @param p Partícula.
     * @param distnorm Distribuição Normal.
     */
    private void perturbar(Particula p, boolean distnorm)
    {
        perturbar(p, 1.0, distnorm);
    }

    /**
     * Mutação.
     *
     * @param p Partícula.
     * @param pm Taxa de mutação.
     * @param distnorm Distribuição Normal.
     */
    private void perturbar(Particula p, double pm, boolean distnorm)
    {
        if (pm > Math.random())
        {
            List<String> pos = new ArrayList<>(p.posicao());

            final int index = rand.nextInt(pos.size());
            String[] clausula = pos.get(index).split(" ");

            if (StringUtils.isNumeric(clausula[2]))
            {
                // Artigo: Empirical Study of Particle Swarm Optimization Mutation Operators
                final double valor = Double.parseDouble(clausula[2]);
                double newValor;

                if (distnorm)
                {
                    // Proposta de Higashi et al. (2003)
                    // Mutação não uniforme
                    final double alfa = 0.1 * (max.get(clausula[0]) - min.get(clausula[0]));
                    final double R = new NormalDistribution(0, alfa).sample();
                    newValor = valor + R;
                }
                else
                {
                    // Proposta de Michalewitz (1996)
                    // Mutação uniforme
                    if (Math.random() < 0.5)
                    {
                        newValor = valor + (max.get(clausula[1]) - valor) * Math.random();
                    }
                    else
                    {
                        newValor = valor - (valor - min.get(clausula[1])) * Math.random();
                    }
                }

                pos.add(String.format(Locale.ROOT, "%s %s %.3f", clausula[0], clausula[1], newValor));
            }
            else
            {
                if (Math.random() < 0.5)
                {
                    pos.add(criarCondicao());
                }
                else
                {
                    double rnd = Math.random();
                    int indexOper = 0;

                    for (int k = 1, len = LISTA_OPERADORES.length; k < len; k++)
                    {
                        if (PROB_OPERADORES[k - 1] >= rnd && PROB_OPERADORES[k] < rnd)
                        {
                            indexOper = k - 1;
                        }
                    }

                    clausula[1] = LISTA_OPERADORES[indexOper];

                    pos.add(String.format(Locale.ROOT, "%s %s %s", clausula[0], clausula[1], clausula[2]));
                }
            }

            p.setPosicao(pos);
        }
    }

    /**
     * Mapa de todas as saídas (classes) possíves para cada id da tupla.
     *
     */
    private void mapaSaidaId()
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

        String sql = "SELECT * "
                + "FROM " + tabela + " "
                + "LIMIT 1";

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
     * @param classe
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
     * Retorna uma lista com clásulas WHERE.
     *
     * @return Lista de clásulas WHERE.
     */
    private Set<String> criarWhere()
    {
        int numCols = colunas.length;
        Set<String> listaWhere = new HashSet<>();

        double R = RandomUtils.nextDouble(1, numCols);
        int maxWhere = (int) Math.ceil(FastMath.log(2.0, R)) + 1;
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

        final double prob = 0.9;

        String valor;

        // verifica se a condição ocorrerá com o campo constante ou valor numérico
        if (rand.nextDouble() < prob)
        {
            final String coluna = colunas[colIndex];
            final Double minCol = min.get(coluna);
            final Double maxCol = max.get(coluna);
            final double newVal = RandomUtils.nextDouble(minCol, maxCol);
            
            valor = String.format(Locale.ROOT, "%.3f", newVal);
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

        return String.format(Locale.ROOT, "%s %s %s", col, oper, valor);
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
     * Retorna tipos de saída.
     *
     * @return Tipos de saída.
     */
    public Map<String, List<String>> getSaidasPorId()
    {
        return mapaSaida;
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
        for (String saida : tipoSaidas)
        {
            repositorio.put(saida, new ArrayList<Particula>());
        }
    }

    /**
     * Cria GBest.
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

        // verifica tamanho do repositório
        FronteiraPareto.verificarTamanhoDoRepositorio(rep);
    }

    /**
     * Faz a validação cruzada k-pastas.
     *
     */
    private void criarKpastas()
    {
        kpastas = new ArrayList<>();

        for (int i = 0; i < NUM_K; i++)
        {
            kpastas.add(new ArrayList<String>());
        }

        dividirKPastas(NUM_K);
    }

    /**
     * Validação Cruzada.
     *
     * @param numPastas Número de pastas.
     */
    private void dividirKPastas(int numPastas)
    {
        Map<String, List<String>> temp = new HashMap<>();
        int total = 0;

        // deep cloning
        for (String saida : tipoSaidas)
        {
            temp.put(saida, new ArrayList<String>());

            List<String> mapaSaidaLista = mapaSaida.get(saida);

            for (int i = 0, size = mapaSaidaLista.size(); i < size; i++)
            {
                temp.get(saida).add(mapaSaidaLista.get(i));
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

                while (k < numPastas)
                {
                    if (!ids.isEmpty())
                    {
                        String id = ids.remove(0);
                        kpastas.get(k).add(id);
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
    }

    /**
     * Retorna k-pastas.
     *
     * @return Lista de k-pastas estratificada.
     */
    public List<List<String>> getKPasta()
    {
        return kpastas;
    }
}
