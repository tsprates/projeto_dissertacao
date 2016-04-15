package com.blogspot.tsprates.pso;

import java.awt.Dimension;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class Grafico extends ApplicationFrame
{

    private static final long serialVersionUID = 1L;

    private final String titulo, eixoX, eixoY;

    private final XYSeriesCollection dataset = new XYSeriesCollection();

    public Grafico(String titulo, String tituloX, String tituloY)
    {
        super(titulo);
        this.titulo = titulo;
        this.eixoX = tituloX;
        this.eixoY = tituloY;
    }

    public Grafico adicionaSerie(String legenda, List<? extends Number> xData,
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

    public Grafico adicionaSerie(String legenda, List<? extends Number> yData)
    {
        final XYSeries serie = new XYSeries(legenda);

        for (int i = 0, len = yData.size(); i < len; i++)
        {
            serie.add((double) i, yData.get(i).doubleValue());
        }

        dataset.addSeries(serie);

        return this;
    }

    public void mostra()
    {
        JFreeChart chart = ChartFactory.createXYLineChart(titulo,
                eixoX, eixoY, dataset,
                PlotOrientation.VERTICAL, true, true, false);
//        XYPlot plot = setBackground(chart);
        createChart(chart);
    }

//    private XYPlot setBackground(JFreeChart chart)
//    {
//        XYPlot plot = chart.getXYPlot();
//        plot.setBackgroundPaint(Color.WHITE);
//        plot.setDomainGridlinePaint(Color.GRAY);
//        plot.setRangeGridlinePaint(Color.GRAY);
//        return plot;
//    }
    
    private void createChart(JFreeChart chart)
    {
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 600));
        setContentPane(chartPanel);
        pack();
        RefineryUtilities.centerFrameOnScreen(this);
        setVisible(true);
    }
}
