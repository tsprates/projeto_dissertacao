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

    /**
     * Limite total de partículas não dominadas.
     */
    private final static int LIMITE_PARTICULAS = 100;

    /**
     * Verifica, e remove se necessário, partículas fornecidas caso seja maior
     * que o limite definido.
     *
     * @see FronteiraPareto#LIMITE_PARTICULAS
     * @param particulas Lista de partículas.
     */
    public static void verificarNumParticulas(Collection<Particula> particulas)
    {
        List<Particula> parts = new ArrayList<>(particulas);

        Collections.sort(parts);

        if (LIMITE_PARTICULAS < parts.size())
        {
            while (LIMITE_PARTICULAS < parts.size())
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
     * @return Se o resultado é igual a 1, a partícula domina alguém na lista de
     * partículas é então incluída, removendo as partículas dominadas. Se o
     * resultado igual a 0, não há relação de dominância e partícula entra na
     * lista. Se resultado igual a -1 a partícula é por alguém na lista e não há
     * alteração na lista de partículas.
     */
    public static int atualizarParticulasNaoDominadas(
            Collection<Particula> particulas, Particula particula)
    {
        double[] pfit = particula.fitness();

        if (particulas.isEmpty())
        {
            particulas.add(particula.clonar());
            return 1;
        }
        else
        {
            boolean domina = false;
            boolean ehDominada = false;

            Iterator<Particula> iter = particulas.iterator();

            while (iter.hasNext())
            {
                double[] pfitIter = iter.next().fitness();

                // se a partícula testada domina a partícula atual
                if (testarDominanciaEntre(pfit, pfitIter))
                {
                    iter.remove(); // remove a partícula atual
                }

                // se partícula testada não é dominada pela partícula atual
                if (testarNaoDominanciaEntre(pfit, pfitIter))
                {
                    domina = true;
                }

                // se a partícula testada é dominada pela partícula atual
                if (ehDominada == false
                        && testarDominanciaEntre(pfitIter, pfit))
                {
                    ehDominada = true;
                }
            }

            // se a partícula testada não é dominada e se não existe na lista de 
            // partículas não dominadas
            if (domina && ehDominada == false
                    && !particulas.contains(particula))
            {
                particulas.add(particula.clonar());
                return 1;
            }

            if (ehDominada)
            {
                return -1;
            }
            else
            {
                return 0;
            }
        }
    }

    /**
     * Verifica a dominância entre as partícula A e B.
     *
     * @param a Partícula A.
     * @param b Partícula B.
     * @return Se o resultado for igual a 1, a partícula A domina a partícula B.
     * Se resultado igual 0 não há relação de dominância. Por fim, se resultado
     * igual a -1, a partícula A é dominada pela partícula B.
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
     * Testa se a partícula A não domina a partícula B.
     *
     * @param pafit Fitness da partícula A.
     * @param pbfit Fitness da partícula B.
     * @return Retorna verdadeiro se a partícula A não domina a partícula B.
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
     * @return Retorna verdadeiro se a partícula A domina a partícula B.
     */
    private static boolean testarDominanciaEntre(double[] pafit, double[] pbfit)
    {
        return (pafit[0] >= pbfit[0] && pafit[1] >= pbfit[1]
                && (pafit[0] > pbfit[0] || pafit[1] > pbfit[1]));
    }
}
