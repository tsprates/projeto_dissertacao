package com.blogspot.tsprates.pso;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class Particula {

    private final List<List<String>> best = new ArrayList<List<String>>();

    private List<String> posicao = new ArrayList<String>();

    private final String classe;

    public Particula(List<String> velocidade, List<String> posicao, String classe) {
	this.classe = classe;
	this.posicao = posicao;
	this.best.add(posicao);
    }

    public List<String> getPosicao() {
	return posicao;
    }

    public void addBest(List<String> whereSql) {
	best.add(whereSql);
    }

    public void removeBest(List<String> whereSql) {
	best.remove(whereSql);
    }

    public String getWhereSql() {
	return joinArray(posicao);
    }

    private String joinArray(List<String> l) {
	return "(" + StringUtils.join(l, ") AND (") + ")";
    }

    public String getClasse() {
	return classe;
    }

    public int getNumWhereSql() {
	return posicao.size();
    }

}