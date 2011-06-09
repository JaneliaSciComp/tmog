/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

//
// IRandomAccess.java
//

package loci.common;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Interface for random access into structures (e.g., files or arrays).
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="http://dev.loci.wisc.edu/trac/java/browser/trunk/components/common/src/loci/common/IRandomAccess.java">Trac</a>,
 * <a href="http://dev.loci.wisc.edu/svn/java/trunk/components/common/src/loci/common/IRandomAccess.java">SVN</a></dd></dl>
 *
 * @author Curtis Rueden ctrueden at wisc.edu
 */
public interface IRandomAccess extends DataInput, DataOutput {

  /**
   * Closes this random access stream and releases
   * any system resources associated with the stream.
   */
  void close() throws IOException;

  /** Returns the current offset in this stream. */
  long getFilePointer() throws IOException;

  /** Returns the length of this stream. */
  long length() throws IOException;

  /**
   * Returns the current order of the stream.
   * @return See above.
   */
  ByteOrder getOrder();

  /**
   * Sets the byte order of the stream.
   * @param order Order to set.
   */
  void setOrder(ByteOrder order);

  /**
   * Reads up to b.length bytes of data
   * from this stream into an array of bytes.
   *
   * @return the total number of bytes read into the buffer.
   */
  int read(byte[] b) throws IOException;

  /**
   * Reads up to len bytes of data from this stream into an array of bytes.
   *
   * @return the total number of bytes read into the buffer.
   */
  int read(byte[] b, int off, int len) throws IOException;

  /**
   * Reads up to buffer.capacity() bytes of data
   * from this stream into a ByteBuffer.
   */
  int read(ByteBuffer buffer) throws IOException;

  /**
   * Reads up to len bytes of data from this stream into a ByteBuffer.
   *
   * @return the total number of bytes read into the buffer.
   */
  int read(ByteBuffer buffer, int offset, int len) throws IOException;

  /**
   * Sets the stream pointer offset, measured from the beginning
   * of this stream, at which the next read or write occurs.
   */
  void seek(long pos) throws IOException;

  /**
   * Writes up to buffer.capacity() bytes of data from the given
   * ByteBuffer to this stream.
   */
  void write(ByteBuffer buf) throws IOException;

  /**
   * Writes up to len bytes of data from the given ByteBuffer to this
   * stream.
   */
  void write(ByteBuffer buf, int off, int len) throws IOException;

}
