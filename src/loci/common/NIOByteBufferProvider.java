/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

//
// NIOByteBufferProvider.java
//

package loci.common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

/**
 * Provides a facade to byte buffer allocation that enables
 * <code>FileChannel.map()</code> usage on platforms where it's unlikely to
 * give us problems and heap allocation where it is. References:
 * <ul>
 *   <li>http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5092131</li>
 *   <li>http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6417205</li>
 * </ul>
 *
 * @author Chris Allan <callan at blackcat dot ca>
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="http://dev.loci.wisc.edu/trac/java/browser/trunk/components/common/src/loci/common/NIOByteBufferProvider.java">Trac</a>,
 * <a href="http://dev.loci.wisc.edu/svn/java/trunk/components/common/src/loci/common/NIOByteBufferProvider.java">SVN</a></dd></dl>
 */
public class NIOByteBufferProvider {

  // -- Constants --

  // -- Fields --

  /** Whether or not we are to use memory mapped I/O. */
  private static boolean useMappedByteBuffer = false;

  /** File channel to allocate or map data from. */
  private FileChannel channel;

  /** If we are to use memory mapped I/O, the map mode. */
  private MapMode mapMode;

  static {
    String mapping = System.getProperty("mappedBuffers");
    useMappedByteBuffer = Boolean.parseBoolean(mapping);
  }

  // -- Constructors --

  /**
   * Default constructor.
   * @param channel File channel to allocate or map byte buffers from.
   * @param mapMode The map mode. Required but only used if memory mapped I/O
   * is to occur.
   */
  public NIOByteBufferProvider(FileChannel channel, MapMode mapMode) {
    this.channel = channel;
    this.mapMode = mapMode;
  }

  /**
   * Allocates or maps the desired file data into memory.
   * @param bufferStartPosition The absolute position of the start of the
   * buffer.
   * @param newSize The buffer size.
   * @return A newly allocated or mapped NIO byte buffer.
   * @throws IOException If there is an issue mapping, aligning or allocating
   * the buffer.
   */
  public ByteBuffer allocate(long bufferStartPosition, int newSize)
    throws IOException {
    if (useMappedByteBuffer) {
      return allocateMappedByteBuffer(bufferStartPosition, newSize);
    }
    return allocateDirect(bufferStartPosition, newSize);
  }

  /**
   * Allocates memory and copies the desired file data into it.
   * @param bufferStartPosition The absolute position of the start of the
   * buffer.
   * @param newSize The buffer size.
   * @return A newly allocated NIO byte buffer.
   * @throws IOException If there is an issue aligning or allocating
   * the buffer.
   */
  protected ByteBuffer allocateDirect(long bufferStartPosition, int newSize)
    throws IOException {
    ByteBuffer buffer = ByteBuffer.allocate(newSize);
    channel.read(buffer, bufferStartPosition);
    return buffer;
  }

  /**
   * Memory maps the desired file data into memory.
   * @param bufferStartPosition The absolute position of the start of the
   * buffer.
   * @param newSize The buffer size.
   * @return A newly mapped NIO byte buffer.
   * @throws IOException If there is an issue mapping, aligning or allocating
   * the buffer.
   */
  protected ByteBuffer allocateMappedByteBuffer(
      long bufferStartPosition, int newSize) throws IOException {
    return channel.map(mapMode, bufferStartPosition, newSize);
  }
}
