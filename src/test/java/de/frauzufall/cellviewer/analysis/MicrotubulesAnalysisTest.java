package de.frauzufall.cellviewer.analysis;

import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import org.json.JSONException;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MicrotubulesAnalysisTest {

	@Test
	public void testFixLineOrder() throws FileNotFoundException, JSONException {
		List<List<Pair<Point, Point>>> in = new ArrayList<>();
		List<Pair<Point, Point>> mt1 = new ArrayList<>();
		mt1.add(new ValuePair<>(new Point(1, 1, 1), new Point(2, 2, 2)));
		mt1.add(new ValuePair<>(new Point(2, 2, 2), new Point(3, 3, 3)));
		mt1.add(new ValuePair<>(new Point(3, 3, 3), new Point(4, 4, 4)));
//		mt1.add(new ValuePair<>(new Point(4, 4, 4), new Point(5, 5, 5)));
//		mt1.add(new ValuePair<>(new Point(5, 5, 5), new Point(6, 6, 6)));
		in.add(mt1);
//		List<Pair<Point, Point>> mt2 = new ArrayList<>();
//		mt2.add(new ValuePair<>(new Point(7, 7, 7), new Point(8, 8, 8)));
//		mt2.add(new ValuePair<>(new Point(8, 8, 8), new Point(3, 3, 3)));
//		mt2.add(new ValuePair<>(new Point(3, 3, 3), new Point(2, 2, 2)));
//		mt2.add(new ValuePair<>(new Point(2, 2, 2), new Point(1, 1, 1)));
//		in.add(mt2);
		Collections.reverse(mt1);
//		Collections.shuffle(mt2);
		mt1.set(2, reverse(mt1.get(2)));
//		mt1.set(4, reverse(mt1.get(4)));
//		mt2.set(0, reverse(mt2.get(0)));
		mt1.add(0, new ValuePair<>(new Point(-1, -1, -1), new Point(1, 1, 1)));
//		mt2.add(0, new ValuePair<>(new Point(6, 6, 6), new Point(7, 7, 7)));
		List<List<Pair<Point, Point>>> corrected = MicrotubulesAnalyzer.correctLineOrder(in, 0);
		long[] dimensions = new long[]{10, 10, 10};
		System.out.println("drawing in");
		RandomAccessibleInterval<UnsignedByteType> rendering = MicrotubulesAnalyzer.render(in, dimensions, 1);
		System.out.println("drawing corrected");
		RandomAccessibleInterval<UnsignedByteType> rendering2 = MicrotubulesAnalyzer.render(corrected, dimensions, 1);
		MicrotubulesAnalyzer.compare(rendering, rendering2);
		assertEquals(1, corrected.size());
		assertEquals(4, corrected.get(0).size());
//		assertEquals(5, corrected.get(1).size());
		assertPointEquals(corrected.get(0).get(0), -1, 1);
		assertPointEquals(corrected.get(0).get(1), 1, 2);
		assertPointEquals(corrected.get(0).get(2), 2, 3);
		assertPointEquals(corrected.get(0).get(3), 3, 4);
//		assertPointEquals(corrected.get(0).get(4), 4, 5);
//		assertPointEquals(corrected.get(0).get(5), 5, 6);

//		assertPointEquals(corrected.get(1).get(0), 6, 7);
//		assertPointEquals(corrected.get(1).get(1), 7, 8);
//		assertPointEquals(corrected.get(1).get(2), 8, 3);
//		assertPointEquals(corrected.get(1).get(3), 3, 2);
//		assertPointEquals(corrected.get(1).get(4), 2, 1);
	}

	private ValuePair<Point, Point> reverse(Pair<Point, Point> pointPointPair) {
		Point a = pointPointPair.getA();
		Point b = pointPointPair.getB();
		return new ValuePair<>(b, a);
	}

	private void assertPointEquals(Pair<Point, Point> point, int pA, int pB) {
		assertEquals(pA, point.getA().getIntPosition(0));
		assertEquals(pA, point.getA().getIntPosition(1));
		assertEquals(pA, point.getA().getIntPosition(2));
		assertEquals(pB, point.getB().getIntPosition(0));
		assertEquals(pB, point.getB().getIntPosition(1));
		assertEquals(pB, point.getB().getIntPosition(2));
	}
}
