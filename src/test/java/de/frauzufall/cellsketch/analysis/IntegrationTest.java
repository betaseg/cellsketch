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
