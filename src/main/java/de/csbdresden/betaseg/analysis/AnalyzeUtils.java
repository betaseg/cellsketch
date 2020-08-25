package de.csbdresden.betaseg.analysis;

import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.table.Table;
import sc.fiji.project.ImageFileItem;
import sc.fiji.project.MaskFileItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnalyzeUtils {

	public static <T extends IntegerType<T>> RandomAccessibleInterval<ByteType> asMask(RandomAccessibleInterval<T> image) {
		return Converters.convert(image, (in, out) -> {
			if(in.getInteger() != 0) out.setOne();
			else out.setZero();
		}, new ByteType());
	}

	static void calculateDistanceTransform(OpService ops, MaskFileItem input, ImageFileItem<FloatType> output) {
		calculateDistanceTransform(ops, input.getImage(), output);
	}

	static <T extends IntegerType<T>> void calculateDistanceTransform(OpService ops, RandomAccessibleInterval<T> input, ImageFileItem<FloatType> output) {
		Img<BitType> img = ops.create().img(input, new BitType());
		LoopBuilder.setImages(input, img).multiThreaded().forEachPixel((in, out) -> out.set(in.getInteger() == 0));
		RandomAccessibleInterval<FloatType> distance = ops.image().distancetransform(img);
		output.setImage(distance);
		try {
			output.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
