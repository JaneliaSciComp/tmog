/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

//
// CodedEnum.java
//
package loci.common.enumeration;

/**
 * Enumeration which is coded.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="http://dev.loci.wisc.edu/trac/java/browser/trunk/components/common/src/loci/common/enumeration/CodedEnum.java">Trac</a>,
 * <a href="http://dev.loci.wisc.edu/svn/java/trunk/components/common/src/loci/common/enumeration/CodedEnum.java">SVN</a></dd></dl>
 */
public interface CodedEnum {

  /**
   * Retrieves the integer "code" for this enumeration. It is expected that the
   * code be unique across the enumerated type.
   * @return See above.
   */
  public int getCode();

}
