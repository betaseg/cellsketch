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

import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.math.ImgMath;
import net.imglib2.algorithm.math.Max;
import net.imglib2.img.Img;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.scijava.app.StatusService;
import org.scijava.table.Table;
import de.frauzufall.cellsketch.model.ImageFileItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnalyzeUtils {

	public static void calculateDistanceTransform(OpService ops, ImageFileItem input, ImageFileItem<FloatType> output, boolean recalculateDistanceMaps) throws IOException {
		if(output.exists() && !recalculateDistanceMaps) {
			ops.context().service(StatusService.class).showStatus("Not recalculating distance transform map of " + input.getName());
			return;
		}
		calculateDistanceTransform(ops, input.getImage(), output, false);
	}

	public static void calculateDistanceTransformInner(OpService ops, ImageFileItem input, ImageFileItem<FloatType> output, boolean recalculateDistanceMaps) throws IOException {
		if(output.exists() && !recalculateDistanceMaps) {
			ops.context().service(StatusService.class).showStatus("Not recalculating distance transform map of " + input.getName());
			return;
		}
		calculateDistanceTransform(ops, input.getImage(), output, true);
	}

	public static <T extends IntegerType<T>> void calculateDistanceTransform(OpService ops, RandomAccessibleInterval<T> input, ImageFileItem<FloatType> output, boolean inverted) throws IOException {
//		Img<FloatType> img = ops.create().img(input, new FloatType());
//		LoopBuilder.setImages(input, img).multiThreaded().forEachPixel((in, out) -> out.set(in.getInteger() == 0? Float.MAX_VALUE : 0));
		Img<BitType> img = ops.create().img(input, new BitType());
		if(inverted) {
			LoopBuilder.setImages(input, img).multiThreaded().forEachPixel((in, out) -> out.set(in.getInteger() != 0));
		} else {
			LoopBuilder.setImages(input, img).multiThreaded().forEachPixel((in, out) -> out.set(in.getInteger() == 0));
		}
//		DistanceTransform.transform(img, new EuclidianDistanceIsotropic(1));
		RandomAccessibleInterval<FloatType> distance = ops.image().distancetransform(img);
		output.setImage(distance);
		output.setMaxValue(max(Views.iterable(distance)));
		output.save();
		output.saveConfig();
	}

	private static <T extends FloatType> double max(IterableInterval<T> img) {
		final Cursor<T> cursor = img.cursor();
		float max = 0;
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			final T t = cursor.get();
			max = Math.max( t.get(), max );
		}
		return max;
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
