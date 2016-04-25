package com.blogspot.tsprates.pso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Fronteira Pareto.
 *
 * @author thiago
 */
public class FronteiraPareto
{

    /**
     * Adiciona partículas não dominadas.
     *
     * @param particulas Lista de partícula.
     * @param particula Partícula.
     */
    public void atualizarParticulas(List<Particula> particulas,
            Particula particula)
    {
        double[] partFit = particula.fitness();

        if (particulas.isEmpty())
        {
            particulas.add(new Particula(particula));
        }
        else
        {
            boolean removido = false;

            Iterator<Particula> it = particulas.iterator();
            while (it.hasNext())
            {
                Particula p = it.next();
                double[] pfit = p.fitness();
                if (testarSeParticulaDominaOutra(partFit, pfit))
                {
                    removido = true;
                    it.remove();
                }
            }

            boolean contem = particulas.contains(particula);
            if (removido && !contem)
            {
                particulas.add(new Particula(particula));
            }
            else
            {

                boolean adiciona = false;
                for (Particula p : particulas)
                {
                    double[] pfit = p.fitness();
                    boolean testeIf = testarSolucoesInteressantes(partFit, pfit);
                    if (testeIf)
                    {
                        adiciona = true;
                        break;
                    }
                }

                if (adiciona && !contem)
                {
                    particulas.add(new Particula(particula));
                }
            }
        }

        Collections.sort(particulas);
    }

    /**
     * Testar por soluções interessantes.
     *
     * @param partFit
     * @param pfit
     * @return
     */
    private static boolean testarSolucoesInteressantes(double[] partFit, double[] pfit)
    {
        return Double.compare(partFit[0], pfit[0]) > 0
                || Double.compare(partFit[1], pfit[1]) > 0;
    }

    /**
     * Testa se a partícula A domina a partícula B.
     *
     * @param fitPa Fitness da partícula A.
     * @param fitPb Fitness da partícula B.
     * @return
     */
    private static boolean testarSeParticulaDominaOutra(double[] fitPa, double[] fitPb)
    {
        return Double.compare(fitPa[0], fitPb[0]) >= 0
                && Double.compare(fitPa[1], fitPb[1]) >= 0
                && (Double.compare(fitPa[0], fitPb[0]) > 0
                || Double.compare(fitPa[1], fitPb[1]) > 0);
    }

    /**
     * Soluções não dominadas.
     * 
     * @param particulas
     * @return 
     */
    public static List<Particula> getParticulasNaoDominadas(List<Particula> particulas)
    {
        List<Particula> particulasNaoDominadas = new ArrayList<>(particulas);

        for (int i = 0, len = particulas.size(); i < len; i++)
        {
            Particula part = particulas.get(i);
            double[] partfit = part.fitness();
            String partwhere = part.whereSql();

            Iterator<Particula> it = particulasNaoDominadas.iterator();
            while (it.hasNext())
            {
                Particula p = it.next();
                double[] pfit = p.fitness();
                String pwhere = p.whereSql();

                if (testarSeParticulaDominaOutra(partfit, pfit)
                        && !partwhere.equals(pwhere))
                {
                    it.remove();
                }
            }
        }
        return particulasNaoDominadas;
    }
}
