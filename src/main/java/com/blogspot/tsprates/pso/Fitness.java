package com.blogspot.tsprates.pso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author thiago
 */
public class Fitness implements InterfaceFitness {

    private final Connection conexao;

    private final String tabela;

    private final String colCod;

    private final Map<String, Set<Integer>> classeSaidas;

    private List<String> resultadoLista;

    public Fitness(Connection c, final String colCod, final String tab, final Map<String, Set<Integer>> classeSaidas, final Particula p) {
        this.conexao = c;
        this.colCod = colCod;
        this.tabela = tab;
        this.classeSaidas = classeSaidas;
    }

    public double[] calc(Particula p) {
        double[] result = new double[2];
        result[0] = calcEfetividade(p);
        result[1] = (double) p.getSize();
        return result;
    }

    private List<String> recuperaClasse(String whereSql) {
        String sql = "SELECT " + colCod + " AS id FROM " + tabela + " WHERE " + whereSql;
        List<String> r = new ArrayList<String>();

        try {
            PreparedStatement ps = conexao.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                r.add(rs.getString("id"));
            }

            ps.close();
            rs.close();

            return r;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao recupera as classes no banco de dados.", e);
        }
    }

    private double calcEfetividade(Particula p) {
        this.resultadoLista = recuperaClasse(p.getWhereSql());
        String classePart = p.getClasse();

        this.resultadoLista.removeAll(this.classeSaidas.get(classePart));

        return resultadoLista.size();
    }

}
