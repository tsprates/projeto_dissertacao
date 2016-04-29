package com.blogspot.tsprates.pso;

import java.util.ArrayList;
import java.util.Collection;
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
    public void atualizarParticulas(Collection<Particula> particulas,
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
                if (testarDominancia(partFit, pfit))
                {
                    removido = true;
                    it.remove();
                }
            }

            if (removido && !particulas.contains(particula))
            {
                particulas.add(new Particula(particula));
            }
            else
            {

                boolean adiciona = false;
                for (Particula p : particulas)
                {
                    double[] pfit = p.fitness();
                    if (testarNaoDominancia(partFit, pfit))
                    {
                        adiciona = true;
                        break;
                    }
                }

                if (adiciona && !particulas.contains(particula))
                {
                    particulas.add(new Particula(particula));
                }
            }
        }
    }

    /**
     * Testar por soluções eficientes.
     *
     * @param pafit
     * @param pbfit
     * @return
     */
    private static boolean testarNaoDominancia(double[] pafit, double[] pbfit)
    {
        return (pafit[0] > pbfit[0] || pafit[1] > pbfit[1]);
    }

    /**
     * Testa se a partícula A domina a partícula B.
     *
     * @param pafit Fitness da partícula A.
     * @param pbfit Fitness da partícula B.
     * @return
     */
    private static boolean testarDominancia(double[] pafit, double[] pbfit)
    {
        return (pafit[0] >= pbfit[0] && pafit[1] >= pbfit[1]
                && (pafit[0] > pbfit[0] || pafit[1] > pbfit[1]));
    }

    /**
     * Soluções não dominadas.
     *
     * @param particulas
     * @return
     */
    public static Collection<Particula> getParticulasNaoDominadas(
            Collection<Particula> particulas)
    {
        List<Particula> particulasNaoDominadas = new ArrayList<>(particulas);

        for (Particula part : particulas)
        {
            double[] partfit = part.fitness();
            String partwhere = part.whereSql();

            Iterator<Particula> it = particulasNaoDominadas.iterator();
            while (it.hasNext())
            {
                Particula pi = it.next();
                double[] pifit = pi.fitness();
                String piwhere = pi.whereSql();

                if (testarDominancia(partfit, pifit)
                        && !partwhere.equals(piwhere))
                {
                    it.remove();
                }
            }
        }

        Collections.sort(particulasNaoDominadas);

        return particulasNaoDominadas;
    }

}
