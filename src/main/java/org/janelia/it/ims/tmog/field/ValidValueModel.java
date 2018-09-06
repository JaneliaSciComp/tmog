/*
 * Copyright (c) 2015 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.field;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.matchers.CompositeMatcherEditor;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import org.janelia.it.ims.tmog.config.preferences.FieldDefault;
import org.janelia.it.ims.tmog.config.preferences.FieldDefaultSet;
import org.janelia.it.ims.tmog.target.Target;
import org.janelia.it.utils.FilterMap;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This model supports inserting a selected value from a predefined set
 * of valid values into a rename pattern.
 *
 * @author Eric Trautman
 */
public class ValidValueModel extends AbstractListModel<ValidValue>
        implements ComboBoxModel<ValidValue>, DataField, DefaultValueModel {

    private String displayName;
    private boolean isRequired;
    private boolean isAutoComplete;
    private FilterList<ValidValue> validValues;
    private String globalValueFilter;
    private String filterField;
    private FilterMap filterMap;
    private ValidValue selectedValue;
    private String errorMessage;
    private String prefix;
    private String suffix;
    private boolean isCopyable;
    private boolean markedForTask;
    private boolean sharedForAllSessionFiles;
    private DefaultValueList defaultValueList;

    public ValidValueModel() {
        this.isRequired = false;
        this.isAutoComplete = false;

        validValues = new FilterList<ValidValue>(new BasicEventList<ValidValue>());
        validValues.add(ValidValue.NONE);

        this.isCopyable = true;
        this.markedForTask = true;
        this.sharedForAllSessionFiles = false;
        this.defaultValueList = new DefaultValueList();
    }

    public ValidValueModel(ValidValueModel instance) {
        this.displayName = instance.displayName;
        this.isRequired = instance.isRequired;
        this.isAutoComplete = instance.isAutoComplete;
        this.validValues = instance.validValues; // shallow copy should be safe
        this.globalValueFilter = instance.globalValueFilter;
        this.filterField = instance.filterField;
        this.filterMap = instance.filterMap; // shallow copy should be safe
        this.selectedValue = instance.selectedValue;
        this.prefix = instance.prefix;
        this.suffix = instance.suffix;
        this.isCopyable = instance.isCopyable;
        this.markedForTask = instance.markedForTask;
        this.sharedForAllSessionFiles = instance.sharedForAllSessionFiles;
        this.defaultValueList = instance.defaultValueList;  // shallow copy is ok
    }

    public boolean hasFilter() {
        return ((globalValueFilter != null) || ((filterField != null) && (filterMap != null)));
    }

    public void filterValues(TableModel tableModel,
                             int currentRow) {

        MatcherEditor<ValidValue> matcherEditor = null;

        if (hasFilter()) {

            if ((filterField == null) || (filterMap == null)) {

                // only globalValueFilter defined, apply it
                matcherEditor = buildTextMatcherEditor(globalValueFilter);

            } else {

                // field filters defined, see if we can find them ...

                Integer filterFieldIndex = null;
                final int columnCount = tableModel.getColumnCount();
                for (int i = 0; i < columnCount; i++) {
                    if (filterField.equals(tableModel.getColumnName(i))) {
                        filterFieldIndex = i;
                        break;
                    }
                }

                if (filterFieldIndex == null) {

                    if (globalValueFilter != null) {

                        // can't find field, but can still apply globalValueFilter
                        matcherEditor = buildTextMatcherEditor(globalValueFilter);
                    }

                } else {

                    // apply field (and potentially global) filters
                    final Object filterModel = tableModel.getValueAt(currentRow, filterFieldIndex);
                    if (filterModel instanceof DataField) {
                        matcherEditor = getMatcherEditor((DataField) filterModel);
                    }
                }

            }
        }

        validValues.setMatcherEditor(matcherEditor);

    }

    public void addValidValue(ValidValue validValue) {
        if (validValue.isDefined()) {
            validValues.add(validValue);
            if (validValue.isDefault() && (selectedValue == null)) {
                selectedValue = validValue;
            }
        }
    }

    public int size() {
        return validValues.size();
    }
    
    public void sortValues(Comparator<ValidValue> comparator) {
        Collections.sort(validValues, comparator);
    }

    public boolean isAutoComplete() {
        return isAutoComplete;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setAutoComplete(boolean autoComplete) {
        isAutoComplete = autoComplete;
    }

    public DefaultValueList getDefaultValueList() {
        return defaultValueList;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Integer getDisplayWidth() {
        return null;
    }

    public boolean isEditable() {
        return true;
    }

    public boolean isVisible() {
        return true;
    }

    public boolean isCopyable() {
        return isCopyable;
    }

    public boolean isMarkedForTask() {
        return markedForTask;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setMarkedForTask(boolean markedForTask) {
        this.markedForTask = markedForTask;
    }

    public boolean isSharedForAllSessionFiles() {
        return sharedForAllSessionFiles;
    }

    public void setSharedForAllSessionFiles(boolean sharedForAllSessionFiles) {
        this.sharedForAllSessionFiles = sharedForAllSessionFiles;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setGlobalValueFilter(String globalValueFilter) {
        this.globalValueFilter = globalValueFilter;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setFilterField(String filterField) {
        this.filterField = filterField;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setFilterMap(String filterMapString) {
        this.filterMap = new FilterMap(filterMapString);
    }

    public void addDefaultValue(DefaultValue defaultValue) {
        defaultValueList.add(defaultValue);
    }

    public ValidValueModel getNewInstance(boolean isCloneRequired) {
        ValidValueModel instance = this;
        if (isCloneRequired || (! sharedForAllSessionFiles)) {
            instance = new ValidValueModel(this);
        }
        return instance;
    }

    public String getCoreValue() {
        String coreValue;
        if (selectedValue != null) {
            coreValue = selectedValue.getValue();
            if (coreValue == null) {
                coreValue = "";
            }
        } else {
            coreValue = "";
        }
        return coreValue;
    }

    public String getFileNameValue() {
        String fileNameValue;
        String value = null;

        if (selectedValue != null) {
            value = selectedValue.getValue();
        }

        if ((value != null) && (value.length() > 0)) {
            StringBuilder sb = new StringBuilder(64);
            if (prefix != null) {
                sb.append(prefix);
            }
            sb.append(value);
            if (suffix != null) {
                sb.append(suffix);
            }
            fileNameValue = sb.toString();
        } else {
            fileNameValue = "";
        }

        return fileNameValue;
    }

    public boolean verify() {
        boolean isValid = true;
        errorMessage = null;
        if (isRequired && (selectedValue == null)) {
            isValid = false;
            errorMessage = "Please enter a value for this required field.";
        }
        return isValid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Initializes this field's value based upon the specified target.
     *
     * @param  target  the target being processed.
     */
    public void initializeValue(Target target) {
        final String defaultValue = defaultValueList.getValue(target);
        applyValue(defaultValue);
    }

    @Override
    public void applyValue(String value) {
        if (value != null) {
            for (ValidValue validValue : validValues) {
                if (value.equals(validValue.getValue())) {
                    setSelectedValue(validValue);
                    break;
                }
            }
        }
    }

    public void applyDefault(FieldDefaultSet defaultSet) {
        final FieldDefault fieldDefault =
                defaultSet.getFieldDefault(displayName);
        if (fieldDefault != null) {
            final String value = fieldDefault.getValue();
            applyValue(value);
        }
    }

    public void addAsDefault(FieldDefaultSet defaultSet) {
        final String coreValue = getCoreValue();
        if (coreValue.length() > 0) {
            FieldDefault fieldDefault = new FieldDefault();
            fieldDefault.setName(displayName);
            fieldDefault.setValue(coreValue);
            defaultSet.addFieldDefault(fieldDefault);
        }
    }

    public boolean isRequired() {
        return isRequired;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public ValidValue getSelectedValue() {
        return selectedValue;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setRequired(boolean required) {
        if (isRequired != required) {
            isRequired = required;
            if (isRequired) {
                validValues.remove(ValidValue.NONE);
            } else {
                validValues.add(ValidValue.NONE);
            }
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setCopyable(boolean copyable) {
        isCopyable = copyable;
    }

    public void setSelectedValue(ValidValue newValue) {

        if (((selectedValue != null) && !selectedValue.equals(newValue)) ||
             ((selectedValue == null) && (newValue != null))) {

            selectedValue = newValue;
            fireContentsChanged(this, -1, -1);
        }
    }

    public Object getSelectedItem() {
        return getSelectedValue();
    }

    public void setSelectedItem(Object selectedItem) {
        if (selectedItem instanceof ValidValue) {
            setSelectedValue((ValidValue)selectedItem);
        } else {
            setSelectedValue(null);
        }
    }

    public int getSize() {
        return validValues.size();
    }

    public ValidValue getElementAt(int index) {
        return validValues.get(index);
    }

    @Override
    public String toString() {
        @SuppressWarnings("StringBufferReplaceableByString")
        final StringBuilder sb = new StringBuilder();
        sb.append("ValidValueModel");
        sb.append("{displayName='").append(displayName).append('\'');
        sb.append(", validValues=").append(validValues);
        sb.append(", selectedValue=").append(selectedValue);
        sb.append('}');
        return sb.toString();
    }

    protected EventList<ValidValue> getValidValues() {
        return validValues;
    }

    /**
     * Copies (shallow) the valid value list from the specified model to this model.
     *
     * @param  model  model to copy value list from.
     */
    protected void setValuesFromModel(ValidValueModel model) {
        this.validValues = model.validValues;
    }

    /**
     * Clears the current list of valid values.
     * This should only be called if a new set of values is to be loaded.
     */
    protected void clearValidValues() {
        validValues.clear();
        selectedValue = null;
    }

    private MatcherEditor<ValidValue> getMatcherEditor(DataField filterModel) {

        MatcherEditor<ValidValue> matcherEditor;

        final String filterMapKey = filterModel.getCoreValue();

        String[] newFilters = null;
        if ((filterMapKey != null) && (filterMapKey.length() > 0)) {
            newFilters = filterMap.getFilters(filterMapKey);
        }

        if (newFilters == null) {
            newFilters = new String[]{""};
        }

        if (newFilters.length == 1) {

            if (globalValueFilter == null) {

                // simply apply single field filter
                matcherEditor = buildTextMatcherEditor(newFilters[0]);

            } else {

                // apply global filter AND field filter
                final EventList<MatcherEditor<ValidValue>> matcherEditors = new BasicEventList<MatcherEditor<ValidValue>>();
                matcherEditors.add(buildTextMatcherEditor(globalValueFilter));
                matcherEditors.add(buildTextMatcherEditor(newFilters[0]));

                final CompositeMatcherEditor<ValidValue> compositeMatcherEditor = new CompositeMatcherEditor<ValidValue>(matcherEditors);
                compositeMatcherEditor.setMode(CompositeMatcherEditor.AND);

                matcherEditor = compositeMatcherEditor;

            }

        } else {

            // build composite field matcher (using OR)

            final EventList<MatcherEditor<ValidValue>> fieldMatcherEditors = new BasicEventList<MatcherEditor<ValidValue>>();
            for (String newFilter : newFilters) {
                fieldMatcherEditors.add(buildTextMatcherEditor(newFilter));
            }

            final CompositeMatcherEditor<ValidValue> compositeFieldMatcherEditor = new CompositeMatcherEditor<ValidValue>(fieldMatcherEditors);
            compositeFieldMatcherEditor.setMode(CompositeMatcherEditor.OR);

            if (globalValueFilter == null) {

                // simply apply composite field filters
                matcherEditor = compositeFieldMatcherEditor;

            } else {

                // apply global filter AND composite field filters
                final EventList<MatcherEditor<ValidValue>> matcherEditors = new BasicEventList<MatcherEditor<ValidValue>>();
                matcherEditors.add(buildTextMatcherEditor(globalValueFilter));
                matcherEditors.add(compositeFieldMatcherEditor);

                final CompositeMatcherEditor<ValidValue> compositeMatcherEditor = new CompositeMatcherEditor<ValidValue>(matcherEditors);
                compositeMatcherEditor.setMode(CompositeMatcherEditor.AND);

                matcherEditor = compositeMatcherEditor;
            }
        }

        return matcherEditor;
    }

    private TextMatcherEditor<ValidValue> buildTextMatcherEditor(String filterText) {
        final TextMatcherEditor<ValidValue> textMatcherEditor = new TextMatcherEditor<ValidValue>();
        textMatcherEditor.setFilterator(TEXT_FILTERATOR);
        textMatcherEditor.setFilterText(new String[] {filterText});
        if (filterText.startsWith("/") && filterText.endsWith("/")) {
            final String regularExpression = filterText.substring(1, filterText.length() - 1);
            textMatcherEditor.setFilterText(new String[] { regularExpression });
            textMatcherEditor.setMode(TextMatcherEditor.REGULAR_EXPRESSION);
        } else {
            textMatcherEditor.setFilterText(new String[] {filterText});
        }
        return textMatcherEditor;
    }

    private static final TextFilterator<ValidValue> TEXT_FILTERATOR = new TextFilterator<ValidValue>() {
        @Override
        public void getFilterStrings(List<String> baseList,
                                     ValidValue element) {
            if (element != null) {
                baseList.add(element.getValue());
            }
        }
    };


}
