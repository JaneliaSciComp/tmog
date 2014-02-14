/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.view.component;

import org.janelia.it.ims.tmog.DataTableModel;
import org.janelia.it.ims.tmog.TransmogrifierTableModel;
import org.janelia.it.ims.tmog.config.preferences.ColumnDefault;
import org.janelia.it.ims.tmog.config.preferences.ColumnDefaultSet;
import org.janelia.it.ims.tmog.config.preferences.TransmogrifierPreferences;
import org.janelia.it.ims.tmog.config.preferences.ViewDefault;
import org.janelia.it.ims.tmog.field.AutoCompleteEditor;
import org.janelia.it.ims.tmog.field.ButtonEditor;
import org.janelia.it.ims.tmog.field.ButtonRenderer;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.DataFieldGroupEditor;
import org.janelia.it.ims.tmog.field.DataFieldGroupModel;
import org.janelia.it.ims.tmog.field.DataFieldGroupRenderer;
import org.janelia.it.ims.tmog.field.TargetRenderer;
import org.janelia.it.ims.tmog.field.ValidValue;
import org.janelia.it.ims.tmog.field.ValidValueEditor;
import org.janelia.it.ims.tmog.field.ValidValueModel;
import org.janelia.it.ims.tmog.field.ValidValueRenderer;
import org.janelia.it.ims.tmog.field.VerifiedFieldEditor;
import org.janelia.it.ims.tmog.field.VerifiedFieldModel;
import org.janelia.it.ims.tmog.field.VerifiedFieldRenderer;
import org.janelia.it.ims.tmog.target.FileTarget;
import org.janelia.it.ims.tmog.task.TaskProgressInfo;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

/**
 * This component supports the display of a data table driven by
 * a configurable model.
 *
 * @author Eric Trautman
 */
public class DataTable extends JTable {

    /** {@link javax.swing.plaf.basic.BasicTableUI.Actions} */
    protected static final String NEXT_COLUMN_CELL = "selectNextColumnCell";
    protected static final String PREVIOUS_COLUMN_CELL =
            "selectPreviousColumnCell";

    protected static final int CELL_MARGIN = 5;
    protected static final int ROW_WITH_MARGIN_HEIGHT =
            ButtonPanel.BUTTON_HEIGHT + (2 * CELL_MARGIN);

    /**
     * List of column classes that have default renderers and/or editors.
     * This list allows {@link NestedDataTable} instances to reuse
     * parent data table renderer and editor instances.
     */
    protected static final Class[] columnClasses = {
            FileTarget.class,
            ButtonPanel.ButtonType.class,
            ValidValueModel.class,
            VerifiedFieldModel.class,
            DataFieldGroupModel.class
    };

    /**
     * Width used for minimized columns that should not grow
     * when other columns are resized.
     */
    private static final int MINIMIZED_COLUMN_WIDTH = 20;

    /** The listener for this table's keyboard shortcuts. */
    private KeyListener keyListener;

    /** The default settings for this table's columns. */
    private ColumnDefaultSet columnDefaults;

    /**
     * Constructs an empty data table.
     */
    public DataTable() {
        this(true);
    }

