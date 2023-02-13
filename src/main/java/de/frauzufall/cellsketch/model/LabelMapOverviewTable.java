package de.frauzufall.cellsketch.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LabelMapOverviewTable {

	private static final String count = "count";
	private static final String meanSize = "mean size in micrometer^3";
	private static final String stdevSize = "stdev size in micrometer^3";
	private static final String medianSize = "median size in micrometer^3";
	private static final String percentageConnectedTo = "percentage connected to ";
	private static final String numberConnectedTo = "number connected to ";
	private static final String numberDisconnectedFrom = "number disconnected from ";

	public static String getCountColumnName() { return count; }
	public static String getMeanSizeColumnName() { return meanSize; }
	public static String getStdevSizeColumnName() { return stdevSize; }
	public static String getMedianSizeColumnName() { return medianSize; }
	public static String getPercentageConnectedToColumnName(String other) { return percentageConnectedTo + other; }
	public static String getNumberConnectedToColumnName(String other) { return numberConnectedTo + other; }
	public static String getNumberDisconnectedFronColumnName(String other) { return numberDisconnectedFrom + other; }

}
