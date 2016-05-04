package com.blogspot.tsprates.pso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author thiago
 */
public class SolucoesNaoDominadas
{
    private final Connection conexao;
    
    private final String tabela;
    
    private final Set<String> tipoSaidas;
    
    /**
     * Construtor
     * 
     * @param conexao
     * @param tabela
     * @param tipoSaidas 
     */
    public SolucoesNaoDominadas(Connection conexao, String tabela, Set<String> tipoSaidas)
    {
        this.conexao = conexao;
        this.tabela = tabela;
        this.tipoSaidas = tipoSaidas;
        
        criarTabelaSolucoesNaoDominadas();
    }
    
    /**
     * Salva resultado no banco de dados.
     *
     * @param repositorio Repositório contendo as soluções não dominadas encontradas.
     */
    public void salvar(Map<String, List<Particula>> repositorio)
    {

        PreparedStatement pstmt;

        String sql = "INSERT INTO " + tabela + "_fronteira(classe, complexidade, efetividade) "
                + "VALUES(?,?,?)";

        try
        {

            pstmt = conexao.prepareStatement(sql);
            conexao.setAutoCommit(false);

            for (String cls : tipoSaidas)
            {
                List<Particula> gbestList = repositorio.get(cls);
                for (Particula p : gbestList)
                {
                    double[] fit = p.fitness();
                    pstmt.setString(1, cls);
                    pstmt.setDouble(2, fit[0]);
                    pstmt.setDouble(3, fit[1]);
                    pstmt.addBatch();
                }
            }

            pstmt.executeBatch();
            conexao.commit();
            System.out.println("Resultados inseridos com sucesso!");

        }
        catch (SQLException e)
        {
            try
            {
                conexao.rollback();
            }
            catch (SQLException ex)
            {
                throw new RuntimeException("Erro ao salvar resultados!", ex);
            }

        }
        
        limparSolucoesDominadasSalvas();
    }

    /**
     * Remove soluções dominadas do banco de dados.
     *
     */
    private void limparSolucoesDominadasSalvas()
    {

        PreparedStatement pstmt;

        String sql = "DO $do$\n"
                + "DECLARE r " + tabela + "_fronteira%ROWTYPE;\n"
                + "BEGIN\n"
                + "FOR r IN SELECT * FROM " + tabela + "_fronteira\n"
                + " LOOP\n"
                + "     DELETE FROM " + tabela + "_fronteira AS w "
                + "     WHERE w.complexidade <= r.complexidade "
                + "         AND w.efetividade <= r.efetividade "
                + "         AND (w.complexidade < r.complexidade OR w.efetividade < r.efetividade) "
                + "         AND w.classe=r.classe;\n"
                + " END LOOP;\n "
                + "END\n"
                + "$do$;";

        try
        {
            pstmt = conexao.prepareStatement(sql);
            pstmt.execute();
            System.err.println("Resultados dominados atualizado com sucesso!");
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Erro ao remove resultados dominados!", e);
        }
    }

    /**
     * Cria tabela de soluções não dominadas do banco de dados.
     *
     */
    private void criarTabelaSolucoesNaoDominadas()
    {

        PreparedStatement pstmt;

        String sql = "CREATE TABLE IF NOT EXISTS " + tabela + "_fronteira\n"
                + "(\n"
                + "  id serial NOT NULL,\n"
                + "  classe \"char\",\n"
                + "  complexidade double precision,\n"
                + "  efetividade double precision,\n"
                + "  CONSTRAINT " + tabela + "_fronteira_pk_id PRIMARY KEY (id)\n"
                + ");";

        try
        {
            pstmt = conexao.prepareStatement(sql);
            pstmt.execute();
            System.out.println();
            System.err.println("Tabela de soluções não dominadas inicializada com sucesso!");
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Erro ao criar tabela de soluções não dominadas!", e);
        }
    }
}
