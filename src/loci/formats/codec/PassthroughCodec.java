/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

//
// PassthroughCodec.java
//

package loci.formats.codec;

import loci.common.RandomAccessInputStream;
import loci.formats.FormatException;

import java.io.IOException;

/**
 * A codec which just returns the exact data it was given, performing no
 * compression or decompression.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="http://dev.loci.wisc.edu/trac/java/browser/trunk/components/bio-formats/src/loci/formats/codec/PassthroughCodec.java">Trac</a>,
 * <a href="http://dev.loci.wisc.edu/svn/java/trunk/components/bio-formats/src/loci/formats/codec/PassthroughCodec.java">SVN</a></dd></dl>
 */
public class PassthroughCodec extends BaseCodec {

  /* (non-Javadoc)
   * @see loci.formats.codec.BaseCodec#decompress(byte[], loci.formats.codec.CodecOptions)
   */
  @Override
  public byte[] decompress(byte[] data, CodecOptions options)
      throws FormatException {
    return data;
  }

  /* (non-Javadoc)
   * @see loci.formats.codec.BaseCodec#decompress(loci.common.RandomAccessInputStream, loci.formats.codec.CodecOptions)
   */
  @Override
  public byte[] decompress(RandomAccessInputStream in, CodecOptions options)
      throws FormatException, IOException {
    throw new RuntimeException("Not implemented.");
  }

  /* (non-Javadoc)
   * @see loci.formats.codec.Codec#compress(byte[], loci.formats.codec.CodecOptions)
   */
  public byte[] compress(byte[] data, CodecOptions options)
      throws FormatException {
    return data;
  }

}
