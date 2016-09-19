package com.blogspot.tsprates.pso;

import org.apache.commons.lang3.RandomUtils;

import java.util.*;

/**
 * Fronteira Pareto.
 *
 * @author thiago
 */
public class FronteiraPareto
{

    private final static int LIMITE_REPO = 30;

    /**
     * Verifica e remove partículas da coleção repositório, caso este seja maior
     * que o limite definido.
     *
     * @param repositorio Lista de partículas.
     */
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

                // se a partícula testada domina a partícula atual
                if (testarDominancia(pfit, pfitAtual))
                {
                    iter.remove(); // remove a partícula atual
                }

                // se partícula testada não é dominada pela partícula atual
                if (testarNaoDominancia(pfit, pfitAtual))
                {
                    adiciona = true;
                }

                // se a partícula testada é dominada pela partícula atual
                if (dominada == false && testarDominancia(pfitAtual, pfit))
                {
                    dominada = true;
                }
            }

            // se a partícula testada não é dominada 
            // e não existe dentro da lista de partículas
            if ((adiciona == true && dominada == false)
                    && !particulas.contains(particula))
            {
                particulas.add(new Particula(particula));
            }
        }
    }

    /**
     * Verifica a dominância entre as partícula A e B.
     *
     * @param pa Partícula A.
     * @param pb Partícula B.
     * @return
     */
    public static int verificarDominanciaParticulas(Particula pa, Particula pb)
    {
        double[] afit = pa.fitness();
        double[] bfit = pb.fitness();

        if (testarDominancia(afit, bfit))
        {
            return 1;
        }
        else if (testarNaoDominancia(afit, bfit))
        {
            return 0;
        }
        else
        {
            return -1;
        }
    }

    /**
     * Testa se a partícula A não-domina a partícula B.
     *
     * @param pafit Fitness da partícula A.
     * @param pbfit Fitness da partícula B.
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

//    /**
//     * Soluções não dominadas.
//     *
//     * @param particulas
//     * @return
//     */
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
