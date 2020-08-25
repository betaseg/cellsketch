package de.csbdresden.betaseg;

import sc.fiji.project.TableColumnDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MicrotubulesOverviewTable implements TableColumnDefinition {

	private static final String count = "number of microtubules";

	private static final String totalLength = "total length in micrometer";
	private static final String meanLength = "mean length in micrometer";
	private static final String stdevLength = "stdev length in micrometer";
	private static final String medianLength = "median length in micrometer";
	private static final String meanTortuosity = "mean tortuosity in micrometer";
	private static final String stdevTortuosity = "stdev tortuosity in micrometer";
	private static final String medianTortuosity = "median tortuosity in micrometer";
	private static final String percentageConnectedToCentrioles = "percentage connected to centrioles";
	private static final String numberConnectedToCentrioles = "number connected to centrioles";
	private static final String numberDisconnectedFromCentrioles = "number disconnected from centrioles";
	private static final String percentageConnectedToGolgi = "percentage connected to golgi";
	private static final String numberConnectedToGolgi = "number connected to golgi";
	private static final String numberDisconnectedFromGolgi = "number disconnected from golgi";
	private static final List<String> columns = createColumns();

	private static List<String> createColumns() {
		List<String> result = new ArrayList<>();
		result.add(count);
		result.add(totalLength);
		result.add(meanLength);
		result.add(stdevLength);
		result.add(medianLength);
		result.add(meanTortuosity);
		result.add(stdevTortuosity);
		result.add(medianTortuosity);
		result.add(percentageConnectedToCentrioles);
		result.add(numberConnectedToCentrioles);
		result.add(numberDisconnectedFromCentrioles);
		result.add(percentageConnectedToGolgi);
		result.add(numberConnectedToGolgi);
		result.add(numberDisconnectedFromGolgi);
		return Collections.unmodifiableList(result);
	}

	@Override
	public List<String> getColumns() {
		return columns;
	}

	public static String getCountColumn() {
		return count;
	}

	public static String getTotalLengthColumn() {
		return totalLength;
	}

	public static String getMeanLengthColumn() {
		return meanLength;
	}

	public static String getStdevLengthColumn() {
		return stdevLength;
	}

	public static String getMedianLengthColumn() {
		return medianLength;
	}

	public static String getMeanTortuosityColumn() {
		return meanTortuosity;
	}

	public static String getStdevTortuosityColumn() {
		return stdevTortuosity;
	}

	public static String getMedianTortuosityColumn() {
		return medianTortuosity;
	}

	public static String getPercentageConnectedToCentriolesColumn() {
		return percentageConnectedToCentrioles;
	}

	public static String getNumberConnectedToCentriolesColumn() {
		return numberConnectedToCentrioles;
	}

	public static String getNumberDisconnectedFromCentriolesColumn() {
		return numberDisconnectedFromCentrioles;
	}

	public static String getPercentageConnectedToGolgiColumn() {
		return percentageConnectedToGolgi;
	}

	public static String getNumberConnectedToGolgiColumn() {
		return numberConnectedToGolgi;
	}

	public static String getNumberDisconnectedFromGolgiColumn() {
		return numberDisconnectedFromGolgi;
	}
}
