package com.blogspot.tsprates.pso;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class Particula {

    private List<String> posicao = new ArrayList<String>();

    private List<String> velocidade = new ArrayList<String>();

    private final String classe;
    
    private double[] fitness;
    
    private final FitnessInterface f;

    public Particula(List<String> velocidade, List<String> posicao,
	    String classe, FitnessInterface f) {
	this.classe = classe;
	this.posicao = posicao;
	this.velocidade = velocidade;
	this.f = f;
	this.fitness = f.calcula(this);
    }

    public List<String> getPosicao() {
	return posicao;
    }
    
    public void setPosicao(List<String> posicao) {
 	this.posicao = posicao;
 	this.fitness = f.calcula(this);
     }

    public String getWhereSql() {
	return joinArray(getPosicao());
    }

    public String getClasse() {
	return classe;
    }

    public int getNumWhereSql() {
	return getPosicao().size();
    }
    
    public double[] getFitness() {
	return fitness;
    }

    public List<String> getVelocidade() {
	return velocidade;
    }

    public void setVelocidade(List<String> velocidade) {
	this.velocidade = velocidade;
    }

    private String joinArray(List<String> l) {
	return "(" + StringUtils.join(l, ") AND (") + ")";
    }

}