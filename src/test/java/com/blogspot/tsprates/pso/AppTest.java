package com.blogspot.tsprates.pso;

import java.util.ArrayList;
import java.util.List;
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

//    /**
//     * Rigourous Test :-)
//     */
//    public void testApp()
//    {
//        assertTrue(true);
//    }

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

        assertEquals(1, FronteiraPareto.verificarDominanciaParticulas(pa, pb));
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

        assertEquals(0, FronteiraPareto.verificarDominanciaParticulas(pa, pb));
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

        assertEquals(-1, FronteiraPareto.verificarDominanciaParticulas(pa, pb));
    }

    public void test_particula_nao_domina_e_nem_dominada_por_outra_particula()
    {
        double[] partfit =
        {
            0.4, 0.25, 1
        };

        Particula part = Mockito.mock(Particula.class);
        when(part.fitness()).thenReturn(partfit);
        when(part.clonar()).thenReturn(part);

        double[][] partsfit = new double[][]
        {
            {
                0.2, 0.3, 1
            },
            {
                0.3, 0.4, 1
            },
            {
                0.5, 0.1, 1
            }

        };

        List<Particula> parts = new ArrayList<>();
        for (double[] pf : partsfit)
        {
            Particula temp = Mockito.mock(Particula.class);
            when(temp.fitness()).thenReturn(pf);
            parts.add(temp);
        }

        assertEquals(3, parts.size());

        FronteiraPareto.atualizarParticulas(parts, part);

        assertEquals(4, parts.size());
    }

    public void test_particula_domina_outra_particula()
    {
        double[] partfit =
        {
            0.4, 0.3, 1
        };

        Particula part = Mockito.mock(Particula.class);
        when(part.fitness()).thenReturn(partfit);
        when(part.clonar()).thenReturn(part);

        double[][] partsfit = new double[][]
        {
            {
                0.2, 0.3, 1
            },
            {
                0.3, 0.4, 1
            },
            {
                0.5, 0.1, 1
            }

        };

        List<Particula> parts = new ArrayList<>();
        for (double[] pf : partsfit)
        {
            Particula temp = Mockito.mock(Particula.class);
            when(temp.fitness()).thenReturn(pf);
            parts.add(temp);
        }

        assertEquals(3, parts.size());

        FronteiraPareto.atualizarParticulas(parts, part);

        assertEquals(3, parts.size());
    }

    public void test_particula_e_dominada_por_outra_particula()
    {
        double[] partfit =
        {
            0.1, 0.3, 1
        };

        Particula part = Mockito.mock(Particula.class);
        when(part.fitness()).thenReturn(partfit);
        when(part.clonar()).thenReturn(part);

        double[][] partsfit = new double[][]
        {
            {
                0.2, 0.3, 1
            },
            {
                0.3, 0.4, 1
            },
            {
                0.5, 0.1, 1
            }

        };

        List<Particula> parts = new ArrayList<>();
        for (double[] pf : partsfit)
        {
            Particula temp = Mockito.mock(Particula.class);
            when(temp.fitness()).thenReturn(pf);
            parts.add(temp);
        }

        assertEquals(3, parts.size());

        FronteiraPareto.atualizarParticulas(parts, part);

        assertEquals(3, parts.size());
    }
}
