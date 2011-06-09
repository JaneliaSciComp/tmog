/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

//
// HandleException.java
//

package loci.common;

import java.io.IOException;

/**
 * HandleException is the exception thrown when something goes wrong in
 * one of the custom I/O classes.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="http://dev.loci.wisc.edu/trac/java/browser/trunk/components/common/src/loci/common/HandleException.java">Trac</a>,
 * <a href="http://dev.loci.wisc.edu/svn/java/trunk/components/common/src/loci/common/HandleException.java">SVN</a></dd></dl>
 */
public class HandleException extends IOException {

  public HandleException() { super(); }
  public HandleException(String s) { super(s); }
  public HandleException(String s, Throwable cause) {
    super(s);
    initCause(cause);
  }
  public HandleException(Throwable cause) {
    super();
    initCause(cause);
  }

}

