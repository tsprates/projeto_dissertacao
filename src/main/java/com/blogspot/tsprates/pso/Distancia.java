package com.blogspot.tsprates.pso;

import java.util.List;
import org.apache.commons.math3.ml.distance.EuclideanDistance;

public class Distancia
{

    private static EuclideanDistance distanciaEuclidiana
            = new EuclideanDistance();

    /**
     * Retorna a partícula mais próxima dentre a lista de partícula passada,
     * através do cálculo da distância Euclidiana.
     *
     * @param parts Lista de partículas.
     * @param p Partícula.
     * @return
     */
    public static Particula retornarParticulaMaisProxima(List<Particula> parts,
            Particula p)
    {
        Particula particulaProxima = parts.get(0);

        double distancia = distanciaEuclidiana.compute(
                particulaProxima.fitness(), p.fitness());

        for (int i = 0, size = parts.size(); i < size; i++)
        {
            double d = distanciaEuclidiana.compute(parts.get(i).fitness(),
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
