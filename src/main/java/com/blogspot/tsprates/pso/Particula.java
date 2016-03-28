package com.blogspot.tsprates.pso;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * Classe Partícula.
 *
 * @author thiago
 */
public class Particula
{

    private Set<String> posicao = new HashSet<>();

    private String classe;

    private double[] fitness;

    private final InterfaceFitness calculadorFitness;
    
    private List<Particula> pbest = new ArrayList<>();
    

    /**
     * Construtor.
     *
     * @param velocidade
     * @param posicao
     * @param classe
     * @param f
     */
    public Particula(Collection<String> posicao,
            String classe, InterfaceFitness f)
    {
        this.posicao = new HashSet<>(posicao);
        this.classe = classe;
        this.calculadorFitness = f;

        final Particula that = this;
        this.fitness = this.calculadorFitness.calcula(that);
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
        this.fitness = this.calculadorFitness.calcula(this);
    }

    /**
     * Retorna uma cláusula WHERE SQL correspondente a posição da partícula.
     *
     * @return String WHERE SQL.
     */
    public String toWhereSql()
    {
        return join(posicao());
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

    @Override
    public String toString()
    {
        return join(posicao);
    }

    
    /**
     * 
     */
    public void atualizaPBest() {
	FronteiraPareto.atualizaParticulasNaoDominadas(pbest, this);
    }

    /**
     * 
     * 
     * @return
     */
    public List<Particula> getPbest() {
	return pbest;
    }

}
