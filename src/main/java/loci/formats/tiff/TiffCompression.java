/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

//
// TiffCompression.java
//

package loci.formats.tiff;

import loci.common.DataTools;
import loci.common.enumeration.CodedEnum;
import loci.common.enumeration.EnumException;
import loci.formats.FormatException;
import loci.formats.codec.Codec;
import loci.formats.codec.CodecOptions;
import loci.formats.codec.LZWCodec;
import loci.formats.codec.PassthroughCodec;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for performing compression operations with a TIFF file.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="http://dev.loci.wisc.edu/trac/java/browser/trunk/components/bio-formats/src/loci/formats/tiff/TiffCompression.java">Trac</a>,
 * <a href="http://dev.loci.wisc.edu/svn/java/trunk/components/bio-formats/src/loci/formats/tiff/TiffCompression.java">SVN</a></dd></dl>
 *
 * @author Curtis Rueden ctrueden at wisc.edu
 * @author Eric Kjellman egkjellman at wisc.edu
 * @author Melissa Linkert melissa at glencoesoftware.com
 * @author Chris Allan callan at blackcat.ca
 */
public enum TiffCompression implements CodedEnum {

  // (TIFF code, codec, codec name)
  UNCOMPRESSED(1, new PassthroughCodec(), "Uncompressed"),
  LZW(5, new LZWCodec(), "LZW");

  /** Code for the TIFF compression in the actual TIFF file. */
  private int code;

  /** TIFF compression codec. */
  private Codec codec;

  /** Name of the TIFF compression codec. */
  private String codecName;

  /** Reverse lookup of code to TIFF compression enumerate value. */
  private static Map<Integer, TiffCompression> lookup =
    new HashMap<Integer, TiffCompression>();

  static {
    for (TiffCompression v : EnumSet.allOf(TiffCompression.class)) {
      lookup.put(v.getCode(), v);
    }
  }

  // -- TiffCompression methods --

  /**
   * Default constructor.
   * @param code Integer "code" for the TIFF compression type.
   * @param codec TIFF compression codec.
   * @param codecName String name of the compression type.
   */
  private TiffCompression(int code, Codec codec, String codecName) {
    this.code = code;
    this.codec = codec;
    this.codecName = codecName;
  }

  /**
   * Retrieves a TIFF compression instance by code.
   * @param code Integer "code" for the TIFF compression type.
   * @return See above.
   */
  public static TiffCompression get(int code) {
    TiffCompression toReturn = lookup.get(code);
    if (toReturn == null) {
      throw new EnumException(
          "Unable to find TiffCompresssion with code: " + code);
    }
    return toReturn;
  }

  /* (non-Javadoc)
   * @see loci.common.CodedEnum#getCode()
   */
  public int getCode() {
    return code;
  }

  /**
   * Retrieves the name of the TIFF compression codec.
   * @return See above.
   */
  public String getCodecName() {
    return codecName;
  }

  // -- TiffCompression methods - decompression --

  /** Decodes a strip of data. */
  public byte[] decompress(byte[] input, CodecOptions options)
    throws FormatException, IOException
  {
    if (codec == null) {
      throw new FormatException(
          "Sorry, " + getCodecName() + " compression mode is not supported");
    }
    return codec.decompress(input, options);
  }

  /** Undoes in-place differencing according to the given predictor value. */
  public static void undifference(byte[] input, IFD ifd)
    throws FormatException
  {
    int predictor = ifd.getIFDIntValue(IFD.PREDICTOR, 1);
    if (predictor == 2) {
      int[] bitsPerSample = ifd.getBitsPerSample();
      int len = bitsPerSample.length;
      long width = ifd.getImageWidth();
      boolean little = ifd.isLittleEndian();
      int planarConfig = ifd.getPlanarConfiguration();

      int bytes = ifd.getBytesPerSample()[0];

      if (planarConfig == 2 || bitsPerSample[len - 1] == 0) len = 1;
      len *= bytes;

      for (int b=0; b<input.length; b+=bytes) {
        if (b / len % width == 0) continue;
        int value = DataTools.bytesToInt(input, b, bytes, little);
        value += DataTools.bytesToInt(input, b - len, bytes, little);
        DataTools.unpackBytes(value, input, b, bytes, little);
      }
    }
    else if (predictor != 1) {
      throw new FormatException("Unknown Predictor (" + predictor + ")");
    }
  }

  // -- TiffCompression methods - compression --

  /**
   * Creates a set of codec options for compression.
   * @param ifd The IFD to create codec options for.
   * @return A new codec options instance populated using metadata from
   * <code>ifd</code>.
   */
  public CodecOptions getCompressionCodecOptions(IFD ifd)
    throws FormatException{
    CodecOptions options = new CodecOptions();
    options.width = (int) ifd.getImageWidth();
    options.height = (int) ifd.getImageLength();
    options.bitsPerSample = ifd.getBitsPerSample()[0];
    options.channels = ifd.getSamplesPerPixel();
    options.littleEndian = ifd.isLittleEndian();
    options.interleaved = true;
    options.signed = false;
    return options;
  }

  /** Encodes a strip of data. */
  public byte[] compress(byte[] input, CodecOptions options)
    throws FormatException, IOException
  {
    if (codec == null) {
      throw new FormatException(
          "Sorry, " + getCodecName() + " compression mode is not supported");
    }
    return codec.compress(input, options);
  }

  /** Performs in-place differencing according to the given predictor value. */
  public static void difference(byte[] input, IFD ifd) throws FormatException {
    int predictor = ifd.getIFDIntValue(IFD.PREDICTOR, 1);
    if (predictor == 2) {
      int[] bitsPerSample = ifd.getBitsPerSample();
      long width = ifd.getImageWidth();
      boolean little = ifd.isLittleEndian();
      int planarConfig = ifd.getPlanarConfiguration();
      int bytes = ifd.getBytesPerSample()[0];
      int len = bytes * (planarConfig == 2 ? 1 : bitsPerSample.length);

      for (int b=input.length-bytes; b>=0; b-=bytes) {
        if (b / len % width == 0) continue;
        int value = DataTools.bytesToInt(input, b, bytes, little);
        value -= DataTools.bytesToInt(input, b - len, bytes, little);
        DataTools.unpackBytes(value, input, b, bytes, little);
      }
    }
    else if (predictor != 1) {
      throw new FormatException("Unknown Predictor (" + predictor + ")");
    }
  }

}
