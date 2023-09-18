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
package de.frauzufall.cellsketch.analysis;

import de.frauzufall.cellsketch.CellProject;
import org.json.JSONException;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.ui.UIService;
import de.frauzufall.cellsketch.model.LabelMapItemGroup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IntegrationTest {

	@Test
	public void testPipeline() throws IOException, JSONException {
		double scaleX = 1;
		double scaleY = 1;
		double scaleZ = 5;

		Context context = new Context();
		context.service(UIService.class).showUI();
		File projectDir = Files.createTempDirectory("project").toFile();
		CellProject project = new CellProject(projectDir, "myproject", context);
		String input = getClass().getResource("/example-project/sections-180-220.tif").getFile();
		String mitochondria = getClass().getResource("/example-project/mitochondria.tif").getFile();
		String memNeurGlia = getClass().getResource("/example-project/membranes-neurites-glia.tif").getFile();
		project.create(new File(input), 0.02, scaleX, scaleY, scaleZ);
		LabelMapItemGroup maskItem = project.addLabelMapItem(new File(mitochondria), "mito", 0, 0.02, scaleX, scaleY, scaleZ);
		LabelMapItemGroup labelmapitem = project.addLabelMapItem(new File(memNeurGlia), "memNeurGlia", 0, 0.02, scaleX, scaleY, scaleZ);
		assertEquals(4, labelmapitem.getItems().size());
		assertEquals(3, project.getItems().size());
		project.saveConfig();
		project.unload();
//		project.run();

//
//		new CellAnalyzer(project, false, context.service(OpService.class), 0.02).analyze();
//		assertEquals(3, labelmapitem.getLabelMap().getTagItems().size());
//		assertEquals("size in um^3", ((LabelTagItem) labelmapitem.getLabelMap().getTagItems().get(0)).getName());
//		double minDistance = context.service(OpService.class).stats().min(Views.iterable(labelmapitem.getDistanceMap().getImage())).getRealDouble();
//		double maxDistance = context.service(OpService.class).stats().max(Views.iterable(labelmapitem.getDistanceMap().getImage())).getRealDouble();
//		assertTrue(maxDistance != minDistance);
//		assertTrue(context.service(OpService.class).stats().max(Views.iterable(maskItem.getDistanceMap().getImage())).getRealDouble() > 1);
////		context.service(UIService.class).show(Views.iterable(maskItem.getLabelMap().getImage()));
////		context.service(UIService.class).show(Views.iterable(maskItem.getDistanceMap().getImage()));
//		assertEquals("size in um^3", ((LabelTagItem) labelmapitem.getLabelMap().getTagItems().get(0)).getName());
//		System.out.println(labelmapitem.getItems());
//		assertEquals(7, labelmapitem.getItems().size());
//		System.out.println(project.getItems());
//
//		new CellAnalyzer(project, true, context.service(OpService.class), 0.02).analyze();
//		System.out.println(labelmapitem.getItems());
//		assertEquals(7, labelmapitem.getItems().size());
//		System.out.println(project.getItems());
	}
}
