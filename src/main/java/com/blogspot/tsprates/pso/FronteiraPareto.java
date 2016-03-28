package com.blogspot.tsprates.pso;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FronteiraPareto {
    
    /**
     * Adiciona partículas não dominadas.
     *
     * @param listaParticulas Lista de partícula.
     * @param particula Partícula.
     */
    public static void atualizaParticulasNaoDominadas(Collection<Particula> listaParticulas, Particula particula)
    {
        double[] pfit = particula.fitness();

        if (listaParticulas.isEmpty())
        {
            listaParticulas.add(particula);
        }
        else
        {
            List<Particula> removeItens = new ArrayList<>();

            for (Particula p : listaParticulas)
            {
                double[] fit = p.fitness();
                if (pfit[0] >= fit[0] && pfit[1] >= fit[1]
                        && (pfit[0] > fit[0] || pfit[1] > fit[1]))
                {
                    removeItens.add(p);
                }

            }

            if (removeItens.size() > 0)
            {
                listaParticulas.removeAll(removeItens);
                listaParticulas.add(particula);
            }
        }
    }
}
