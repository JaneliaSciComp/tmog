/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

//
// BaseCodec.java
//

package loci.formats.codec;

import loci.common.RandomAccessInputStream;
import loci.formats.FormatException;

import java.io.IOException;

/**
 * BaseCodec contains default implementation and testing for classes
 * implementing the Codec interface, and acts as a base class for any
 * of the compression classes.
 * Base 1D compression and decompression methods are not implemented here, and
 * are left as abstract. 2D methods do simple concatenation and call to the 1D
 * methods
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="http://dev.loci.wisc.edu/trac/java/browser/trunk/components/bio-formats/src/loci/formats/codec/BaseCodec.java">Trac</a>,
 * <a href="http://dev.loci.wisc.edu/svn/java/trunk/components/bio-formats/src/loci/formats/codec/BaseCodec.java">SVN</a></dd></dl>
 *
 * @author Eric Kjellman egkjellman at wisc.edu
 */
public abstract class BaseCodec implements Codec {

  // -- Constants --

  // -- Codec API methods --

  /**
   * 2D data block encoding default implementation.
   * This method simply concatenates data[0] + data[1] + ... + data[i] into
   * a 1D block of data, then calls the 1D version of compress.
   *
   * @param data The data to be compressed.
   * @param options Options to be used during compression, if appropriate.
   * @return The compressed data.
   * @throws FormatException If input is not a compressed data block of the
   *   appropriate type.
   */
  public byte[] compress(byte[][] data, CodecOptions options)
    throws FormatException
  {
    int len = 0;
    for (int i = 0; i < data.length; i++) {
      len += data[i].length;
    }
    byte[] toCompress = new byte[len];
    int curPos = 0;
    for (int i = 0; i < data.length; i++) {
      System.arraycopy(data[i], 0, toCompress, curPos, data[i].length);
      curPos += data[i].length;
    }
    return compress(toCompress, options);
  }

  /* @see Codec#decompress(byte[]) */
  public byte[] decompress(byte[] data) throws FormatException {
    return decompress(data, null);
  }

  /* @see Codec#decompress(byte[][]) */
  public byte[] decompress(byte[][] data) throws FormatException {
    return decompress(data, null);
  }

  /* @see Codec#decompress(byte[], CodecOptions) */
  public byte[] decompress(byte[] data, CodecOptions options)
    throws FormatException
  {
    try {
      RandomAccessInputStream r = new RandomAccessInputStream(data);
      byte[] t = decompress(r, options);
      r.close();
      return t;
    }
    catch (IOException e) {
      throw new FormatException(e);
    }
  }

  /* @see Codec#decompress(RandomAccessInputStream, CodecOptions) */
  public abstract byte[] decompress(RandomAccessInputStream in,
    CodecOptions options) throws FormatException, IOException;

  /**
   * 2D data block decoding default implementation.
   * This method simply concatenates data[0] + data[1] + ... + data[i] into
   * a 1D block of data, then calls the 1D version of decompress.
   *
   * @param data The data to be decompressed.
   * @return The decompressed data.
   * @throws FormatException If input is not a compressed data block of the
   *   appropriate type.
   */
  public byte[] decompress(byte[][] data, CodecOptions options)
    throws FormatException
  {
    int len = 0;
    for (int i = 0; i < data.length; i++) {
      len += data[i].length;
    }
    byte[] toDecompress = new byte[len];
    int curPos = 0;
    for (int i = 0; i < data.length; i++) {
      System.arraycopy(data[i], 0, toDecompress, curPos, data[i].length);
      curPos += data[i].length;
    }
    return decompress(toDecompress, options);
  }

}
