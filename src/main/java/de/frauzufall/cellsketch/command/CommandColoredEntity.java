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
import org.kohsuke.args4j.Option;
import org.scijava.Context;
import org.scijava.plugin.Parameter;
import org.scijava.util.ColorRGBA;
import org.scijava.widget.FileWidget;

import java.io.File;
import java.io.IOException;

public abstract class CommandColoredEntity extends CommandWithCmdLineParser {

	@Parameter(label = "Cell project directory (.n5)", style = FileWidget.DIRECTORY_STYLE)
	@Option(name = "--project")
	protected File project;

	@Parameter(required = false)
	protected CellProject projectObject;

	@Parameter(label = "Name of input dataset")
	@Option(name = "--name")
	protected String name;

	@Parameter(label = "Scale factor X for input dataset", stepSize="0.0001")
	@Option(name = "--scale_x")
	protected double scaleX = 1;
//	protected double scaleX = 0.25;

	@Parameter(label = "Scale factor Y for input dataset", stepSize="0.0001")
	@Option(name = "--scale_y")
	protected double scaleY = 1;

	@Parameter(label = "Scale factor Z for input dataset", stepSize="0.0001")
	@Option(name = "--scale_z")
	protected double scaleZ = 1;
//	protected double scaleZ = 0.2125;

	@Parameter(label = "Color", required = false)
	@Option(name = "--color")
	protected ColorRGBA color = new ColorRGBA(200, 200, 200, 200);

	@Parameter
	protected Context context;

	protected CellProject getOrLoadCellProject() {
		CellProject project;
		if(projectObject != null) {
			project = projectObject;
		} else {
			project = new CellProject(this.project, context);
			try {
				project.load();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return project;
	}
}
