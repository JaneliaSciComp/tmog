/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer;

import org.janelia.it.ims.imagerenamer.config.InputFileFilter;
import org.janelia.it.ims.imagerenamer.config.ProjectConfiguration;
import org.janelia.it.ims.imagerenamer.config.output.OutputDirectoryConfiguration;
import org.janelia.it.ims.imagerenamer.field.ButtonEditor;
import org.janelia.it.ims.imagerenamer.field.ButtonRenderer;
import org.janelia.it.ims.imagerenamer.field.FileRenderer;
import org.janelia.it.ims.imagerenamer.field.ValidValueEditor;
import org.janelia.it.ims.imagerenamer.field.ValidValueModel;
import org.janelia.it.ims.imagerenamer.field.ValidValueRenderer;
import org.janelia.it.ims.imagerenamer.field.VerifiedFieldEditor;
import org.janelia.it.ims.imagerenamer.field.VerifiedFieldModel;
import org.janelia.it.ims.imagerenamer.field.VerifiedFieldRenderer;
import org.janelia.it.ims.imagerenamer.filefilter.DirectoryOnlyFilter;
import org.janelia.it.ims.imagerenamer.plugin.CopyListener;
import org.janelia.it.ims.imagerenamer.plugin.SessionListener;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileFilter;
import java.net.URL;

/**
 * This class manages the main or overall view for renaming a set of
 * files in a particular directory.
 *
 * @author Peter Davies
 * @author Eric Trautman
 */
public class MainView {

    private File lsmDirectory;
    private JLabel lsmDirectoryField;
    private JButton lsmDirectoryBtn;
    private JButton outputDirectoryBtn;
    private JLabel outputDirectoryField;
    private JPanel appPanel;
    @SuppressWarnings({"UnusedDeclaration"})
    private JPanel directoryPanel; // referenced by MainView.form
    private JButton copyAndRenameBtn;
    private JTable fileTable;
    private JProgressBar copyProgressBar;
    private JLabel copyProgressLabel;
    private JLabel projectLabel;
    private FileTableModel tableModel;
    private ProjectConfiguration projectConfig;
    private MainView thisMainView;
    private CopyAndRenameTask task;
    private boolean isRenameTaskInProgress;
    private ImageIcon viewIcon;
    private JTabbedPane parentTabbedPane;

    public MainView(ProjectConfiguration projectConfig,
                    File lsmDirectory,
                    JTabbedPane parentTabbedPane) {
        thisMainView = this;
        this.projectConfig = projectConfig;
        this.lsmDirectory = lsmDirectory;
        this.parentTabbedPane = parentTabbedPane;
        this.viewIcon = new ImageIcon(ENTER_VALUES_ICON.getImage());

        this.projectLabel.setText(projectConfig.getName());
        setupFileTable();
        setupInputDirectory();
        setupOutputDirectory();
        setupProcess();
    }

    public JPanel getPanel() {
        return appPanel;
    }

    public File getLsmDirectory() {
        return lsmDirectory;
    }

    public JLabel getLsmDirectoryField() {
        return lsmDirectoryField;
    }

    public JLabel getOutputDirectoryField() {
        return outputDirectoryField;
    }

    public JTable getFileTable() {
        return fileTable;
    }

    public JProgressBar getCopyProgressBar() {
        return copyProgressBar;
    }

    public JLabel getCopyProgressLabel() {
        return copyProgressLabel;
    }

    public FileTableModel getTableModel() {
        return tableModel;
    }

    public boolean isRenameTaskInProgress() {
        return isRenameTaskInProgress;
    }

    public void setRenameTaskInProgress(boolean renameTaskInProgress,
                                        boolean updateIcon) {
        this.isRenameTaskInProgress = renameTaskInProgress;
        if (isRenameTaskInProgress) {
            copyAndRenameBtn.setText(RENAME_CANCEL_BUTTON_TEXT);
            copyAndRenameBtn.setToolTipText(RENAME_CANCEL_TOOL_TIP_TEXT);
            if (updateIcon) {
                setViewIconToWait();
            }
        } else {
            copyAndRenameBtn.setText(RENAME_START_BUTTON_TEXT);
            copyAndRenameBtn.setToolTipText(RENAME_START_TOOL_TIP_TEXT);
            task = null;
            if (updateIcon) {
                setViewIconToEnterValues();
            }
        }
    }

    public ImageIcon getViewIcon() {
        return viewIcon;
    }

