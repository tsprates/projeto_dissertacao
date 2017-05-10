package com.blogspot.tsprates.pso;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.util.FastMath;

import java.sql.*;
import java.util.*;
import java.util.Map.Entry;

import static com.blogspot.tsprates.pso.Formatador.*;
import static com.blogspot.tsprates.pso.FronteiraPareto.*;

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

    private final static double TURBULENCIA = 3;

    private final int NUM_K;

    private final String tabela;

    private List<Particula> particulas = new ArrayList<>();

    private final Map<String, Integer> enxameNicho;

    private final int numParts;

    private final double w, c1, c2;

    private final Fitness fitness;

    private List<List<String>> kpastas;

    private final Formatador format;

    private final int maxNumAvaliacao;

    private final String colClasse, colId;

    private final List<String> colunas = new ArrayList<>();

    private final Map<String, Integer> tipoColunas = new HashMap<>();

    private final Map<String, Double> max = new HashMap<>();

    private final Map<String, Double> min = new HashMap<>();

    private final Map<String, List<String>> mapaClasseId = new HashMap<>();

    private final Set<String> classes = new TreeSet<>();

    private final Map<String, List<Particula>> repositorio = new HashMap<>();

    private double[] valorMedioGlobal;

    private Map<String, double[]> valorMedioPorClasse;

    private final List<String> regrasVisitadas = new ArrayList<>();

    /**
     * Construtor.
     *
     * @param conexao Conexão com banco de dados.
     * @param config Configurações.
     * @param formatador Formatador de casas decimais.
     * @param numKpastas Número de K-Pastas.
     */
    public Pso(Connection conexao, Properties config, Formatador formatador,
            int numKpastas)
    {
        this.conexao = conexao;
        this.tabela = config.getProperty("tabela");
        this.colClasse = config.getProperty("saida");
        this.colId = config.getProperty("id");

        this.w = Double.valueOf(config.getProperty("w"));
        this.c1 = Double.valueOf(config.getProperty("c1"));
        this.c2 = Double.valueOf(config.getProperty("c2"));

        this.numParts = Integer.valueOf(config.getProperty("npop"));
        this.maxNumAvaliacao = Integer.valueOf(config.getProperty("maxiter"));

        this.format = formatador;

        this.NUM_K = numKpastas;

        carregarColunasTabela();
        carregarClasses();
        carregarClassePorId();
        carregarMaxMinColunasTabela();

        this.enxameNicho = dividirNichoEnxame();

        criarRepositorio();

        this.fitness = new Fitness(conexao, colId, tabela, mapaClasseId);
    }

    /**
     * Carrega PSO.
     */
    public void carregar()
    {
        final long tempoInicial = System.nanoTime();

        // validação cruzada
        kpastas = criarKpastas();
        fitness.setKPastas(kpastas);

        final Map<String, double[]> kpastasClasse = criarValorMedioKpastas();

        for (int i = 0; i < NUM_K; i++)
        {
            fitness.setK(i);

            particulas = criarEnxameInicial();

            resetRepositorio();

            regrasVisitadas.clear();

            System.out.printf("\nPartição: %d \n", i + 1);
            System.out.printf("\nTeste: %s \n", kpastas.get(i));

            fitness.resetNumAvaliacao();

            while (fitness.numAvaliacao() < maxNumAvaliacao)
            {
                for (int indexPart = 0; indexPart < numParts; indexPart++)
                {
                    Particula particula = particulas.get(indexPart);

                    // gbest
                    atualizarRepositorio(particula);

                    // pbest
                    particula.atualizarPbest();

                    // operador de turbulência
                    aplicarTurbulencia(indexPart);

                    // atualiza posição da partícula
                    atualizarPosicao(indexPart);
                }

                buscaLocal();
            }

            mostrarTreinamento();

            // Fase de teste
            Map<String, List<double[]>> teste = fitness.testar(repositorio);
            mostrarTeste(teste);

            // seleciona as melhores efetividade
            selecionarEfetividade(teste, kpastasClasse);
        }

        calcularValorMedio(kpastasClasse);

        valorMedioPorClasse = new TreeMap<>(kpastasClasse);

        // média das melhores efetividades
        valorMedioGlobal = valorMedioGlobalKpastas(kpastasClasse);

        final long tempoFinal = System.nanoTime();

        System.out.printf("\nTempo decorrido: %s\n",
                formatarTempoDecorrido(tempoInicial, tempoFinal));
    }

    /**
     * Calcula o Valor Médio para as K-Pastas para cada classe.
     *
     * @param kpastasClasse Mapa K-Pastas para cada classe.
     */
    private void calcularValorMedio(Map<String, double[]> kpastasClasse)
    {
        // atualiza média das K-Pastas
        for (Entry<String, double[]> it : kpastasClasse.entrySet())
        {
            double[] arr = it.getValue();
            arr[0] /= NUM_K;
            arr[1] /= NUM_K;
            kpastasClasse.put(it.getKey(), arr);
        }
    }

    /**
     * Calcula o Valor Médio Global.
     *
     * @param kpastasClasse Mapa K-Pastas para cada classe.
     * @return Valor médio obtido do mapa K-Pastas para cada classe.
     */
    private double[] valorMedioGlobalKpastas(
            Map<String, double[]> kpastasClasse)
    {
        final double[] total = new double[2];

        for (double d[] : kpastasClasse.values())
        {
            total[0] += d[0];
            total[1] += d[1];
        }

        final int size = classes.size();

        total[0] /= size;
        total[1] /= size;

        return total;
    }

    /**
     * Inicializa um mapa de resultados de valores médios para cada classe do
     * enxame.
     *
     * @return Cria um mapa com os resultados encontrados.
     */
    private Map<String, double[]> criarValorMedioKpastas()
    {
        final Map<String, double[]> kpastasClasse = new HashMap<>();

        for (String cl : classes)
        {
            kpastasClasse.put(cl, new double[2]);
        }

        return kpastasClasse;
    }

    /**
     * Seleciona a melhor efetividade para cada classe dos resultados.
     *
     * @param teste Fitness por classe.
     * @param kpastasClasse Mapa K-Pastas para cada classe.
     */
    private void selecionarEfetividade(
            Map<String, List<double[]>> teste,
            Map<String, double[]> kpastasClasse)
    {
        for (Entry<String, List<double[]>> entrada : teste.entrySet())
        {
            final String cl = entrada.getKey();
            final List<double[]> fits = entrada.getValue();

            final double[] f = fits.get(0);

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

            double[] arr = kpastasClasse.get(cl);
            arr[0] += maiorEfet;
            arr[1] += acur;
            kpastasClasse.put(cl, arr);
        }
    }

    /**
     * Mostra avaliação (teste).
     *
     * @param teste Lista de fitness encontrados por classe.
     */
    private void mostrarTeste(Map<String, List<double[]>> teste)
    {
        System.out.println("\n\nFase de teste:\n");

        // tabela de teste
        System.out.print(TAB_CABECALHO);
        System.out.println();

        for (String cl : classes)
        {
            List<double[]> r = teste.get(cl);
            List<Particula> rep = repositorio.get(cl);

            for (int i = 0, l = r.size(); i < l; i++)
            {
                final double[] fit = r.get(i);
                mostrarLinhaTabela(cl, fit, rep.get(i).whereSql());
            }
        }

        System.out.println();
    }

    /**
     * Mostra tabela de classes.
     */
    private void mostrarTreinamento()
    {
        System.out.println("\n\nFase de treinamento:\n");

        System.out.println(TAB_CABECALHO);

        Map<String, List<Particula>> solucoes = new TreeMap<>(repositorio);

        for (Entry<String, List<Particula>> parts : solucoes.entrySet())
        {
            String classe = parts.getKey();
            List<Particula> listaParts = parts.getValue();

            Collections.sort(listaParts);

            // tabela de treinamento
            for (Particula part : listaParts)
            {
                final double[] fit = part.fitness();
                mostrarLinhaTabela(classe, fit, part.whereSql());
            }
        }
    }

    /**
     * Monta uma linha da tabela de resultado do algoritmo.
     *
     * @param classe Nicho do enxame.
     * @param fo Funções objetivo.
     * @param whereSql Cláusula WHERE.
     */
    private void mostrarLinhaTabela(String classe, double[] fo, String whereSql)
    {
        String compl = format.formatar(fo[0]);
        String efet = format.formatar(fo[1]);
        String acur = format.formatar(fo[2]);

        System.out.printf(TAB_LINHA, formatarClasse(classe), compl,
                efet, acur, whereSql);
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
            perturbar(particulas.get(indexPart), true);
        }
        else if ((indexPart % TURBULENCIA) == 1)
        {
            perturbar(particulas.get(indexPart), false);
        }
    }

    /**
     * Atualiza posição.
     *
     * @param indexPart Índice da partícula.
     */
    private void atualizarPosicao(int indexPart)
    {
        final Particula part = particulas.get(indexPart);

        final List<String> partPos = new ArrayList<>(part.posicao());
        final int partPosSize = partPos.size();

        // velocidade
        if (FastMath.random() < w)
        {
            perturbar(part);
        }

        // pbest
        if (FastMath.random() < c1)
        {
            final List<Particula> pbest = new ArrayList<>(part.getPbest());
            recombinar(pbest, part, partPos, partPosSize);
        }

        // gbest
        if (FastMath.random() < c2)
        {
            final List<Particula> gbest = repositorio.get(part.classe());
            recombinar(gbest, part, partPos, partPosSize);
        }

        // avaliação da nova partícula
        part.avaliar();
    }

    /**
     * Busca Local.
     */
    private void buscaLocal()
    {
        for (String cl : classes)
        {
            final List<Particula> rep = repositorio.get(cl);

            for (int i = 0; i < rep.size(); i++)
            {
                final Particula p = rep.get(i);
                buscaLocalPareto(p);
            }
        }
    }

    /**
     * Busca Local Pareto.
     *
     * @param p Partícula.
     */
    private void buscaLocalPareto(Particula p)
    {
        final String cl = p.classe();
        final List<Particula> rep = repositorio.get(cl);

        final Particula pl = p.clonar();

        final double len = FastMath.log(colunas.size()) + 1;

        for (int i = 0; i < len; i++)
        {
            perturbar(pl, false);

            final String where = pl.whereSql();

            if (regrasVisitadas.contains(where))
            {
                continue;
            }
            else
            {
                regrasVisitadas.add(where);
            }

            pl.avaliar();

            if (verificarDominanciaEntre(pl, p) >= 0)
            {
                atualizarParticulasNaoDominadas(rep, pl);
                break;
            }
        }
    }

    /**
     * Operador de crossover.
     *
     * @param bestParts Gbest ou Pbest.
     * @param part Partícula.
     * @param partPos Posição da partícula.
     * @param partPosSize Tamanho do vetor posição da partícula.
     */
    private void recombinar(List<Particula> bestParts, Particula part,
            List<String> partPos, int partPosSize)
    {
        final Particula bestPart = Distancia.retornarParticulaMaisProxima(bestParts, part);

        final List<String> bestPos = new ArrayList<>(bestPart.posicao());
        final int bestPosSize = bestPos.size();

        final List<String> newPos = new ArrayList<>();

        int i = 0;

        while (i < bestPosSize)
        {
            if (FastMath.random() < 0.5)
            {
                newPos.add(bestPos.get(RandomUtils.nextInt(0, bestPosSize)));
            }
            else
            {
                newPos.add(partPos.get(RandomUtils.nextInt(0, partPosSize)));
            }

            i++;
        }

        while (i < partPosSize)
        {
            newPos.add(partPos.get(RandomUtils.nextInt(0, partPosSize)));
            i++;
        }

        part.setPosicao(newPos);
    }

    /**
     * Mutação.
     *
     * @param p Partícula.
     * @param mutUnif Mutação Uniforme.
     */
    private void perturbar(Particula p, boolean mutUnif)
    {
        final List<String> pos = new ArrayList<>(p.posicao());

        if (FastMath.random() < 0.5)
        {
            pos.add(criarCondicao());
        }
        else
        {
            final int index = RandomUtils.nextInt(0, pos.size());
            final String[] termo = pos.get(index).split(" ");

            String oper = termo[1];
            String val = termo[2];

            // Artigo: Empirical Study of Particle Swarm Optimization Mutation Operators
            if (NumberUtils.isNumber(termo[2]) && FastMath.random() < 0.5)
            {
                final double newVal;

                if (mutUnif)
                {
                    newVal = mutUniforme(termo);
                }
                else
                {
                    newVal = mutGaussiana(termo);
                }

                val = formatarValorNumericoWhere(newVal);
            }
            else
            {
                oper = mutOperador();
            }

            pos.set(index, formatarCondicaoWhere(termo[0], oper, val));
        }

        p.setPosicao(pos);
    }

    /**
     * Mutação Gaussiana.
     *
     * @param termo
     * @return
     */
    private double mutGaussiana(String[] termo)
    {
        final double valor = Double.parseDouble(termo[2]);

        try
        {
            // Proposta de Andrews (2006)
            // Mutação gaussiana
            final double alfa = 0.1 * (max.get(termo[0]) - min.get(termo[0])) + Double.MIN_VALUE;
            final double r = new NormalDistribution(0, alfa).sample();
            return valor + r;
        }
        catch (NotStrictlyPositiveException ex)
        {
            throw new RuntimeException("Erro ao gerar distribuição gaussiana.", ex);
        }
    }

    /**
     * Mutação Uniforme.
     *
     * @param termo
     * @return
     */
    private double mutUniforme(String[] termo)
    {
        final double valor = Double.parseDouble(termo[2]);

        // Proposta de Michalewitz (1996)
        // Mutação uniforme
        if (FastMath.random() < 0.5)
        {
            return valor + (max.get(termo[0]) - valor) * FastMath.random();
        }
        else
        {
            return valor - (valor - min.get(termo[0])) * FastMath.random();
        }
    }

    /**
     * Mutação do Operador (Roleta dos operadores).
     *
     * @see #LISTA_OPERADORES
     * @return Retorna um operador da tabela de operadores.
     */
    private String mutOperador()
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

        return LISTA_OPERADORES[indexOper];
    }

    /**
     * Mutação uniforme.
     *
     * @param p Partícula.
     */
    private void perturbar(Particula p)
    {
        perturbar(p, true);
    }

    /**
     * Cria um mapa de classe por cada ID de registro da tabela.
     */
    private void carregarClassePorId()
    {
        for (String cl : classes)
        {
            mapaClasseId.put(cl, new ArrayList<String>());
        }

        String sql = "SELECT " + colClasse + ", " + colId + " AS col_id "
                + "FROM " + tabela;

        try (PreparedStatement ps = conexao.prepareStatement(sql);
                ResultSet rs = ps.executeQuery())
        {
            while (rs.next())
            {
                String coluna = rs.getString(colClasse);
                mapaClasseId.get(coluna).add(rs.getString("col_id"));
            }

            if (mapaClasseId.size() > numParts)
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
     * Recupera as colunas da tabela.
     */
    private void carregarColunasTabela()
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

                if (!colClasse.equalsIgnoreCase(coluna)
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
     * Recupera as classes da tabela.
     */
    private void carregarClasses()
    {
        String sql = "SELECT DISTINCT " + colClasse + " "
                + "FROM " + tabela + " "
                + "ORDER BY " + colClasse + " ASC";

        try (PreparedStatement ps = conexao.prepareStatement(sql);
                ResultSet rs = ps.executeQuery())
        {
            while (rs.next())
            {
                classes.add(rs.getString(colClasse));
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException(
                    "Erro ao carregar as classes de saídas no banco de dados.", e);
        }
    }

    /**
     * Recupera os máximos e mínimos das colunas.
     */
    private void carregarMaxMinColunasTabela()
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
        final int numClasses = classes.size();
        final int numPopNicho = numParts / numClasses;

        final Map<String, Integer> nicho = new HashMap<>();

        int resto = numParts % numClasses;

        for (String cl : classes)
        {
            if (resto > 0)
            {
                nicho.put(cl, numPopNicho + 1);
                resto -= 1;
            }
            else
            {
                nicho.put(cl, numPopNicho);
            }
        }

        return nicho;
    }

    /**
     * Gera a população inicial.
     *
     * @return Lista contendo a população de partículas.
     */
    private List<Particula> criarEnxameInicial()
    {
        final List<Particula> newParts = new ArrayList<>();

        for (String cl : enxameNicho.keySet())
        {
            final List<Particula> nichoParticulas = new ArrayList<>();

            for (int i = 0, len = enxameNicho.get(cl); i < len; i++)
            {
                Particula particula = criarParticula(cl);
                nichoParticulas.add(particula);
            }

            // seta o gbest para cada nicho
            inicializarRepositorio(cl, nichoParticulas);

            newParts.addAll(nichoParticulas);
        }

        return newParts;
    }

    /**
     * Cria uma partícula para a classe definida.
     *
     * @param classe Classe.
     * @return Uma nova partícula.
     */
    private Particula criarParticula(String classe)
    {
        final Set<String> pos = criarWhere();
        return new Particula(pos, classe, fitness);
    }

    /**
     * Carrega as partículas iniciais não dominadas para cada objetivo do
     * problema.
     *
     * @param classe Nicho do enxame.
     * @param particulas Lista de partículas.
     */
    private void inicializarRepositorio(String classe,
            List<Particula> particulas)
    {
        final List<Particula> rep = repositorio.get(classe);

        for (Particula part : particulas)
        {
            atualizarParticulasNaoDominadas(rep, part);
        }
    }

    /**
     * Retorna um conjunto de condições que compõe uma cláusula SQL WHERE.
     *
     * @return Conjunto de condições da cláusula SQL WHERE.
     */
    private Set<String> criarWhere()
    {
        final int numCols = colunas.size();
        final Set<String> conjWhere = new HashSet<>();

        final double r = RandomUtils.nextDouble(1, numCols);
        final int maxWhere = (int) FastMath.ceil(FastMath.log(2.0, r)) + 1;

        for (int i = 0; i < maxWhere; i++)
        {
            String cond = criarCondicao();
            conjWhere.add(cond);
        }

        return conjWhere;
    }

    /**
     * Cria uma cláusula SQL WHERE.
     *
     * @return String da cláusula SQL WHERE.
     */
    private String criarCondicao()
    {
        final int numOper = LISTA_OPERADORES.length;
        final int numCols = colunas.size();

        final int colIndex = RandomUtils.nextInt(0, numCols);
        final int operIndex = RandomUtils.nextInt(0, numOper);

        final double prob = 0.9;

        String valor;

        // verifica se a condição será 
        // outro atributo ou valor numérico
        if (prob > FastMath.random())
        {
            final String coluna = colunas.get(colIndex);
            final Double minCol = min.get(coluna);
            final Double maxCol = max.get(coluna);
            final double newVal = (maxCol - minCol) * FastMath.random() + minCol;

            valor = formatarValorNumericoWhere(newVal);
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

        return formatarCondicaoWhere(col, oper, valor);
    }

    /**
     * Retorna a população (enxame) de partículas.
     *
     * @return Lista de partículas.
     */
    public List<Particula> getEnxame()
    {
        return particulas;
    }

    /**
     * Retorna as classes do enxame.
     *
     * @return Classes ou nichos do enxame.
     */
    public Set<String> classes()
    {
        return classes;
    }

    /**
     * Cria GBest.
     */
    private void criarRepositorio()
    {
        // Lista não dominados (gbest)
        for (String cl : classes)
        {
            repositorio.put(cl, new ArrayList<Particula>());
        }
    }

    /**
     * Reset GBest.
     */
    private void resetRepositorio()
    {
        // Lista não dominados (gbest)
        for (String cl : classes)
        {
            repositorio.get(cl).clear();
        }
    }

    /**
     * Atualiza repositório de partículas não dominadas.
     *
     * @param p Partícula.
     */
    private void atualizarRepositorio(Particula p)
    {
        final String classe = p.classe();
        final List<Particula> gbestLista = repositorio.get(classe);

        atualizarParticulasNaoDominadas(gbestLista, p);

        final List<Particula> rep = new ArrayList<>(gbestLista);
        repositorio.put(classe, rep);

        // Verifica tamanho do repositório
        verificarNumParticulas(rep);
    }

    /**
     * Validação Cruzada K-Pastas.
     *
     */
    private List<List<String>> criarKpastas()
    {
        final List<List<String>> kpastasTemp = new ArrayList<>();

        for (int i = 0; i < NUM_K; i++)
        {
            kpastasTemp.add(new ArrayList<String>());
        }

        final Map<String, List<String>> temp = new HashMap<>();
        final int total = randMapaClasseId(temp);

        final List<String> listaClasses = new ArrayList<>(classes);

        int k = 0;

        for (int i = 0; i < total;)
        {
            for (int j = 0, size = listaClasses.size(); j < size;)
            {
                String index = listaClasses.get(j);
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
     * Cria um clone com elementos aleatórios do mapa classe
     * {@link #mapaClasseId}.
     *
     * @param map Mapa de classe por ID.
     * @return Número total de registro do mapa.
     */
    private int randMapaClasseId(Map<String, List<String>> map)
    {
        int total = 0;

        // Deep cloning
        for (String cl : classes)
        {
            map.put(cl, new ArrayList<String>());

            List<String> mapaClasseTemp = mapaClasseId.get(cl);

            for (int i = 0, size = mapaClasseTemp.size(); i < size; i++)
            {
                map.get(cl).add(mapaClasseTemp.get(i));
                total++;
            }

            Collections.shuffle(map.get(cl));
        }

        return total;
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
     * Retorna o valor médio global (média da classes) para as K-Pastas.
     *
     * @return Retorna um array com o valor médio global da efetividade e
     * acurácia.
     */
    public double[] valorMedioGlobal()
    {
        return valorMedioGlobal;
    }

    /**
     * Retorna o valor médio por classes para as K-Pastas.
     *
     * @return Retorna um mapa da efetividade e acurácia.
     */
    public Map<String, double[]> valorMedioPorClasses()
    {
        return valorMedioPorClasse;
    }
}