    /**
     * Constructs an empty data table.
     *
     * @param  setDefaultRenderersAndEditors  flag indicating whether default
     *                                        renderers and editors should be
     *                                        set.  This allows nested data
     *                                        tables to reuse parent renderer
     *                                        and editor instances.
     */
    protected DataTable(boolean setDefaultRenderersAndEditors) {
        super();

        setBackground(UIManager.getColor("Label.background"));
        this.columnDefaults = null;

        TableColumnModel columnModel = getColumnModel();
        DataTableHeader tableHeader = new DataTableHeader(columnModel);
        setTableHeader(tableHeader);

        if (setDefaultRenderersAndEditors) {
            // NOTE: when adding/removing renderers and editors,
            // be sure to keep the columnClasses defined above in sync
            setDefaultRenderer(FileTarget.class,
                               new TargetRenderer());
            setDefaultRenderer(ButtonPanel.ButtonType.class,
                               new ButtonRenderer());
            setDefaultRenderer(ValidValueModel.class,
                               new ValidValueRenderer());
            setDefaultRenderer(VerifiedFieldModel.class,
                               new VerifiedFieldRenderer());
            setDefaultRenderer(DataFieldGroupModel.class,
                               new DataFieldGroupRenderer());

            setDefaultEditor(ButtonPanel.ButtonType.class,
                             new ButtonEditor());
            setDefaultEditor(ValidValueModel.class,
                             new ValidValueEditor());
            setDefaultEditor(VerifiedFieldModel.class,
                             new VerifiedFieldEditor());
            setDefaultEditor(DataFieldGroupModel.class,
                             new DataFieldGroupEditor());
        }

        ActionMap actionMap = getActionMap();
        Action nextColumnCellAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                selectAndEditNextCell();
            }
        };
        actionMap.put(NEXT_COLUMN_CELL, nextColumnCellAction);

        Action previousColumnCellAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                selectAndEditPreviousCell();
            }
        };
        actionMap.put(PREVIOUS_COLUMN_CELL, previousColumnCellAction);

        addKeyListener(getKeyListener());

        tableHeader.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                handleHeaderClick(e);
            }

        });
    }

    /**
     * This works around an old bug
     * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4127936">
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4127936</a>
     * in {@link JTable#getScrollableTracksViewportWidth}.
     *
     * Solution was posted at
     * <a href="http://www.daniweb.com/forums/thread29263.html">
     * http://www.daniweb.com/forums/thread29263.html</a>.
     *
     * @return true if the width of the viewport should determine the width
     *         of the table; otherwise false.
     */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        boolean tracksWidth = false;
        if (autoResizeMode != AUTO_RESIZE_OFF) {
            if (getParent() instanceof JViewport) {
                tracksWidth = (getParent().getWidth() >
                               getPreferredSize().width);
            }
        }
        return tracksWidth;
    }

    /**
     * Passes change events to JTable first and then resizes row
     * heights if necessary.
     *
     * @param  e  table change event.
     */
    @Override
    public void tableChanged(TableModelEvent e) {
        // save selected cell because it will get cleared by super.tableChanged
        final int previouslySelectedRow = getSelectedRow();
        final int previouslySelectedColumn = getSelectedColumn();

        // handle event normally
        super.tableChanged(e);

        // resize rows if the event came from a tmog model
        final Object source = e.getSource();
        //noinspection MagicConstant
        if (e.getType() == TransmogrifierTableModel.UPDATE_ROW_HEIGHTS) {
            if (isEditing()) {
                // stop editing so that edited cells get resized properly
                getCellEditor().stopCellEditing();
            }
            if (source instanceof TransmogrifierTableModel) {
                resizeAllRowHeights((TransmogrifierTableModel) getModel());
                restoreSelectionAfterResize(previouslySelectedRow,
                                            previouslySelectedColumn);
            }
        }
    }

    /**
     * Sets the data model for this table and then resizes all columns
     * and rows.
     *
     * @param  dataModel  table data model.
     */
    @Override
    public void setModel(TableModel dataModel) {
        super.setModel(dataModel);

        if (dataModel instanceof TransmogrifierTableModel) {
            TransmogrifierTableModel model =
                    (TransmogrifierTableModel) dataModel;

            DataTableHeader dtHeader = (DataTableHeader) getTableHeader();
            dtHeader.updateModel(model);
            resizeAllColumnWidths(model);
            resizeAllRowHeights(model);

            // TODO: assess impact of repeating these calls for nested field groups

            // explicitly set auto complete editor as needed
            final TableColumnModel columnModel = getColumnModel();
            TableColumn tableColumn;
            TableCellEditor editor;
            for (Integer column : model.getAutoCompleteColumns()) {
                tableColumn = columnModel.getColumn(column);
                editor = tableColumn.getCellEditor();
                if (editor == null) {
                    tableColumn.setCellEditor(new AutoCompleteEditor());
                }
            }
        }
    }

    /**
     * Applies column default information from the specified model's
     * project view preferences, sets the model for this table,
     * and then resizes all columns and rows.
     *
     * @param  dataModel  model for this table.
     */
    public void setModelAndColumnDefaults(DataTableModel dataModel) {

        ViewDefault viewDefault =
                TransmogrifierPreferences.getProjectViewPreferences(
                        dataModel.getProjectName());
        if ((viewDefault != null) && (viewDefault.hasColumnDefaults())) {
            setColumnDefaults(viewDefault.getColumnDefaultsCopy(), false);
        } else {
            setColumnDefaults(null, false);
        }

        setModel(dataModel);
    }

    /**
     * Displays drop-down menu if a valid value cell just gained focus
     * from a keyboard event.
     * 
     * @param  ks         the <code>KeyStroke</code> queried
     * @param  e          the <code>KeyEvent</code>
     * @param  condition  one of the following values:
     * <ul>
     * <li>JComponent.WHEN_FOCUSED
     * <li>JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
     * <li>JComponent.WHEN_IN_FOCUSED_WINDOW
     * </ul>
     * @param  pressed    true if the key is pressed
     * 
     * @return true if there was a binding to an action, and the action
     *         was enabled
     */
    @Override
    protected boolean processKeyBinding(KeyStroke ks,
                                        KeyEvent e,
                                        int condition,
                                        boolean pressed) {

        Component editor = this.getEditorComponent();
        if (editor instanceof JComboBox) {
            int keyCode = e.getKeyCode();
            if ((condition == WHEN_FOCUSED) && (! pressed)) {
                if ((keyCode == KeyEvent.VK_TAB) ||
                    (keyCode == KeyEvent.VK_LEFT) ||
                    (keyCode == KeyEvent.VK_RIGHT) ||
                    (keyCode == KeyEvent.VK_UP) ||
                    (keyCode == KeyEvent.VK_DOWN)) {
                    JComboBox comboBox = (JComboBox) editor;

                    if (! comboBox.isPopupVisible()) {
                        comboBox.requestFocusInWindow();
                        return true;
                    }
                }
            }
        }

        return super.processKeyBinding(ks, e, condition, pressed);
    }

    /**
     * Edits the currently selected cell.
     */
    public void editAndRequestFocusForSelectedCell() {
        int row = getSelectedRow();
        int column = getSelectedColumn();

        // don't re-edit selected cell if it is already being edited
        if ((! isEditing()) ||
            (row != getEditingRow()) ||
            (column != getEditingColumn())) {
            editCellAt(row, column);
        }

        Component editor = getEditorComponent();
        if (editor instanceof NestedDataTable) {
            NestedDataTable nestedTable = (NestedDataTable) editor;
            nestedTable.editAndRequestFocusForSelectedCell();
        } else if (editor != null) {
            editor.requestFocusInWindow();
        }
    }

    /**
     * Selects the first invalid cell identified by this table's model.
     */
    public void selectErrorCell() {
        TransmogrifierTableModel model = (TransmogrifierTableModel) getModel();
        Integer errorRow = model.getErrorRow();
        Integer errorColumn = model.getErrorColumn();
        if ((errorRow != null) && (errorColumn != null)) {
            changeSelection(errorRow, errorColumn, false, false);
            if (model.isNestedTableColumn(errorColumn)) {
                editCellAt(errorRow, errorColumn);
                Component editor = getEditorComponent();
                if (editor instanceof NestedDataTable) {
                    NestedDataTable nestedTable = (NestedDataTable) editor;
                    nestedTable.selectErrorCell();
                }
            }
        }
    }

    /**
     * Selects the specified row in this table.
     *
     * @param  rowIndex  row to be selected.
     */
    public void selectRow(int rowIndex) {
        TableModel model = getModel();
        if (model instanceof DataTableModel) {
            changeSelection(rowIndex,
                            ((DataTableModel) model).getTargetColumnIndex(),
                            false,
                            false);
        }
    }

    /**
     * @return the data panel parent for dialogs (so that centering doesn't
     *         get messed up by wide tables with horizontal scroll bars).
     */
    public Container getDialogParent() {
        Container parent = getParent();
        if (parent != null) {
            parent = parent.getParent();
            if (parent != null) {
                parent = parent.getParent();
            }
        }
        return parent;
    }

    /**
     * Displays a correction confirmation dialog for the specified field.
     *
     * @param  field  field that has failed validation.
     *
     * @return an integer indicating the option
     *         (e.g. {@link JOptionPane#YES_OPTION}) selected by the user.
     */
    public int showInvalidEntryConfimDialog(DataField field) {
        final String dialogMsg = field.getErrorMessage() +
                "  Would you like to correct the field now?";

        return NarrowOptionPane.showConfirmDialog(
                getDialogParent(),
                dialogMsg,
                "Invalid Entry", // title
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Displays the specified message in a dialog and requests focus
     * for the associated error cell.
     *
     * @param  message      error message to display.
     */
    public void displayErrorDialog(String message) {

        NarrowOptionPane.showMessageDialog(getDialogParent(),
                                           message, // field to display
                                           "Invalid Entry", // title
                                           JOptionPane.ERROR_MESSAGE);

        editAndRequestFocusForSelectedCell();
    }

    /**
     * Updates table row highlighting and (optionally) progress bar components
     * based upon the specified task progress information.
     *
     * @param  info               the latest task progress information.
     */
    public void updateProgress(TaskProgressInfo info) {
        changeSelection(info.getLastRowProcessed(), 1, false, false);
    }

    /**
     * @return a listener that will handle shortcut key events
     *         for editing this table's cells.
     */
    public KeyListener getKeyListener() {
        if (keyListener == null) {
            keyListener = new DataTableKeyListener(this);
        }
        return keyListener;
    }

    /**
     * Selects and edits the next "selectable" cell that follows the
     * currently selected cell.
     */
    public void selectAndEditNextCell() {
        int selectedRowIndex = getSelectedRow();
        int selectedColumnIndex = getSelectedColumn() + 1;

        TransmogrifierTableModel model =
                (TransmogrifierTableModel) getModel();

        final int numberOfRows = model.getRowCount();
        final int numberOfColumns = model.getColumnCount();

        if (selectedRowIndex == -1) {
            selectedRowIndex = 0;
            selectedColumnIndex = 0;
        }

        boolean isChangeNeeded = false;
        for (int columnsChecked = 0;
             columnsChecked < numberOfColumns;
             columnsChecked++) {

            if (! model.isSelectable(selectedColumnIndex)) {
                selectedColumnIndex++;
                if (selectedColumnIndex >= numberOfColumns) {
                    selectedColumnIndex = 0;
                    selectedRowIndex++;
                    if (selectedRowIndex >= numberOfRows) {
                        if (propagateSelection(true)) {
                            break;
                        } else {
                            selectedRowIndex = 0;
                        }
                    }
                }
            } else {
                isChangeNeeded = true;
                break;
            }

        }

        if (isChangeNeeded) {
            selectAndEditCell(selectedRowIndex,
                              selectedColumnIndex,
                              true);
        }
    }

    /**
     * Selects and edits the next "selectable" cell that precedes the
     * currently selected cell.
     */
    public void selectAndEditPreviousCell() {
        int selectedRowIndex = getSelectedRow();
        int selectedColumnIndex = getSelectedColumn() - 1;

        TransmogrifierTableModel model =
                (TransmogrifierTableModel) getModel();

        final int numberOfRows = model.getRowCount();
        final int numberOfColumns = model.getColumnCount();

        if (selectedRowIndex == -1) {
            selectedRowIndex = numberOfRows - 1;
            selectedColumnIndex = numberOfColumns - 1;
        }

        boolean isChangeNeeded = false;
        for (int columnsChecked = 0;
             columnsChecked < numberOfColumns;
             columnsChecked++) {

            if (! model.isSelectable(selectedColumnIndex)) {
                selectedColumnIndex--;
                if (selectedColumnIndex < 0) {
                    selectedColumnIndex = numberOfColumns - 1;
                    selectedRowIndex--;
                    if (selectedRowIndex < 0) {
                        if (propagateSelection(false)) {
                            break;
                        } else {
                            selectedRowIndex = numberOfRows - 1;
                        }
                    }
                }
            } else {
                isChangeNeeded = true;
                break;
            }

        }

        if (isChangeNeeded) {
            selectAndEditCell(selectedRowIndex,
                              selectedColumnIndex,
                              false);
        }
    }

    /**
     * @return the current defaults for this table or null if
     *         they do not exist.
     */
    public ColumnDefaultSet getColumnDefaults() {
        return columnDefaults;
    }

    /**
     * Sets the column defaults for this table.
     *
     * @param  columnDefaults  the defaults for this table or
     *                         null if the defaults should be cleared.
     *
     * @param  updateDisplay  true if the columns should be refreshed
     *                        immediately; false if they will be refreshed
     *                        by another process.
     */
    public void setColumnDefaults(ColumnDefaultSet columnDefaults,
                                  boolean updateDisplay) {
        this.columnDefaults = columnDefaults;

        TableModel model = getModel();
        if (model instanceof TransmogrifierTableModel) {
            final int numColumns = model.getColumnCount();
            final TableColumnModel colModel = getColumnModel();
            TableColumn col;
            Object headerValue;
            DataFieldGroupHeader headerGroup;
            ColumnDefault groupDefault;
            for (int i = 0; i < numColumns; i++) {
                col = colModel.getColumn(i);
                headerValue = col.getHeaderValue();
                if (headerValue instanceof DataFieldGroupHeader) {
                    headerGroup = (DataFieldGroupHeader) headerValue;
                    if (columnDefaults == null) {
                        groupDefault = null;
                    } else {
                        groupDefault = columnDefaults.getColumnDefault(
                                headerGroup.getGroupName());
                    }
                    headerGroup.setColumnDefault(groupDefault, updateDisplay);
                }
            }

            if (updateDisplay) {
                resizeAllColumnWidths((TransmogrifierTableModel) model);
                repaint();
            }
        }

    }

    /**
     * Sets this table's column defaults based upon the current view state.
     */
    public void setColumnDefaultsToCurrent() {

        columnDefaults = new ColumnDefaultSet();

        TableModel model = getModel();
        final int numColumns = model.getColumnCount();
        final TableColumnModel colModel = getColumnModel();
        TableColumn col;
        Object headerValue;
        String headerString;
        DataFieldGroupHeader headerGroup;
        ColumnDefault columnDefault;
        for (int i = 0; i < numColumns; i++) {
            col = colModel.getColumn(i);
            headerValue = col.getHeaderValue();
            if (headerValue instanceof String) {
                headerString = ((String) headerValue).trim();
                if (headerString.length() > 0) {
                    columnDefault = new ColumnDefault(headerString);
                    columnDefault.setWidth(col.getWidth());
                    columnDefaults.addColumnDefault(columnDefault);
                }
            } else if (headerValue instanceof DataFieldGroupHeader) {
                headerGroup = (DataFieldGroupHeader) headerValue;
                headerGroup.setColumnDefaultsToCurrent();
                columnDefault = headerGroup.getColumnDefault();
                columnDefaults.addColumnDefault(columnDefault);
            }
        }
    }

    /**
     * Sets this table's column defaults so that they proportionally fit
     * within the specified width.
     *
     * @param  fitWidth  expected total width for the table after
     *                   proportionally resizing its columns.
     */
    public void setColumnDefaultsToFit(int fitWidth) {

        TableModel model = getModel();
        if (model instanceof DataTableModel) {
            DataTableModel dataTableModel = (DataTableModel) model;

            setColumnDefaults(null, true); // clear any current defaults
            setColumnDefaultsToCurrent();  // mark preferred data sizes

            final int tableWidth = getWidth();
            final int scalableWidth =  getScalableColumnWidth(dataTableModel);
            final int fixedWidth = tableWidth - scalableWidth;
            final double fitRemaining = fitWidth - fixedWidth;
            final double factor = fitRemaining / (double) scalableWidth;
            columnDefaults.scale(factor);
            setColumnDefaults(columnDefaults, true);
        }
    }

    /**
     * @param  dataTableModel  the current table model.
     *
     * @return the total width of all scalable columns in the model.
     *         Button and target columns are not scalable.
     */
    protected int getScalableColumnWidth(DataTableModel dataTableModel) {
        int scalableWidth = 0;

        final int numColumns = dataTableModel.getColumnCount();
        final TableColumnModel colModel = getColumnModel();
        TableColumn tableColumn;
        for (int columnIndex = 0; columnIndex < numColumns; columnIndex++) {
            tableColumn = colModel.getColumn(columnIndex);

            if (dataTableModel.isNestedTableColumn(columnIndex)) {

                final Object headerValue = tableColumn.getHeaderValue();
                if (headerValue instanceof DataFieldGroupHeader) {
                    final DataFieldGroupHeader headerGroup =
                            (DataFieldGroupHeader) headerValue;
                    final ColumnDefault groupDefault =
                            headerGroup.getColumnDefault();
                    scalableWidth += groupDefault.getTotalWidth();
                }

            } else if (! (dataTableModel.isButtonColumn(columnIndex) ||
                          dataTableModel.isTargetColumn(columnIndex))) {

                scalableWidth += tableColumn.getWidth();

            }
        }

        return scalableWidth;
    }

    /**
     * Resizes this table's column widths based upon its data model.
     *
     * @param  tmogModel  the table's data model.
     */
    protected void resizeAllColumnWidths(TransmogrifierTableModel tmogModel) {

        int targetColumnIndex = -1;
        if (tmogModel instanceof DataTableModel) {
            targetColumnIndex =
                    ((DataTableModel) tmogModel).getTargetColumnIndex();
        }

        final int numColumns = tmogModel.getColumnCount();
        TableColumnModel colModel = getColumnModel();
        colModel.setColumnMargin(CELL_MARGIN);
        TableColumn tableColumn;
        int preferredWidth;
        boolean useDefaults;
        int maxWidth;
        for (int columnIndex = 0; columnIndex < numColumns; columnIndex++) {
            tableColumn = colModel.getColumn(columnIndex);
            // ignore defaults for the target column
            useDefaults = (columnIndex != targetColumnIndex);
            preferredWidth = getPreferredWidth(columnIndex, useDefaults);

            maxWidth = tableColumn.getMaxWidth();
            if (tmogModel.isButtonColumn(columnIndex)) {
                tableColumn.setMinWidth(preferredWidth);
                tableColumn.setMaxWidth(preferredWidth);
            } else if (preferredWidth == MINIMIZED_COLUMN_WIDTH) {
                // if column should be minimized (see handleHeaderClick below),
                // set max width so that it stays minimized
                tableColumn.setMaxWidth(MINIMIZED_COLUMN_WIDTH);
            } else  if (maxWidth == MINIMIZED_COLUMN_WIDTH) {
                // if column should be expanded (see handleHeaderClick below),
                // set max width to unlimited
                tableColumn.setMaxWidth(Integer.MAX_VALUE);
            }

            // set preferred width after max width has been "corrected"
            tableColumn.setPreferredWidth(preferredWidth);
        }
    }

    /**
     * @param  columnIndex  column to check.
     * @param  useDefaults  indicates if column default values should
     *                      be used when defined.
     *
     * @return the preferred width of the specified column.
     */
    protected int getPreferredWidth(int columnIndex,
                                    boolean useDefaults) {

        int preferredWidth;

        final int numRows = this.getRowCount();
        final JTableHeader tableHeader = getTableHeader();
        final TableColumnModel columnModel = tableHeader.getColumnModel();
        final TableColumn column = columnModel.getColumn(columnIndex);

        // ----------------------------------------
        // 1. Use explicit default width if defined.

        Integer defaultWidth = null;
        if (useDefaults &&
            (columnDefaults != null) && (columnDefaults.size() > 0)) {

            final Object headerValue = column.getHeaderValue();
            ColumnDefault columnDefault;
            if (headerValue instanceof String) {
                columnDefault = columnDefaults.getColumnDefault(
                        (String) headerValue);
                if (columnDefault != null) {
                    defaultWidth = columnDefault.getWidth();
                }
            } else if (headerValue instanceof DataFieldGroupHeader) {
                DataFieldGroupHeader headerGroup =
                        (DataFieldGroupHeader) headerValue;
                columnDefault = headerGroup.getColumnDefault();
                if (columnDefault != null) {
                    final ColumnDefaultSet columnDefaultSet =
                            columnDefault.getNestedColumnDefaults();
                    final int totalWidth = columnDefaultSet.getTotalWidth();
                    if (totalWidth > 0) {
                        defaultWidth = totalWidth;
                    }
                }
            }
        }

        if (defaultWidth != null) {
            return defaultWidth;
        }

        // ----------------------------------------
        // 2. Default preferred width to column header's preferred width.

        TableCellRenderer renderer = column.getHeaderRenderer();
        if (renderer == null) {
            renderer = tableHeader.getDefaultRenderer();
        }
        Object value = column.getHeaderValue();
        Component component;
        if (value == null) {
            // null header should identify a button column
            preferredWidth = ButtonPanel.BUTTON_WIDTH;
        } else {
            component = renderer.getTableCellRendererComponent(this,
                                                               value,
                                                               false,
                                                               false,
                                                               0,
                                                               columnIndex);
            preferredWidth = component.getPreferredSize().width;
        }

        // ----------------------------------------
        // 3. Look at configured display and actual widths for the column's
        //    first row value and update preferred width as needed.
        //    Optimize process by flagging completion for columns whose
        //    subsequent rows won't contain values with differing preferred
        //    widths.

        boolean foundPreferredSize = false;
        int width;
        if (numRows > 0) {
            value = getValueAt(0, columnIndex);

            if (value instanceof DataField) {
                Integer displayWidth = ((DataField) value).getDisplayWidth();
                if ((displayWidth != null) && (displayWidth > preferredWidth)) {
                    preferredWidth = displayWidth;
                }
            }

            if ((value == null) || (value instanceof ButtonPanel.ButtonType)) {
                if (ButtonPanel.BUTTON_WIDTH > preferredWidth) {
                    preferredWidth = ButtonPanel.BUTTON_WIDTH;
                }
                foundPreferredSize = true;
            } else if (value instanceof DataFieldGroupModel) {
                foundPreferredSize = true;
            } else {

                if (value instanceof ValidValueModel) {
                    final ValidValueModel validValueModel = (ValidValueModel) value;
                    component = new JComboBox<ValidValue>(validValueModel);
                    foundPreferredSize = true;
                } else {
                    renderer = getCellRenderer(0, columnIndex);
                    component =
                            renderer.getTableCellRendererComponent(this,
                                                                   value,
                                                                   false,
                                                                   false,
                                                                   0,
                                                                   columnIndex);
                }

                width = component.getPreferredSize().width;
                if (width > preferredWidth) {
                    preferredWidth = width;
                }
            }
        }

        // ----------------------------------------
        // 4. If we still need to check values in each row
        //    (e.g. for text fields with default values),
        //    loop through the remaining rows and update the
        //    preferred width as needed.

        if (! foundPreferredSize) {
            for (int rowIndex = 1; rowIndex < numRows; rowIndex++) {
                value = getValueAt(rowIndex, columnIndex);
                component = renderer.getTableCellRendererComponent(this,
                                                                   value,
                                                                   false,
                                                                   false,
                                                                   rowIndex,
                                                                   columnIndex);
                width = component.getPreferredSize().width;
                if (width > preferredWidth) {
                    preferredWidth = width;
                }
            }
        }

        return preferredWidth + (2 * CELL_MARGIN);
    }

    /**
     * Resizes this table's row heights based upon its data model.
     *
     * @param  tmogModel  the table's data model.
     */
    protected void resizeAllRowHeights(TransmogrifierTableModel tmogModel) {

        final int oldHeight = getRowHeight();
        if (oldHeight != ROW_WITH_MARGIN_HEIGHT) {
            setRowHeight(ROW_WITH_MARGIN_HEIGHT);
        }

        final int oldMargin = getRowMargin();
        if (oldMargin != CELL_MARGIN) {
            setRowMargin(CELL_MARGIN);
        }

        final Set<Integer> nestedTableColumns =
                tmogModel.getNestedTableColumns();
        if (nestedTableColumns.size() > 0) {
            final int numRows = tmogModel.getRowCount();
            for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
                resizeRowHeight(rowIndex, nestedTableColumns);
            }
        }
    }

    /**
     * Resize the height of the specified row by determining the maximum
     * number of rows nested within each of the parent row's nested table
     * columns.
     *
     * @param  rowIndex            index of the row to resize.
     * @param  nestedTableColumns  indexes of the nested table columns within
     *                             the row being resized.
     */
    protected void resizeRowHeight(int rowIndex,
                                   Set<Integer> nestedTableColumns) {
        int maxRowCount = 1;
        DataFieldGroupModel nestedModel;
        for (Integer columnIndex : nestedTableColumns) {
            nestedModel = (DataFieldGroupModel)
                    dataModel.getValueAt(rowIndex, columnIndex);

            // TODO: consider handling models nested within nested models
            //       skipping this since deeply nested models aren't needed
            //       now and don't really work well with the UI
            int rowCount = nestedModel.getRowCount();
            if (rowCount > maxRowCount) {
                maxRowCount = rowCount;
            }
        }

        // NOTE: addition of (CELL_MARGIN / 2) compensates for nested table
        //       insets? without showing bottom line
        final int preferredHeight = (maxRowCount * ROW_WITH_MARGIN_HEIGHT) +
                                    (CELL_MARGIN / 2);
        if (preferredHeight != getRowHeight(rowIndex)) {
            setRowHeight(rowIndex, preferredHeight);
        }
    }

    /**
     * This method provides a hook for subclasses to propagate
     * boundary selection events (select next from the last row/column and
     * select previous from the first row/column) rather than wrapping.
     * This default implementation does not propagate boundary selection
     * events.
     *
     * @param  isNext  true for select next events;
     *                 false for select previous events.
     *
     * @return true if the event was propagated (handled).
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected boolean propagateSelection(boolean isNext) {
        return false;
    }

    /**
     * This method is called after a table model update forces
     * the row heights to be resized (see {@link #tableChanged}).
     *
     * @param  row     row to select.
     * @param  column  column to select.
     */
    protected void restoreSelectionAfterResize(int row,
                                               int column) {
        final int rowCount = getRowCount();
        if (row >= rowCount) {
            row = rowCount - 1;
        }
        TransmogrifierTableModel model = (TransmogrifierTableModel) getModel();
        if (! model.isSelectable(column)) {
            column = -1;
        }
        changeSelection(row,
                        column,
                        false,
                        false);
    }

    /**
     * Selects and edits the specified cell.
     *
     * @param  row     row index for the cell.
     * @param  column  column index for the cell.
     * @param  isNext  indicates whether the selection resulted from a
     *                 next cell event (true) or a previous cell event (false).
     */
    protected void selectAndEditCell(int row,
                                     int column,
                                     boolean isNext) {
        boolean continueWithAction = true;
        TableCellEditor editor;
        if (isEditing()) {
            editor = getCellEditor();
            continueWithAction = editor.stopCellEditing();
        }

        if (continueWithAction) {
            changeSelection(row,
                            column,
                            false,
                            false);
            editAndRequestFocusForSelectedCell();
            editor = getCellEditor();
            if (editor instanceof DataFieldGroupEditor) {
                DataFieldGroupEditor dfge = (DataFieldGroupEditor) editor;
                dfge.selectDefaultCell(isNext);
            }
        }
    }

    /**
     * Supports column width toggle based upon double clicking the header
     * column border.
     *
     * @param  e  click event.
     */
    protected void handleHeaderClick(MouseEvent e) {
        if (e.getClickCount() == 2) {
            Point p = new Point(e.getPoint());

            JTableHeader tableHeader = getTableHeader();
            int columnIndex = tableHeader.columnAtPoint(p);
            Rectangle r = tableHeader.getHeaderRect(columnIndex);
            r.grow(-3, 0);
            if (! r.contains(p)) {
                p.x = p.x - 4;
                columnIndex = tableHeader.columnAtPoint(p);
                TableColumnModel columnModel =
                        tableHeader.getColumnModel();
                TableColumn column = columnModel.getColumn(columnIndex);
                // header clicks should override column defaults
                final boolean useDefaults = false;
                final int preferredWidth =
                        getPreferredWidth(columnIndex, useDefaults);
                if (column.getWidth() > MINIMIZED_COLUMN_WIDTH) {
                    // set max width so that column stays minimized
                    // when other columns are resized
                    column.setMaxWidth(MINIMIZED_COLUMN_WIDTH);
                    column.setPreferredWidth(MINIMIZED_COLUMN_WIDTH);
                } else {
                    column.setMaxWidth(Integer.MAX_VALUE);
                    column.setPreferredWidth(preferredWidth);
                }
                repaintTableForHeaderClick();
            }
        }
    }

    /**
     * Simply repaints this table.  Can be used as hook for repainting
     * other dependent components when a header click is handled.
     */
    protected void repaintTableForHeaderClick() {
        repaint();
    }
}
