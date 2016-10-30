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

    private final static int LIMITE_PARTICULAS = 30;

    /**
     * Verifica, e remove se necessário, partículas fornecidas caso seja maior
     * que o limite definido.
     *
     * @see FronteiraPareto#LIMITE_PARTICULAS
     * @param particulas Lista de partículas.
     */
    public static void verificarTamanho(Collection<Particula> particulas)
    {
        List<Particula> parts = new ArrayList<>(particulas);
        Collections.sort(parts);

        final int repSize = parts.size();

        if (repSize > LIMITE_PARTICULAS)
        {
            while (parts.size() > LIMITE_PARTICULAS)
            {
                int index = parts.size() - 1;
                parts.remove(RandomUtils.nextInt(1, index));
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
            particulas.add(particula.clonar());
        }
        else
        {
            boolean domina = false;
            boolean ehDominada = false;

            Iterator<Particula> iter = particulas.iterator();

            while (iter.hasNext())
            {
                double[] pIterfit = iter.next().fitness();

                // se a partícula testada domina a partícula atual
                if (testarDominanciaEntre(pfit, pIterfit))
                {
                    iter.remove(); // remove a partícula atual
                }

                // se partícula testada não é dominada pela partícula atual
                if (testarNaoDominanciaEntre(pfit, pIterfit))
                {
                    domina = true;
                }

                // se a partícula testada é dominada pela partícula atual
                if (ehDominada == false && testarDominanciaEntre(pIterfit, pfit))
                {
                    ehDominada = true;
                }
            }

            // se a partícula testada não é dominada 
            // e não existe dentro da lista de partículas
            if ((domina == true && ehDominada == false)
                    && !particulas.contains(particula))
            {
                particulas.add(particula.clonar());
            }
        }
    }

    /**
     * Verifica a dominância entre as partícula A e B.
     *
     * @param a Partícula A.
     * @param b Partícula B.
     * @return
     */
    public static int verificarDominanciaEntre(Particula a, Particula b)
    {
        double[] pafit = a.fitness();
        double[] pbfit = b.fitness();

        if (testarDominanciaEntre(pafit, pbfit))
        {
            return 1;
        }
        else if (testarNaoDominanciaEntre(pafit, pbfit))
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
    private static boolean testarNaoDominanciaEntre(double[] pafit, double[] pbfit)
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
    private static boolean testarDominanciaEntre(double[] pafit, double[] pbfit)
    {
        return (pafit[0] >= pbfit[0] && pafit[1] >= pbfit[1]
                && (pafit[0] > pbfit[0] || pafit[1] > pbfit[1]));
    }
}
