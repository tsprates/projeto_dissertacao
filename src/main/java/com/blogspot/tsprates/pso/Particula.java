package com.blogspot.tsprates.pso;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Classe Partícula.
 * 
 * @author thiago
 */
public class Particula {

    private List<String> posicao = new ArrayList<>();

    private List<String> velocidade = new ArrayList<>();

    private final String classe;
    
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
    public Particula(List<String> velocidade, List<String> posicao,
	    String classe, InterfaceFitness f) {
	this.classe = classe;
	this.posicao = posicao;
	this.velocidade = velocidade;
	this.calculadorFitness = f;
        
        final Particula p = this;
	this.fitness = calculadorFitness.calcula(p);
    }

    /**
     * Get posição da partícula.
     * 
     * @return Lista de String WHERE de nova posição.
     */
    public List<String> getPosicao() {
	return posicao;
    }
    
    /**
     * Seta nova posição da partícula.
     * 
     * @param posicao Lista de String WHERE de nova posição.
     */
    public void setPosicao(List<String> posicao) {
 	this.posicao = posicao;
 	this.fitness = calculadorFitness.calcula(this);
     }

    /**
     * Retorna uma cláusula WHERE SQL correspondente a posição da partícula.
     * 
     * @return String WHERE SQL.
     */
    public String asWhereSql() {
	return join(getPosicao());
    }

    /**
     * Retorna a classe da partícula.
     * 
     * @return String representando a partícula.
     */
    public String getClasse() {
	return classe;
    }

    /**
     * Retorna a dimensão (tamanho) da cláusula WHERE.
     * 
     * @return Tamanho da cláusula WHERE.
     */
    public int getNumWhere() {
	return getPosicao().size();
    }
    
    /**
     * Retorna fitness da partícula.
     * 
     * @return Array de doubles.
     */
    public double[] getFitness() {
	return fitness;
    }
    
    /**
     * Retorna a velocidade da partícula.
     * 
     * @return Lista de String WHERE de nova velocidade.
     */
    public List<String> getVelocidade() {
	return velocidade;
    }

    /**
     * Seta nova velocidade da partícula.
     * 
     * @param velocidade Lista de String WHERE de nova velocidade.
     */
    public void setVelocidade(List<String> velocidade) {
	this.velocidade = velocidade;
    }

    /**
     * Retorna string de cláusulas WHERE.
     * 
     * @param l
     * @return String de cláusulas WHERE.
     */
    private String join(List<String> l) {
	return "(" + StringUtils.join(l, ") AND (") + ")";
    }

}