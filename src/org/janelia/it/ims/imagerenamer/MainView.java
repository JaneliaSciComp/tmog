/*
 * Copyright © 2007 Howard Hughes Medical Institute. 
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.imagerenamer;

import org.apache.log4j.Logger;
import org.janelia.it.ims.imagerenamer.config.OutputDirectory;
import org.janelia.it.ims.imagerenamer.config.ProjectConfiguration;
import org.janelia.it.ims.imagerenamer.field.*;
import org.janelia.it.ims.imagerenamer.filefilter.DirectoryOnlyFilter;
import org.janelia.it.ims.imagerenamer.filefilter.FileNameExtensionFilter;
import org.janelia.it.ims.imagerenamer.plugin.CopyListener;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileFilter;

/**
 * This class manages the main or overall view for renaming a set of
 * files in a particular directory.
 *
 * @author Peter Davies
 * @author Eric Trautman
 */
public class MainView {

    /**
     * The logger for this class.
     */
    private static final Logger LOG = Logger.getLogger(MainView.class);

    public static final String LSM_EXTENSION = ".lsm";
    private static final FileFilter LSM_FILE_FILTER =
            new FileNameExtensionFilter(LSM_EXTENSION);

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
    private boolean isRenameTaskInProgress;

    public MainView(ProjectConfiguration projectConfig,
                    File lsmDirectory) {
        thisMainView = this;
        this.projectConfig = projectConfig;
        this.lsmDirectory = lsmDirectory;

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

    public void setRenameTaskInProgress(boolean renameTaskInProgress) {
        isRenameTaskInProgress = renameTaskInProgress;
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
        if (!projectConfig.isOutputDirectoryManuallyChosen()) {
            outputDirectoryField.setText("");
        }
        fileTable.setModel(new DefaultTableModel());
        setFileTableEnabled(true);
        copyAndRenameBtn.setEnabled(false);
    }

    public void setFileTableEnabled(boolean isEnabled) {
        Component[] cList = {
                lsmDirectoryBtn, outputDirectoryBtn, fileTable, copyAndRenameBtn};
        for (Component c : cList) {
            if (isEnabled != c.isEnabled()) {
                c.setEnabled(isEnabled);
            }
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
        File[] files = lsmDirectory.listFiles(LSM_FILE_FILTER);
        boolean acceptSelectedFile = (files.length > 0);
        boolean isOutputDerived =
                (!projectConfig.isOutputDirectoryManuallyChosen());

        StringBuilder reject = new StringBuilder();
        String outputPath = null;
        if (acceptSelectedFile) {
            if (isOutputDerived) {
                OutputDirectory outputCfg =
                        projectConfig.getOutputDirectory();
                outputPath =
                        outputCfg.getDerivedPath(lsmDirectory,
                                files);
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
            reject.append(") does not contain any LSM files.  ");
            reject.append("Please choose another directory.");
        }

        if (acceptSelectedFile) {
            lsmDirectoryField.setText(
                    lsmDirectory.getAbsolutePath());
            if (isOutputDerived) {
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
                    "LSM Directory Selection Error",
                    JOptionPane.ERROR_MESSAGE);
            resetFileTable();
        }
    }

    private void setupOutputDirectory() {
        outputDirectoryBtn.setVisible(
                projectConfig.isOutputDirectoryManuallyChosen());
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
    }

    private void setupProcess() {
        copyAndRenameBtn.setEnabled(false);
        copyAndRenameBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileTable.editCellAt(-1, -1); // stop any current editor                
                String outputDirectoryName = outputDirectoryField.getText();
                File outputDirectory = new File(outputDirectoryName);
                String outputFailureMsg = null;
                if (!outputDirectory.exists()) {
                    try {
                        outputDirectory.mkdir();
                    } catch (Exception e1) {
                        outputFailureMsg =
                                "Failed to create output directory " +
                                        outputDirectory.getAbsolutePath() + ".";
                        LOG.error(outputFailureMsg, e1);
                    }
                }
                if (!outputDirectory.isDirectory()) {
                    outputFailureMsg =
                            "The output directory must be set to a valid directory.";
                }

                if (outputFailureMsg != null) {

                    JOptionPane.showMessageDialog(appPanel,
                            outputFailureMsg,
                            "Error",
                            JOptionPane.ERROR_MESSAGE);

                } else if (tableModel.validateAllFields(
                        fileTable,
                        projectConfig.getRowValidators(),
                        appPanel,
                        outputDirectory)) {
                    setFileTableEnabled(false);
                    CopyAndRenameTask task =
                            new CopyAndRenameTask(thisMainView);
                    for (CopyListener listener :
                            projectConfig.getCopyListeners()) {
                        task.addCopyListener(listener);
                    }
                    setRenameTaskInProgress(true);
                    ImageRenamer.getExecutorService().submit(task);
                    //task.execute();
                }
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
}