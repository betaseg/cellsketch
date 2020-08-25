/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2014 - 2020 ImageJ developers.
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

package de.csbdresden.betaseg.export;

import net.imagej.mesh.Mesh;
import net.imagej.mesh.naive.NaiveDoubleMesh;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.BooleanType;
import net.imglib2.view.Views;
import org.apache.commons.math3.util.MathArrays;

/**
 * This is a marching cubes implementation. It is inspired by Paul Bourke's
 * (http://paulbourke.net/geometry/polygonise/) implementation. Especially the
 * lookup tables are from his implementation.
 *
 * @author Tim-Oliver Buchholz (University of Konstanz)
 * @author Tobias Pietzsch
 */
class MarchingCubesBooleanType {

	private static final double[] p0 = {0, 0, 1};
	private static final double[] p1 = {1, 0, 1};
	private static final double[] p2 = {1, 0, 0};
	private static final double[] p3 = {0, 0, 0};
	private static final double[] p4 = {0, 1, 1};
	private static final double[] p5 = {1, 1, 1};
	private static final double[] p6 = {1, 1, 0};
	private static final double[] p7 = {0, 1, 0};

	private static <T extends BooleanType<T>> byte[] mask(final RandomAccessibleInterval<T> input) {
		final int msx = (int) input.dimension(0);
		final int msy = (int) input.dimension(1);
		final int msz = (int) input.dimension(2);

		final int isx = msx + 2;
		final int isy = msy + 2;
		final int isz = msz + 2;

		final int is = isx * isy * isz;
		final byte[] indices = new byte[is];
		final Cursor<T> c = Views.flatIterable(input).cursor();
		int j = isx * isy + isx + 1;
		for (int z = 0; z < msz; ++z) {
			for (int y = 0; y < msy; ++y) {
				for (int x = 0; x < msx; ++x) {
					if (c.next().get())
						indices[j] = 1;
					++j;
				}
				j += 2;
			}
			j += 2 * isx;
		}

		for (int i = 0; i < is - 1; ++i)
			indices[i] = ubyte(indices[i] | (indices[i + 1] << 1));

		for (int i = 0; i < is - isx; ++i)
			indices[i] = ubyte(indices[i] | (indices[i + isx] << 2));

		for (int i = 0; i < is - (isx * isy); ++i)
			indices[i] = ubyte(indices[i] | (indices[i + isx * isy] << 4));

		return indices;
	}

	private static byte ubyte(final int unsignedByte) {
		return (byte) (unsignedByte & 0xff);
	}

