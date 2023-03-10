/*-
 * #%L
 * N5 Viewer
 * %%
 * Copyright (C) 2017 - 2022 Igor Pisarev, Stephan Saalfeld
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package de.frauzufall.cellsketch;

import bdv.BigDataViewer;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.transformation.TransformedSource;
import bdv.ui.splitpanel.SplitPanel;
import bdv.util.*;
import bdv.util.volatiles.VolatileTypeMatcher;
import bdv.util.volatiles.VolatileViews;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerFrame;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.converter.Converter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.bdv.MultiscaleDatasets;
import org.janelia.saalfeldlab.n5.bdv.N5Source;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.*;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalMultichannelMetadata;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalMultiscaleMetadata;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalSpatialMetadata;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import sc.fiji.labeleditor.plugin.interfaces.bdv.BdvInterface;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static bdv.BigDataViewer.createConverterToARGB;
import static bdv.BigDataViewer.wrapWithTransformedSource;


/**
 *
 * This class is partly copied from the N5Viewer by Igor Pisarev and John Bogovic
 */
public class N5LabelViewer {

	private final List<BdvSource> rawSources;
	private int numTimepoints = 1;

	private boolean is2D = true;

	private final BdvHandle bdv;

	public BdvHandle getBdv() {
		return bdv;
	}

	public SplitPanel getBdvSplitPanel() {
		return bdv.getSplitPanel();
	}

	/**
	 * Creates a new N5Viewer with the given data sets.
	 * @param parentFrame parent frame, can be null
	 * @param dataSelection data sets to display
	 * @param labelEditorInterface
	 * @param context
	 * @throws IOException
	 */
	public < T extends NumericType< T > & NativeType< T >,
					V extends Volatile< T > & NumericType< V >,
					R extends N5Reader >
	N5LabelViewer(final DataSelection dataSelection, final BdvInterface labelEditorInterface, final DefaultBdvProject project) throws IOException
	{
		Prefs.showScaleBar( true );

		// TODO: These setups are not used anymore, because BdvFunctions creates its own.
		//       They either need to be deleted from here or integrated somehow.
		final List< ConverterSetup > converterSetups = new ArrayList<>();

		final List< SourceAndConverter< T > > sourcesAndConverters = new ArrayList<>();

		final List<N5Metadata> selected = new ArrayList<>();
		for( final N5Metadata meta : dataSelection.metadata )
		{
			if( meta instanceof N5ViewerMultichannelMetadata )
			{
				final N5ViewerMultichannelMetadata mc = (N5ViewerMultichannelMetadata)meta;
				for( final MultiscaleMetadata<?> m : mc.getChildrenMetadata() )
					selected.add( m );
			}
			else if ( meta instanceof CanonicalMultichannelMetadata )
			{
				final CanonicalMultichannelMetadata mc = (CanonicalMultichannelMetadata)meta;
				for( final N5Metadata m : mc.getChildrenMetadata() )
					selected.add( m );
			}
			else
				selected.add( meta );
		}

		final List<N5Source<T>> sources = new ArrayList<>();
		final List<N5Source<V>> volatileSources = new ArrayList<>();

		buildN5Sources(dataSelection.n5, selected, converterSetups, sourcesAndConverters, sources, volatileSources);

		BdvHandle bdvHandle = null;

		BdvOptions options = BdvOptions.options().frameTitle(CellProject.appName).accumulateProjectorFactory(labelEditorInterface.projector());
		if (is2D) {
			options = options.is2D();
		}

		List<BdvSource> bdvSources = new ArrayList<>();
		for (final SourceAndConverter<?> sourcesAndConverter : sourcesAndConverters) {
			if (bdvHandle == null) {
				// Create and show a BdvHandleFrame with the first source
				BdvStackSource<?> source = BdvFunctions.show(sourcesAndConverter, options);
				ViewerFrame frame = ((BdvHandleFrame)source.getBdvHandle()).getBigDataViewer().getViewerFrame();
				frame.addWindowListener(new WindowListener() {
					@Override
					public void windowOpened(WindowEvent e) {

					}

					@Override
					public void windowClosing(WindowEvent e) {
					}

					@Override
					public void windowClosed(WindowEvent e) {
						project.dispose();
					}

					@Override
					public void windowIconified(WindowEvent e) {

					}

					@Override
					public void windowDeiconified(WindowEvent e) {

					}

					@Override
					public void windowActivated(WindowEvent e) {

					}

					@Override
					public void windowDeactivated(WindowEvent e) {

					}
				});
				bdvSources.add(source);
				bdvHandle = source.getBdvHandle();
				labelEditorInterface.setup(bdvHandle);
			}
			else {
				// Subsequent sources are added to the existing handle
				bdvSources.add(BdvFunctions.show(sourcesAndConverter, BdvOptions.options().addTo(bdvHandle)));
			}
		}
		this.bdv = bdvHandle;
		this.rawSources = bdvSources;
	}

