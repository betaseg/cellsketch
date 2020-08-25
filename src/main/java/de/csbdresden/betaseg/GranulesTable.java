package de.csbdresden.betaseg;

import sc.fiji.project.TableColumnDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GranulesTable implements TableColumnDefinition {

	private static final String size = "size in micrometer^3";
	private static final String closestToMicrotubuleX = "closest to microtubule x";
	private static final String closestToMicrotubuleY = "closest to microtubule y";
	private static final String closestToMicrotubuleZ = "closest to microtubule z";
	private static final String distanceToMicrotubule = "distance to microtubule in micrometer";
	private static final String connectedToMicrotubule = "connected to microtubule";
//	private static final String connectedMicrotubuleId = "microtubule id";
	private static final String closestToMembraneX = "closest to membrane x";
	private static final String closestToMembraneY = "closest to membrane y";
	private static final String closestToMembraneZ = "closest to membrane z";
	private static final String distanceToMembrane = "distance to membrane in micrometer";
	private static final String closestToNucleusX = "closest to nucleus x";
	private static final String closestToNucleusY = "closest to nucleus y";
	private static final String closestToNucleusZ = "closest to nucleus z";
	private static final String distanceToNucleus = "distance to nucleus in micrometer";

	private static final List<String> columns = createColumns();

	private static List<String> createColumns() {
		List<String> result = new ArrayList<>();
		result.add(size);
		result.add(closestToMicrotubuleX);
		result.add(closestToMicrotubuleY);
		result.add(closestToMicrotubuleZ);
		result.add(distanceToMicrotubule);
		result.add(connectedToMicrotubule);
//		result.add(connectedMicrotubuleId);
		result.add(closestToMembraneX);
		result.add(closestToMembraneY);
		result.add(closestToMembraneZ);
		result.add(distanceToMembrane);
		result.add(closestToNucleusX);
		result.add(closestToNucleusY);
		result.add(closestToNucleusZ);
		result.add(distanceToNucleus);
		return Collections.unmodifiableList(result);
	}

	public static int getSizeColumn() { return columns.indexOf(size); }
	public static int getClosestPointToMicrotubuleXColumn() { return columns.indexOf(closestToMicrotubuleX); }
	public static int getClosestPointToMicrotubuleYColumn() { return columns.indexOf(closestToMicrotubuleY); }
	public static int getClosestPointToMicrotubuleZColumn() { return columns.indexOf(closestToMicrotubuleZ); }
	public static int getDistanceToMicrotubuleColumn() { return columns.indexOf(distanceToMicrotubule); }
	public static int getConnectedToMicrotubuleColumn() { return columns.indexOf(connectedToMicrotubule); }
//	public static int getConnectedMicrotubuleIdColumn() { return columns.indexOf(connectedMicrotubuleId); }
	public static int getClosestPointToMembraneXColumn() { return columns.indexOf(closestToMembraneX); }
	public static int getClosestPointToMembraneYColumn() { return columns.indexOf(closestToMembraneY); }
	public static int getClosestPointToMembraneZColumn() { return columns.indexOf(closestToMembraneZ); }
	public static int getDistanceToMembraneColumn() { return columns.indexOf(distanceToMembrane); }
	public static int getClosestPointToNucleusXColumn() { return columns.indexOf(closestToNucleusX); }
	public static int getClosestPointToNucleusYColumn() { return columns.indexOf(closestToNucleusY); }
	public static int getClosestPointToNucleusZColumn() { return columns.indexOf(closestToNucleusZ); }
	public static int getDistanceToNucleusColumn() { return columns.indexOf(distanceToNucleus); }

	@Override
	public List<String> getColumns() {
		return columns;
	}
}
