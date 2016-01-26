package com.blogspot.tsprates.pso;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class Particula {
    private List<String> listaWhere = new ArrayList<>();
    
    private String classe;

    public List<String> getListaWhere() {
	return listaWhere;
    }

    public Particula(List<String> listaWhere, String classe) {
	this.listaWhere = listaWhere;
	this.classe = classe;
    }

    public String getWhereSql() {
	return "(" + StringUtils.join(listaWhere, ") AND (") + ")";
    }

    public int getSize() {
	return listaWhere.size();
    }
}
