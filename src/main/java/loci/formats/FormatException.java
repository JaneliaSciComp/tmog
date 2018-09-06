/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

//
// FormatException.java
//

package loci.formats;

/**
 * FormatException is the exception thrown when something
 * goes wrong performing a file format operation.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="http://dev.loci.wisc.edu/trac/java/browser/trunk/components/bio-formats/src/loci/formats/FormatException.java">Trac</a>,
 * <a href="http://dev.loci.wisc.edu/svn/java/trunk/components/bio-formats/src/loci/formats/FormatException.java">SVN</a></dd></dl>
 */
public class FormatException extends Exception {

  public FormatException() { super(); }
  public FormatException(String s) { super(s); }
  public FormatException(String s, Throwable cause) { super(s, cause); }
  public FormatException(Throwable cause) { super(cause); }

}

