package com.blogspot.tsprates.pso;

import org.apache.commons.math3.ml.distance.EuclideanDistance;

import java.util.List;

/**
 * Calcula Distância.
 *
 * @author thiago
 */
public class Distancia
{

    private static final EuclideanDistance DIST = new EuclideanDistance();

    /**
     * Retorna a partícula mais próxima por meio do cálculo da distância
     * Euclidiana no espaço de objetivos.
     *
     * @param parts Lista de partículas não dominadas.
     * @param p Partícula.
     * @return A partícula mais próxima.
     */
    public static Particula retornarParticulaMaisProxima(List<Particula> parts,
            Particula p)
    {
        Particula particulaProxima = parts.get(0);

        double distancia = DIST.compute(particulaProxima.fitness(),
                p.fitness());

        for (int i = 0, size = parts.size(); i < size; i++)
        {
            double d = DIST.compute(parts.get(i).fitness(),
                    particulaProxima.fitness());

            if (d > distancia)
            {
                particulaProxima = parts.get(i);
                distancia = d;
            }
        }

        return particulaProxima;
    }
}
