/*
 * #%L
 * UI component for image segmentation label comparison and selection
 * %%
 * Copyright (C) 2019 - 2020 DAIS developers
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
package sc.fiji.project;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.AbstractConvertedRandomAccess;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.Views;

/**
 * A {@link IntType} {@link RandomAccess} on a {@link IntType} source
 * {@link RandomAccessibleInterval}. It returns the value of pixels that have
 * pixel with different values in their 4-neighborhood (or n-dimensional equivalent)
 * and returns 0 for all other pixels.
 *
 * @author Tobias Pietzsch
 * @author Deborah Schmidt
 */
public final class IntTypeBoundaryRandomAccess4< T extends IntegerType<T>> extends AbstractConvertedRandomAccess< T, IntType >
{
	private final int n;

	private final IntType type;
	private int timeDim;

	public IntTypeBoundaryRandomAccess4(final RandomAccessibleInterval<T> sourceInterval, int timeDim)
	{
		super(Views.extend(sourceInterval, new OutOfBoundsConstantValueFactory(new IntType(0))).randomAccess() );
		n = sourceInterval.numDimensions();
		type = new IntType();
		this.timeDim = timeDim;
	}

	private IntTypeBoundaryRandomAccess4(final IntTypeBoundaryRandomAccess4< T > ba )
	{
		super( ba.source.copyRandomAccess() );
		this.n = ba.n;
		this.type = ba.type.copy();
	}

	@Override
	public IntType get()
	{
		T center = source.get().copy();
		if ( center.getInteger() > 0 )
		{
			for ( int d = 0; d < n; ++d )
			{
				if(d == timeDim) {
					continue;
				}
				bck( d );
				if ( !source.get().equals(center) )
				{
					fwd( d );
					type.set( center.getInteger() );
					return type;
				}
				fwd( d );
				fwd( d );
				if ( !source.get().equals(center) )
				{
					bck( d );
					type.set( center.getInteger() );
					return type;
				}
				bck( d );
			}
		}
		type.setZero();
		return type;
	}

	@Override
	public IntTypeBoundaryRandomAccess4< T > copy()
	{
		return new IntTypeBoundaryRandomAccess4<>( this );
	}
}
