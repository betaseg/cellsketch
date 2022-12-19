package de.frauzufall.cellviewer;


import sc.fiji.project.TableColumnDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MicrotubulesTable implements TableColumnDefinition {

	private static final String length = "length in micrometer";
	private static final String tortuosity = "tortuosity";
	private static final String distanceEnd1Membrane = "distance of MT end 1 to membrane in um";
	private static final String distanceEnd2Membrane = "distance of MT end 2 to membrane in um";
	private static final String distanceEnd1Centrioles = "distance of MT end 1 to centrioles in um";
	private static final String distanceEnd2Centrioles = "distance of MT end 2 to centrioles in um";
	private static final String connectedToCentrioles = "connected to centrioles";
	private static final String distanceEnd1Golgi = "distance of MT end 1 to golgi in um";
	private static final String distanceEnd2Golgi = "distance of MT end 2 to golgi in um";
	private static final String connectedToGolgi = "connected to golgi";
	private static final String distanceEnd1Nucleus = "distance of MT end 1 to nucleus in um";
	private static final String distanceEnd2Nucleus = "distance of MT end 2 to nucleus in um";
	private static final List<String> columns = createColumns();

	private static List<String> createColumns() {
		List<String> result = new ArrayList<>();
		result.add(length);
		result.add(tortuosity);
		result.add(distanceEnd1Membrane);
		result.add(distanceEnd2Membrane);
		result.add(distanceEnd1Centrioles);
		result.add(distanceEnd2Centrioles);
		result.add(connectedToCentrioles);
		result.add(distanceEnd1Golgi);
		result.add(distanceEnd2Golgi);
		result.add(connectedToGolgi);
		result.add(distanceEnd1Nucleus);
		result.add(distanceEnd2Nucleus);
		return Collections.unmodifiableList(result);
	}

	public static int getLengthColumn() { return columns.indexOf(length); }
	public static int getTortuosityColumn() { return columns.indexOf(tortuosity); }
	public static int getDistanceEnd1MembraneColumn() { return columns.indexOf(distanceEnd1Membrane); }
	public static int getDistanceEnd2MembraneColumn() { return columns.indexOf(distanceEnd2Membrane); }
	public static int getDistanceEnd1CentriolesColumn() { return columns.indexOf(distanceEnd1Centrioles); }
	public static int getDistanceEnd2CentriolesColumn() { return columns.indexOf(distanceEnd2Centrioles); }
	public static int getConnectedToCentriolesColumn() { return columns.indexOf(connectedToCentrioles); }
	public static int getDistanceEnd1GolgiColumn() { return columns.indexOf(distanceEnd1Golgi); }
	public static int getDistanceEnd2GolgiColumn() { return columns.indexOf(distanceEnd2Golgi); }
	public static int getConnectedToGolgiColumn() { return columns.indexOf(connectedToGolgi); }
	public static int getDistanceEnd1NucleusColumn() { return columns.indexOf(distanceEnd1Nucleus); }
	public static int getDistanceEnd2NucleusColumn() { return columns.indexOf(distanceEnd2Nucleus); }

	@Override
	public List<String> getColumns() {
		return columns;
	}
}
