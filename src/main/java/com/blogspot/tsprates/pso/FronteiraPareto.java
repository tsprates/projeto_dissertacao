package com.blogspot.tsprates.pso;

import java.util.Iterator;
import java.util.Set;

public class FronteiraPareto
{

    /**
     * Adiciona partículas não dominadas.
     *
     * @param listaParticulas Lista de partícula.
     * @param particula Partícula.
     */
    public void atualizaParticulasNaoDominadas(Set<Particula> listaParticulas, Particula particula)
    {
        double[] partFit = particula.fitness();

        if (listaParticulas.isEmpty())
        {
            listaParticulas.add(new Particula(particula));
        }
        else
        {
            boolean removido = false;

            Iterator<Particula> it = listaParticulas.iterator();
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
                listaParticulas.add(new Particula(particula));
            }
            else
            {

                boolean adiciona = false;
                for (Particula p : listaParticulas)
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
                    listaParticulas.add(new Particula(particula));
                }
            }
        }
    }
}
