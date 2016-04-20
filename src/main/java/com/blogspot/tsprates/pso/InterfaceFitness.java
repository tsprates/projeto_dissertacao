package com.blogspot.tsprates.pso;

/**
 * Interface fitness.
 *
 * @author thiago
 *
 */
interface InterfaceFitness
{

    /**
     * Calcula o fitness, e devolve um array numérico de tamanho 2, que o
     * primeiro elemento é a acurácia da regra e o segundo sua complexidade
     * (número de nós) da cláusula WHERE.
     *
     * @param part Partícula.
     * @return Fitness para MOPSO.
     */
    public double[] calcular(final Particula part);
}
