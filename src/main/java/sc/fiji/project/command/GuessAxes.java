package sc.fiji.project.command;

import net.imagej.ImageJ;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class)
public class GuessAxes implements Command {

	@Parameter(label = "Axes (use concatenation of X, Y, Z, T, C, e.g. XYZCT)")
	private String axes = "XYZ";

	@Parameter(type = ItemIO.INPUT, visibility = ItemVisibility.MESSAGE)
	private String dimensions = "dimensions";

	@Override
	public void run() {}

	public static void main(String...args) {
		ImageJ ij = new ImageJ();
		ij.ui().showUI();
		ij.command().run(GuessAxes.class, true);
	}
}
