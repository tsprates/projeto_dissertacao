package com.blogspot.tsprates.pso;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * Particles Swarm Optimization (PSO).
 *
 * @author thiago
 *
 */
public class App
{

    /**
     * Main.
     *
     * @param args
     */
    public static void main(String[] args)
    {
        System.out.println("--------------------------------------------");
        System.out.println(" Projeto de Dissertação Mestrado            ");
        System.out.println(" Implementação PSO                          ");
        System.out.println("--------------------------------------------");

        if (args.length > 0 && Files.exists(Paths.get(args[0])))
        {
            Connection conexaoDb = new DbFactory().conectar();
            Properties config = carregarConfigArquivo(args[0]);

            final Formatador format = new Formatador();

            final String tituloGrafico = StringUtils
                    .capitalize(config.getProperty("tabela"));

            final int exec = 30;

            Pso pso = new Pso(conexaoDb, config, format);
            final Set<String> classes = pso.getClasses();

            Map<String, List<Double>> efetividade;
            Map<String, List<Double>> acuracia;

            List<Double> efetividadeGlobal = new ArrayList<>();
            List<Double> acuraciaGlobal = new ArrayList<>();

            for (int iter = 0; iter < exec; iter++)
            {
                System.out.println();
                System.out.println("Execução: " + (iter + 1));
                System.out.println();

                pso.carrega();

                efetividade = pso.getEfetividade();
                acuracia = pso.getAcuracia();

                double soma = 0;
                for (String c : classes)
                {
                    soma += Collections.max(efetividade.get(c));
                }
                acuraciaGlobal.add(soma / (double) classes.size());

            }

            graficoAcuraciaGlobal(tituloGrafico, acuraciaGlobal);
        }
        else
        {
            System.err.println("É necessário definir um arquivo de configuração.");
        }

    }

    /**
     *
     * @param titulo
     * @param valores
     */
    private static void graficoAcuraciaGlobal(final String titulo,
            List<Double> valores)
    {
        final String eixoX = "População";
        final String eixoY = "Sensibilidade x Especificidade";
        Grafico g = new Grafico(titulo, eixoX, eixoY);
        g.adicionaSerie("Mopso", valores);
        g.mostra();
    }

    /**
     *
     * @param classes
     * @param efetividadeMedia
     * @param format
     * @throws MathIllegalArgumentException
     */
//    private static void mostraMedia(Set<String> classes, final Map<String, List<Double>> efetividadeMedia, final Formatador formatar)
//            throws MathIllegalArgumentException
//    {
//        StandardDeviation sd = new StandardDeviation();
//        Mean mean = new Mean();
//
//        StringBuilder builder = new StringBuilder("Classe\tMédia\tDesvio\n\n");
//        for (String classe : classes)
//        {
//            List<Double> val = efetividadeMedia.get(classe);
//            double[] v = new double[val.size()];
//            for (int i = 0, len = val.size(); i < len; i++)
//            {
//                v[i] = val.get(i);
//            }
//
//            double desvio = sd.evaluate(v);
//            double media = mean.evaluate(v);
//            builder.append(classe).append("\t").append(formatar.formatar(media))
//                    .append("\t").append(formatar.formatar(desvio)).append("\n");
//        }
//        System.out.println(builder.toString());
//    }
    /**
     * Gráfico de resultados de efetividade.
     *
     * @param tituloGrafico
     * @param pso
     */
//    private static void graficoEfetividade(final String tituloGrafico,
//            Map<String, List<Double>> mapa)
//    {
//        final String eixoX = "População";
//        final String eixoY = "Sensibilidade x Especificidade";
//        Grafico g = new Grafico(tituloGrafico, eixoX, eixoY);
//        for (Entry<String, List<Double>> ent : mapa.entrySet())
//        {
//            g.adicionaSerie(ent.getKey(), ent.getValue());
//        }
//        g.mostra();
//    }
    /**
     * Retorna arquivo de configurações.
     *
     * @param configFile Configurations
     * @return Properties
     */
    private static Properties carregarConfigArquivo(String configFile)
    {
        try (FileInputStream fis = new FileInputStream(configFile))
        {
            Properties prop = new Properties();
            prop.load(fis);
            return prop;
        }
        catch (IOException e)
        {
            throw new RuntimeException(
                    "Arquivo de configurações não encontrado.", e);
        }
    }
}
