package sc.fiji.project.command;

import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class)
public class ImportFile implements Command {

	@Parameter(label = "File")
	private File file;

	@Parameter(label = "Copy into project")
	private boolean copy = false;

	@Override
	public void run() {

	}

	public static void main(String...args) {
		ImageJ ij = new ImageJ();
		ij.ui().showUI();
		ij.command().run(ImportFile.class, true);
	}
}
