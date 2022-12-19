package sc.fiji.project.command;

import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class)
public class ImportStackSlice implements Command {

	@Parameter(label = "Stack file")
	File stackFile;

	@Parameter(label = "Dimension")
	private int dimension = 0;

	@Parameter(label = "Position")
	long position = 0;

	@Override
	public void run() {
	}

	public static void main(String...args) {
		ImageJ ij = new ImageJ();
		ij.ui().showUI();
		ij.command().run(ImportStackSlice.class, true);
	}
}
