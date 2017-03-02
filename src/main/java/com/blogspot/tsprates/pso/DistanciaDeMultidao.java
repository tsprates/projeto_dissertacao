package com.blogspot.tsprates.pso;

import org.apache.commons.math3.util.Precision;
import java.util.*;

/**
 * Distância de multidão. Baseado no NSGA-II.
 *
 * @author thiago
 */
public class DistanciaDeMultidao implements Comparator<Particula>
{

    private final Map<Particula, Double> ranking = new HashMap<>();

    /**
     * Realiza o ranqueamento pela distância de multidão para cada partícula.
     *
     * @param particulas
     * @return DistanciaDeMultidao
     */
    public DistanciaDeMultidao ranquearParticulas(Collection<Particula> particulas)
    {
        final List<Particula> tmpParts = new ArrayList<>(particulas);
        final int numParts = tmpParts.size();

        Collections.sort(tmpParts);

        ranking.clear();

        for (int i = 0; i < numParts; i++)
        {
            ranking.put(tmpParts.get(i), 0.0);
        }

        if (numParts == 1)
        {
            ranking.put(tmpParts.get(0), Double.MAX_VALUE);
        }

        if (numParts == 2)
        {
            ranking.put(tmpParts.get(numParts - 1), Double.MAX_VALUE);
        }

        if (numParts > 2)
        {

            final double[] primeiro = tmpParts.get(0).fitness();
            final double[] ultimo = tmpParts.get(numParts - 1).fitness();

            for (int k = 1, len = numParts - 1; k < len; k++)
            {
                final double[] a = tmpParts.get(k + 1).fitness();
                final double[] b = tmpParts.get(k - 1).fitness();

                final Particula particula = tmpParts.get(k);

                double d = 0.0;
                for (int i = 0; i < 2; i++)
                {
                    d += (a[i] - b[i]) / (ultimo[i] - primeiro[i]);
                }

                ranking.put(particula, d);
            }
        }

        return this;
    }

    @Override
    public int compare(Particula p1, Particula p2)
    {
        Double temp1 = ranking.get(p1);
        Double temp2 = ranking.get(p2);

        return Precision.compareTo(temp1, temp2, 0.00001);
    }
}
