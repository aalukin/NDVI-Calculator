package NDVICalculator;

import org.geotools.feature.DefaultFeatureCollection;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

/**
 * Homework 2 application
 */
public class App 
{

    /**
     * Application frame
     */
    private JFrame frame;

    /**
     * Application panel
     */
    private JPanel panel;

    public static void main( String[] args )
    {
        new App().launch();
    }

    /**
     * Launch application
     */
    private void launch(){
        frame = new JFrame("Moscow Boundaries NDVI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 150);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        panel = new JPanel();
        frame.add(panel);
        fillPanel();

        frame.setVisible(true);
    }

    /**
     * Calculate NDVI button
     */
    private JButton calculateNDVIButton = null;

    /**
     * Save cropped NDVI raster files checkbox
     */
    private JCheckBox saveNDVIRasterCheckbox = null;

    /**
     * Save all vector files button
     */
    private JButton saveAllButton = null;

    /**
     * Save only GeoJSON file button
     */
    private JButton saveGeoJSONButton = null;

    /**
     * Save only Shapefile button
     */
    private JButton saveShapefileButton = null;

    /**
     * Save only KML file button
     */
    //private JButton saveKMLButton = null;

    private void fillPanel() {
        this.calculateNDVIButton = new JButton("Calculate NDVI from resources");
        this.panel.add(this.calculateNDVIButton);
        this.calculateNDVIButton.addActionListener(this.calculateNDVIActionListener);

        this.saveNDVIRasterCheckbox = new JCheckBox("Save cropped NDVI raster");
        this.panel.add(this.saveNDVIRasterCheckbox);

        this.saveAllButton = new JButton("Save all vector files");
        this.saveAllButton.setEnabled(false);
        this.panel.add(saveAllButton);
        this.saveAllButton.addActionListener(this.saveAllFilesActionListener);

        this.saveGeoJSONButton = new JButton("Save GeoJSON file");
        this.saveGeoJSONButton.setEnabled(false);
        this.panel.add(this.saveGeoJSONButton);
        this.saveGeoJSONButton.addActionListener(this.saveGeoJSONActionListener);

        this.saveShapefileButton = new JButton("Save Shapefile");
        this.saveShapefileButton.setEnabled(false);
        this.panel.add(this.saveShapefileButton);
        this.saveShapefileButton.addActionListener(this.saveShapefileActionListener);
    }

    private VectorOutputs vectorOutputs = null;

    private ActionListener calculateNDVIActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                calculateNDVIButton.setEnabled(false);
                saveNDVIRasterCheckbox.setEnabled(false);

                File file = null;
                if (saveNDVIRasterCheckbox.isSelected()) {
                    JFileChooser fileChooser = new JFileChooser();
                    if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                        file = fileChooser.getSelectedFile();
                    } else {
                        calculateNDVIButton.setEnabled(true);
                        saveNDVIRasterCheckbox.setEnabled(true);
                        return;
                    }
                }

                ShapeFile shapeFile = new ShapeFile();
                TiffFiles tiffFiles = new TiffFiles();
                shapeFile.transformToCRS(tiffFiles.getCRS());
                NDVI ndvi = new NDVI(tiffFiles, shapeFile);
                ndvi.crop();
                if (file != null){
                    ndvi.printCroppedImages(file);
                }
                DefaultFeatureCollection features = ndvi.crateFeaturesCollection();
                vectorOutputs = new VectorOutputs(features);

                saveAllButton.setEnabled(true);
                saveGeoJSONButton.setEnabled(true);
                saveShapefileButton.setEnabled(true);
            } catch (Exception ex) {
                ex.printStackTrace();
                calculateNDVIButton.setEnabled(true);
                saveNDVIRasterCheckbox.setEnabled(true);
            }
        }
    };

    /**
     * Save all types of vector files action listener
     */
    private ActionListener saveAllFilesActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (vectorOutputs == null) {
                retrieveDisables();
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION){
                File dir = fileChooser.getSelectedFile();
                boolean res = dir.mkdir();
                if (!res) {
                    JOptionPane.showMessageDialog(frame, "Error, cannot create directory \'" + dir.getName() + "\'");
                    return;
                }

                // Save shape file
                File shapeDir = new File(dir.getAbsolutePath() + File.separator + "Shapefiles");
                res = shapeDir.mkdir();
                if (!res) {
                    JOptionPane.showMessageDialog(frame, "Error, cannot create shapefile directory");
                    return;
                }
                File shapeFile = new File(shapeDir.getAbsolutePath() + File.separator + dir.getName() + ".shp");
                try {
                    vectorOutputs.createShapefile(shapeFile);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                // Save GeoJSON
                File geoJSONFile = new File(dir.getAbsolutePath() + File.separator + dir.getName() + ".geojson");
                try {
                    vectorOutputs.createGeoJSONFile(geoJSONFile);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                JOptionPane.showMessageDialog(frame, "All files are saved");
            }
        }
    };

    /**
     * Save GeoJSON file action listener
     */
    private ActionListener saveGeoJSONActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (vectorOutputs == null) {
                retrieveDisables();
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String path = file.getAbsolutePath();
                if (!path.endsWith(".geojson") && !path.endsWith(".json")) {
                    path += ".geojson";
                    file = new File(path);
                }
                try {
                    vectorOutputs.createGeoJSONFile(file);
                    JOptionPane.showMessageDialog(frame, "GeoJSON is saved");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    };

    /**
     * Save shapefile action listener
     */
    private ActionListener saveShapefileActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (vectorOutputs == null) {
                retrieveDisables();
                return;
            }
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                boolean res = file.mkdir();
                if (!res) {
                    JOptionPane.showMessageDialog(frame, "Error, cannot create shapefile directory");
                }
                file = new File(file.getAbsolutePath() + File.separator + file.getName() + ".shp");
                try {
                    vectorOutputs.createShapefile(file);
                    JOptionPane.showMessageDialog(frame, "Shapefile is saved");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    };

//    /**
//     * Save kml file action listener
//     */
//    private ActionListener saveKMLFileActionListener = new ActionListener() {
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            if (vectorOutputs == null) {
//                retrieveDisables();
//                return;
//            }
//
//            JFileChooser fileChooser = new JFileChooser();
//            if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
//                File file = fileChooser.getSelectedFile();
//                String path = file.getAbsolutePath();
//                if (!path.endsWith(".kml")) {
//                    path += ".kml";
//                    file = new File(path);
//                }
//                try {
//                    vectorOutputs.createKML(file);
//                } catch (IOException ex) {
//                    ex.printStackTrace();
//                }
//            }
//        }
//    };

    /**
     * Return frame in start options
     */
    private void retrieveDisables(){
        this.saveAllButton.setEnabled(false);
        this.saveShapefileButton.setEnabled(false);
        this.saveGeoJSONButton.setEnabled(false);
        //this.saveKMLButton.setEnabled(false);
        this.calculateNDVIButton.setEnabled(true);
        this.saveNDVIRasterCheckbox.setEnabled(true);
    }
}
