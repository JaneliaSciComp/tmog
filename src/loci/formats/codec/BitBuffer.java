/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

//
// BitBuffer.java
//

package loci.formats.codec;

/**
 * A class for reading arbitrary numbers of bits from a byte array.
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="http://dev.loci.wisc.edu/trac/java/browser/trunk/components/bio-formats/src/loci/formats/codec/BitBuffer.java">Trac</a>,
 * <a href="http://dev.loci.wisc.edu/svn/java/trunk/components/bio-formats/src/loci/formats/codec/BitBuffer.java">SVN</a></dd></dl>
 *
 * @author Eric Kjellman egkjellman at wisc.edu
 */
public class BitBuffer {

  // -- Constants --

  /** Various bitmasks for the 0000xxxx side of a byte. */
  private static final int[] BACK_MASK = {
    0x00, // 00000000
    0x01, // 00000001
    0x03, // 00000011
    0x07, // 00000111
    0x0F, // 00001111
    0x1F, // 00011111
    0x3F, // 00111111
    0x7F  // 01111111
  };

  /** Various bitmasks for the xxxx0000 side of a byte. */
  private static final int[] FRONT_MASK = {
    0x0000, // 00000000
    0x0080, // 10000000
    0x00C0, // 11000000
    0x00E0, // 11100000
    0x00F0, // 11110000
    0x00F8, // 11111000
    0x00FC, // 11111100
    0x00FE  // 11111110
  };

  private byte[] byteBuffer;
  private int currentByte;
  private int currentBit;
  private int eofByte;
  private boolean eofFlag;

  /** Default constructor. */
  public BitBuffer(byte[] byteBuffer) {
    this.byteBuffer = byteBuffer;
    currentByte = 0;
    currentBit = 0;
    eofByte = byteBuffer.length;
  }

  /**
   * Skips a number of bits in the BitBuffer.
   *
   * @param bits Number of bits to skip
   */
  public void skipBits(long bits) {
    if (bits < 0) {
      throw new IllegalArgumentException("Bits to skip may not be negative");
    }

    // handles skipping past eof
    if ((long) eofByte * 8 < (long) currentByte * 8 + currentBit + bits) {
      eofFlag = true;
      currentByte = eofByte;
      currentBit = 0;
      return;
    }

    int skipBytes = (int) (bits / 8);
    int skipBits = (int) (bits % 8);
    currentByte += skipBytes;
    currentBit += skipBits;
    while (currentBit >= 8) {
      currentByte++;
      currentBit -= 8;
    }
  }

  /**
   * Returns an int value representing the value of the bits read from
   * the byte array, from the current position. Bits are extracted from the
   * "left side" or high side of the byte.<p>
   * The current position is modified by this call.<p>
   * Bits are pushed into the int from the right, endianness is not
   * considered by the method on its own. So, if 5 bits were read from the
   * buffer "10101", the int would be the integer representation of
   * 000...0010101 on the target machine. <p>
   * In general, this also means the result will be positive unless a full
   * 32 bits are read. <p>
   * Requesting more than 32 bits is allowed, but only up to 32 bits worth of
   * data will be returned (the last 32 bits read). <p>
   *
   * @param bitsToRead the number of bits to read from the bit buffer
   * @return the value of the bits read
   */
  public int getBits(int bitsToRead) {
    if (bitsToRead < 0) {
      throw new IllegalArgumentException("Bits to read may not be negative");
    }
    if (bitsToRead == 0) return 0;
    if (eofFlag) return -1; // Already at end of file
    int toStore = 0;
    while (bitsToRead != 0 && !eofFlag) {
      if (currentBit < 0 || currentBit > 7) {
        throw new IllegalStateException("byte=" +
          currentByte + ", bit = " + currentBit);
      }

      // if we need to read from more than the current byte in the buffer...
      int bitsLeft = 8 - currentBit;
      if (bitsToRead >= bitsLeft) {
        toStore <<= bitsLeft;
        bitsToRead -= bitsLeft;
        int cb = (int) byteBuffer[currentByte];
        if (currentBit == 0) {
          // we can read in a whole byte, so we'll do that.
          toStore += cb & 0xff;
        }
        else {
          // otherwise, only read the appropriate number of bits off the back
          // side of the byte, in order to "finish" the current byte in the
          // buffer.
          toStore += cb & BACK_MASK[bitsLeft];
          currentBit = 0;
        }
        currentByte++;
      }
      else {
        // We will be able to finish using the current byte.
        // read the appropriate number of bits off the front side of the byte,
        // then push them into the int.
        toStore = toStore << bitsToRead;
        int cb = byteBuffer[currentByte] & 0xff;
        toStore += (cb & (0x00FF - FRONT_MASK[currentBit])) >>
          (bitsLeft - bitsToRead);
        currentBit += bitsToRead;
        bitsToRead = 0;
      }
      // If we reach the end of the buffer, return what we currently have.
      if (currentByte == eofByte) {
        eofFlag = true;
        return toStore;
      }
    }
    return toStore;
  }

}
