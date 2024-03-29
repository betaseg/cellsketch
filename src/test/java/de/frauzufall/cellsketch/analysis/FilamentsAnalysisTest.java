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

public class FilamentsAnalysisTest {

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
		List<List<Pair<Point, Point>>> corrected = FilamentsImporter.correctLineOrder(in, 0);
		long[] dimensions = new long[]{10, 10, 10};
		System.out.println("drawing in");
		RandomAccessibleInterval<UnsignedByteType> rendering = FilamentsImporter.render(in, dimensions, 1);
		System.out.println("drawing corrected");
		RandomAccessibleInterval<UnsignedByteType> rendering2 = FilamentsImporter.render(corrected, dimensions, 1);
//		FilamentsImporter.compare(rendering, rendering2);
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
