package org.janelia.it.ims.imagerenamer;

import loci.formats.in.ZeissLSMReader;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.File;


public class PreviewImageFrame extends JFrame {
    private JLabel label;
    private JSlider slider;
    private JLabel sliderLabel;
    private JLabel imageLabel = new JLabel();

    public PreviewImageFrame(File file, int imageSize) {
        final String filename = file.getName();
        final String filePath = file.getPath();
        final int previewImageSize = imageSize;
        JFrame frame = new JFrame();
        frame.setTitle(filename);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        frame.add(panel);
        label = new JLabel(new ImageIcon());
        label.setVerticalAlignment(SwingConstants.BOTTOM);
        label.setText("");
        panel.add(label, BorderLayout.NORTH);

        imageLabel.setText(" ");

        PreviewImageTask previewImageTask = new PreviewImageTask(filePath, previewImageSize, 0, label, imageLabel);
        previewImageTask.execute();

        JPanel subPanel = new JPanel(new BorderLayout());

        ZeissLSMReader zeissLSMReader;
        try {
            zeissLSMReader = new ZeissLSMReader();
            zeissLSMReader.setId(filePath);
            slider = new JSlider(JSlider.HORIZONTAL, 0, zeissLSMReader.getTiffDimensions()[2], 0);
            sliderLabel = new JLabel("Layer: " + slider.getValue());
            subPanel.add(sliderLabel, BorderLayout.WEST);
            subPanel.add(slider, BorderLayout.CENTER);
            subPanel.add(imageLabel, BorderLayout.SOUTH);
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error displaying preview.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        panel.add(subPanel, BorderLayout.SOUTH);

        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                PreviewImageTask previewImageTask = new PreviewImageTask(filePath, previewImageSize, slider.getValue(), label, imageLabel);
                sliderLabel.setText("Layer: " + slider.getValue());
                previewImageTask.execute();
            }
        }

        );
        frame.setSize(previewImageSize + 100, previewImageSize + 75);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();

        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }

        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }

        frame.setLocation((screenSize.width - frameSize.width) / 2,
                (screenSize.height - frameSize.height) / 2);
        frame.setVisible(true);
        frame.setResizable(false);
    }
}