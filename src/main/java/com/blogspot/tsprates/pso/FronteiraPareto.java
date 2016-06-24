package com.blogspot.tsprates.pso;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;

/**
 * Fronteira Pareto.
 *
 * @author thiago
 */
public class FronteiraPareto
{

    private final static int LIMITE_REPO = 30;

    public static void verificarTamanhoDoRepositorio(Collection<Particula> repositorio)
    {
        List<Particula> rep = new ArrayList<>(repositorio);
        Collections.sort(rep);
        final int repSize = rep.size();
        if (repSize > LIMITE_REPO)
        {
            while (rep.size() > LIMITE_REPO)
            {
                int index = rep.size() - 1;
                rep.remove(RandomUtils.nextInt(1, index));
            }
        }
    }

    /**
     * Adiciona partículas não dominadas.
     *
     * @param particulas Lista de partícula.
     * @param particula Partícula.
     */
    public static void atualizarParticulas(Collection<Particula> particulas,
            Particula particula)
    {
        double[] pfit = particula.fitness();

        if (particulas.isEmpty())
        {
            particulas.add(new Particula(particula));
        }
        else
        {
            boolean adiciona = false;
            boolean dominada = false;

            Iterator<Particula> iter = particulas.iterator();

            while (iter.hasNext())
            {
                double[] pfitAtual = iter.next().fitness();

                // testa se a partícula domina partícula atual
                if (testarDominancia(pfit, pfitAtual))
                {
                    iter.remove();
                }

                // testa se não é dominada
                if (testarNaoDominancia(pfit, pfitAtual))
                {
                    adiciona = true;
                }

                if (dominada == false && testarDominancia(pfitAtual, pfit))
                {
                    dominada = true;
                }
            }

            if ((adiciona == true && dominada == false)
                    && !particulas.contains(particula))
            {
                particulas.add(new Particula(particula));
            }
        }
    }

    /**
     * Testar por soluções eficientes.
     *
     * @param pafit Partícula A.
     * @param pbfit Partícula B.
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
//    public static Collection<Particula> getParticulasNaoDominadas(
//            Collection<Particula> particulas)
//    {
//        List<Particula> particulasNaoDominadas = new ArrayList<>(particulas);
//
//        for (Particula part : particulas)
//        {
//            double[] partfit = part.fitness();
//            String partwhere = part.whereSql();
//
//            Iterator<Particula> it = particulasNaoDominadas.iterator();
//            while (it.hasNext())
//            {
//                Particula pi = it.next();
//                double[] pifit = pi.fitness();
//                String piwhere = pi.whereSql();
//
//                if (testarDominancia(partfit, pifit)
//                        && !partwhere.equals(piwhere))
//                {
//                    it.remove();
//                }
//            }
//        }
//
//        return particulasNaoDominadas;
//    }
}
