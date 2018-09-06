/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.config.preferences;

import org.janelia.it.utils.StringUtil;

/**
 * A default path name and value.
 *
 * @author Eric Trautman
 */
public class PathDefault
        extends NamedObject {

    /** Name of the source directory path default. */
    public static final String SOURCE_DIRECTORY = "sourceDirectory";

    /** Name of the transfer directory path default. */
    public static final String TRANSFER_DIRECTORY = "transferDirectory";

    private String value;

    public PathDefault() {
    }

    public PathDefault(String name) {
        super(name);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @param  indent         indentation string to prepend to all elements.
     * @param  stringBuilder  builder to use for appending element xml strings.
     */
    // TODO: replace this with jaxb annotations whenever we can drop jdk1.5
    protected void appendXml(String indent,
                             StringBuilder stringBuilder) {
        final String name = getName();
        if (StringUtil.isDefined(name) && StringUtil.isDefined(value)) {
            stringBuilder.append(indent);
            stringBuilder.append("<pathDefault name=\"");
            stringBuilder.append(StringUtil.getDefinedXmlValue(name));
            stringBuilder.append("\">");
            stringBuilder.append(StringUtil.getDefinedXmlValue(value));
            stringBuilder.append("</pathDefault>\n");
        }
    }
}