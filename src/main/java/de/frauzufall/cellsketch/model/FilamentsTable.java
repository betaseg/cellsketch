package de.frauzufall.cellsketch.model;


public class FilamentsTable {

	public static String getLengthColumnName() { return "length in micrometer"; }
	public static String getTortuosityColumnName() { return "tortuosity"; }
	public static String getDistanceEnd1ToColumnName(String other) { return "distance of MT end 1 to " + other + " in um"; }
	public static String getDistanceEnd2ToColumnName(String other) { return "distance of MT end 2 to " + other + " in um"; }
	public static String getConnectedToColumnName(String other) { return "connected to " + other; }
}
