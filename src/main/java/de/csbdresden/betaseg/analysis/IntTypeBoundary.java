/*
 * #%L
 * DenoiSeg plugin
 * %%
 * Copyright (C) 2019 - 2020 Center for Systems Biology Dresden
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
package de.csbdresden.betaseg.analysis;

import net.imglib2.AbstractWrappedInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.IntType;

public final class IntTypeBoundary< T extends IntegerType<T>>
	extends AbstractWrappedInterval< RandomAccessibleInterval< T > > implements RandomAccessibleInterval<IntType>
{

	public IntTypeBoundary(final RandomAccessibleInterval<T> img)
	{
		super( img );
	}

	@Override
	public RandomAccess<IntType> randomAccess() {
		return new IntTypeBoundaryRandomAccess4< T >( sourceInterval );
	}

	@Override
	public RandomAccess<IntType> randomAccess(Interval interval) {
		return randomAccess();
	}

}