	public < T extends NumericType< T > & NativeType< T >,
				V extends Volatile< T > & NumericType< V >,
				R extends N5Reader >
	List<BdvSource> addData( final DataSelection selection ) throws IOException
	{
		final ArrayList< ConverterSetup > converterSetups = new ArrayList<>();
		final ArrayList< SourceAndConverter< T > > sourcesAndConverters = new ArrayList<>();

		final List<N5Metadata> selected = new ArrayList<>();
		for( final N5Metadata meta : selection.metadata )
		{
			if( meta instanceof N5ViewerMultichannelMetadata )
			{
				final N5ViewerMultichannelMetadata mc = (N5ViewerMultichannelMetadata)meta;
				selected.addAll(Arrays.asList(mc.getChildrenMetadata()));
			}
			else if ( meta instanceof CanonicalMultichannelMetadata )
			{
				final CanonicalMultichannelMetadata mc = (CanonicalMultichannelMetadata)meta;
				selected.addAll(Arrays.asList(mc.getChildrenMetadata()));
			}
			else
				selected.add( meta );
		}

		final List<N5Source<T>> sources = new ArrayList<>();
		final List<N5Source<V>> volatileSources = new ArrayList<>();

		buildN5Sources(selection.n5, selected, converterSetups, sourcesAndConverters, sources, volatileSources);

		final List<BdvSource> bdvSources = new ArrayList<>();
		for (final SourceAndConverter<?> sourcesAndConverter : sourcesAndConverters) {
			bdvSources.add(BdvFunctions.show(sourcesAndConverter, BdvOptions.options().addTo(bdv)));
		}

		return bdvSources;
	}

