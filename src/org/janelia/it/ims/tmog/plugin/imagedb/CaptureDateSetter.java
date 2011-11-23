/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.janelia.it.ims.tmog.field.DataField;
import org.janelia.it.ims.tmog.field.DatePatternField;
import org.janelia.it.ims.tmog.plugin.PluginDataRow;
import org.janelia.it.ims.tmog.plugin.RelativePathUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class sets the image capture date using a configured field name
 * to retrieve the property value from a data field row.
 *
 * @author Eric Trautman
 */
public class CaptureDateSetter extends SimpleSetter {

    /**
     * The type value for capture dates stored in the image_property table.
     */
    public static final String TYPE = "capture_date";

    /**
     * Value constructor.
     *
     * @param  fieldName  the display name of the data field that contains
     *                    capture date information.
     */
    public CaptureDateSetter(String fieldName) {
        super(TYPE, fieldName);
    }

    /**
     * Sets the capture date for the specified image.
     *
     * @param  row    current row being processed.
     * @param  image  image to be updated.
     */
    public void setProperty(PluginDataRow row,
                            Image image) {

        final String value = deriveValue(row);

        if ((value != null) && (value.length() > 0)) {
            final DataField field = row.getDataField(getFieldName());
            if (field instanceof DatePatternField) {
                String pattern = ((DatePatternField) field).getDatePattern();
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                Date captureDate;
                try {
                    captureDate = sdf.parse(value);
                    image.setCaptureDate(captureDate);
                } catch (ParseException e) {
                    final String relativePath =
                            RelativePathUtil.getRelativePath(
                                    row.getTargetFile());
                    LOG.warn("Unable to parse capture date for '" +
                             relativePath +
                             "'.  Continuing processing without the date.", e);
                }
            }
        }
    }

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(CaptureDateSetter.class);

}