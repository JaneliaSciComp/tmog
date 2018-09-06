/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.ims.tmog.plugin.imagedb;

/**
 * Information about a line and its parents.
 *
 * @author Eric Trautman
 */
public class Line {

    private Integer id;
    private String name;
    private String labName;
    private String genotype;

    private Line parentA;
    private Line parentB;

    public Line(String name,
                String labName) {
        this(null, name, labName);
    }

    public Line(Integer id,
                String name,
                String labName) {
        this.id = id;
        this.name = name;
        this.labName = labName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getLabName() {
        return labName;
    }

    public String getGenotype() {
        return genotype;
    }

    public Line getParentA() {
        return parentA;
    }

    public void setParentA(Line parentA) {
        this.parentA = parentA;
    }

    public Line getParentB() {
        return parentB;
    }

    public void setParentB(Line parentB) {
        this.parentB = parentB;
    }

    public void defaultGenotypeToLineName() {
        genotype = name;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("Line{id=");
        sb.append(id);
        sb.append(", name='");
        sb.append(name);
        sb.append("', labName='");
        sb.append(labName);
        if (genotype != null) {
            sb.append("', genotype='");
            sb.append(genotype);
        }
        sb.append("'");
        if (parentA != null) {
            sb.append(", parentA=");
            sb.append(parentA);
        }
        if (parentB != null) {
            sb.append(", parentB=");
            sb.append(parentB);
        }
        sb.append("}");
        return sb.toString();
    }
}