	public < T extends NumericType< T > & NativeType< T >,
					V extends Volatile< T > & NumericType< V >> void buildN5Sources(
		final N5Reader n5,
		final List<N5Metadata> selectedMetadata,
		final List< ConverterSetup > converterSetups,
		final List< SourceAndConverter< T > > sourcesAndConverters,
		final List<N5Source<T>> sources,
		final List<N5Source<V>> volatileSources) throws IOException
	{
		final ArrayList<MetadataSource<?>> additionalSources = new ArrayList<>();

		int i;
		for ( i = 0; i < selectedMetadata.size(); ++i )
		{
			String[] datasetsToOpen = null;
			AffineTransform3D[] transforms = null;

			final N5Metadata metadata = selectedMetadata.get( i );
			final String srcName = metadata.getName();
			if (metadata instanceof N5SingleScaleMetadata) {
				final N5SingleScaleMetadata singleScaleDataset = (N5SingleScaleMetadata) metadata;
				final String[] tmpDatasets= new String[]{ singleScaleDataset.getPath() };
				final AffineTransform3D[] tmpTransforms = new AffineTransform3D[]{ singleScaleDataset.spatialTransform3d() };

				final MultiscaleDatasets msd = MultiscaleDatasets.sort( tmpDatasets, tmpTransforms );
				datasetsToOpen = msd.getPaths();
				transforms = msd.getTransforms();
			} else if (metadata instanceof N5MultiScaleMetadata) {
				final N5MultiScaleMetadata multiScaleDataset = (N5MultiScaleMetadata) metadata;
				datasetsToOpen = multiScaleDataset.getPaths();
				transforms = multiScaleDataset.spatialTransforms3d();
			} else if (metadata instanceof N5CosemMetadata ) {
				final N5CosemMetadata singleScaleCosemDataset = (N5CosemMetadata) metadata;
				datasetsToOpen = new String[]{ singleScaleCosemDataset.getPath() };
				transforms = new AffineTransform3D[]{ singleScaleCosemDataset.spatialTransform3d() };
			} else if (metadata instanceof CanonicalSpatialMetadata ) {
				final CanonicalSpatialMetadata canonicalDataset = (CanonicalSpatialMetadata) metadata;
				datasetsToOpen = new String[]{ canonicalDataset.getPath() };
				transforms = new AffineTransform3D[]{ canonicalDataset.getSpatialTransform().spatialTransform3d() };
			} else if (metadata instanceof N5CosemMultiScaleMetadata ) {
				final N5CosemMultiScaleMetadata multiScaleDataset = (N5CosemMultiScaleMetadata) metadata;
				final MultiscaleDatasets msd = MultiscaleDatasets.sort( multiScaleDataset.getPaths(), multiScaleDataset.spatialTransforms3d() );
				datasetsToOpen = msd.getPaths();
				transforms = msd.getTransforms();
			} else if (metadata instanceof CanonicalMultiscaleMetadata ) {
				final CanonicalMultiscaleMetadata multiScaleDataset = (CanonicalMultiscaleMetadata) metadata;
				final MultiscaleDatasets msd = MultiscaleDatasets.sort( multiScaleDataset.getPaths(), multiScaleDataset.spatialTransforms3d() );
				datasetsToOpen = msd.getPaths();
				transforms = msd.getTransforms();
			}
			else if( metadata instanceof N5DatasetMetadata ) {
				final List<MetadataSource<?>> addTheseSources = MetadataSource.buildMetadataSources(n5, (N5DatasetMetadata)metadata);
				if( addTheseSources != null )
					additionalSources.addAll(addTheseSources);
			}
			else {
				datasetsToOpen = new String[]{ metadata.getPath() };
				transforms = new AffineTransform3D[] { new AffineTransform3D() };
			}

			if( datasetsToOpen == null || datasetsToOpen.length == 0 )
				continue;

			// is2D should be true at the end of this loop if all sources are 2D
			is2D = true;

			@SuppressWarnings( "rawtypes" )
			final RandomAccessibleInterval[] images = new RandomAccessibleInterval[datasetsToOpen.length];
			for ( int s = 0; s < images.length; ++s )
			{
				final CachedCellImg<?, ?> vimg = N5Utils.openVolatile( n5, datasetsToOpen[s] );
				if( vimg.numDimensions() == 2 )
				{
					images[ s ] = Views.addDimension(vimg, 0, 0);
					is2D = is2D && true;
				}
				else
				{
					images[ s ] = vimg;
					is2D = is2D && false;
				}
			}

			final RandomAccessibleInterval[] vimages = new RandomAccessibleInterval[images.length];
			for (int s = 0; s < images.length; ++s) {
				vimages[s] = VolatileViews.wrapAsVolatile(images[s]);
			}
			// TODO: Ideally, the volatile views should use a caching strategy
			//   where blocks are enqueued with reverse resolution level as
			//   priority. However, this would require to predetermine the number
			//   of resolution levels, which would man a lot of duplicated code
			//   for analyzing selectedMetadata. Instead, wait until SharedQueue
			//   supports growing numPriorities, then revisit.
			//   See https://github.com/imglib/imglib2-cache/issues/18.
			//   Probably it should look like this:
//			sharedQueue.ensureNumPriorities(images.length);
//			for (int s = 0; s < images.length; ++s) {
//				final int priority = images.length - 1 - s;
//				final CacheHints cacheHints = new CacheHints(LoadingStrategy.BUDGETED, priority, false);
//				vimages[s] = VolatileViews.wrapAsVolatile(images[s], sharedQueue, cacheHints);
//			}

			@SuppressWarnings("unchecked")
			final T type = (T) Util.getTypeFromInterval(images[0]);
			final N5Source<T> source = new N5Source<>(
					type,
					srcName,
					images,
					transforms);

			@SuppressWarnings("unchecked")
			final V volatileType = (V) VolatileTypeMatcher.getVolatileTypeForType(type);
			final N5Source<V> volatileSource = new N5Source<>(
					volatileType,
					srcName,
					vimages,
					transforms);

			sources.add(source);
			volatileSources.add(volatileSource);

			addSourceToListsGenericType(source, volatileSource, i + 1, converterSetups, sourcesAndConverters);
		}

		for( final MetadataSource src : additionalSources ) {
			if( src.numTimePoints() > numTimepoints )
				numTimepoints = src.numTimePoints();

			addSourceToListsGenericType( src, i + 1, converterSetups, sourcesAndConverters );
		}
	}

