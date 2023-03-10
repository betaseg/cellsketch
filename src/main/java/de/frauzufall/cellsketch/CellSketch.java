package de.frauzufall.cellsketch;

import org.scijava.AbstractGateway;
import org.scijava.Context;
import org.scijava.Gateway;
import org.scijava.plugin.Plugin;
import org.scijava.service.SciJavaService;

@Plugin(type = Gateway.class)
public class CellSketch extends AbstractGateway {

	@Override
	public void launch(String... args) {
	}

	public CellSketch() {
		this(new Context());
	}

	public CellSketch(final Context context) {
		super("CellSketch", context);
	}

	@Override
	public String getShortName() {
		return "cellsketch";
	}

	public static void main(final String... args) {
		final CellSketch cellsketch = new CellSketch();
		cellsketch.launch(args);
	}
}
