package com.blogspot.tsprates.pso;

import java.util.List;

import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.knowm.xchart.style.XYStyler;

public class Grafico
{

    private XYChart chart;

    public void mostra()
    {
        new SwingWrapper<XYChart>(chart).displayChart();
    }

    public Grafico(String titulo)
    {
        chart = new XYChartBuilder().width(800).height(600).build();

        XYStyler styler = chart.getStyler();
        
        styler.setDefaultSeriesRenderStyle(
                XYSeriesRenderStyle.Scatter);
        styler.setChartTitleVisible(true);
        styler.setLegendPosition(LegendPosition.OutsideE);
        styler.setMarkerSize(16);
        
        chart.setTitle(titulo);
        chart.setXAxisTitle("Complexidade");
        chart.setYAxisTitle("Sensibilidade x Especificidade");

        styler.setXAxisMax(1);
        styler.setXAxisMin(0);
        styler.setYAxisMax(1);
        styler.setYAxisMin(0);
    }

    public Grafico adicionaSerie(String legenda, List<? extends Number> xData,
            List<? extends Number> yData)
    {
        chart.addSeries(legenda, xData, yData);
        return this;
    }

}
