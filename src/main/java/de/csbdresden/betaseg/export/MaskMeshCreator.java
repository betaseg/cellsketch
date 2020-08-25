package de.csbdresden.betaseg.export;

import net.imagej.mesh.Mesh;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.util.concurrent.Callable;

public class MaskMeshCreator extends Thread implements Callable<MyMesh> {

	private final ImgFactory<FloatType> floatFactory;
	private final RandomAccessibleInterval<? extends RealType> image;
	private final OpService opService;

	public MaskMeshCreator(OpService opService, RandomAccessibleInterval<ByteType> image, ImgFactory<FloatType> floatFactory) {
		this.opService = opService;
		this.image = image;
		this.floatFactory = floatFactory;
	}

	@Override
	public MyMesh call() {
		RandomAccessibleInterval<FloatType> smoothed = floatFactory.create(Intervals.expand(image, 2));
		ImgLabeling<Integer, IntType> labeling = opService.create().imgLabeling(image);
		LoopBuilder.setImages(image, labeling).multiThreaded().forEachPixel((pixel, labelPixel) -> {
			if((int)pixel.getRealFloat() > 0) labelPixel.add(1);
		});
		smoothed = Views.translate(smoothed, image.min(0)-2, image.min(1)-2, image.min(2)-2);
		opService.filter().gauss(smoothed, Views.expandZero(image, 2, 2, 2), 1);
//		LabelRegions<Integer> regions = new LabelRegions<>(labeling);
//		Mesh res = opService.geom().marchingCubes(region);
//		Mesh res = new RegionMarchingCubes().calculate(region);
//		Mesh res = Meshes.marchingCubes(smoothed, 0.15);
//		res = Meshes.removeDuplicateVertices(res, 2);
//		res = Meshes.simplify(res, 0.25f, 6);
		Mesh res = MarchingCubesRealType.calculate(smoothed, 0.15);
		res = RemoveDuplicateMeshVertices.calculate(res, 2);
		res = new SimplifyMesh(res).simplify(0.25f, 6);
		MyMesh mymesh = new MyMesh();
		mymesh.mesh = res;
		mymesh.label = new IntType(1);
		return mymesh;
	}
}
