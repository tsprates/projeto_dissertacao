package com.blogspot.tsprates.pso;

import java.util.List;

import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.Styler.LegendPosition;

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

	chart.getStyler().setDefaultSeriesRenderStyle(
		XYSeriesRenderStyle.Scatter);
	chart.getStyler().setChartTitleVisible(true);
	chart.getStyler().setLegendPosition(LegendPosition.OutsideE);
	chart.getStyler().setMarkerSize(16);
	chart.setTitle(titulo);
	chart.setXAxisTitle("Complexidade");
	chart.setYAxisTitle("Sensibilidade x Especificidade");
	
	chart.getStyler().setXAxisMax(1);
	chart.getStyler().setXAxisMin(0);
	chart.getStyler().setYAxisMax(1);
	chart.getStyler().setYAxisMin(0);
    }

    public Grafico adicionaSerie(String legenda, List<? extends Number> xData, List<? extends Number> yData) 
    {
	chart.addSeries(legenda, xData, yData);
	return this;
    }

}
