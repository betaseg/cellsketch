package de.frauzufall.cellsketch.model;

import net.imglib2.type.numeric.real.FloatType;

public interface HasDistanceMap extends Item {

	ImageFileItem<FloatType> getDistanceMap();
	ImageFileItem distanceMapSource();
    Double getConnectedToFilamentsEndThresholdInUM();
}
