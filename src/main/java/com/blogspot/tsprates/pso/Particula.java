package com.blogspot.tsprates.pso;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * Classe Partícula.
 *
 * @author thiago
 */
public class Particula implements Comparable<Particula>
{

    private Set<String> posicao = new HashSet<>();

    private String strPos = null;

    private String classe;

    private final FronteiraPareto fp;

    private double[] fitness;

    private final InterfaceFitness calculadorFitness;

    private Set<Particula> pbest;

    /**
     * Construtor.
     *
     * @param posicao
     * @param classe
     * @param fit
     * @param fp
     */
    public Particula(Set<String> posicao, String classe, InterfaceFitness fit,
            FronteiraPareto fp)
    {
        this.posicao = new HashSet<>(posicao);
        this.strPos = join(posicao);
        this.fp = fp;
        this.classe = classe;
        this.pbest = new HashSet<>();
        
        this.calculadorFitness = fit;
        final Particula that = this;
        this.fitness = calculadorFitness.calcular(that);
    }

    /**
     *
     * @param p
     */
    public Particula(Particula p)
    {
        this(p.posicao, p.classe, p.calculadorFitness, p.fp);
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
        this.posicao = new HashSet<>(posicao);
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
        this.pbest = new HashSet<>(pbest);
    }

    /**
     * Atualiza pbest (memória da partícula).
     * 
     */
    public void atualizaPbest()
    {
        fp.atualizarParticulas(pbest, this);
        this.pbest = new HashSet<>(FronteiraPareto.getParticulasNaoDominadas(pbest));
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 41 * hash + Objects.hashCode(this.strPos);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final Particula other = (Particula) obj;
        return Objects.equals(this.strPos, other.strPos);
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
