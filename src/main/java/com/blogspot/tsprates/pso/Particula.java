package com.blogspot.tsprates.pso;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

/**
 * Classe Partícula.
 *
 * @author thiago
 */
public class Particula implements Comparable<Particula>
{

    private Set<String> posicao;

    private String strPos;

    private String classe;

    private double[] fitness;

    private final Fitness calculadorFitness;

    private final DistanciaDeMultidao distanciaDeMultidao;

    private Set<Particula> pbest;

    /**
     * Construtor.
     *
     * @param posicao
     * @param classe
     * @param fitness
     * @param distanciaDeMultidao Distância de Multidão.
     */
    public Particula(Set<String> posicao, String classe, Fitness fitness,
            DistanciaDeMultidao distanciaDeMultidao)
    {
        this.posicao = new TreeSet<>(posicao);
        this.strPos = join(posicao);
        this.classe = classe;
        this.pbest = new TreeSet<>();

        this.calculadorFitness = fitness;

        final Particula that = this;
        this.fitness = calculadorFitness.calcular(that);

        this.distanciaDeMultidao = distanciaDeMultidao;
    }

    /**
     *
     * @param p
     */
    public Particula(Particula p)
    {
        this(p.posicao, p.classe, p.calculadorFitness, p.distanciaDeMultidao);
    }

    /**
     * Get posição da partícula.
     *
     * @return Lista de String WHERE de nova posição.
     */
    public Set<String> posicao()
    {
        return posicao;
    }

    /**
     * Seta nova posição da partícula.
     *
     * @param posicao Lista de String WHERE de nova posição.
     */
    public void setPosicao(Collection<String> posicao)
    {
        this.posicao = new TreeSet<>(posicao);
        this.strPos = join(this.posicao);
        this.fitness = calculadorFitness.calcular(this);
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
     * Retorna a classe da partícula.
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
     * Retorna a dimensão (tamanho) da cláusula WHERE.
     *
     * @return Tamanho da cláusula WHERE.
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
     * @param l
     * @return String de cláusulas WHERE.
     */
    private String join(Set<String> l)
    {
        return "(" + StringUtils.join(l, ") AND (") + ")";
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
        this.pbest = new TreeSet<>(pbest);
    }

    /**
     * Atualiza pbest (memória da partícula).
     *
     */
    public void atualizaPbest()
    {
        FronteiraPareto.atualizarParticulas(pbest, this);
        this.pbest = new TreeSet<>(pbest);
        
        // verifica tamanho do repositório
        FronteiraPareto.verificarTamanhoDoRepositorio(this.pbest, 
                distanciaDeMultidao);
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
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Particula other = (Particula) obj;
        if (!Arrays.equals(fitness, other.fitness))
            return false;
        return true;
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

}
