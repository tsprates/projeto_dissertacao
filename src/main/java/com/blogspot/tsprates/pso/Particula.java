package com.blogspot.tsprates.pso;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Classe Partícula.
 *
 * @author thiago
 */
public class Particula
{

    private List<String[]> posicao = new ArrayList<>();

    private List<String[]> velocidade = new ArrayList<>();

    private String classe;

    private double[] fitness;

    private final InterfaceFitness calculadorFitness;

    /**
     * Construtor.
     *
     * @param velocidade
     * @param posicao
     * @param classe
     * @param f
     */
    public Particula(List<String[]> velocidade, List<String[]> posicao,
            String classe, InterfaceFitness f)
    {
        this.posicao = posicao;
        this.velocidade = velocidade;
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
    public List<String[]> posicao()
    {
        return posicao;
    }

    /**
     * Seta nova posição da partícula.
     *
     * @param posicao Lista de String WHERE de nova posição.
     */
    public void setPosicao(List<String[]> posicao)
    {
        this.posicao = posicao;
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
     * Retorna a velocidade da partícula.
     *
     * @return Lista de String WHERE de nova velocidade.
     */
    public List<String[]> velocidade()
    {
        return velocidade;
    }

    /**
     * Seta nova velocidade da partícula.
     *
     * @param velocidade Lista de String WHERE de nova velocidade.
     */
    public void setVelocidade(List<String[]> velocidade)
    {
        this.fitness = calculadorFitness.calcula(this);
        this.velocidade = velocidade;
    }

    /**
     * Retorna string de cláusulas WHERE.
     *
     * @param l
     * @return String de cláusulas WHERE.
     */
    private String join(List<String[]> l)
    {
        List<String> list = new LinkedList<>();

        for (String[] iter : l)
        {
            list.add(String.format("%s %s %s", iter[0], iter[1], iter[2]));
        }

        return "(" + StringUtils.join(list, ") AND (") + ")";
    }

    @Override
    public String toString()
    {
        return join(posicao);
    }

}
