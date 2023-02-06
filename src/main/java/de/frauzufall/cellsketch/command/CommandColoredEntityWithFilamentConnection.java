package de.frauzufall.cellsketch.command;

import org.kohsuke.args4j.Option;
import org.scijava.plugin.Parameter;

public abstract class CommandColoredEntityWithFilamentConnection extends CommandColoredEntity {

	@Parameter(label = "Analyze connection to filaments ends", required = false)
	@Option(name = "--analyzeConnectionFilamentEnds")
	protected boolean analyzeConnectionFilamentEnds = false;

	@Parameter(label = "Threshold to count filament ends as connected in um", required = false, stepSize = "0.0001")
	@Option(name = "--thresholdConnectionFilamentEnds")
	protected double thresholdConnectionFilamentEnds;

}
