/*-
 * #%L
 * cellsketch
 * %%
 * Copyright (C) 2020 - 2023 Deborah Schmidt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package de.frauzufall.cellsketch.ui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Dimension;
import java.text.NumberFormat;
import java.util.List;

public class PlotUtil {

	public static void displayHistogram(double[] values, String series, String title, String xLabel, String yLabel, int bins) {
		HistogramDataset dataset = new HistogramDataset();
		dataset.setType(HistogramType.RELATIVE_FREQUENCY);
		dataset.addSeries(series, values, bins);
		JFreeChart histogram = ChartFactory.createHistogram(title,
				xLabel, yLabel, dataset);
		XYPlot plot = (XYPlot) histogram.getPlot();
		plot.setDomainGridlinesVisible(false);
		plot.setRangeGridlinesVisible(false);
		plot.setBackgroundAlpha(0);
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
		NumberFormat percentInstance = NumberFormat.getPercentInstance();
		percentInstance.setMaximumFractionDigits(0);
		rangeAxis.setNumberFormatOverride(percentInstance);
		XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
		renderer.setBarPainter(new StandardXYBarPainter());
		displayInFrame(histogram);
	}

	private static void displayInFrame(JFreeChart chart) {
		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		chartPanel.setBackground(Color.white);
		JFrame frame = new JFrame();
		frame.setPreferredSize(new Dimension(500, 400));
		frame.setContentPane(chartPanel);
		frame.pack();
		frame.setVisible(true);
	}

	public static double[] toDoubleArray(List<Object> column) {
		final double[] array = new double[column.size()];
		for (int i = 0; i < array.length; i++)
			array[i] = Double.parseDouble(column.get(i).toString());
		return array;
	}
}
