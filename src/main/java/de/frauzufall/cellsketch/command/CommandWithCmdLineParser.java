package de.frauzufall.cellsketch.command;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.scijava.command.Command;
import org.scijava.util.ColorRGBA;

import static org.kohsuke.args4j.OptionHandlerFilter.ALL;

public abstract class CommandWithCmdLineParser implements Command {

	protected boolean parseArguments(String[] args) {
		CmdLineParser.registerHandler(ColorRGBA.class, ColorOptionHandler.class);
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			System.err.println(e.getMessage());
			parser.printUsage(System.err);
			System.err.println();
			System.err.println("  Example: java " + this.getClass().getName() + " " +parser.printExample(ALL));
			return false;
		}
		return true;
	}
}
