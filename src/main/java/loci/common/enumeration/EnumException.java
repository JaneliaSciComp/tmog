/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

//
// EnumException.java
//

package loci.common.enumeration;

/**
 * @author callan
 *
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="http://dev.loci.wisc.edu/trac/java/browser/trunk/components/bio-formats/src/loci/formats/in/EnumException.java">Trac</a>,
 * <a href="http://dev.loci.wisc.edu/svn/java/trunk/components/bio-formats/src/loci/formats/in/EnumException.java">SVN</a></dd></dl>
 */
public class EnumException extends RuntimeException {

  private static final long serialVersionUID = -4969429871517178079L;

  public EnumException() { super(); }
  public EnumException(String s) { super(s); }
  public EnumException(String s, Throwable cause) { super(s, cause); }
  public EnumException(Throwable cause) { super(cause); }

}
