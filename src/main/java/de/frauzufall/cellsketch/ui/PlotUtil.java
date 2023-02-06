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
