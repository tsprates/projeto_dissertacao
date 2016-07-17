package com.blogspot.tsprates.pso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 *
 * @author thiago
 */
public class SolucoesNaoDominadas
{

    private final Connection conexao;

    private final String tabela;

    private final Set<String> tipoSaidas;

    private final static String PREFIX_TAB = "frontpareto_";

    /**
     * Construtor
     *
     * @param conexao
     * @param tabela
     * @param tipoSaidas
     */
    public SolucoesNaoDominadas(Connection conexao, String tabela,
            Set<String> tipoSaidas)
    {
        this.conexao = conexao;
        this.tabela = tabela;
        this.tipoSaidas = tipoSaidas;

        criarTabelaSolucoesNaoDominadas();
    }

    /**
     * Salva resultado no banco de dados.
     *
     * @param repositorio Repositório soluções não dominadas encontradas.
     */
    public void salvar(Map<String, List<Particula>> repositorio)
    {

        PreparedStatement pstmt;

        String sql = "INSERT " + "INTO " + PREFIX_TAB + tabela + "(classe, complexidade, efetividade, condicao) "
                + "VALUES(?, ?, ?, ?)";

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
                    pstmt.setString(4, p.whereSql());
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
    }

    /**
     * Remove soluções dominadas do banco de dados.
     *
     */
    public void limparSolucoesDominadasSalvas()
    {

        PreparedStatement pstmt;

        String sql = "BEGIN;"
                + "DO $do$\n"
                + "DECLARE r " + PREFIX_TAB + tabela + "%ROWTYPE;\n"
                + "BEGIN\n"
                + "FOR r IN SELECT * FROM " + PREFIX_TAB + tabela + "\n"
                + " LOOP\n"
                + "     DELETE FROM " + PREFIX_TAB + tabela + " AS fp "
                + "     WHERE fp.complexidade <= r.complexidade "
                + "         AND fp.efetividade <= r.efetividade "
                + "         AND (fp.complexidade < r.complexidade OR fp.efetividade < r.efetividade) "
                + "         AND fp.classe=r.classe;\n"
                + " END LOOP;\n "
                + "END\n" + "$do$;" + "COMMIT;";

        try
        {
            pstmt = conexao.prepareStatement(sql);
            pstmt.execute();

            System.out.println("Resultados dominados atualizados com sucesso!");
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Erro ao remove resultados dominados!",
                    e);
        }
    }

    /**
     * Cria tabela de soluções não dominadas do banco de dados.
     *
     */
    private void criarTabelaSolucoesNaoDominadas()
    {

        PreparedStatement pstmt;

        String sql = "CREATE TABLE IF NOT EXISTS " + PREFIX_TAB + tabela + "(\n"
                + "     classe \"char\",\n"
                + "     complexidade double precision,\n"
                + "     efetividade double precision,\n"
                + "     condicao text\n"
                + ");";

        try
        {
            pstmt = conexao.prepareStatement(sql);
            pstmt.execute();

            System.out.println();
            System.out.println(
                    "Tabela de soluções não dominadas inicializada com sucesso!");
        }
        catch (SQLException e)
        {
            throw new RuntimeException(
                    "Erro ao criar tabela de soluções não dominadas!", e);
        }
    }

    /**
     * Retorna fronteira Pareto salva no banco de dados.
     *
     * @return Fronteira Pareto.
     */
    public Map<String, List<Double[]>> get()
    {

        Map<String, List<Double[]>> retorno = new HashMap<>();

        for (String saida : tipoSaidas)
        {
            retorno.put(saida, new ArrayList<Double[]>());
        }

        String sql = "SELECT DISTINCT classe, complexidade, efetividade "
                + "FROM " + PREFIX_TAB + tabela + " "
                + "ORDER BY complexidade DESC, efetividade DESC";

        try (PreparedStatement ps = conexao.prepareStatement(sql);
                ResultSet rs = ps.executeQuery())
        {
            while (rs.next())
            {
                retorno.get(rs.getString("classe"))
                        .add(new Double[]
                                {
                                    rs.getDouble("complexidade"),
                                    rs.getDouble("efetividade")
                        });
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Erro ao retornar Fronteira Pareto.", e);
        }

        return retorno;
    }
}
