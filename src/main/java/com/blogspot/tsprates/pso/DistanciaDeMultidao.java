package com.blogspot.tsprates.pso;

import org.apache.commons.math3.util.Precision;
import java.util.*;

/**
 * Operador da distância de multidão. Baseado no NSGA-II.
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
        List<Particula> tempParts = new ArrayList<>(particulas);
        final int numParts = tempParts.size();

        Collections.sort(tempParts);

        ranking.clear();

        for (int i = 0; i < numParts; i++)
        {
            ranking.put(tempParts.get(i), 0.0);
        }

        if (numParts == 1)
        {
            ranking.put(tempParts.get(0), Double.MAX_VALUE);
        }

        if (numParts == 2)
        {
            ranking.put(tempParts.get(numParts - 1), Double.MAX_VALUE);
        }

        if (numParts > 2)
        {

            double[] objMax = tempParts.get(0).fitness();
            double[] objMin = tempParts.get(numParts - 1).fitness();

            for (int k = 1, len = numParts - 1; k < len; k++)
            {
                double[] a = tempParts.get(k + 1).fitness();
                double[] b = tempParts.get(k - 1).fitness();

                Particula particula = tempParts.get(k);

                double d = 0.0;
                for (int i = 0; i < 2; i++)
                {
                    d += (a[i] - b[i])
                            / (objMax[i] - objMin[i] + Double.MIN_VALUE);
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
