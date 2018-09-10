/*
 * Copyright (c) 2015 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.impl.filter.StringTextFilterator;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;
import org.apache.log4j.Logger;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.view.component.NarrowOptionPane;
import org.janelia.it.ims.tmog.view.loader.DataSetLoader;
import org.janelia.it.ims.tmog.view.loader.SlideImageDataLoader;
import org.janelia.it.ims.tmog.view.loader.SlideLoader;
import org.janelia.it.utils.BackgroundWorker;

import javax.swing.*;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Set;

/**
 * A dialog window for selecting slide images.
 *
 * @author Eric Trautman
 */
public class SelectSlideDialog
        extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox<String> dataSetComboBox;
    private JComboBox<String> slideComboBox;
    private JComboBox<String> objectiveComboBox;
    private JLabel spinnerLabel;

    private String family;
    private InputSelectionView inputSelectionView;
    private JTextArea directoryField;

    private DataSetLoader dataSetLoader;
    private SlideLoader slideLoader;
    private EventList<String> slideList;
    private SlideImageDataLoader slideImageDataLoader;

    public SelectSlideDialog(final String family,
                             final InputSelectionView inputSelectionView,
                             final JTextArea directoryField) {

        this.family = family;
        this.inputSelectionView = inputSelectionView;
        this.directoryField = directoryField;
        this.slideList = null;

        setTitle("Load SAGE Data");
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
        buttonOK.setEnabled(false);

        dataSetComboBox.setEnabled(false);

        dataSetComboBox.addActionListener(new AutoCompleteActionListener() {
            @Override
            public void selectionPerformed() {

                final Object selectedItem = dataSetComboBox.getSelectedItem();
                final String dataSet = String.valueOf(selectedItem);

                // make sure a valid data set was selected (possible to type invalid data set with auto complete)
                if (dataSetLoader.hasDataSet(dataSet)) {

                    slideComboBox.setEnabled(false);
                    slideLoader = new SlideLoader(family, dataSet);
                    slideLoader.addPropertyChangeListener(new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {

                            if (slideLoader.isDoneEvent(evt)) {

                                spinnerLabel.setVisible(false);

                                if (slideLoader.hasFailed()) {

                                    logAndDisplayError("Slide Retrieval Failure",
                                                       "slides",
                                                       slideLoader.getFailureCause());

                                } else if (!slideLoader.isCancelled()) {

                                    if (slideLoader.foundSlides()) {

                                        setSlides(slideLoader.getSlides());

                                    } else {

                                        NarrowOptionPane.showMessageDialog(
                                                slideComboBox,
                                                "No slides were found for '" + family +
                                                "' images with the data set '" + dataSet + "'.  \n" +
                                                "Please select a different data set.",
                                                "No Slides Found",
                                                JOptionPane.WARNING_MESSAGE);

                                    }
                                }
                            }

                        }
                    });

                    spinnerLabel.setText("loading slides");
                    spinnerLabel.setVisible(true);
                    slideLoader.submitTask();
                }
            }
        });

        slideComboBox.setEnabled(false);
        slideComboBox.addActionListener(new AutoCompleteActionListener() {
            @Override
            public void selectionPerformed() {
                final Object selectedItem = slideComboBox.getSelectedItem();
                final String slide = String.valueOf(selectedItem);
                if (slideLoader.hasSlide(slide)) {
                    objectiveComboBox.setEnabled(false);
                    objectiveComboBox.removeAllItems();
                    for (String objective : slideLoader.getObjectiveList(slide)) {
                        objectiveComboBox.addItem(objective);
                    }
                    objectiveComboBox.setEnabled(true);
                    buttonOK.setEnabled(true);
                    objectiveComboBox.requestFocus();
                }
            }
        });

        objectiveComboBox.setEnabled(false);

        spinnerLabel.setVisible(true);
        spinnerLabel.setText("loading data sets");

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when window is closed
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        dataSetLoader = new DataSetLoader();
        dataSetLoader.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {

                if (dataSetLoader.isDoneEvent(evt)) {

                    spinnerLabel.setVisible(false);

                    if (dataSetLoader.hasFailed()) {

                        logAndDisplayError("Data Set Retrieval Failure",
                                           "data sets",
                                           dataSetLoader.getFailureCause());
                        onCancel();

                    } else if (!dataSetLoader.isCancelled()) {

                        setDataSets(dataSetLoader.getDataSetNames());

                    }
                }
            }
        });

        // load the data sets ...
        dataSetLoader.submitTask();
    }

    private static String getSelectedItem(JComboBox comboBox) {
        String value = null;
        if (comboBox.getSelectedIndex() > -1) {
            value = String.valueOf(comboBox.getSelectedItem());
        }
        return value;
    }

    private void onOK() {

        final String dataSet = getSelectedItem(dataSetComboBox);
        final String slide = getSelectedItem(slideComboBox);
        final String objective = getSelectedItem(objectiveComboBox);

        if ((dataSet != null) && (slide != null) && (objective != null)) {

            buttonOK.setEnabled(false);

            slideImageDataLoader = new SlideImageDataLoader(family, dataSet, slide, objective);

            slideImageDataLoader.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {

                    if (slideImageDataLoader.isProgressEvent(evt)) {

                        handleImagePathLoaderUpdate(evt);

                    } else if (slideImageDataLoader.isDoneEvent(evt)) {

                        spinnerLabel.setVisible(false);

                        if (slideImageDataLoader.hasFailed()) {

                            logAndDisplayError("Image Path Retrieval Failure",
                                               "image paths",
                                               slideImageDataLoader.getFailureCause());

                        } else if (!slideImageDataLoader.isCancelled()) {

                            setImagePaths(slideImageDataLoader.getImagePaths());

                        }
                    }
                }
            });

            spinnerLabel.setText("retrieving image paths for slide " + slide);
            spinnerLabel.setVisible(true);

            slideImageDataLoader.submitTask();

        } else {
            NarrowOptionPane.showMessageDialog(
                    this,
                    "Please select a data set, slide, and objective.",
                    "Missing Selection",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleImagePathLoaderUpdate(PropertyChangeEvent evt) {
        Object value = evt.getNewValue();
        if (value instanceof List) {
            List list = (List) value;
            int size = list.size();
            if (size > 0) {
                Object lastItem = list.get(size - 1);
                if (lastItem instanceof String) {
                    spinnerLabel.setText((String) lastItem);
                }
            }
        }
    }

    private void onCancel() {
        cancelBackgroundWorker(dataSetLoader);
        cancelBackgroundWorker(slideLoader);
        cancelBackgroundWorker(slideImageDataLoader);

        slideComboBox.setSelectedIndex(-1);
        objectiveComboBox.setSelectedIndex(-1);

        dispose();
    }

    private void cancelBackgroundWorker(BackgroundWorker worker) {
        if (worker != null) {
            try {
                worker.cancel(true);
            } catch (Throwable t) {
                LOG.debug("caught exception cancelling background worker", t);
            }
        }
    }

    private void logAndDisplayError(String title,
                                    String context,
                                    Throwable failureCause) {
        LOG.error(failureCause);
        NarrowOptionPane.showMessageDialog(
                this,
                "The following error occurred when attempting to retrieve " + context + ":\n" +
                failureCause.getMessage(),
                title,
                JOptionPane.ERROR_MESSAGE);
    }

    private void setDataSets(final Set<String> dataSetNames) {

        final EventList<String> dataSetList = GlazedLists.eventList(dataSetNames);
        final AutoCompleteSupport support = AutoCompleteSupport.install(dataSetComboBox,
                                                                        dataSetList,
                                                                        new StringTextFilterator<String>());
        support.setFilterMode(TextMatcherEditor.CONTAINS);

        dataSetComboBox.setEnabled(true);
        dataSetComboBox.requestFocus();
    }

    private void setSlides(final Set<String> slides) {

        if (slideList != null) {
            slideList.clear();
            slideComboBox.setEnabled(false);
        }

        if (slides.size() > 0) {

            if (slideList == null) {

                slideList = GlazedLists.eventList(slides);
                final AutoCompleteSupport support = AutoCompleteSupport.install(slideComboBox,
                                                                                slideList,
                                                                                new StringTextFilterator<String>());
                support.setFilterMode(TextMatcherEditor.CONTAINS);

            } else {

                slideList.addAll(GlazedLists.eventList(slides));

            }

            slideComboBox.setEnabled(true);
            slideComboBox.requestFocus();

        }

    }

    private void setImagePaths(final List<FileTarget> imagePaths) {

        try {
            spinnerLabel.setText("setting image defaults");

            directoryField.setText("Data Set:  " + slideImageDataLoader.getDataSet() +
                                   "     Slide:  " + slideImageDataLoader.getSlide() +
                                   "     Objective:  " + slideImageDataLoader.getObjective());

            directoryField.setToolTipText(getToolTip(imagePaths.size()));

            inputSelectionView.processInputTargets(imagePaths);

            onCancel();

        } catch (Exception e) {
            logAndDisplayError("Image Path Retrieval Failure",
                               "image paths",
                               slideImageDataLoader.getFailureCause());
        }
    }

    private String getToolTip(int numTargets) {
        StringBuilder toolTip = new StringBuilder(64);
        toolTip.append(numTargets);
        toolTip.append(" image");
        if (numTargets > 1) {
            toolTip.append("s");
        }
        toolTip.append(" found");
        return toolTip.toString();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer >>> IMPORTANT!! <<< DO NOT edit this method OR call it in your
     * code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                    GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                    GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                               GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                               GridConstraints.SIZEPOLICY_CAN_GROW,
                                               GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                               GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("Load Image Data");
        buttonOK.setMnemonic('L');
        buttonOK.setDisplayedMnemonicIndex(0);
        panel2.add(buttonOK, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                 GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                 GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        buttonCancel.setMnemonic('C');
        buttonCancel.setDisplayedMnemonicIndex(0);
        panel2.add(buttonCancel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                     GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                     GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(5, 2, new Insets(10, 10, 10, 10), -1, 20));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                    GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                    GridConstraints.SIZEPOLICY_CAN_GROW,
                                                    GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                    GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Data Set:");
        label1.setDisplayedMnemonic('D');
        label1.setDisplayedMnemonicIndex(0);
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(56, 28), null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel3.add(spacer2, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Slide:");
        label2.setDisplayedMnemonic('S');
        label2.setDisplayedMnemonicIndex(0);
        panel3.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Objective:");
        label3.setDisplayedMnemonic('O');
        label3.setDisplayedMnemonicIndex(0);
        panel3.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dataSetComboBox = new JComboBox();
        panel3.add(dataSetComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(400, -1), new Dimension(52, 28), null, 0, false));
        slideComboBox = new JComboBox();
        panel3.add(slideComboBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        objectiveComboBox = new JComboBox();
        panel3.add(objectiveComboBox, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        spinnerLabel = new JLabel();
        spinnerLabel.setIcon(new ImageIcon(getClass().getResource("/16-spinner.gif")));
        spinnerLabel.setText("loading data sets");
        panel3.add(spinnerLabel, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(600, -1), null, null, 0, false));
        label1.setLabelFor(dataSetComboBox);
        label2.setLabelFor(slideComboBox);
        label3.setLabelFor(objectiveComboBox);
    }

    /** @noinspection ALL */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

    private abstract class AutoCompleteActionListener
            implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {

            // comboBoxChange events are fired when using arrow keys to scroll list and when clicking on an entry,
            // so we need to distinguish between the two actions by looking for the button modifier
            final boolean wasButtonClicked = "comboBoxChanged".equals(e.getActionCommand()) &&
                                             ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0);

            // hitting enter after scrolling fires the comboBoxEdited event
            final boolean isSelectionDone = wasButtonClicked || "comboBoxEdited".equals(e.getActionCommand());

            if (isSelectionDone) {
                selectionPerformed();
            }
        }

        public abstract void selectionPerformed();
    }

    private static final Logger LOG = Logger.getLogger(SelectSlideDialog.class);
}
