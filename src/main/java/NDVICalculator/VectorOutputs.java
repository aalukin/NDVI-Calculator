package NDVICalculator;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

class VectorOutputs {

    /**
     * Features collection
     */
    private DefaultFeatureCollection features = null;

    VectorOutputs(DefaultFeatureCollection features) {
        this.features = features;
    }

    /**
     * Create geoJSON file
     */
    void createGeoJSONFile(File file) throws IOException {
        FeatureJSON featureJSON = new FeatureJSON();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            featureJSON.writeFeatureCollection(this.features, fos);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Save feature collection in shapefile
     * @param file file to save
     */
    void createShapefile(File file) throws IOException {
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        Map<String, Serializable> params = new HashMap<>();
        params.put("url", file.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore dataStore = (ShapefileDataStore) dataStoreFactory.createDataStore(params);
        dataStore.createSchema(features.getSchema());

        Transaction transaction = new DefaultTransaction("create");
        String typeName = dataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);

        if (featureSource instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
            featureStore.setTransaction(transaction);
            try {
                featureStore.addFeatures(features);
                transaction.commit();
            } catch (Exception ex) {
                ex.printStackTrace();
                transaction.rollback();
            } finally {
                transaction.close();
            }
        }
    }
}
