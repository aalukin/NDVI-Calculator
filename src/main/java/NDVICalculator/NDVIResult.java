package NDVICalculator;

import com.vividsolutions.jts.geom.Geometry;
import it.geosolutions.jaiext.JAIExt;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.coverage.Coverage;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.FormatDescriptor;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

class NDVIResult {

    // Register JAI-ext operations
    static {
        JAIExt.initJAIEXT();
    }

    private GridCoverage2D NIRC = null;

    private GridCoverage2D REDC = null;

    private RenderedImage NIR = null;

    private RenderedImage RED = null;

    private Coverage NDVIC = null;

    private Geometry geometry = null;

    private double min = 2;
    private double max = -2;
    private double average = 0;

    NDVIResult(GridCoverage2D NIRC, GridCoverage2D REDC, Geometry geometry) {
        this.NIRC = NIRC;
        this.NIR = NIRC.getRenderedImage();
        this.REDC = REDC;
        this.RED = REDC.getRenderedImage();
        this.NDVIC = createNDVIC();
        this.geometry = geometry;
        calculateAttributes();
    }

    /**
     * NDVI coverage getter
     * @return NDVI coverage
     */
    Coverage getNDVIC() {
        return this.NDVIC;
    }

    Geometry getGeometry() {
        return this.geometry;
    }

    double getMin() {
        return this.min;
    }

    double getMax() {
        return this.max;
    }

    double getAverage() {
        return  this.average;
    }

    /**
     * Calculate NDVI coverage
     * @return NDVI coverage
     */
    private Coverage createNDVIC() {
        ParameterBlock pbSubtracted = new ParameterBlock();
        pbSubtracted.addSource(this.NIR);
        pbSubtracted.addSource(this.RED);
        RenderedOp subtractedImage = JAI.create("subtract", pbSubtracted);

        ParameterBlock pbAdded = new ParameterBlock();
        pbAdded.addSource(this.NIR);
        pbAdded.addSource(this.RED);
        RenderedOp addedImage = JAI.create("add", pbAdded);

        RenderedOp typeAdd = FormatDescriptor.create(addedImage, DataBuffer.TYPE_DOUBLE, null);
        RenderedOp typeSub = FormatDescriptor.create(subtractedImage, DataBuffer.TYPE_DOUBLE, null);


        ParameterBlock pbNDVI = new ParameterBlock();
        pbNDVI.addSource(typeSub);
        pbNDVI.addSource(typeAdd);
        RenderedOp NDVIop = JAI.create("divide", pbNDVI);

        GridCoverageFactory gridCoverageFactory = new GridCoverageFactory();
        ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(REDC.getEnvelope());

        return gridCoverageFactory.create("Raster", NDVIop, referencedEnvelope);
    }

    /**
     * Calculate min, max, average NDVI attributes
     */
    private void calculateAttributes() {
        Raster raster = ((GridCoverage2D)NDVIC).getRenderedImage().getData();
        int height = raster.getHeight();
        int width = raster.getWidth();
        double[] pixels = new double[height * width];
        raster.getPixels(raster.getMinX(), raster.getMinY(), width, height, pixels);

        Raster nirRaster = NIR.getData(); // For checking of no data
        double[] nirPixels = new double[height * width];
        nirRaster.getPixels(nirRaster.getMinX(), nirRaster.getMinY(), width, height, nirPixels);

        int count = 0;
        for (int i = 0; i < pixels.length; ++i) {
            double pixel = pixels[i];
            double nirPixel = nirPixels[i];
            // Checking for no data
            if (nirPixel != -9999.0) {
                ++count;
                this.max = Math.max(this.max, pixel);
                this.min = Math.min(this.min, pixel);
                this.average += pixel;
            }
        }
        this.average /= count;
    }
}
