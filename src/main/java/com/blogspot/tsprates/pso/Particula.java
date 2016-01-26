package com.blogspot.tsprates.pso;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class Particula {
    private List<String> vel = new ArrayList<String>();

    private List<String> pos = new ArrayList<String>();
    
    private String classe;

    public List<String> getVelocidade() {
	return vel;
    }
    
    public List<String> getPosicao() {
	return pos;
    }

    public Particula(List<String> vel, List<String> pos, String classe) {
	this.vel = vel;
	this.pos = pos;
	this.classe = classe;
    }

    public String getVelocidadeSql() {
	return "(" + StringUtils.join(vel, ") AND (") + ")";
    }

    public String getPosicaoSql() {
	return "(" + StringUtils.join(pos, ") AND (") + ")";
    }

    public int getSize() {
	return vel.size();
    }
    
}
