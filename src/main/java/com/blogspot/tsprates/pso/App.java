package com.blogspot.tsprates.pso;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

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
            Connection conexaoDb = new DbFactory().conecta();
            Properties config = getConfigs(args[0]);

            final Formatador format = new Formatador();

            final String tituloGrafico = String.format("%s - %s Iteração",
                    config.getProperty("tabela"),
                    config.getProperty("maxiter"));

            final int exec = 10;
            final Map<String, List<Double>> efetividadeMedia = new HashMap<>();
            Map<String, List<Double>> efetividade;

            for (int iter = 0; iter < exec; iter++)
            {
                System.out.println();
                System.out.println("Execução: " + (iter + 1));
                System.out.println();

                Pso pso = new Pso(conexaoDb, config, format);
                pso.carrega();

                efetividade = pso.getEfetividade();

                // Inicia efetividade média com valor zero.
                if (iter == 0)
                {
                    for (Entry<String, List<Double>> ent : efetividade.entrySet())
                    {
                        final int size = ent.getValue().size();
                        final List<Double> zeros = new ArrayList<>(Collections.nCopies(size, 0.0));
                        efetividadeMedia.put(ent.getKey(), zeros);

                    }
                }

                // cada execução atualiza resultados da iteração anterior
                for (Entry<String, List<Double>> ent : efetividade.entrySet())
                {
                    List<Double> efetMed = efetividadeMedia.get(ent.getKey());
                    final List<Double> listaValores = ent.getValue();
                    for (int i = 0, len = listaValores.size(); i < len; i++)
                    {
                        double valor = listaValores.get(i);
                        double valorAtual = efetMed.get(i);
                        efetMed.set(i, valorAtual + valor);
                    }
                }

            }

            Set<String> classes = efetividadeMedia.keySet();
            for (String classe : classes)
            {
                for (int i = 0, len = efetividadeMedia.get(classe).size();
                        i < len; i++)
                {
                    final List<Double> results = efetividadeMedia.get(classe);
                    double valor = results.get(i);
                    efetividadeMedia.get(classe).set(i, valor / (double) exec);
                }
            }

            graficoEfetividade(tituloGrafico, efetividadeMedia);

            mostraMedia(classes, efetividadeMedia, format);
        }
        else
        {
            System.err.println("É necessário definir um arquivo de configuração.");
        }

    }

    /**
     * 
     * @param classes
     * @param efetividadeMedia
     * @param format
     * @throws MathIllegalArgumentException 
     */
    private static void mostraMedia(Set<String> classes, final Map<String, 
            List<Double>> efetividadeMedia, final Formatador format) 
            throws MathIllegalArgumentException
    {
        StandardDeviation sd = new StandardDeviation();
        Mean mean = new Mean();
        
        StringBuilder builder = new StringBuilder("Classe\tMédia\tDesvio\n");
        for (String classe : classes)
        {
            List<Double> val = efetividadeMedia.get(classe);
            double[] v = new double[val.size()];
            for (int i = 0, len = val.size(); i < len; i++)
            {
                v[i] = val.get(i);
            }
            
            double desvio = sd.evaluate(v);
            double media = mean.evaluate(v);
            builder.append(classe).append("\t").append(format.format(media))
                    .append("\t").append(format.format(desvio)).append("\n");
        }
        System.out.println(builder.toString());
    }

    /**
     * Gráfico de resultados de efetividade.
     *
     * @param tituloGrafico
     * @param pso
     */
    private static void graficoEfetividade(final String tituloGrafico,
            Map<String, List<Double>> mapa)
    {
        final String eixoX = "População";
        final String eixoY = "Sensibilidade x Especificidade";
        Grafico g = new Grafico(tituloGrafico, eixoX, eixoY);
        for (Entry<String, List<Double>> ent : mapa.entrySet())
        {
            g.adicionaSerie(ent.getKey(), ent.getValue());
        }
        g.mostra();
    }

    /**
     * Retorna arquivo de configurações.
     *
     * @param configFile Configurations
     * @return Properties
     */
    private static Properties getConfigs(String configFile)
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
