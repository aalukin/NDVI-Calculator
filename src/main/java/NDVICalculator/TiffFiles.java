package NDVICalculator;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;

class TiffFiles {

    private final static File NIRGTIFF_FILE = new File("src/resources/LC81790212015146LGN00_sr_band5.tif");
    private final static File REDGTIFF_FILE = new File("src/resources/LC81790212015146LGN00_sr_band4.tif");

    //private RenderedImage NIR;
    //private RenderedImage RED;

    private GridCoverage2D NIRC;
    private GridCoverage2D REDC;

    /**
     * Raster's coordinate reference system
     */
    private CoordinateReferenceSystem crs;


    TiffFiles() throws Exception {
        this.openTiffFiles();
    }

    /**
     * NIRC getter
     * @return NIRC
     */
    GridCoverage2D getNIRC() {
        return this.NIRC;
    }

    /**
     * REDC getter
     * @return REDC
     */
    GridCoverage2D getREDC() {
        return this.REDC;
    }

    /**
     * Raster CRS getter
     * @return tiff's crs
     */
    CoordinateReferenceSystem getCRS() {
        return this.crs;
    }

    public GeoTiffReader reader;
    private void openTiffFiles() throws Exception {
        GeoTiffReader reader = new GeoTiffReader(NIRGTIFF_FILE);
        this.reader = reader;
        NIRC = reader.read(null);
        reader = new GeoTiffReader(REDGTIFF_FILE);
        REDC = reader.read(null);

        if (!CRS.equalsIgnoreMetadata(NIRC.getCoordinateReferenceSystem(), NIRC.getCoordinateReferenceSystem())){
            throw new Exception("Tiffs crs is not equal");
        }
        this.crs = NIRC.getCoordinateReferenceSystem();
    }
}
