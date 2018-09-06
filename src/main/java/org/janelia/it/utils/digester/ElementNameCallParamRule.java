/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.utils.digester;

import org.apache.commons.digester.Rule;
import org.xml.sax.Attributes;

/**
 * A call parameter rule that references the current element name.
 *
 * @author Eric Trautman
 */
public class ElementNameCallParamRule extends Rule {

    protected int paramIndex = 0;

    /**
     * Construct a "call parameter" rule that will save the name of this
     * element as the parameter value.
     *
     * @param  paramIndex  zero-relative parameter number.
     */
    public ElementNameCallParamRule(int paramIndex) {
        this.paramIndex = paramIndex;
    }

    /**
     * Process the start of this element.
     *
     * @param namespace the namespace URI of the matching element, or an
     *   empty string if the parser is not namespace aware or the element has
     *   no namespace
     * @param name the local name if the parser is namespace aware, or just
     *   the element name otherwise
     * @param attributes The attribute list for this element
     */
    @Override
    public void begin(String namespace,
                      String name,
                      Attributes attributes) throws Exception {
        Object parameters[] = (Object[]) digester.peekParams();
        parameters[paramIndex] = name;
    }

    @Override
    public String toString() {
        return "ElementNameCallParamRule{" +
               "paramIndex=" + paramIndex +
               '}';
    }
}