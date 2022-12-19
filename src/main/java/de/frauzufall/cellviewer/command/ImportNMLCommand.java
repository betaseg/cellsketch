/*-
 * #%L
 * CSBDeep: CNNs for image restoration of fluorescence microscopy.
 * %%
 * Copyright (C) 2017 - 2018 Deborah Schmidt, Florian Jug, Benjamin Wilhelm
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

package de.frauzufall.cellviewer.command;

import de.frauzufall.cellviewer.analysis.NMLReader;
import net.imglib2.RandomAccessibleInterval;
import org.jdom2.DataConversionException;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

/**
 */
@Plugin(type = Command.class,
	menuPath = "File>Import>Knossos NML (XML)", headless = true)
public class ImportNMLCommand implements Command {

	@Parameter(label = "Knossos NML file (.xml)")
	private File input;

	@Parameter(type = ItemIO.OUTPUT)
	private RandomAccessibleInterval output;

	@Parameter(label = "Scale input by")
	private double scale = 0.25;

	@Parameter(label = "Radius of microtubules (px)")
	private int radius = 1;

	@Parameter(label = "Label microtubules with indices")
	private boolean asIndices = true;

	@Parameter
	private LogService logService;

	@Override
	public void run() {
		if(scale == 0.0) {
			logService.error("NML Importer: scale cannot be zero.");
			return;
		}
		try {
			if(asIndices) output = NMLReader.readIndexed(input, new double[]{scale, scale, scale}, radius);
			else output = NMLReader.read(input, scale, radius);
		} catch (NMLReader.NMLReaderIOException | DataConversionException e) {
			e.printStackTrace();
		}
	}

}
