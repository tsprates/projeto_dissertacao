package com.blogspot.tsprates.pso;

import java.util.Collection;
import java.util.Iterator;

public class FronteiraPareto
{

    /**
     * Adiciona partículas não dominadas.
     *
     * @param particulas Lista de partícula.
     * @param particula Partícula.
     */
    public void atualizaParticulasNaoDominadas(Collection<Particula> particulas, Particula particula)
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
                if (Double.compare(partFit[0], pfit[0]) >= 0 
                        && Double.compare(partFit[1], pfit[1]) >= 0
                        && (Double.compare(partFit[0], pfit[0]) > 0 
                            || Double.compare(partFit[1], pfit[1]) > 0))
                {
                    removido = true;
                    it.remove();
                }
            }

            if (removido)
            {
                particulas.add(new Particula(particula));
            }
            else
            {

                boolean adiciona = false;
                for (Particula p : particulas)
                {
                    double[] pfit = p.fitness();
                    if ((Double.compare(partFit[0], pfit[0]) > 0 
                            && Double.compare(partFit[1], pfit[1]) >= 0)
                            || (Double.compare(partFit[0], pfit[0]) >= 0 
                                && Double.compare(partFit[1], pfit[1]) > 0))
                    {
                        adiciona = true;
                        break;
                    }
                }

                if (adiciona)
                {
                    particulas.add(new Particula(particula));
                }
            }
        }
    }
}
