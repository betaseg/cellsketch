package de.csbdresden.betaseg.analysis;

import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.ByteType;

import java.io.IOException;

public class WaterBinarizer {

	public static void main(String...args) throws IOException {
		run();
	}

	private static <T extends IntegerType<T>> void run() throws IOException {
		ImageJ ij = new ImageJ();
		String inPath = "/home/random/Development/imagej/project/3DAnalysisFIBSegmentation/owncloud/data_for_paper/high_c2/export/golgi_indices.tif";
		String outPath = "/home/random/Development/imagej/project/3DAnalysisFIBSegmentation/owncloud/data_for_paper/high_c2/export/golgi_mask_separated.tif";
		RandomAccessibleInterval<T> in = (RandomAccessibleInterval<T>) ij.io().open(inPath);
		IntTypeBoundary<T> boundary = new IntTypeBoundary<>(in);
		RandomAccessibleInterval<ByteType> mask = new CellImgFactory<>(new ByteType()).create(in);
		LoopBuilder.setImages(in, boundary, mask).multiThreaded().forEachPixel((inPix, boundPix, maskPix) -> {
			if(inPix.getInteger() > 0 && boundPix.getInteger() == 0) {
				maskPix.setOne();
			} else {
				maskPix.setZero();
			}
		});
		ij.scifio().datasetIO().save(ij.dataset().create(mask), outPath);
	}

}