	static <T extends BooleanType<T>> Mesh calculate(final RandomAccessibleInterval<T> input) {
		final double[][] vertlist = new double[12][];

		final int msx = (int) input.dimension(0);
		final int msy = (int) input.dimension(1);
		final int msz = (int) input.dimension(2);
		final int isx = msx + 2;
		final int isy = msy + 2;
		final int isz = msz + 2;
		final byte[] mask = mask(input);

		Mesh output = new NaiveDoubleMesh();

		final int minX = (int) input.min(0) - 1;
		final int minY = (int) input.min(1) - 1;
		final int minZ = (int) input.min(2) - 1;
		final int maxX = (int) input.max(0) + 1;
		final int maxY = (int) input.max(1) + 1;
		final int maxZ = (int) input.max(2) + 1;

		for (int z = minZ; z < maxZ; ++z) {
			for (int y = minY; y < maxY; ++y) {
				for (int x = minX; x < maxX; ++x) {
					final int mx = (x - minX);
					final int my = (y - minY);
					final int mz = (z - minZ);
					final int mindex = mask[mz * (isx * isy) + my * isx + mx] & 0xff;

					final int EDGE = MarchingCubesRealType.EDGE_TABLE[mindex];
					if (EDGE != 0) {
						/* Find the vertices where the surface intersects the cube */
						if (0 != (EDGE & 1)) {
							vertlist[0] = interpolatePoint(p0, p1,
									(mindex & 1 << 5) != 0);
						}
						if (0 != (EDGE & 2)) {
							vertlist[1] = interpolatePoint(p1, p2,
									(mindex & 1 << 1) != 0);
						}
						if (0 != (EDGE & 4)) {
							vertlist[2] = interpolatePoint(p2, p3,
									(mindex & 1 << 0) != 0);
						}
						if (0 != (EDGE & 8)) {
							vertlist[3] = interpolatePoint(p3, p0,
									(mindex & 1 << 4) != 0);
						}
						if (0 != (EDGE & 16)) {
							vertlist[4] = interpolatePoint(p4, p5,
									(mindex & 1 << 7) != 0);
						}
						if (0 != (EDGE & 32)) {
							vertlist[5] = interpolatePoint(p5, p6,
									(mindex & 1 << 3) != 0);
						}
						if (0 != (EDGE & 64)) {
							vertlist[6] = interpolatePoint(p6, p7,
									(mindex & 1 << 2) != 0);
						}
						if (0 != (EDGE & 128)) {
							vertlist[7] = interpolatePoint(p7, p4,
									(mindex & 1 << 6) != 0);
						}
						if (0 != (EDGE & 256)) {
							vertlist[8] = interpolatePoint(p0, p4,
									(mindex & 1 << 6) != 0);
						}
						if (0 != (EDGE & 512)) {
							vertlist[9] = interpolatePoint(p1, p5,
									(mindex & 1 << 7) != 0);
						}
						if (0 != (EDGE & 1024)) {
							vertlist[10] = interpolatePoint(p2, p6,
									(mindex & 1 << 3) != 0);
						}
						if (0 != (EDGE & 2048)) {
							vertlist[11] = interpolatePoint(p3, p7,
									(mindex & 1 << 2) != 0);
						}

						/* Create the triangle */
						final byte[] TRIANGLE = MarchingCubesRealType.TRIANGLE_TABLE[mindex];
						for (int i = 0; i < TRIANGLE.length; i += 3) {
							final double v0x = vertlist[TRIANGLE[i]][0];
							final double v0y = vertlist[TRIANGLE[i]][1];
							final double v0z = vertlist[TRIANGLE[i]][2];
							final double v1x = vertlist[TRIANGLE[i + 1]][0];
							final double v1y = vertlist[TRIANGLE[i + 1]][1];
							final double v1z = vertlist[TRIANGLE[i + 1]][2];
							final double v2x = vertlist[TRIANGLE[i + 2]][0];
							final double v2y = vertlist[TRIANGLE[i + 2]][1];
							final double v2z = vertlist[TRIANGLE[i + 2]][2];
							if (positiveArea(v0x, v0y, v0z, v1x, v1y, v1z, v2x, v2y, v2z)) {
								output.triangles().add(x + v0x, y + v0y, z + v0z, x + v1x, y + v1y, z + v1z, x + v2x, y + v2y, z + v2z);
							}
						}
					}
				}
			}
		}
		return output;
	}

	private static boolean positiveArea(double v0x, double v0y, double v0z, //
	                                    double v1x, double v1y, double v1z, //
	                                    double v2x, double v2y, double v2z) {
		final double p1x = v0x - v1x;
		final double p1y = v0y - v1y;
		final double p1z = v0z - v1z;
		final double p2x = v2x - v0x;
		final double p2y = v2y - v0y;
		final double p2z = v2z - v0z;

		// cross product
		final double cpx = MathArrays.linearCombination(p1y, p2z, -p1z, p2y);
		final double cpy = MathArrays.linearCombination(p1z, p2x, -p1x, p2z);
		final double cpz = MathArrays.linearCombination(p1x, p2y, -p1y, p2x);

		return cpx != 0 || cpy != 0 || cpz != 0;
	}

	private static double[] interpolatePoint(double[] p0, double[] p1, boolean v1) {
		return v1 ? p1 : p0;
	}
}
