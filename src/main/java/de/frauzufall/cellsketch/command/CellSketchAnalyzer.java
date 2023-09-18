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
import de.frauzufall.cellsketch.analysis.CellAnalyzer;
import net.imagej.ops.OpService;
import org.kohsuke.args4j.Option;
import org.scijava.Context;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Plugin(type = Command.class,
		menuPath = "Analyze>" + CellProject.appName + ">Spatial analysis", headless = true)
public class CellSketchAnalyzer extends CommandWithCmdLineParser {

	@Parameter(label = "Cell project directory (.n5)", style = FileWidget.DIRECTORY_STYLE)
	@Option(name = "--project")
	private File project;

	@Parameter(label = "Max distance between two connected organelles in μm")
	@Option(name = "--connected_threshold_in_um")
	private float connectedThresholdInUM = 0.02f;


	@Parameter(label = "Skip existing distance transform maps.")
	@Option(name = "--skip_existing_distance_maps")
	private boolean skipExistingDistanceMaps = false;

	@Parameter(required = false)
	protected CellProject projectObject = null;

	@Parameter
	private Context context;

	@Parameter
	private OpService ops;

	@Override
	public void run() {
		boolean projectExists = true;
		if(projectObject == null) {
			projectExists = false;
			projectObject = new CellProject(this.project, context);
			try {
				projectObject.loadConfig();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		new CellAnalyzer(projectObject, skipExistingDistanceMaps, ops, connectedThresholdInUM).analyze();
		if(!projectExists) {
			projectObject.dispose();
		}
	}

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		new CellSketchAnalyzer().doMain(args);
	}

	public void doMain(String[] args) throws ExecutionException, InterruptedException {
		if (!parseArguments(args)) return;
		Map<String, Object> command_args = new HashMap<>();
		if(this.project != null) command_args.put("project", this.project);
		command_args.put("skipExistingDistanceMaps", this.skipExistingDistanceMaps);
		command_args.put("connectedThresholdInUM", this.connectedThresholdInUM);
		Context context = new Context();
		context.service(CommandService.class).run(this.getClass(), false, command_args).get();
		context.dispose();
		context.service(StatusService.class).showStatus("Done analyzing Cellsketch project.");
	}
}
