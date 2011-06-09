/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

//
// CodecOptions.java
//

package loci.formats.codec;

/**
 * Options for compressing and decompressing data.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="http://dev.loci.wisc.edu/trac/java/browser/trunk/components/bio-formats/src/loci/formats/codec/CodecOptions.java">Trac</a>,
 * <a href="http://dev.loci.wisc.edu/svn/java/trunk/components/bio-formats/src/loci/formats/codec/CodecOptions.java">SVN</a></dd></dl>
 */
public class CodecOptions {

  /** Width, in pixels, of the image. */
  public int width;

  /** Height, in pixels, of the image. */
  public int height;

  /** Number of channels. */
  public int channels;

  /** Number of bits per channel. */
  public int bitsPerSample;

  /** Indicates endianness of pixel data. */
  public boolean littleEndian;

  /** Indicates whether or not channels are interleaved. */
  public boolean interleaved;

  /** Indicates whether or not the pixel data is signed. */
  public boolean signed;

  /**
   * If compressing, this is the maximum number of raw bytes to compress.
   * If decompressing, this is the maximum number of raw bytes to return.
   */
  public int maxBytes;

  /** Pixels for preceding image. */
  public byte[] previousImage;

  /**
   * Used with codecs allowing lossy and lossless compression.
   * Default is set to true.
   */
  public boolean lossless;

  // -- Constructors --

  /** Construct a new CodecOptions. */
  public CodecOptions() { }

  /** Construct a new CodecOptions using the given CodecOptions. */
  public CodecOptions(CodecOptions options) {
    this.width = options.width;
    this.height = options.height;
    this.channels = options.channels;
    this.bitsPerSample = options.bitsPerSample;
    this.littleEndian = options.littleEndian;
    this.interleaved = options.interleaved;
    this.signed = options.signed;
    this.maxBytes = options.maxBytes;
    this.previousImage = options.previousImage;
    this.lossless = options.lossless;
  }

  // -- Static methods --

  /** Return CodecOptions with reasonable default values. */
  public static CodecOptions getDefaultOptions() {
    CodecOptions options = new CodecOptions();
    options.littleEndian = false;
    options.interleaved = false;
    options.lossless = true;
    return options;
  }

}
