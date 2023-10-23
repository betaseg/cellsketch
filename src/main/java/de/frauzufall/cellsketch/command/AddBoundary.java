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
import net.imglib2.type.numeric.ARGBType;
import org.kohsuke.args4j.Option;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.ColorRGBA;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Plugin(type = Command.class,
		menuPath = "Analyze>" + CellProject.appName + ">Add boundary", headless = true)
public class AddBoundary extends CommandColoredEntityWithFilamentConnection {

	@Parameter(label = "Boundary mask file")
	@Option(name = "--input")
	private File input;

	@Override
	public void run() {
		CellProject project = getOrLoadCellProject();
		String progressName = "Adding boundary from " + input;
		try {
			Double threshold = null;
			project.startProgress(progressName);
			if(analyzeConnectionFilamentEnds) threshold = thresholdConnectionFilamentEnds;
			project.setBoundary(input, name, ARGBType.rgba(color.getRed(), color.getGreen(), color.getBlue(), 10), threshold, scaleX, scaleY, scaleZ);
			project.configChanged();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			project.endProgress(progressName);
		}
	}

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		new AddBoundary().doMain(args);
	}

	public void doMain(String[] args) throws ExecutionException, InterruptedException {
		if (!parseArguments(args)) return;
		Map<String, Object> command_args = new HashMap<>();
		if(this.project != null) command_args.put("project", this.project);
		command_args.put("input", this.input);
		command_args.put("name", this.name);
		command_args.put("color", this.color);
		command_args.put("scaleX", this.scaleX);
		command_args.put("scaleY", this.scaleY);
		command_args.put("scaleZ", this.scaleZ);
		command_args.put("analyzeConnectionFilamentEnds", this.analyzeConnectionFilamentEnds);
		command_args.put("thresholdConnectionFilamentEnds", this.thresholdConnectionFilamentEnds);
		Context context = new Context();
		context.service(CommandService.class).run(this.getClass(), false, command_args).get();
		context.dispose();
		System.out.println("Done.");
	}

}
