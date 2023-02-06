package de.frauzufall.cellsketch.model;

public class FilamentsOverviewTable {

	public static String getCountColumnName() {
		return "number of filaments";
	}

	public static String getTotalLengthColumnName() {
		return "total length in micrometer";
	}

	public static String getMeanLengthColumnName() {
		return "mean length in micrometer";
	}

	public static String getStdevLengthColumnName() {
		return "stdev length in micrometer";
	}

	public static String getMedianLengthColumnName() {
		return "median length in micrometer";
	}

	public static String getMeanTortuosityColumnName() {
		return "mean tortuosity in micrometer";
	}

	public static String getStdevTortuosityColumnName() {
		return "stdev tortuosity in micrometer";
	}

	public static String getMedianTortuosityColumnName() {
		return "median tortuosity in micrometer";
	}

	public static String getPercentageConnectedToColumnName(String other) {
		return "percentage connected to " + other;
	}

	public static String getNumberConnectedToColumnName(String other) {
		return "number connected to " + other;
	}

	public static String getNumberDisconnectedFromColumn(String other) {
		return "number disconnected from " + other;
	}

}
