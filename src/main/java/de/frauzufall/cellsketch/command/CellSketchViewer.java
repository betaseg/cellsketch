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
import org.kohsuke.args4j.Option;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Plugin(type = Command.class,
		menuPath = "Analyze>" + CellProject.appName + ">Display project", headless = true)
public class CellSketchViewer extends CommandWithCmdLineParser {

	@Parameter(label = "Cell project directory (.n5)", style = FileWidget.DIRECTORY_STYLE)
	@Option(name = "--project")
	private File projectDir;

	@Parameter
	private Context context;

	@Override
	public void run() {
		CellProject project = new CellProject(projectDir, context);
		try {
			project.load();
			project.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		new CellSketchViewer().doMain(args);
	}

	public void doMain(String[] args) throws ExecutionException, InterruptedException {
		if (!parseArguments(args)) return;
		Map<String, Object> command_args = new HashMap<>();
		if(this.projectDir != null) command_args.put("projectDir", this.projectDir);
		CellSketch cellSketch = new CellSketch();
		cellSketch.ui().showUI();
		cellSketch.command().run(this.getClass(), true, command_args).get();
	}
}
