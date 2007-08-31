package org.janelia.it.ims.imagerenamer;

import loci.formats.in.ZeissLSMReader;
import org.jdesktop.swingworker.SwingWorker;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * User: chapmanp
 * Date: Aug 28, 2007
 * Time: 10:19:29 AM
 */
public class PreviewImageTask extends SwingWorker<Void, CopyProgressInfo> {
    private String filename = "";
    private int imageSize;
    private int layer = 0;
    private JLabel label;
    private JLabel imageLabel;

    public PreviewImageTask(String filename, int imageSize, int layer, JLabel label, JLabel imageLabel) {
        this.filename = filename;
        this.imageSize = imageSize;
        this.layer = layer;
        this.label = label;
        this.imageLabel = imageLabel;
    }

    public Void doInBackground() {
        try {
            label.setText("  ");//Strange hack to get image to display
            label.setText("");
            imageLabel.setText("Loading...");
            ZeissLSMReader zeissLSMReader = new ZeissLSMReader();
            zeissLSMReader.setId(filename);
            BufferedImage image;
            image = zeissLSMReader.openImage(layer);
            image = shrink(image, imageSize);
            ((ImageIcon) (label.getIcon())).setImage(image);
            imageLabel.setText(" ");
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Could not display image", filename, JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    public static BufferedImage shrink(BufferedImage source, int size) {
        int width = source.getWidth();
        int height = source.getHeight();
        float scaleFactor = (width > height) ? (float) size / width : (float) size / height;
        width = (int) (source.getWidth() * scaleFactor);
        height = (int) (source.getHeight() * scaleFactor);
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = result.createGraphics();
        graphics.drawImage(source.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING), 0, 0, null);
        graphics.dispose();
        return result;
    }
}
