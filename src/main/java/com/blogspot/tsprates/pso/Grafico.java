package com.blogspot.tsprates.pso;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Gráfico.
 *
 * @author thiago
 */
public class Grafico extends ApplicationFrame
{

    private static final long serialVersionUID = 1L;

    private final String titulo, eixoX, eixoY;

    private final XYSeriesCollection dataset = new XYSeriesCollection();

    private int numSeries = 0;

    /**
     * Construtor.
     *
     * @param titulo
     * @param tituloX
     * @param tituloY
     */
    public Grafico(String titulo, String tituloX, String tituloY)
    {
        super(titulo);
        this.titulo = titulo;
        this.eixoX = tituloX;
        this.eixoY = tituloY;
    }

    /**
     * Adiciona série ao gráfico.
     *
     * @param legenda
     * @param xData
     * @param yData
     * @return
     */
    public Grafico adicionarSerie(String legenda, List<? extends Number> xData,
            List<? extends Number> yData)
    {
        final XYSeries serie = new XYSeries(legenda);

        for (int i = 0, len = xData.size(); i < len; i++)
        {
            serie.add(xData.get(i), yData.get(i));
        }

        dataset.addSeries(serie);
        return this;
    }

    /**
     * Adiciona série ao gráfico.
     *
     * @param legenda
     * @param yData
     * @return
     */
    public Grafico adicionarSerie(String legenda, List<? extends Number> yData)
    {
        final XYSeries serie = new XYSeries(legenda);

        for (int i = 0, len = yData.size(); i < len; i++)
        {
            serie.add((double) i + 1, yData.get(i).doubleValue());
        }

        dataset.addSeries(serie);
        numSeries++;

        return this;
    }

    /**
     * Mostra gráfico resultante.
     */
    public void mostrar()
    {
        JFreeChart chart = ChartFactory.createXYLineChart(titulo, eixoX, eixoY,
                dataset, PlotOrientation.VERTICAL, true, true, false);

        XYPlot plot = chart.getXYPlot();

        setEixoY(plot);
        setLineRenderer(plot);

        chart.setAntiAlias(true);
        criarGrafico(chart);
    }

    /**
     * Formata eixo X.
     *
     * @param plot
     */
    private void setEixoY(XYPlot plot)
    {
        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        range.setRange(0.0, 1.0);
        range.setTickUnit(new NumberTickUnit(0.05));

        DecimalFormat fmt = new DecimalFormat("0.00");
        range.setNumberFormatOverride(fmt);
    }

    /**
     * Formata gráfico.
     *
     * @param plot
     */
    private void setLineRenderer(XYPlot plot)
    {
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        for (int i = 0; i < numSeries; i++)
        {
            renderer.setSeriesLinesVisible(i, true);
            renderer.setSeriesShapesVisible(i, true);
        }

        plot.setRenderer(renderer);
    }

    /**
     * Criar gráfico.
     *
     * @param chart
     */
    private void criarGrafico(JFreeChart chart)
    {
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 600));
        setContentPane(chartPanel);
        pack();
        RefineryUtilities.centerFrameOnScreen(this);
        setVisible(true);
    }
}
