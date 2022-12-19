package de.frauzufall.cellviewer;

import sc.fiji.project.TableColumnDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GranulesOverviewTable implements TableColumnDefinition {

	private static final String count = "number of granules";
	private static final String meanSize = "mean size in micrometer^3";
	private static final String stdevSize = "stdev size in micrometer^3";
	private static final String medianSize = "median size in micrometer^3";
	private static final String percentageConnectedToMT = "percentage connected to MT";
	private static final String numberConnectedToMT = "number connected to MT";
	private static final String numberDisconnectedFromMT = "number disconnected from MT";

	private static final List<String> columns = createColumns();

	private static List<String> createColumns() {
		List<String> result = new ArrayList<>();
		result.add(count);
		result.add(meanSize);
		result.add(stdevSize);
		result.add(medianSize);
		result.add(percentageConnectedToMT);
		result.add(numberConnectedToMT);
		result.add(numberDisconnectedFromMT);
		return Collections.unmodifiableList(result);
	}

	public static int getCountColumn() { return columns.indexOf(count); }
	public static int getMeanSizeColumn() { return columns.indexOf(meanSize); }
	public static int getStdevSizeColumn() { return columns.indexOf(stdevSize); }
	public static int getMedianSizeColumn() { return columns.indexOf(medianSize); }
	public static int getPercentageConnectedToMTColumn() { return columns.indexOf(percentageConnectedToMT); }
	public static int getNumberConnectedToMTColumn() { return columns.indexOf(numberConnectedToMT); }
	public static int getNumberDisconnectedFronMTColumn() { return columns.indexOf(numberDisconnectedFromMT); }

	@Override
	public List<String> getColumns() {
		return columns;
	}
}
