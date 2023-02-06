package de.frauzufall.cellsketch.model;

public class LabelMapTable {

	private static final String size = "size in um^3";
	private static final String connectedToStr = "connected to ";
	private static final String distanceToStrStart = "distance to ";
	private static final String distanceToStrEnd = " in um";

	public static String getSizeColumnName() {
		return size;
	}
	public static String getConnectedToColumnName(String other) { return connectedToStr + other; }
	public static String getDistanceToColumnName(String other) { return distanceToStrStart + other + distanceToStrEnd; }
}
