/*
 * Copyright 2007 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Center Software Copyright 1.0
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_0.html).
 */

package org.janelia.it.chacrm;

import java.util.regex.Pattern;

/**
 * A transformant is a putative CRM associated with an expression
 * vector and landing site. Any CRM may have one or more unique CRMs,
 * and transformants are unique both between and within CRMs. Every
 * transformant is identified by an alphanumeric transformant ID.
 *
 * @author R. Svirskas
 */
public class Transformant {

    /**
     * Transformant status is stored in the <i>featureprop</i> table as
     * a string in the following set:
     */
    public enum Status {
        Rearrayed, gsi_ready, transformant, crossed, imaged, gsi_failed
    }

    /** The identifier for this transformant. */
    private String transformantID;

    /** The current status for this transformant. */
    private Status status;

    /** The feature identifier for this transformant. */
    private Integer featureID;

    /** The latest image location for this transformant. */
    private ImageLocation imageLocation;

    /**
     * Basic constructor.
     *
     * @param  transformantID  the transformant identifier.
     * @param  status          the transformant status.
     * @param  featureID       the feature identifier.
     */
    public Transformant(String transformantID,
                        Status status,
                        Integer featureID) {
        this.transformantID = transformantID;
        this.status = status;
        this.featureID = featureID;
        this.imageLocation = null;
    }

    /**
     * @return the transformant identifier.
     */
    public String getTransformantID() {
        return transformantID;
    }

    /**
     * @return the transformant status.
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @return the associated feature identifier.
     */
    public Integer getFeatureID() {
        return featureID;
    }

    /**
     * @return the latest image location for this transformant.
     */
    public ImageLocation getImageLocation() {
        return imageLocation;
    }

    /**
     * Sets the latest image location for this transformant.
     *
     * @param  imageLocation  the latest image location.
     */
    public void setImageLocation(ImageLocation imageLocation) {
        this.imageLocation = imageLocation;
    }

    /**
     * Sets the transformant's status.
     *
     * @param  status  the new status for the transformant.
     *
     * @throws IllegalStateException
     *   if the specified status is invalid given the transformant's
     *   current status.
     */
    public void setStatus(Status status) throws IllegalStateException {
        boolean isValidStatusTransition = false;
        if (status != null) {
            // TODO: complete status transition rules
            switch (status) {
                case imaged:
                    isValidStatusTransition =
                            ((this.status == Status.crossed) ||
                                    (this.status == Status.imaged));
                    break;
            }
        }

        if (! isValidStatusTransition) {
            throw new IllegalStateException(
                    "Attempted to change transformant status from " +
                            this.status + " to " + status);
        }

        this.status = status;
    }


    /**
     * @return a string representation of this object.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Transformant");
        sb.append("{transformantID='").append(transformantID).append('\'');
        sb.append(", status=").append(status);
        sb.append(", featureID=").append(featureID);
        sb.append(", imageLocation=").append(imageLocation);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Constructs a transformant identifier from the specified
     * component values.
     *
     * @param  plate   rearray plate
     * @param  well    rearray well label
     * @param  vector  expression vector
     * @param  site    landing site
     *
     * @return a transformant identifier.
     */
	public static String constructTransformantID(String plate,
                                                 String well,
                                                 String vector,
                                                 String site) {
        StringBuilder sb = new StringBuilder();
        sb.append(plate);
        sb.append(well);
        sb.append("_");
        sb.append(vector);
        sb.append("_");
        sb.append(site);
        return sb.toString();
	}

    /**
	 * A syntactically valid transformant ID takes the following form:
	 * [plate][well label]_[vector code]_[landing site code].
	 * [plate] is a numeric rearray plate number. [well label] is an
	 * alphanumeric well coordinate (A01 - H12). [vector code] is a
	 * two-letter code (AA-ZZ) that represents the expression vector.
	 * [landing site code] is a two-digit code (01-99) that represents
	 * the landing site.
     *
     * @param  transformantID  identifier to validate.
	 * @return validity of transformant ID (true or false)
	 */
	public static boolean isIDSyntacticallyValid(String transformantID) {
		return(PATTERN.matcher(transformantID).matches());
	}

    // Validation regex
	private static final String VALID =
            "^[1-9]{1,2}[A-Z](?:0[1-9]|1[0-2])_[A-Z]{2}_(?:0[1-9]|[1-9][0-9])$";
	private static final Pattern PATTERN;
    static {
        PATTERN = Pattern.compile(VALID);
    }
}