	/**
	 * Add the given {@code source} to the lists of {@code converterSetups}
	 * (using specified {@code setupId}) and {@code sources}. For this, the
	 * {@code source} is wrapped with an appropriate {@link Converter} to
	 * {@link ARGBType} and into a {@link TransformedSource}.
	 *
	 * @param source
	 *            source to add.
	 * @param setupId
	 *            id of the new source for use in {@code SetupAssignments}.
	 * @param converterSetups
	 *            list of {@link ConverterSetup}s to which the source should be
	 *            added.
	 * @param sources
	 *            list of {@link SourceAndConverter}s to which the source should
	 *            be added.
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	private static < T > void addSourceToListsGenericType(
			final Source< T > source,
			final int setupId,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< T > > sources )
	{
		addSourceToListsGenericType( source, null, setupId, converterSetups, sources );
	}

	/**
	 * Add the given {@code source} to the lists of {@code converterSetups}
	 * (using specified {@code setupId}) and {@code sources}. For this, the
	 * {@code source} is wrapped with an appropriate {@link Converter} to
	 * {@link ARGBType} and into a {@link TransformedSource}.
	 *
	 * @param source
	 *            source to add.
	 * @param setupId
	 *            id of the new source for use in {@code SetupAssignments}.
	 * @param converterSetups
	 *            list of {@link ConverterSetup}s to which the source should be
	 *            added.
	 * @param sources
	 *            list of {@link SourceAndConverter}s to which the source should
	 *            be added.
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	private static < T, V extends Volatile< T > > void addSourceToListsGenericType(
			final Source< T > source,
			final Source< V > volatileSource,
			final int setupId,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< T > > sources )
	{
		final T type = source.getType();
		if ( type instanceof RealType || type instanceof ARGBType || type instanceof VolatileARGBType )
			addSourceToListsNumericType( ( Source ) source, ( Source ) volatileSource, setupId, converterSetups, ( List ) sources );
		else
			throw new IllegalArgumentException( "Unknown source type. Expected RealType, ARGBType, or VolatileARGBType" );
	}

	/**
	 * Add the given {@code source} to the lists of {@code converterSetups}
	 * (using specified {@code setupId}) and {@code sources}. For this, the
	 * {@code source} is wrapped with an appropriate {@link Converter} to
	 * {@link ARGBType} and into a {@link TransformedSource}.
	 *
	 * @param source
	 *            source to add.
	 * @param volatileSource
	 *            corresponding volatile source.
	 * @param setupId
	 *            id of the new source for use in {@code SetupAssignments}.
	 * @param converterSetups
	 *            list of {@link ConverterSetup}s to which the source should be
	 *            added.
	 * @param sources
	 *            list of {@link SourceAndConverter}s to which the source should
	 *            be added.
	 */
	private static < T extends NumericType< T >, V extends Volatile< T > & NumericType< V > > void addSourceToListsNumericType(
			final Source< T > source,
			final Source< V > volatileSource,
			final int setupId,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< T > > sources )
	{
		final SourceAndConverter< V > vsoc = ( volatileSource == null )
				? null
				: new SourceAndConverter<>( volatileSource, createConverterToARGB( volatileSource.getType() ) );
		final SourceAndConverter< T > soc = new SourceAndConverter<>( source, createConverterToARGB( source.getType() ), vsoc );
		final SourceAndConverter< T > tsoc = wrapWithTransformedSource( soc );

		converterSetups.add( BigDataViewer.createConverterSetup( tsoc, setupId ) );
		sources.add( tsoc );
	}

	public List<BdvSource> getSourceSources() {
		return rawSources;
	}
}
