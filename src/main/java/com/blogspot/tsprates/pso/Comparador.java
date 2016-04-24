package com.blogspot.tsprates.pso;

import java.util.Comparator;

/**
 *
 * @author thiago
 */
class Comparador implements Comparator<Particula>
{

    @Override
    public int compare(Particula a, Particula b)
    {
        double[] fita = a.fitness();
        double[] fitb = b.fitness();

        if (fita[0] == fitb[0])
        {
            if (fita[1] == fitb[1])
            {
                return 0;
            }
            else if (fita[1] < fitb[1])
            {
                return -1;
            }
            else
            {
                return 1;
            }
        }
        else if (fita[0] < fitb[0])
        {
            return -1;
        }
        else
        {
            return 1;
        }
    }
}
