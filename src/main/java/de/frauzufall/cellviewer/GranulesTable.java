package de.frauzufall.cellviewer;

import sc.fiji.project.TableColumnDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GranulesTable implements TableColumnDefinition {

	private static final String size = "size in um^3";
	private static final String distanceToMicrotubule = "distance to microtubule in um";
	private static final String connectedToMicrotubule = "connected to microtubule";
	private static final String distanceToMembrane = "distance to membrane in um";
	private static final String distanceToNucleus = "distance to nucleus in um";
	private static final String distanceToGolgi = "distance to golgi in um";

	private static final List<String> columns = createColumns();

	private static List<String> createColumns() {
		List<String> result = new ArrayList<>();
		result.add(size);
		result.add(distanceToMicrotubule);
		result.add(connectedToMicrotubule);
		result.add(distanceToMembrane);
		result.add(distanceToNucleus);
		result.add(distanceToGolgi);
		return Collections.unmodifiableList(result);
	}

	public static int getSizeColumn() { return columns.indexOf(size); }
	public static int getDistanceToMicrotubuleColumn() { return columns.indexOf(distanceToMicrotubule); }
	public static int getConnectedToMicrotubuleColumn() { return columns.indexOf(connectedToMicrotubule); }
	public static int getDistanceToMembraneColumn() { return columns.indexOf(distanceToMembrane); }
	public static int getDistanceToNucleusColumn() { return columns.indexOf(distanceToNucleus); }
	public static int getDistanceToGolgiColumn() { return columns.indexOf(distanceToGolgi); }

	@Override
	public List<String> getColumns() {
		return columns;
	}
}
