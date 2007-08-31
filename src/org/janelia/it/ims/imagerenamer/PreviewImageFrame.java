package org.janelia.it.ims.imagerenamer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.File;


public class PreviewImageFrame extends JFrame {
    private JLabel label;
    private JSpinner spinner;

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
        label.setText("Loading Image");
        panel.add(label, BorderLayout.CENTER);

        PreviewImageTask previewImageTask = new PreviewImageTask(filePath, previewImageSize, 0, label);
        previewImageTask.execute();

        JPanel subPanel = new JPanel();

        SpinnerModel spinnerModel = new SpinnerNumberModel(0, 0, 1000, 1);
        spinner = new JSpinner(spinnerModel);
        JComponent editor = spinner.getEditor();
        subPanel.add(spinner);
        panel.add(subPanel, BorderLayout.SOUTH);
        if (editor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) editor).getTextField().setColumns(3);
        }


        spinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                PreviewImageTask previewImageTask = new PreviewImageTask(filePath, previewImageSize, (Integer) spinner.getValue(), label);
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
