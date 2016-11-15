package com.blogspot.tsprates.pso;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Classe Partícula.
 *
 * @author thiago
 */
public class Particula implements Comparable<Particula>
{

    private Set<String[]> posicao;

    private String strPos;

    private String classe;

    private double[] fitness;

    private final Fitness calculadoraFitness;

    private Set<Particula> pbest;

    private final Comparator<String[]> comp = new Comparator<String[]>()
    {
        @Override
        public int compare(String[] a, String[] b)
        {
            return a[0].compareTo(b[0]);
        }
    };

    /**
     * Construtor.
     *
     * @param posicao Conjunto de cláusulas WHERE que representa a posição da
     * partícula.
     * @param classe Rótulo da partícula.
     * @param fitness Calculadora de fitness.
     */
    public Particula(Set<String[]> posicao, String classe, Fitness fitness)
    {
        this.posicao = new HashSet<>(posicao);
        this.strPos = join(posicao);
        this.classe = classe;
        this.pbest = new HashSet<>();

        this.calculadoraFitness = fitness;

        final Particula that = this;
        this.fitness = calculadoraFitness.calcular(that);
    }

    /**
     *
     * @param p
     */
    public Particula(Particula p)
    {
        this(p.posicao, p.classe, p.calculadoraFitness);
    }

    /**
     * Posição da partícula.
     *
     * @return Lista de String WHERE de nova posição.
     */
    public Set<String[]> posicao()
    {
        return posicao;
    }

    /**
     * Seta uma nova posição da partícula.
     *
     * @param posicao Lista de String WHERE de nova posição.
     */
    public void setPosicao(Collection<String[]> posicao)
    {
        this.posicao = new HashSet<>(posicao);
        this.strPos = join(this.posicao);
        this.fitness = calculadoraFitness.calcular(this);
    }

    /**
     * Retorna uma cláusula WHERE SQL correspondente a posição da partícula.
     *
     * @return String WHERE SQL.
     */
    public String whereSql()
    {
        return strPos;
    }

    @Override
    public String toString()
    {
        return strPos;
    }

    /**
     * Retorna a classe (nicho) da partícula.
     *
     * @return String representando a partícula.
     */
    public String classe()
    {
        return classe;
    }

    /**
     * Seta a classe da partícula.
     *
     * @param classe Classe da partícula.
     */
    public void setClasse(String classe)
    {
        this.classe = classe;
    }

    /**
     * Retorna o tamanho da cláusula WHERE.
     *
     * @return Tamanho da cláusula WHERE (número de condições).
     */
    public int numWhere()
    {
        return posicao().size();
    }

    /**
     * Retorna fitness da partícula.
     *
     * @return Array de doubles.
     */
    public double[] fitness()
    {
        return fitness;
    }

    /**
     * Retorna string de cláusulas WHERE.
     *
     * @param where Cláusula WHERE.
     * @return String de cláusulas WHERE.
     */
    private String join(Set<String[]> where)
    {
        List<String[]> tempWhere = new ArrayList<>(where);
        List<String> conds = new ArrayList<>();

        Collections.sort(tempWhere, comp);

        for (String[] iter : tempWhere)
        {
            conds.add(StringUtils.join(iter, " "));
        }

        return "(" + StringUtils.join(conds, ") AND (") + ")";
    }

    /**
     *
     * @return
     */
    public Collection<Particula> getPbest()
    {
        return pbest;
    }

    /**
     * Seta pbest.
     *
     * @param pbest
     */
    public void setPbest(List<Particula> pbest)
    {
        this.pbest = new HashSet<>(pbest);
    }

    /**
     * Atualiza pbest (memória da partícula).
     *
     */
    public void atualizarPbest()
    {
        FronteiraPareto.atualizarParticulasNaoDominadas(pbest, this);
        this.pbest = new HashSet<>(pbest);

        // verifica tamanho permitido do repositório
        FronteiraPareto.verificarNumParticulas(this.pbest);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(fitness);
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }

        Particula other = (Particula) obj;
        return Arrays.equals(fitness, other.fitness);
    }

    @Override
    public int compareTo(Particula part)
    {
        double[] pfit = part.fitness();

        if (fitness[1] == pfit[1])
        {
            if (fitness[0] == pfit[0])
            {
                return 0;
            }
            else if (fitness[0] < pfit[0])
            {
                return -1;
            }
            else
            {
                return 1;
            }
        }
        else if (fitness[1] < pfit[1])
        {
            return -1;
        }
        else
        {
            return 1;
        }
    }

    /**
     * Gera uma cópia da partícula.
     *
     * @return Partícula clonada.
     */
    public Particula clonar()
    {
        return new Particula(posicao, classe, calculadoraFitness);
    }
}
