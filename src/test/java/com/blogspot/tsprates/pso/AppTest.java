package com.blogspot.tsprates.pso;

import java.util.Arrays;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;

/**
 * Unit test for simple App.
 */
public class AppTest
        extends TestCase
{

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest(String testName)
    {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite(AppTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue(true);
    }

    public void test_particula_A_domina_particula_B()
    {
        double[] afit =
        {
            0.4, 0.4, 1
        };

        double[] bfit =
        {
            0.3, 0.4, 1
        };

        Particula pa = Mockito.mock(Particula.class);
        when(pa.fitness()).thenReturn(afit);

        Particula pb = Mockito.mock(Particula.class);
        when(pb.fitness()).thenReturn(bfit);

        assertTrue(FronteiraPareto.verificarDominanciaParticulas(pa, pb) == 1);
    }

    public void test_particula_A_nao_domina_particula_B()
    {
        double[] afit =
        {
            0.4, 0.4, 1
        };

        double[] bfit =
        {
            0.3, 0.5, 1
        };

        Particula pa = Mockito.mock(Particula.class);
        when(pa.fitness()).thenReturn(afit);

        Particula pb = Mockito.mock(Particula.class);
        when(pb.fitness()).thenReturn(bfit);

        assertTrue(FronteiraPareto.verificarDominanciaParticulas(pa, pb) == 0);
    }

    public void test_particula_B_domina_particula_A()
    {
        double[] afit =
        {
            0.4, 0.4, 1
        };

        double[] bfit =
        {
            0.5, 0.5, 1
        };

        Particula pa = Mockito.mock(Particula.class);
        when(pa.fitness()).thenReturn(afit);

        Particula pb = Mockito.mock(Particula.class);
        when(pb.fitness()).thenReturn(bfit);

        assertTrue(FronteiraPareto.verificarDominanciaParticulas(pa, pb) == -1);
    }
}
