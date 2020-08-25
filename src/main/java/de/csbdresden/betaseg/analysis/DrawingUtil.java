package de.csbdresden.betaseg.analysis;

import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.region.hypersphere.HyperSphere;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

public class DrawingUtil {

	public static void drawPoint(RandomAccessibleInterval image, float[] point, int radius, int val) {
		long[] lpoint = new long[point.length];
		for (int i = 0; i < point.length; i++) {
			lpoint[i] = (long) point[i];
		}
		drawPoint(image, lpoint, radius, val);
	}

	public static void drawPoint(RandomAccessibleInterval image, long[] point, int radius, int val) {
		RandomAccess<RealType<?>> ra = image.randomAccess();
		ra.setPosition(point);
		HyperSphere<RealType<?>> hyperSphere = new HyperSphere<>(image, ra, radius);
		try {
			for (RealType<?> value : hyperSphere) value.setReal(val);
		}
		catch(ArrayIndexOutOfBoundsException e) {}
	}

	public static void drawPoint(Img image, Point point, int radius, int val) {
		long[] lpoint = new long[point.numDimensions()];
		for (int i = 0; i < lpoint.length; i++) {
			lpoint[i] = (long) point.getLongPosition(i);
		}
		drawPoint(image, lpoint, radius, val);
	}
}
