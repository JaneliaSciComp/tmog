/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

//
// TiffConstants.java
//

package loci.formats.tiff;

/**
 * Generally useful TIFF-related constants.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="http://dev.loci.wisc.edu/trac/java/browser/trunk/components/bio-formats/src/loci/formats/tiff/TiffConstants.java">Trac</a>,
 * <a href="http://dev.loci.wisc.edu/svn/java/trunk/components/bio-formats/src/loci/formats/tiff/TiffConstants.java">SVN</a></dd></dl>
 *
 * @author Curtis Rueden ctrueden at wisc.edu
 * @author Eric Kjellman egkjellman at wisc.edu
 * @author Melissa Linkert melissa at glencoesoftware.com
 * @author Chris Allan callan at blackcat.ca
 */
public final class TiffConstants {

  // -- Constants --

  /** The number of bytes in each IFD entry. */
  public static final int BYTES_PER_ENTRY = 12;

  /** The number of bytes in each IFD entry of a BigTIFF file. */
  public static final int BIG_TIFF_BYTES_PER_ENTRY = 20;

  // TIFF header constants
  public static final int MAGIC_NUMBER = 42;
  public static final int BIG_TIFF_MAGIC_NUMBER = 43;
  public static final int LITTLE = 0x49;
  public static final int BIG = 0x4d;

  // -- Constructor --

  private TiffConstants() { }

}
