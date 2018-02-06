package NDVICalculator;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;

class ShapeFile {

    /**
     * Shapefile
     */
    private final static File SHAPEFILE = new File("src/resources/mo-shape/mo.shp");

    /**
     * Features source
     */
    private FeatureSource<SimpleFeatureType, SimpleFeature> source;

    /**
     * List of read geometries
     */
    private List<Geometry> geometries = null;

    /**
     * Shapes coordinate reference system
     */
    private CoordinateReferenceSystem crs;

    ShapeFile() throws IOException, ParseException {
        this.source = readShapeFile();
        this.geometries = readGeometries();
        this.crs = this.source.getSchema().getCoordinateReferenceSystem();
    }

    /**
     * Get shapefile geometries
     * @return shapefile geometries
     */
    List<Geometry> getGeometries() {
        return this.geometries;
    }

    /**
     * Open shapefile and read source
     * @return source
     */
    private FeatureSource<SimpleFeatureType, SimpleFeature> readShapeFile() throws IOException {
        Map<String, Object> map = new TreeMap<>();

        try {
            map.put("url", SHAPEFILE.toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];
        return dataStore.getFeatureSource(typeName);
    }

    /**
     * Read polygons from source
     * @return list of polygons
     */
    private List<Geometry> readGeometries() throws IOException, ParseException {
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
        WKTReader reader = new WKTReader(geometryFactory);
        List<Geometry> geometries = new ArrayList<>();
        try (FeatureIterator iterator = this.source.getFeatures().features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = (SimpleFeature) iterator.next();
                Geometry geometry = reader.read(feature.getAttributes().get(0).toString());
                geometries.add(geometry);
            }
        }

        return geometries;
    }

    /**
     * Transform geometries crs to other crs
     * @param crs new crs
     */
    void transformToCRS(CoordinateReferenceSystem crs) throws FactoryException, TransformException {
        if (!CRS.equalsIgnoreMetadata(this.crs, crs) && this.geometries.size() > 0) {
            MathTransform transform = CRS.findMathTransform(this.crs, crs, true);
            ListIterator<Geometry> iterator = this.geometries.listIterator();
            while (iterator.hasNext()) {
                Geometry geometry = iterator.next();
                geometry = JTS.transform(geometry, transform);
                Geometry intersection = geometry.intersection(JTS.toGeometry(geometry.getEnvelopeInternal()));
                iterator.set(intersection);
            }
        }
    }
}
