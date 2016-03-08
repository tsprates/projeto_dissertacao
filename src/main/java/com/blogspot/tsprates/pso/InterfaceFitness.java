package com.blogspot.tsprates.pso;

/**
 * Interface fitness.
 * 
 * @author thiago
 *
 */
interface FitnessInterface {

    /**
     * Calcula o fitness, e devolve um array numérico de tamanho 2, que o
     * primeiro elemento é a acurácia da regra e o segundo sua complexidade
     * (número de nós) da cláusula WHERE.
     * 
     * @param p
     *            Partícula.
     * @return Fitness para MOPSO.
     */
    public double[] calcula(Particula p);
}
