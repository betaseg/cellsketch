package de.csbdresden.betaseg.analysis;

import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import java.io.IOException;

public class Watershedder {

	public static void main(String...args) throws IOException {
		run();
	}

	private static <T extends RealType<T>> void run() throws IOException {
		ImageJ ij = new ImageJ();
		String inPath = "/home/random/Development/imagej/project/3DAnalysisFIBSegmentation/owncloud/data_for_paper/high_c2/high_c2_source.tif";
		String maskPath = "/home/random/Development/imagej/project/3DAnalysisFIBSegmentation/owncloud/data_for_paper/high_c2/high_c2_golgi_corrected.tif";
		String outPath = "/home/random/Development/imagej/project/3DAnalysisFIBSegmentation/owncloud/data_for_paper/high_c2/export/golgi_indices.tif";
		RandomAccessibleInterval<T> in = (RandomAccessibleInterval<T>) ij.get(DatasetIOService.class).open(inPath);
		RandomAccessibleInterval<T> _in = ij.op().copy().rai(in);
		ij.op().image().invert(Views.iterable(_in), Views.iterable(in));
		in = ij.op().filter().gauss(_in, 1);
		RandomAccessibleInterval<? extends RealType<?>> mask = ij.get(DatasetIOService.class).open(maskPath);
		ImgLabeling<Integer, IntType> out = ij.op().create().imgLabeling(in);
		RandomAccessibleInterval<BitType> maskBit = Converters.convert(mask, (o, bitType) -> bitType.set(o.getRealFloat() > 0), new BitType());
		ij.op().image().watershed(out, in, true, true, maskBit);
		ij.scifio().datasetIO().save(ij.dataset().create(out.getIndexImg()), outPath);
	}

}
