package com.blogspot.tsprates.pso;

/**
 *
 * @author thiago
 */
interface InterfaceFitness<T> {
    public T calc(Particula p);
}
