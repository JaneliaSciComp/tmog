/*
 * Copyright © 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.chacrm;

/**
 * This class encapsulates information about a transformant image location.
 *
 * @author Eric Trautman
 */
public class ImageLocation {

    /** The image location's relative path. */
    private String relativePath;

    /** The image location's ChaCRM feature property rank. */
    private Integer rank;

    /**
     * Value constructor.
     *
     * @param  relativePath  the image location's relative path.
     * @param  rank          the image location's ChaCRM feature property rank.
     */
    public ImageLocation(String relativePath,
                         Integer rank) {
        this.relativePath = relativePath;
        this.rank = rank;
    }


    /**
     * @return the image location's relative path.
     */
    public String getRelativePath() {
        return relativePath;
    }

    /**
     * @return the image location's ChaCRM feature property rank.
     */
    public Integer getRank() {
        return rank;
    }

    /**
     * @return a string representation of this object.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ImageLocation");
        sb.append("{relativePath='").append(relativePath).append('\'');
        sb.append(", rank=").append(rank);
        sb.append('}');
        return sb.toString();
    }
}
