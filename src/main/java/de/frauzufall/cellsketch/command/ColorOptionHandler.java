package de.frauzufall.cellsketch.command;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;
import org.scijava.util.ColorRGBA;

public class ColorOptionHandler extends OneArgumentOptionHandler<ColorRGBA> {

	public ColorOptionHandler(CmdLineParser parser, OptionDef option,
                              Setter<? super ColorRGBA> setter) {
		super(parser, option, setter);
	}

	@Override
	protected ColorRGBA parse(String argument) throws NumberFormatException {
		String[] colorParts = argument.split(":");
		return new ColorRGBA(Integer.parseInt(colorParts[0]), Integer.parseInt(colorParts[1]), Integer.parseInt(colorParts[2]), Integer.parseInt(colorParts[3]));
	}

}
