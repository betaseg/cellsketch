/*-
 * #%L
 * cellsketch
 * %%
 * Copyright (C) 2020 - 2023 Deborah Schmidt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package de.frauzufall.cellsketch.command;

import de.frauzufall.cellsketch.CellProject;
import de.frauzufall.cellsketch.CellSketch;
import net.imglib2.type.numeric.ARGBType;
import org.kohsuke.args4j.Option;
import org.scijava.Context;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.FileWidget;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Plugin(type = Command.class,
		menuPath = "Analyze>" + CellProject.appName + ">New project", headless = true)
public class CellSketchCreator extends CommandWithCmdLineParser {

	@Parameter(label = "Project parent directory", style = FileWidget.DIRECTORY_STYLE)
	@Option(name = "--parent")
	private File parent;

	@Parameter(label = "Project name")
	@Option(name = "--name")
	private String projectName;

	@Parameter(label = "Source dataset")
	@Option(name = "--input")
	private File input;

	@Parameter(label = "Headless")
	@Option(name = "--headless")
	private boolean headless = false;

	@Parameter(label = "Scale factor X for input dataset", stepSize="0.0001")
	@Option(name = "--scale_x")
	protected double scaleX = 1;

	@Parameter(label = "Scale factor Y for input dataset", stepSize="0.0001")
	@Option(name = "--scale_y")
	protected double scaleY = 1;

	@Parameter(label = "Scale factor Z for input dataset", stepSize="0.0001")
	@Option(name = "--scale_z")
	protected double scaleZ = 1;

	@Parameter(label = "Pixel of scaled dataset to μm factor")
	@Option(name = "--pixel_to_um")
	private double pixelToUM = 0.004 *4;

	@Parameter
	private Context context;

	@Parameter
	private UIService ui;

	@Override
	public void run() {
		context.service(StatusService.class).showStatus("Creating new project in " + parent + File.separator + projectName + "..");
		CellProject project = new CellProject(parent, projectName, context);
		project.setEditable(true);
		project.create(input, pixelToUM, scaleX, scaleY, scaleZ);
		try {
			project.getSourceItem().loadConfig();
			project.getSourceItem().setColor(ARGBType.rgba(130, 130, 130, 255));
			project.saveConfig();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(!ui.isHeadless()) {
			project.setEditable(false);
			project.run();
		}
	}

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		new CellSketchCreator().doMain(args);
	}

	public void doMain(String[] args) throws ExecutionException, InterruptedException {
		if (!parseArguments(args)) return;
		Map<String, Object> command_args = new HashMap<>();
		command_args.put("projectName", this.projectName);
		command_args.put("parent", this.parent);
		command_args.put("input", this.input);
		command_args.put("pixelToUM", this.pixelToUM);
		command_args.put("scaleX", this.scaleX);
		command_args.put("scaleY", this.scaleY);
		command_args.put("scaleZ", this.scaleZ);
		command_args.put("headless", this.headless);
		CellSketch cellSketch = new CellSketch();
		if(!headless) {
			cellSketch.ui().showUI();
		}
		cellSketch.ui().setHeadless(headless);
		cellSketch.command().run(this.getClass(), true, command_args).get();
		if(headless) {
			cellSketch.dispose();
		}
	}
}
