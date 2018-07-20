package com.github.tsprates.pso;

import org.apache.commons.math3.ml.distance.EuclideanDistance;

import java.util.List;

/**
 * Calcula Distância.
 *
 * @author thiago
 */
public class Distancia
{

    private static final EuclideanDistance DIST_EUCL = new EuclideanDistance();

    /**
     * Retorna a partícula mais próxima por meio do cálculo da distância Euclidiana no espaço de objetivos.
     *
     * @param parts Lista de partículas não dominadas.
     * @param p     Partícula.
     * @return Partícula mais próxima.
     */
    public static Particula retornarParticulaMaisProxima( List<Particula> parts, Particula p )
    {
        Particula particulaProxima = parts.get( 0 );

        double distancia = DIST_EUCL.compute( particulaProxima.fitness(), p.fitness() );

        for ( int i = 1, size = parts.size(); i < size; i++ )
        {
            double d = DIST_EUCL.compute( parts.get( i ).fitness(), p.fitness() );

            if ( d < distancia )
            {
                particulaProxima = parts.get( i );
                distancia = d;
            }
        }

        return particulaProxima;
    }
}
