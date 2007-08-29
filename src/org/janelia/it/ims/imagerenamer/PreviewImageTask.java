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

    public PreviewImageTask(String filename, int imageSize) {
        this.filename = filename;
        this.imageSize = imageSize;
    }

    public Void doInBackground() {
        try {
            ZeissLSMReader zeissLSMReader = new ZeissLSMReader();
            zeissLSMReader.setId(filename);
            BufferedImage image = zeissLSMReader.openImage(0);
            image = shrink(image, imageSize);
            JPanel panel = new JPanel();
            Icon icon = new ImageIcon(image);
            JLabel label = new JLabel(icon);
            panel.add(label);
            JLabel fileLabel = new JLabel();
            fileLabel.setText(filename);
            panel.add(fileLabel);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            JOptionPane.showMessageDialog(null, panel, filename, JOptionPane.INFORMATION_MESSAGE);
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
