package NDVICalculator;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.operation.Crop;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class NDVI {

    private TiffFiles tiffFiles = null;
    private ShapeFile shapeFile = null;

    private List<NDVIResult> results = null;

    NDVI(TiffFiles tiffFiles, ShapeFile shapeFile){
        this.tiffFiles = tiffFiles;
        this.shapeFile = shapeFile;
        results = new ArrayList<>(shapeFile.getGeometries().size());
    }

    /**
     * Crop images to boundary polygon tiff
     */
    void crop() throws IOException {
        for (Geometry geometry : shapeFile.getGeometries()) {
            CoverageProcessor processor = new CoverageProcessor();
            ParameterValueGroup params = processor.getOperation("CoverageCrop").getParameters();
            params.parameter("Source").setValue(this.tiffFiles.getNIRC());
            params.parameter(Crop.PARAMNAME_ROI).setValue(geometry);
            GridCoverage2D NIRcoverage = (GridCoverage2D) processor.doOperation(params);
            params.parameter("Source").setValue(this.tiffFiles.getREDC());
            GridCoverage2D REDcoverage = (GridCoverage2D) processor.doOperation(params);
            NDVIResult result = new NDVIResult(NIRcoverage, REDcoverage, geometry);
            results.add(result);
        }
    }


    /**
     * Print cropped tiffs
     */
    void printCroppedImages(File file) throws IOException {
        file.mkdir();
        int i = 0;
        for (NDVIResult result : results) {
            File tiffFile = new File(file.getAbsolutePath() + File.separator + i + ".tiff");
            GridCoverageWriter writer = ((GeoTiffFormat) tiffFiles.reader.getFormat()).getWriter(tiffFile);
            writer.write((GridCoverage) result.getNDVIC(), null);
            writer.dispose();
            ++i;
        }
    }

    /**
     * Create boundaries NDVI feature collection
     */
    DefaultFeatureCollection crateFeaturesCollection() throws FactoryException, TransformException {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("mondvi");
        typeBuilder.setCRS(tiffFiles.getCRS());

        typeBuilder.add("the_geom", MultiPolygon.class);
        typeBuilder.add("min", Double.class);
        typeBuilder.add("max", Double.class);
        typeBuilder.add("avg", Double.class);

        final SimpleFeatureType featureType = typeBuilder.buildFeatureType();

        DefaultFeatureCollection collection = new DefaultFeatureCollection(null, null);
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);

        for (NDVIResult result : results) {
            featureBuilder.add(result.getGeometry());
            featureBuilder.add(result.getMin());
            featureBuilder.add(result.getMax());
            featureBuilder.add(result.getAverage());

            SimpleFeature feature = featureBuilder.buildFeature(null);
            collection.add(feature);
        }

        return collection;
    }
}
