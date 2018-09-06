/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.utils.filexfer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Convenience class for identifying the set of well known
 * (and supported) @link{MessageDigest} algorithms.
 *
 * @author Eric Trautman
 */
public class DigestAlgorithms {

    /** Indicates that no digest calculation should be made for transfers. */
    public static final String NONE = "none";

    /** The md5 digest algorithm name. */
    public static final String MD5 = "md5";

    /** The sha1 digest algorithm name. */
    public static final String SHA1 = "sha1";

    /**
     * @param  digestAlgorithm  name of algorithm to use.
     *
     * @return message digest instance for the specified algorithm.
     *
     * @throws NoSuchAlgorithmException
     *   if the specified algorithm cannot be found.
     */
    public static MessageDigest getMessageDigest(String digestAlgorithm)
            throws NoSuchAlgorithmException {

        MessageDigest digest = null;

        if ((digestAlgorithm != null) &&
            (! NONE.equalsIgnoreCase(digestAlgorithm))) {

            if (AdlerMessageDigest.NAME.equalsIgnoreCase(digestAlgorithm)) {
                digest = new AdlerMessageDigest();
            } else if (CrcMessageDigest.NAME.equalsIgnoreCase(digestAlgorithm)) {
                digest = new CrcMessageDigest();
            } else {
                digest = MessageDigest.getInstance(digestAlgorithm);
            }
        }

        return digest;
    }

    /**
     * @return list of well known algorithm names.  Note that other
     *         valid algorithm names may exist for specific environments.
     */
    public static List<String> getValidNames() {
        return VALID_NAMES;
    }

    private static final List<String> VALID_NAMES =
            Collections.unmodifiableList(
                    Arrays.asList(NONE,
                                  AdlerMessageDigest.NAME,
                                  CrcMessageDigest.NAME,
                                  MD5,
                                  SHA1));

}