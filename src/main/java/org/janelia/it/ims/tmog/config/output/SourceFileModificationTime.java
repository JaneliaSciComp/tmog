/*
 * Copyright 2008 Howard Hughes Medical Institute.
 * All rights reserved.  
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0 
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.ims.tmog.config.output;

import org.janelia.it.ims.tmog.field.DataField;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * This class encapsulates an output directory path fragment that is derived
 * from the last modification time of the source file being renamed.
 *
 * @author Eric Trautman
 */
public class SourceFileModificationTime implements OutputDirectoryComponent {
    private SimpleDateFormat formatter;

    /**
     * Empty constructor.
     */
    public SourceFileModificationTime() {
    }

    /**
     * Note that although this method is not used directly, it needs to exist
     * so that the Java Beans reflection utilities used by Digester work.
     * Digester uses these utilities to parse the XML configuration file.
     *
     * @see org.janelia.it.ims.tmog.config.TransmogrifierConfiguration#load
     *
     * @return the date pattern used to format the file's modification time.
     */
    public String getDatePattern() {
        String datePattern = null;
        if (formatter != null) {
            datePattern = formatter.toPattern();
        }
        return datePattern;
    }

    /**
     * Sets the date pattern used to format the file's modification time.
     *
     * @param  datePattern  date pattern for the formatter.
     */
    public void setDatePattern(String datePattern) {
        this.formatter = new SimpleDateFormat(datePattern);
    }

    /**
     * Uses the specified source data to derive an output directory
     * path fragment.
     *
     * @param  sourceFile    the source file being renamed.
     * @param  dataFields  the validated rename fields supplied by the user.
     *
     * @return the path fragment derived from the specified source data.
     */
    public String getValue(File sourceFile,
                           List<DataField> dataFields) {
        String value = null;

        if ((sourceFile != null) && (formatter != null)) {
            long modTime = sourceFile.lastModified();
            Date sourceDate = new Date(modTime);
            value = formatter.format(sourceDate);
        }
        return value;
    }

    /**
     * @return a description of this output directory path fragment for display.
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder(64);
        if (formatter != null) {
            sb.append("[last modification date with format \"");
            sb.append(formatter.toPattern());
            sb.append("\"]");
        }
        return sb.toString();
    }

    /**
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return getDescription();
    }

}