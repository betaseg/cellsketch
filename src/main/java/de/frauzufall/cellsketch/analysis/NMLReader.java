package de.frauzufall.cellsketch.analysis;

import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.region.hypersphere.HyperSphere;
import net.imglib2.cache.img.DiskCachedCellImg;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import org.jdom2.Attribute;
import org.jdom2.DataConversionException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class NMLReader {

	public static RandomAccessibleInterval<UnsignedByteType> read(File xmlFile, double scale, int radius) throws NMLReaderIOException, DataConversionException {

		final SAXBuilder sax = new SAXBuilder();
		Document doc;
		try
		{
			doc = sax.build(xmlFile);
		}
		catch ( final Exception e )
		{
			throw new NMLReaderIOException( e );
		}
		final Element root = doc.getRootElement();

		long x = (long) (root.getChild("parameters").getChild("MovementArea").getAttribute("max.x").getLongValue() * scale);
		long y = (long) (root.getChild("parameters").getChild("MovementArea").getAttribute("max.y").getLongValue() * scale);
		long z = (long) (root.getChild("parameters").getChild("MovementArea").getAttribute("max.z").getLongValue() * scale);


		long[] dims = new long[]{x,y,z};

		System.out.println(Arrays.toString(dims));

		final RandomAccessibleInterval<UnsignedByteType> output = new DiskCachedCellImgFactory<>( new UnsignedByteType() )
				.create(x, y, z);

		List<Element> things = root.getChildren("thing");
		things.forEach(thing -> drawThing(output, thing, scale, radius, 255));

		return output;
	}

	public static RandomAccessibleInterval<RealType<?>> readIndexed(File xmlFile, double[] scale, int radius) throws NMLReaderIOException, DataConversionException {


		final SAXBuilder sax = new SAXBuilder();
		Document doc;
		try
		{
			doc = sax.build(xmlFile);
		}
		catch ( final Exception e )
		{
			throw new NMLReaderIOException( e );
		}
		final Element root = doc.getRootElement();

		long x = (long) (root.getChild("parameters").getChild("MovementArea").getAttribute("max.x").getLongValue() * scale[0]);
		long y = (long) (root.getChild("parameters").getChild("MovementArea").getAttribute("max.y").getLongValue() * scale[1]);
		long z = (long) (root.getChild("parameters").getChild("MovementArea").getAttribute("max.z").getLongValue() * scale[2]);


		long[] dims = new long[]{x,y,z};

		System.out.println(Arrays.toString(dims));

		final DiskCachedCellImg output = new DiskCachedCellImgFactory<>( new IntType() )
				.create(x, y, z);

		List<List<Pair<Point, Point>>> filaments = new ArrayList<>();

		List<Element> things = root.getChildren("thing");
		things.forEach(thing -> {
			List<Pair<Point, Point>> pointList = toPointList(thing, scale);
			if(pointList != null) filaments.add(pointList);
		});

		int i = 0;
		for (List<Pair<Point, Point>> filament : filaments) {
			i++;
			for (Pair<Point, Point> line : filament) {
				drawLine(output, line.getA(), line.getB(), radius, i);
			}
		}

		return output;
	}

	public static List<List<Pair<Point, Point>>> toPoints(File xmlFile, double[] scaleFactors) throws NMLReaderIOException, DataConversionException {

		List<List<Pair<Point, Point>>> res = new ArrayList<>();

		final SAXBuilder sax = new SAXBuilder();
		Document doc;
		try
		{
			doc = sax.build(xmlFile);
		}
		catch ( final Exception e )
		{
			throw new NMLReaderIOException( e );
		}
		final Element root = doc.getRootElement();

		long x = (long) (root.getChild("parameters").getChild("MovementArea").getAttribute("max.x").getLongValue() * scaleFactors[0]);
		long y = (long) (root.getChild("parameters").getChild("MovementArea").getAttribute("max.y").getLongValue() * scaleFactors[1]);
		long z = (long) (root.getChild("parameters").getChild("MovementArea").getAttribute("max.z").getLongValue() * scaleFactors[2]);


		long[] dims = new long[]{x,y,z};

		System.out.println(Arrays.toString(dims));

		List<Element> things = root.getChildren("thing");
		things.forEach(thing -> {
			List<Pair<Point, Point>> pointList = toPointList(thing, scaleFactors);
			if(pointList != null) res.add(pointList);
		});

		System.out.println("MT count: " + res.size());
		return res;
	}

	public static long[] getDimensions(File xmlFile, double[] scale) throws NMLReaderIOException, DataConversionException {

		final SAXBuilder sax = new SAXBuilder();
		Document doc;
		try
		{
			doc = sax.build(xmlFile);
		}
		catch ( final Exception e )
		{
			throw new NMLReaderIOException( e );
		}
		final Element root = doc.getRootElement();

		long x = (long) (root.getChild("parameters").getChild("MovementArea").getAttribute("max.x").getLongValue() * scale[0]);
		long y = (long) (root.getChild("parameters").getChild("MovementArea").getAttribute("max.y").getLongValue() * scale[1]);
		long z = (long) (root.getChild("parameters").getChild("MovementArea").getAttribute("max.z").getLongValue() * scale[2]);
		return new long[]{x, y, z};
	}

	private static List<Pair<Point, Point>> toPointList(Element thing, double[] scale) {
		List<Pair<Point, Point>> res = new ArrayList<>();
		Element edgesParent = thing.getChild("edges");
		if(edgesParent == null) return null;
		List<Element> edges = edgesParent.getChildren("edge");
		if(edges.size() == 0) return null;
		edges.forEach(edge -> res.add(getPair(edge, thing, scale)));
		return res;
	}

	private static Pair<Point, Point> getPair(Element edge, Element thing, double[] scale) {
		Attribute source = edge.getAttribute("source");
		Attribute target = edge.getAttribute("target");
		Element nodesParent = thing.getChild("nodes");
		if(nodesParent == null) return null;
		List<Element> nodes = nodesParent.getChildren("node");
		Optional<Element> sourceEl = findID(nodes, source);
		Optional<Element> targetEl = findID(nodes, target);
		if(!sourceEl.isPresent() || !targetEl.isPresent()) {
			System.out.println("Could not parse " + edge);
			return null;
		}
		try {
			long x1 = (long) (sourceEl.get().getAttribute("x").getLongValue() * scale[0]);
			long y1 = (long) (sourceEl.get().getAttribute("y").getLongValue() * scale[1]);
			long z1 = (long) (sourceEl.get().getAttribute("z").getLongValue() * scale[2]);
			long x2 = (long) (targetEl.get().getAttribute("x").getLongValue() * scale[0]);
			long y2 = (long) (targetEl.get().getAttribute("y").getLongValue() * scale[1]);
			long z2 = (long) (targetEl.get().getAttribute("z").getLongValue() * scale[2]);
			return new ValuePair<>(new Point(x1, y1, z1), new Point(x2, y2, z2));
		} catch (DataConversionException e) {
			System.out.println("Could not parse " + edge);
			e.printStackTrace();
		}
		return null;
	}

	private static boolean drawThing(RandomAccessibleInterval image, Element thing, double scale, int radius, int val) {
		Element edgesParent = thing.getChild("edges");
		if(edgesParent == null) return false;
		List<Element> edges = edgesParent.getChildren("edge");
		edges.forEach(edge -> drawEdge(image, edge, thing, scale, radius, val));
		return true;
	}

	private static void drawEdge(RandomAccessibleInterval image, Element edge, Element thing, double scale, int radius, int val) {
		Attribute source = edge.getAttribute("source");
		Attribute target = edge.getAttribute("target");
		Element nodesParent = thing.getChild("nodes");
		if(nodesParent == null) return;
		List<Element> nodes = nodesParent.getChildren("node");
		Optional<Element> sourceEl = findID(nodes, source);
		Optional<Element> targetEl = findID(nodes, target);
		if(!sourceEl.isPresent() || !targetEl.isPresent()) {
			System.out.println("Could not parse " + edge);
			return;
		}
		try {
			long x1 = (long) (sourceEl.get().getAttribute("x").getLongValue() * scale);
			long y1 = (long) (sourceEl.get().getAttribute("y").getLongValue() * scale);
			long z1 = (long) (sourceEl.get().getAttribute("z").getLongValue() * scale);
			long x2 = (long) (targetEl.get().getAttribute("x").getLongValue() * scale);
			long y2 = (long) (targetEl.get().getAttribute("y").getLongValue() * scale);
			long z2 = (long) (targetEl.get().getAttribute("z").getLongValue() * scale);
			drawLine(image, new Point(x1, y1, z1), new Point(x2, y2, z2), radius, val);
		} catch (DataConversionException e) {
			System.out.println("Could not parse " + edge);
			e.printStackTrace();
		}
	}

	// source: https://gist.github.com/yamamushi/5823518
	private static Optional<Element> findID(List<Element> node, Attribute source) {

		return node.stream().filter(el -> el.getAttribute("id").getValue().equals(source.getValue())).findFirst();
	}

	public static void drawLine(RandomAccessibleInterval image, Point p1, Point p2, double radius, int val) {
		long dx, dy, dz;
		int i;
		long l;
		long m;
		long n;
		int x_inc;
		int y_inc;
		int z_inc;
		long err_1;
		long err_2;
		long dx2;
		long dy2;
		long dz2;
		long [] point = new long[3];

		point[0] = p1.getLongPosition(0);
		point[1] = p1.getLongPosition(1);
		point[2] = p1.getLongPosition(2);
		dx = p2.getLongPosition(0) - point[0];
		dy = p2.getLongPosition(1) - point[1];
		dz = p2.getLongPosition(2) - point[2];
		x_inc = (dx < 0) ? -1 : 1;
		l = Math.abs(dx);
		y_inc = (dy < 0) ? -1 : 1;
		m = Math.abs(dy);
		z_inc = (dz < 0) ? -1 : 1;
		n = Math.abs(dz);
		dx2 = l << 1;
		dy2 = m << 1;
		dz2 = n << 1;

		if ((l >= m) && (l >= n)) {
			err_1 = dy2 - l;
			err_2 = dz2 - l;
			for (i = 0; i < l; i++) {
				drawPoint(image, point, radius, val);
				if (err_1 > 0) {
					point[1] += y_inc;
					err_1 -= dx2;
				}
				if (err_2 > 0) {
					point[2] += z_inc;
					err_2 -= dx2;
				}
				err_1 += dy2;
				err_2 += dz2;
				point[0] += x_inc;
			}
		} else if ((m >= l) && (m >= n)) {
			err_1 = dx2 - m;
			err_2 = dz2 - m;
			for (i = 0; i < m; i++) {
				drawPoint(image, point, radius, val);
				if (err_1 > 0) {
					point[0] += x_inc;
					err_1 -= dy2;
				}
				if (err_2 > 0) {
					point[2] += z_inc;
					err_2 -= dy2;
				}
				err_1 += dx2;
				err_2 += dz2;
				point[1] += y_inc;
			}
		} else {
			err_1 = dy2 - n;
			err_2 = dx2 - n;
			for (i = 0; i < n; i++) {
				drawPoint(image, point, radius, val);
				if (err_1 > 0) {
					point[1] += y_inc;
					err_1 -= dz2;
				}
				if (err_2 > 0) {
					point[0] += x_inc;
					err_2 -= dz2;
				}
				err_1 += dy2;
				err_2 += dx2;
				point[2] += z_inc;
			}
		}
		point[0] = p2.getIntPosition(0);
		point[1] = p2.getIntPosition(1);
		point[2] = p2.getIntPosition(2);
		drawPoint(image, point, radius, val);
	}

	public static void drawPoint(RandomAccessibleInterval image, long[] point, double radius, int val) {
		RandomAccess<RealType<?>> ra = image.randomAccess();
		ra.setPosition(point);
		HyperSphere<RealType<?>> hyperSphere = new HyperSphere<>(image, ra, Math.max(1, Math.round(radius)));
		try {
			for (RealType<?> value : hyperSphere) value.setReal(val);
		}
		catch(ArrayIndexOutOfBoundsException e) {}
	}

	public static class NMLReaderIOException extends Throwable {
		NMLReaderIOException(Exception e) {
			super(e);
		}
	}
}
