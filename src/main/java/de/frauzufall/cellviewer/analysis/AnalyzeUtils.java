package de.frauzufall.cellviewer.analysis;

import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.scijava.table.Table;
import sc.fiji.project.ImageFileItem;
import sc.fiji.project.MaskFileItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnalyzeUtils {

	public static void calculateDistanceTransform(OpService ops, MaskFileItem input, ImageFileItem<FloatType> output) throws IOException {
		calculateDistanceTransform(ops, input.getImage(), output);
	}

	static <T extends IntegerType<T>> void calculateDistanceTransform(OpService ops, RandomAccessibleInterval<T> input, ImageFileItem<FloatType> output) throws IOException {
		Img<BitType> img = ops.create().img(input, new BitType());
		LoopBuilder.setImages(input, img).multiThreaded().forEachPixel((in, out) -> out.set(in.getInteger() == 0));
		RandomAccessibleInterval<FloatType> distance = ops.image().distancetransform(img);
		FloatType max = ops.stats().max(Views.iterable(distance));
		output.setImage(distance);
		output.setMaxValue(max.getMaxValue());
		output.save();
	}

	static List<String> getTableData(Table table, int requiredDataColumn, int conditionColumn, String conditionValue) {
		java.util.List<String> res = new ArrayList<>();
		for (int i = 0; i < table.getRowCount(); i++) {
			if(String.valueOf(table.get(conditionColumn, i)).equals(conditionValue)) {
				res.add(String.valueOf(table.get(requiredDataColumn, i)));
			}
		}
		return res;
	}

}