    public void setViewIconToWait() {
        setViewLabelIcon(WAIT_ICON);
    }
    public void setViewIconToProcessing() {
        setViewLabelIcon(PROCESSING_ICON);
    }
    public void setViewIconToEnterValues() {
        setViewLabelIcon(ENTER_VALUES_ICON);
    }
    private void setViewLabelIcon(ImageIcon imageIcon) {
        viewIcon.setImage(imageIcon.getImage());
        viewIcon.setDescription(imageIcon.getDescription());
        Graphics g = parentTabbedPane.getGraphics();
        if (g != null) { // only repaint if the icon has already been rendered
            parentTabbedPane.repaint();
        }
    }

    private void setupFileTable() {
        fileTable.setDefaultRenderer(File.class, new FileRenderer());
        fileTable.setDefaultRenderer(JButton.class, new ButtonRenderer());
        fileTable.setDefaultRenderer(ValidValueModel.class,
                                     new ValidValueRenderer());
        fileTable.setDefaultRenderer(VerifiedFieldModel.class,
                                     new VerifiedFieldRenderer());

        fileTable.setDefaultEditor(JButton.class, new ButtonEditor());
        fileTable.setDefaultEditor(ValidValueModel.class,
                                   new ValidValueEditor());
        fileTable.setDefaultEditor(VerifiedFieldModel.class,
                                   new VerifiedFieldEditor(appPanel));

        fileTable.getTableHeader().setReorderingAllowed(false);

        fileTable.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                int code = e.getKeyCode();
                if ((code == KeyEvent.VK_TAB) || code == KeyEvent.VK_RIGHT ||
                    code == KeyEvent.VK_LEFT || code == KeyEvent.VK_UP ||
                    code == KeyEvent.VK_DOWN) {
                    requestFocusForFileTableEditor(
                            fileTable.getEditorComponent());
                }
            }
        });

        fileTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                requestFocusForFileTableEditor(fileTable.getEditorComponent());
            }
        });
    }

    private void requestFocusForFileTableEditor(Component editor) {
        if (editor != null) {
            editor.requestFocus();
        } else {
            int row = fileTable.getSelectedRow();
            fileTable.changeSelection(row,
                                      FileTableModel.getFirstFieldColumn(),
                                      false,
                                      false);
        }
    }

    public void resetFileTable() {
        lsmDirectoryField.setText("");
        OutputDirectoryConfiguration odConfig =
                projectConfig.getOutputDirectory();
        if (odConfig.isDerivedFromEarliestModifiedFile()) {
            outputDirectoryField.setText("");
        }
        fileTable.setModel(new DefaultTableModel());
        setFileTableEnabled(true, false);
    }

    public void setFileTableEnabled(boolean isEnabled,
                                    boolean isCopyButtonEnabled) {
        Component[] cList = {
                lsmDirectoryBtn, outputDirectoryBtn, fileTable};
        for (Component c : cList) {
            if (isEnabled != c.isEnabled()) {
                c.setEnabled(isEnabled);
            }
        }

        if (copyAndRenameBtn.isEnabled() != isCopyButtonEnabled) {
            copyAndRenameBtn.setEnabled(isCopyButtonEnabled);
        }
    }

    private void setupInputDirectory() {
        lsmDirectoryBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                JPanel parentPanel = getPanel();
                JFileChooser fileChooser = new JFileChooser();
                if (lsmDirectory != null) {
                    File parentLsmDirectory = lsmDirectory.getParentFile();
                    if (parentLsmDirectory != null) {
                        fileChooser.setCurrentDirectory(parentLsmDirectory);
                    } else {
                        fileChooser.setCurrentDirectory(lsmDirectory);
                    }
                }
                fileChooser.setFileSelectionMode(
                        JFileChooser.FILES_AND_DIRECTORIES);
                int choice = fileChooser.showDialog(parentPanel,
                                                    "Select Source");

                if (choice == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    if ((selectedFile != null) &&
                        (!selectedFile.isDirectory())) {
                        selectedFile = selectedFile.getParentFile();
                    }

                    if (selectedFile != null) {
                        validateLsmDirectorySelection(selectedFile);
                    }
                }
            }
        });
    }

    private void validateLsmDirectorySelection(File selectedFile) {
        lsmDirectory = selectedFile;

        InputFileFilter inputFilter = projectConfig.getInputFileFilter();
        FileFilter fileFilter = inputFilter.getFilter();
        File[] files = lsmDirectory.listFiles(fileFilter);
        boolean acceptSelectedFile = (files.length > 0);
        OutputDirectoryConfiguration odConfig = projectConfig.getOutputDirectory();
        boolean isOutputDerivedFromSelection =
                odConfig.isDerivedFromEarliestModifiedFile();

        StringBuilder reject = new StringBuilder();
        String outputPath = null;
        if (acceptSelectedFile) {
            if (isOutputDerivedFromSelection) {
                outputPath = odConfig.getDerivedPathForEarliestFile(
                        lsmDirectory, files);
                File derivedOutputDir = new File(outputPath);
                boolean outputDirExists = derivedOutputDir.exists();
                if (outputDirExists) {
                    acceptSelectedFile =
                            derivedOutputDir.canWrite();
                } else {
                    File outputBaseDir =
                            derivedOutputDir.getParentFile();
                    acceptSelectedFile =
                            (outputBaseDir != null) &&
                            outputBaseDir.canWrite();
                }
                if (!acceptSelectedFile) {
                    reject.append("The derived output directory for your selection (");
                    reject.append(outputPath);
                    if (outputDirExists) {
                        reject.append(") does not allow you write access.  ");
                    } else {
                        reject.append(") can not be created.  ");
                    }
                    reject.append("Please verify your access privileges and the ");
                    reject.append("constraints set for this filesystem.");
                }
            }
        } else {
            reject.append("The selected directory (");
            reject.append(lsmDirectory.getAbsolutePath());
            reject.append(") does not contain any files with names that match the pattern '");
            reject.append(inputFilter.getPatternString());
            reject.append("'.  Please choose another directory.");
        }

        if (acceptSelectedFile) {
            lsmDirectoryField.setText(
                    lsmDirectory.getAbsolutePath());
            if (isOutputDerivedFromSelection) {
                outputDirectoryField.setText(outputPath);
            }
            tableModel = new FileTableModel(files,
                                            projectConfig);
            fileTable.setModel(tableModel);
            sizeTable();
            copyAndRenameBtn.setEnabled(true);
        } else {
            JOptionPane.showMessageDialog(appPanel,
                                          reject.toString(),
                                          "Source File Directory Selection Error",
                                          JOptionPane.ERROR_MESSAGE);
            resetFileTable();
        }
    }

    private void setupOutputDirectory() {
        OutputDirectoryConfiguration odCfg = projectConfig.getOutputDirectory();
        boolean isManuallyChosen = odCfg.isManuallyChosen();
        outputDirectoryBtn.setVisible(isManuallyChosen);

        if (isManuallyChosen) {
            outputDirectoryBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.addChoosableFileFilter(
                            new DirectoryOnlyFilter(
                                    lsmDirectoryField.getText()));
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    fileChooser.showDialog(getPanel(), "Select Output Directory");
                    File selectedDirectory = fileChooser.getSelectedFile();
                    if (selectedDirectory != null) {
                        outputDirectoryField.setText(selectedDirectory.getPath());
                    }
                }
            });
        } else if (! odCfg.isDerivedFromEarliestModifiedFile()) {
            outputDirectoryField.setText(odCfg.getDescription());
        }

    }

    private void setupProcess() {
        copyAndRenameBtn.setEnabled(false);
        setRenameTaskInProgress(false, true);

        copyAndRenameBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (isRenameTaskInProgress) {
                    cancelSession();
                } else {
                    if (isSessionReadyToStart()) {
                        startSession();
                    }
                }
            }

            private void cancelSession() {
                task.cancelSession();
                setRenameTaskInProgress(false, false);
                copyAndRenameBtn.setText(RENAME_CANCELLED_BUTTON_TEXT);
                copyAndRenameBtn.setToolTipText(
                        RENAME_CANCELLED_TOOL_TIP_TEXT);
                copyAndRenameBtn.setEnabled(false);
            }

            private boolean isSessionReadyToStart() {
                boolean isReady = false;
                boolean isOutputDirectoryValid = true;

                fileTable.editCellAt(-1, -1); // stop any current editor
                OutputDirectoryConfiguration odCfg =
                        projectConfig.getOutputDirectory();
                File outputDirectory = null;
                if (odCfg.isDerivedForSession()) {
                    outputDirectory = new File(outputDirectoryField.getText());
                    String outputFailureMsg =
                            OutputDirectoryConfiguration.createAndValidateDirectory(
                                    outputDirectory);

                    if (outputFailureMsg != null) {
                        isOutputDirectoryValid = false;
                        JOptionPane.showMessageDialog(appPanel,
                                                      outputFailureMsg,
                                                      "Error",
                                                      JOptionPane.ERROR_MESSAGE);
                    }
                }

                if (isOutputDirectoryValid &&
                    tableModel.validateAllFields(fileTable,
                                                 projectConfig.getRowValidators(),
                                                 appPanel,
                                                 outputDirectory)) {
                    int choice =
                            JOptionPane.showConfirmDialog(
                                    appPanel,
                                    "Your entries have been validated.  Do you wish to continue?",
                                    "Continue with Rename?",
                                    JOptionPane.YES_NO_OPTION);

                    isReady = (choice == JOptionPane.YES_OPTION);
                }

                return isReady;
            }

            private void startSession() {
                setFileTableEnabled(false, true);
                task = new CopyAndRenameTask(thisMainView,
                                             projectConfig.getOutputDirectory(),
                                             outputDirectoryField.getText());
                for (CopyListener listener :
                        projectConfig.getCopyListeners()) {
                    task.addCopyListener(listener);
                }
                for (SessionListener listener :
                        projectConfig.getSessionListeners()) {
                    task.addSessionListener(listener);
                }
                setRenameTaskInProgress(true, true);
                ImageRenamer.submitTask(task);
            }
        });
        
        copyProgressBar.setVisible(false);
        copyProgressLabel.setVisible(false);
    }

    private void sizeTable() {
        final int numColumns = tableModel.getColumnCount();
        if (numColumns > 0) {
            final int cellMargin = 5;
            TableColumnModel colModel = fileTable.getColumnModel();
            colModel.setColumnMargin(cellMargin);
            TableColumn tableColumn;
            // strictly size the button columns
            final int copyButtonHeight = 20;
            final int copyButtonWidth = 20;
            for (int columnIndex = 0;
                 columnIndex < FileTableModel.FILE_COLUMN;
                 columnIndex++) {

                tableColumn = colModel.getColumn(columnIndex);
                int preferredWidth = copyButtonWidth + (2 * cellMargin);
                tableColumn.setMinWidth(preferredWidth);
                tableColumn.setMaxWidth(preferredWidth);
                tableColumn.setPreferredWidth(preferredWidth);
            }

            // set preferred sizes for all other columns
            for (int columnIndex = FileTableModel.FILE_COLUMN;
                 columnIndex < numColumns;
                 columnIndex++) {

                tableColumn = colModel.getColumn(columnIndex);
                String longestValue = tableModel.getLongestValue(columnIndex);
                if (longestValue.length() > 0) {
                    JLabel text = new JLabel(longestValue);
                    text.setFont(fileTable.getFont());
                    Dimension dimension = text.getPreferredSize();
                    int preferredWidth = dimension.width + (2 * cellMargin);
                    tableColumn.setPreferredWidth(preferredWidth);
                }
            }

            int preferredHeight = copyButtonHeight + (2 * cellMargin);
            fileTable.setRowHeight(preferredHeight);
            fileTable.setRowMargin(cellMargin);
        }
    }

    private static final String RENAME_START_BUTTON_TEXT = "Copy and Rename";
    private static final String RENAME_START_TOOL_TIP_TEXT =
            "Copy and rename all files using specified information";
    private static final String RENAME_CANCEL_BUTTON_TEXT =
            "Cancel Rename In Progress";
    private static final String RENAME_CANCEL_TOOL_TIP_TEXT =
            "Cancel the renaming process that is currently running";
    private static final String RENAME_CANCELLED_BUTTON_TEXT =
            "Rename Session Cancelled";
    private static final String RENAME_CANCELLED_TOOL_TIP_TEXT =
            "Waiting for current file processing to complete";

    private static final URL ENTER_VALUES_IMAGE_URL =
            MainView.class.getResource("/16-em-pencil.png");
    private static final ImageIcon ENTER_VALUES_ICON =
            new ImageIcon(ENTER_VALUES_IMAGE_URL, "Entering Values");

    private static final URL WAIT_IMAGE_URL =
            MainView.class.getResource("/16-clock.png");
    private static final ImageIcon WAIT_ICON =
            new ImageIcon(WAIT_IMAGE_URL, "Waiting to Begin Processing");

    private static final URL PROCESSING_IMAGE_URL =
            MainView.class.getResource("/16-spinner.gif");
    private static final ImageIcon PROCESSING_ICON =
            new ImageIcon(PROCESSING_IMAGE_URL, "Processing Files");
}