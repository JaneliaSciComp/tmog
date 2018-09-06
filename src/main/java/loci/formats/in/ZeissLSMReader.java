/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

//
// ZeissLSMReader.java
//

package loci.formats.in;

import loci.common.RandomAccessInputStream;
import loci.formats.CoreMetadata;
import loci.formats.FormatException;
import loci.formats.tiff.IFD;
import loci.formats.tiff.IFDList;
import loci.formats.tiff.PhotoInterp;
import loci.formats.tiff.TiffConstants;
import loci.formats.tiff.TiffParser;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

/**
 * ZeissLSMReader is the file format reader for Zeiss LSM files.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="http://trac.openmicroscopy.org.uk/ome/browser/bioformats.git/components/bio-formats/src/loci/formats/in/ZeissLSMReader.java">Trac</a>,
 * <a href="http://git.openmicroscopy.org/?p=bioformats.git;a=blob;f=components/bio-formats/src/loci/formats/in/ZeissLSMReader.java;hb=HEAD">Gitweb</a></dd></dl>
 *
 * @author Eric Kjellman egkjellman at wisc.edu
 * @author Melissa Linkert melissa at glencoesoftware.com
 * @author Curtis Rueden ctrueden at wisc.edu
 */
public class ZeissLSMReader {


    // -- Constants --
  /** Tag identifying a Zeiss LSM file. */
  private static final int ZEISS_ID = 34412;

  /** Data types. */
  private static final int TYPE_SUBBLOCK = 0;
  private static final int TYPE_ASCII = 2;
  private static final int TYPE_LONG = 4;
  private static final int TYPE_RATIONAL = 5;
  private static final int TYPE_DATE = 6;
  private static final int TYPE_BOOLEAN = 7;

  /** Subblock types. */
  private static final int SUBBLOCK_RECORDING = 0x10000000;
  private static final int SUBBLOCK_LASER = 0x50000000;
  private static final int SUBBLOCK_TRACK = 0x40000000;
  private static final int SUBBLOCK_DETECTION_CHANNEL = 0x70000000;
  private static final int SUBBLOCK_ILLUMINATION_CHANNEL = 0x90000000;
  private static final int SUBBLOCK_BEAM_SPLITTER = 0xb0000000;
  private static final int SUBBLOCK_DATA_CHANNEL = 0xd0000000;
  private static final int SUBBLOCK_TIMER = 0x12000000;
  private static final int SUBBLOCK_MARKER = 0x14000000;
//  private static final int SUBBLOCK_END = (int) 0xffffffff;

  /** Data types. */
  private static final int RECORDING_NAME = 0x10000001;
  private static final int RECORDING_DESCRIPTION = 0x10000002;
  private static final int RECORDING_OBJECTIVE = 0x10000004;
  private static final int RECORDING_ZOOM = 0x10000016;
  private static final int RECORDING_SAMPLE_0TIME = 0x10000036;
  private static final int RECORDING_CAMERA_BINNING = 0x10000052;

  private static final int TRACK_ACQUIRE = 0x40000006;
  private static final int TRACK_TIME_BETWEEN_STACKS = 0x4000000b;

  private static final int LASER_NAME = 0x50000001;
  private static final int LASER_ACQUIRE = 0x50000002;
  private static final int LASER_POWER = 0x50000003;

  private static final int CHANNEL_DETECTOR_GAIN = 0x70000003;
  private static final int CHANNEL_PINHOLE_DIAMETER = 0x70000009;
  private static final int CHANNEL_AMPLIFIER_GAIN = 0x70000005;
  private static final int CHANNEL_FILTER_SET = 0x7000000f;
  private static final int CHANNEL_FILTER = 0x70000010;
  private static final int CHANNEL_ACQUIRE = 0x7000000b;
  private static final int CHANNEL_NAME = 0x70000014;

  private static final int ILLUM_CHANNEL_NAME = 0x90000001;
  private static final int ILLUM_CHANNEL_ATTENUATION = 0x90000002;
  private static final int ILLUM_CHANNEL_WAVELENGTH = 0x90000003;
  private static final int ILLUM_CHANNEL_ACQUIRE = 0x90000004;

//  private static final int START_TIME = 0x10000036;
  private static final int DATA_CHANNEL_NAME = 0xd0000001;
  private static final int DATA_CHANNEL_ACQUIRE = 0xd0000017;

  private static final int BEAM_SPLITTER_FILTER = 0xb0000002;
  private static final int BEAM_SPLITTER_FILTER_SET = 0xb0000003;

  /** Drawing element types. */
//  private static final int TEXT = 13;
//  private static final int LINE = 14;
//  private static final int SCALE_BAR = 15;
//  private static final int OPEN_ARROW = 16;
//  private static final int CLOSED_ARROW = 17;
//  private static final int RECTANGLE = 18;
//  private static final int ELLIPSE = 19;
//  private static final int CLOSED_POLYLINE = 20;
//  private static final int OPEN_POLYLINE = 21;
//  private static final int CLOSED_BEZIER = 22;
//  private static final int OPEN_BEZIER = 23;
//  private static final int CIRCLE = 24;
//  private static final int PALETTE = 25;
//  private static final int POLYLINE_ARROW = 26;
//  private static final int BEZIER_WITH_ARROW = 27;
//  private static final int ANGLE = 28;
//  private static final int CIRCLE_3POINT = 29;

  // -- Static fields --

  private static final Hashtable<Integer, String> METADATA_KEYS = createKeys();

  // -- Fields --

    private byte[][][] lut = null;

  private Vector<IFDList> ifdsList;
  private TiffParser tiffParser;

    private boolean splitPlanes = false;
//    private Vector<String> imageNames;
//  private String binning;
  private Vector<Double> xCoordinates, yCoordinates, zCoordinates;
  private int dimensionM, dimensionP;
//  private Hashtable<String, Integer> seriesCounts;

    // TODO: populate these
    protected RandomAccessInputStream in;
    private boolean littleEndian;
    private CoreMetadata core;
    private Map<String, Object> globalMeta;
    private List<SubBlock> subBlockList;

    /** Constructs a new Zeiss LSM reader. */
    public ZeissLSMReader() {
        globalMeta = new LinkedHashMap<String, Object>();
        subBlockList = new ArrayList<SubBlock>();
    }

    public CoreMetadata getCore() {
        return core;
    }

    public void printAll() {
        System.out.println(core);
        TreeSet<String> orderedKeys =
                new TreeSet<String>(core.seriesMetadata.keySet());
        System.out.println("--------------");
        for (String key : orderedKeys) {
            System.out.println(key + ":   " + core.seriesMetadata.get(key));
        }
    }

    private RandomAccessInputStream getCZTag(IFD ifd)
            throws FormatException, IOException {
        // get TIF_CZ_LSMINFO structure
        short[] s = ifd.getIFDShortArray(ZEISS_ID);
        RandomAccessInputStream ras = null;
        if (s != null) {
            byte[] cz = new byte[s.length];
            for (int i=0; i<s.length; i++) {
                cz[i] = (byte) s[i];
            }
            ras = new RandomAccessInputStream(cz);
            ras.order(littleEndian);
        }

        return ras;
    }

    private Object getSeriesMeta(String key) {
        return core.seriesMetadata.get(key);
    }

    private void addSeriesMeta(String key,
                               Object value) {
        core.seriesMetadata.put(key, value);
    }

    private void addGlobalMeta(String key,
                               Object value) {
        globalMeta.put(key, value);
    }

    private void populateMetadataStore(SubBlock block) {
        subBlockList.add(block);
    }

    private int getEffectiveSizeC() {
        // NB: by definition, imageCount == effectiveSizeC * sizeZ * sizeT
        int sizeZT = core.sizeZ * core.sizeT;
        if (sizeZT == 0) return 0;
        return core.imageCount / sizeZT;
    }

    private int getExtraSeries(String file) throws FormatException, IOException {
        if (in != null) in.close();
        in = new RandomAccessInputStream(file);
        littleEndian = in.read() == TiffConstants.LITTLE;
        in.order(littleEndian);

        tiffParser = new TiffParser(in);
        IFD ifd = tiffParser.getFirstIFD();
        RandomAccessInputStream ras = getCZTag(ifd);
        if (ras == null) return 1;
        ras.order(littleEndian);

        ras.seek(264);
        dimensionP = ras.readInt();
        dimensionM = ras.readInt();
        ras.close();

        int nSeries = dimensionM * dimensionP;
        return nSeries <= 0 ? 1 : nSeries;
    }

    public void initFile(String lsmFileName) throws FormatException, IOException {
        //initFile
        ifdsList = new Vector<IFDList>();
        ifdsList.setSize(1);

        int count = getExtraSeries(lsmFileName);

        RandomAccessInputStream stream =
                new RandomAccessInputStream(lsmFileName);
        TiffParser tp = new TiffParser(stream);
        littleEndian = tp.checkHeader();
        long[] ifdOffsets = tp.getIFDOffsets();
        int ifdsPerSeries = (ifdOffsets.length / 2) / count;

        int offset = 0;
        Object zeissTag = null;
        core = new CoreMetadata();
        core.littleEndian = littleEndian;

        IFDList ifds = new IFDList();
        while (ifds.size() < ifdsPerSeries) {
            tp.setDoCaching(offset == 0);
            IFD ifd = tp.getIFD(ifdOffsets[offset]);
            if (offset == 0) zeissTag = ifd.get(ZEISS_ID);
            if (offset > 0 && ifds.size() == 0) {
                ifd.putIFDValue(ZEISS_ID, zeissTag);
            }
            ifds.add(ifd);
            if (zeissTag != null) offset += 2;
            else offset++;
        }

        for (IFD ifd : ifds) {
            tp.fillInIFD(ifd);
        }

        ifdsList.set(0, ifds);
        
        stream.close();

        xCoordinates = new Vector<Double>();
        yCoordinates = new Vector<Double>();
        zCoordinates = new Vector<Double>();

        lut = new byte[ifdsList.size()][][];

        initMetadata(lsmFileName);
    }

    private void initMetadata(String lsmFileName) throws FormatException, IOException {

        //setSeries(series);
        IFDList ifds = ifdsList.get(0);
        IFD ifd = ifds.get(0);

        in.close();
        in = new RandomAccessInputStream(lsmFileName);
        in.order(core.littleEndian);

        tiffParser = new TiffParser(in);

        PhotoInterp photo = ifd.getPhotometricInterpretation();
        int samples = ifd.getSamplesPerPixel();

        core.sizeX = (int) ifd.getImageWidth();
        core.sizeY = (int) ifd.getImageLength();
        core.rgb = samples > 1 || photo == PhotoInterp.RGB;
        core.interleaved = false;
        core.sizeC = core.rgb ? samples : 1;
        core.pixelType = ifd.getPixelType();
        core.imageCount = ifds.size();
        core.sizeZ = core.imageCount;
        core.sizeT = 1;

        RandomAccessInputStream ras = getCZTag(ifd);
        if (ras == null) {
//      imageNames.add(imageName);
            return;
        }

        ras.seek(16);

        core.sizeZ = ras.readInt();
        ras.skipBytes(4);
        core.sizeT = ras.readInt();

        int dataType = ras.readInt();
        switch (dataType) {
            case 2:
                addSeriesMeta("DataType", "12 bit unsigned integer");
                break;
            case 5:
                addSeriesMeta("DataType", "32 bit float");
                break;
            case 0:
                addSeriesMeta("DataType", "varying data types");
                break;
            default:
                addSeriesMeta("DataType", "8 bit unsigned integer");
        }

        ras.seek(0);
        addSeriesMeta("MagicNumber ", ras.readInt());
        addSeriesMeta("StructureSize", ras.readInt());
        addSeriesMeta("DimensionX", ras.readInt());
        addSeriesMeta("DimensionY", ras.readInt());

        ras.seek(32);
        addSeriesMeta("ThumbnailX", ras.readInt());
        addSeriesMeta("ThumbnailY", ras.readInt());

        // pixel sizes are stored in meters, we need them in microns
        double pixelSizeX = ras.readDouble() * 1000000;
        double pixelSizeY = ras.readDouble() * 1000000;
        double pixelSizeZ = ras.readDouble() * 1000000;

        addSeriesMeta("VoxelSizeX", pixelSizeX);
        addSeriesMeta("VoxelSizeY", pixelSizeY);
        addSeriesMeta("VoxelSizeZ", pixelSizeZ);

        double originX = ras.readDouble() * 1000000;
        double originY = ras.readDouble() * 1000000;
        double originZ = ras.readDouble() * 1000000;

        addSeriesMeta("OriginX", originX);
        addSeriesMeta("OriginY", originY);
        addSeriesMeta("OriginZ", originZ);

        int scanType = ras.readShort();
        switch (scanType) {
            case 0:
                addSeriesMeta("ScanType", "x-y-z scan");
                core.dimensionOrder = "XYZCT";
                break;
            case 1:
                addSeriesMeta("ScanType", "z scan (x-z plane)");
                core.dimensionOrder = "XYZCT";
                break;
            case 2:
                addSeriesMeta("ScanType", "line scan");
                core.dimensionOrder = "XYZCT";
                break;
            case 3:
                addSeriesMeta("ScanType", "time series x-y");
                core.dimensionOrder = "XYTCZ";
                break;
            case 4:
                addSeriesMeta("ScanType", "time series x-z");
                core.dimensionOrder = "XYZTC";
                break;
            case 5:
                addSeriesMeta("ScanType", "time series 'Mean of ROIs'");
                core.dimensionOrder = "XYTCZ";
                break;
            case 6:
                addSeriesMeta("ScanType", "time series x-y-z");
                core.dimensionOrder = "XYZTC";
                break;
            case 7:
                addSeriesMeta("ScanType", "spline scan");
                core.dimensionOrder = "XYCTZ";
                break;
            case 8:
                addSeriesMeta("ScanType", "spline scan x-z");
                core.dimensionOrder = "XYCZT";
                break;
            case 9:
                addSeriesMeta("ScanType", "time series spline plane x-z");
                core.dimensionOrder = "XYTCZ";
                break;
            case 10:
                addSeriesMeta("ScanType", "point mode");
                core.dimensionOrder = "XYZCT";
                break;
            default:
                addSeriesMeta("ScanType", "x-y-z scan");
                core.dimensionOrder = "XYZCT";
        }

        core.indexed = lut != null && lut[0] != null;
        if (core.indexed) {
            core.rgb = false;
        }
        if (core.sizeC == 0) core.sizeC = 1;

        if (core.rgb) {
            // shuffle C to front of order string
            core.dimensionOrder = core.dimensionOrder.replaceAll("C", "");
            core.dimensionOrder = core.dimensionOrder.replaceAll("XY", "XYC");
        }

        if (getEffectiveSizeC() == 0) {
            core.imageCount = core.sizeZ * core.sizeT;
        }
        else {
            core.imageCount = core.sizeZ * core.sizeT * getEffectiveSizeC();
        }

        if (core.imageCount != ifds.size()) {
            int diff = core.imageCount - ifds.size();
            core.imageCount = ifds.size();
            if (diff % core.sizeZ == 0) {
                core.sizeT -= (diff / core.sizeZ);
            }
            else if (diff % core.sizeT == 0) {
                core.sizeZ -= (diff / core.sizeT);
            }
            else if (core.sizeZ > 1) {
                core.sizeZ = ifds.size();
                core.sizeT = 1;
            }
            else if (core.sizeT > 1) {
                core.sizeT = ifds.size();
                core.sizeZ = 1;
            }
        }

        if (core.sizeZ == 0) core.sizeZ = core.imageCount;
        if (core.sizeT == 0) core.sizeT = core.imageCount / core.sizeZ;

        long channelColorsOffset = 0;
        long timeStampOffset = 0;
        long eventListOffset = 0;
        long scanInformationOffset = 0;
        long channelWavelengthOffset = 0;
        long applicationTagOffset = 0;
        @SuppressWarnings({"MismatchedReadAndWriteOfArray"})
        int[] channelColor = new int[core.sizeC];

        int spectralScan = ras.readShort();
        if (spectralScan != 1) {
            addSeriesMeta("SpectralScan", "no spectral scan");
        }
        else addSeriesMeta("SpectralScan", "acquired with spectral scan");

        int type = ras.readInt();
        switch (type) {
            case 1:
                addSeriesMeta("DataType2", "calculated data");
                break;
            case 2:
                addSeriesMeta("DataType2", "animation");
                break;
            default:
                addSeriesMeta("DataType2", "original scan data");
        }

        @SuppressWarnings({"MismatchedReadAndWriteOfArray"})
        long[] overlayOffsets = new long[9];

        overlayOffsets[0] = ras.readInt();
        overlayOffsets[1] = ras.readInt();
        overlayOffsets[2] = ras.readInt();

        channelColorsOffset = ras.readInt();

        addSeriesMeta("TimeInterval", ras.readDouble());
        ras.skipBytes(4);
        scanInformationOffset = ras.readInt();
        applicationTagOffset = ras.readInt();
        timeStampOffset = ras.readInt();
        eventListOffset = ras.readInt();
        overlayOffsets[3] = ras.readInt();
        overlayOffsets[4] = ras.readInt();
        ras.skipBytes(4);

        addSeriesMeta("DisplayAspectX", ras.readDouble());
        addSeriesMeta("DisplayAspectY", ras.readDouble());
        addSeriesMeta("DisplayAspectZ", ras.readDouble());
        addSeriesMeta("DisplayAspectTime", ras.readDouble());

        overlayOffsets[5] = ras.readInt();
        overlayOffsets[6] = ras.readInt();
        overlayOffsets[7] = ras.readInt();
        overlayOffsets[8] = ras.readInt();

        addSeriesMeta("ToolbarFlags", ras.readInt());

        ras.skipBytes(64);


        if (core.sizeC > 1) {
            if (!splitPlanes) splitPlanes = core.rgb;
            core.rgb = false;
            if (splitPlanes) core.imageCount *= core.sizeC;
        }

        // NB: the Zeiss LSM 5.5 specification indicates that there should be
        //     15 32-bit integers here; however, there are actually 16 32-bit
        //     integers before the tile position offset.
        //     We have confirmed with Zeiss that this is correct, and the 6.0
        //     specification was updated to contain the correct information.
        ras.skipBytes(64);

        int tilePositionOffset = ras.readInt();

        ras.skipBytes(36);

        int positionOffset = ras.readInt();

        // Janelia hack:
        // Add check for out of range position offset since it was
        // killing parsing for some LSM files.
        // This simply skips over the problem since I have no idea what the
        // real problem is.
        if (positionOffset > ras.length()) {
            positionOffset = 0;            
        }

        // read referenced structures

        addSeriesMeta("DimensionZ", core.sizeZ);
        addSeriesMeta("DimensionChannels", core.sizeC);
        addSeriesMeta("DimensionM", dimensionM);
        addSeriesMeta("DimensionP", dimensionP);

        xCoordinates.clear();
        yCoordinates.clear();
        zCoordinates.clear();

        if (positionOffset != 0) {
            in.seek(positionOffset);
            int nPositions = in.readInt();
            for (int i=0; i<nPositions; i++) {
                double xPos = originX + in.readDouble() * 1000000;
                double yPos = originY + in.readDouble() * 1000000;
                double zPos = originZ + in.readDouble() * 1000000;
                xCoordinates.add(xPos);
                yCoordinates.add(yPos);
                zCoordinates.add(zPos);

                addGlobalMeta("X position for position #" + (i + 1), xPos);
                addGlobalMeta("Y position for position #" + (i + 1), yPos);
                addGlobalMeta("Z position for position #" + (i + 1), zPos);
            }
        }

        if (tilePositionOffset != 0) {
            in.seek(tilePositionOffset);
            int nTiles = in.readInt();
            for (int i=0; i<nTiles; i++) {
                double xPos = originX + in.readDouble() * 1000000;
                double yPos = originY + in.readDouble() * 1000000;
                double zPos = originZ + in.readDouble() * 1000000;
                if (xCoordinates.size() > i) {
                    xPos += xCoordinates.get(i);
                    xCoordinates.setElementAt(xPos, i);
                }
                else if (xCoordinates.size() == i) {
                    xCoordinates.add(xPos);
                }
                if (yCoordinates.size() > i) {
                    yPos += yCoordinates.get(i);
                    yCoordinates.setElementAt(yPos, i);
                }
                else if (yCoordinates.size() == i) {
                    yCoordinates.add(yPos);
                }
                if (zCoordinates.size() > i) {
                    zPos += zCoordinates.get(i);
                    zCoordinates.setElementAt(zPos, i);
                }
                else if (zCoordinates.size() == i) {
                    zCoordinates.add(zPos);
                }

                addGlobalMeta("X position for position #" + (i + 1), xPos);
                addGlobalMeta("Y position for position #" + (i + 1), yPos);
                addGlobalMeta("Z position for position #" + (i + 1), zPos);
            }
        }

        if (channelColorsOffset != 0) {
            in.seek(channelColorsOffset + 12);
            int colorsOffset = in.readInt();
            int namesOffset = in.readInt();

            // read the color of each channel

            if (colorsOffset > 0) {
                in.seek(channelColorsOffset + colorsOffset);
                lut[0] = new byte[core.sizeC * 3][256];
                core.indexed = true;
                for (int i=0; i<core.sizeC; i++) {
                    int color = in.readInt();

                    channelColor[i] = color;

                    int red = color & 0xff;
                    int green = (color & 0xff00) >> 8;
                    int blue = (color & 0xff0000) >> 16;

                    for (int j=0; j<256; j++) {
                        lut[0][i * 3][j] = (byte) ((red / 255.0) * j);
                        lut[0][i * 3 + 1][j] = (byte) ((green / 255.0) * j);
                        lut[0][i * 3 + 2][j] = (byte) ((blue / 255.0) * j);
                    }
                }
            }

            // read the name of each channel

            if (namesOffset > 0) {
                in.seek(channelColorsOffset + namesOffset + 4);

                for (int i=0; i<core.sizeC; i++) {
                    if (in.getFilePointer() >= in.length() - 1) break;
                    // we want to read until we find a null char
                    String name = in.readCString();
                    if (name.length() <= 128) {
                        addSeriesMeta("ChannelName" + i, name);
                    }
                }
            }
        }

        if (timeStampOffset != 0) {
            in.seek(timeStampOffset + 4);
            int nStamps = in.readInt();
            for (int i=0; i<nStamps; i++) {
                double stamp = in.readDouble();
                addSeriesMeta("TimeStamp" + i, stamp);
            }
        }

        if (eventListOffset != 0) {
            in.seek(eventListOffset + 4);
            int numEvents = in.readInt();
            in.seek(in.getFilePointer() - 4);
            in.order(!in.isLittleEndian());
            int tmpEvents = in.readInt();
            if (numEvents < 0) numEvents = tmpEvents;
            else numEvents = Math.min(numEvents, tmpEvents);
            in.order(!in.isLittleEndian());

            if (numEvents > 65535) numEvents = 0;

            for (int i=0; i<numEvents; i++) {
                if (in.getFilePointer() + 16 <= in.length()) {
                    int size = in.readInt();
                    double eventTime = in.readDouble();
                    int eventType = in.readInt();
                    addSeriesMeta("Event" + i + " Time", eventTime);
                    addSeriesMeta("Event" + i + " Type", eventType);
                    long fp = in.getFilePointer();
                    int len = size - 16;
                    if (len > 65536) len = 65536;
                    if (len < 0) len = 0;
                    addSeriesMeta("Event" + i + " Description", in.readString(len));
                    in.seek(fp + size - 16);
                    if (in.getFilePointer() < 0) break;
                }
            }
        }

        if (scanInformationOffset != 0) {
            in.seek(scanInformationOffset);

            Vector<SubBlock> blocks = new Vector<SubBlock>();

            while (in.getFilePointer() < in.length() - 12) {
                if (in.getFilePointer() < 0) break;
                int entry = in.readInt();
                int blockType = in.readInt();
                int dataSize = in.readInt();

                if (blockType == TYPE_SUBBLOCK) {
                    SubBlock block = null;
                    switch (entry) {
                        case SUBBLOCK_RECORDING:
                            block = new Recording();
                            break;
                        case SUBBLOCK_LASER:
                            block = new Laser();
                            break;
                        case SUBBLOCK_TRACK:
                            block = new Track();
                            break;
                        case SUBBLOCK_DETECTION_CHANNEL:
                            block = new DetectionChannel();
                            break;
                        case SUBBLOCK_ILLUMINATION_CHANNEL:
                            block = new IlluminationChannel();
                            break;
                        case SUBBLOCK_BEAM_SPLITTER:
                            block = new BeamSplitter();
                            break;
                        case SUBBLOCK_DATA_CHANNEL:
                            block = new DataChannel();
                            break;
                        case SUBBLOCK_TIMER:
                            block = new Timer();
                            break;
                        case SUBBLOCK_MARKER:
                            block = new Marker();
                            break;
                    }
                    if (block != null) {
                        blocks.add(block);
                    }
                }
                else if (dataSize + in.getFilePointer() <= in.length() &&
                         dataSize > 0)
                {
                    in.skipBytes(dataSize);
                }
                else break;
            }

            Vector<SubBlock> nonAcquiredBlocks = new Vector<SubBlock>();

            SubBlock[] metadataBlocks =
                    blocks.toArray(new SubBlock[blocks.size()]);
            for (SubBlock block : metadataBlocks) {
                block.addToHashtable();
                if (!block.acquire) {
                    nonAcquiredBlocks.add(block);
                    blocks.remove(block);
                }
            }

            for (int i=0; i<blocks.size(); i++) {
                SubBlock block = blocks.get(i);
                // every valid IlluminationChannel must be immediately followed by
                // a valid DataChannel or IlluminationChannel
                if ((block instanceof IlluminationChannel) && i < blocks.size() - 1) {
                    SubBlock nextBlock = blocks.get(i + 1);
                    if (!(nextBlock instanceof DataChannel) &&
                        !(nextBlock instanceof IlluminationChannel))
                    {
                        ((IlluminationChannel) block).wavelength = null;
                    }
                }
                // every valid DetectionChannel must be immediately preceded by
                // a valid Track or DetectionChannel
                else if ((block instanceof DetectionChannel) && i > 0) {
                    SubBlock prevBlock = blocks.get(i - 1);
                    if (!(prevBlock instanceof Track) &&
                        !(prevBlock instanceof DetectionChannel))
                    {
                        block.acquire = false;
                        nonAcquiredBlocks.add(block);
                    }
                }
                if (block.acquire) populateMetadataStore(block);
            }

            for (SubBlock block : nonAcquiredBlocks) {
                populateMetadataStore(block);
            }
        }

        if (applicationTagOffset != 0) {
            in.seek(applicationTagOffset);
            parseApplicationTags();
        }

//        imageNames.add(imageName);

//        Double pixX = new Double(pixelSizeX);
//        Double pixY = new Double(pixelSizeY);
//        Double pixZ = new Double(pixelSizeZ);
//
//        store.setPixelsPhysicalSizeX(new PositiveFloat(pixX), series);
//        store.setPixelsPhysicalSizeY(new PositiveFloat(pixY), series);
//        store.setPixelsPhysicalSizeZ(new PositiveFloat(pixZ), series);
//
//        for (int i=0; i<core.sizeC; i++) {
//            store.setChannelColor(channelColor[i], series, i);
//        }
//
//        double firstStamp = 0;
//        if (timestamps.size() > 0) {
//            firstStamp = timestamps.get(0).doubleValue();
//        }
//
//        for (int i=0; i<core.imageCount; i++) {
//            int[] zct = FormatTools.getZCTCoords(this, i);
//
//            if (zct[2] < timestamps.size()) {
//                double thisStamp = timestamps.get(zct[2]).doubleValue();
//                store.setPlaneDeltaT(thisStamp - firstStamp, series, i);
//                int index = zct[2] + 1;
//                double nextStamp = index < timestamps.size() ?
//                                   timestamps.get(index).doubleValue() : thisStamp;
//                if (i == core.sizeT - 1 && zct[2] > 0) {
//                    thisStamp = timestamps.get(zct[2] - 1).doubleValue();
//                }
//                store.setPlaneExposureTime(nextStamp - thisStamp, series, i);
//            }
//            if (xCoordinates.size() > series) {
//                store.setPlanePositionX(xCoordinates.get(series), series, i);
//                store.setPlanePositionY(yCoordinates.get(series), series, i);
//                store.setPlanePositionZ(zCoordinates.get(series), series, i);
//            }
//        }

        ras.close();

        // TMOG bug fix:
        // force close so that file can be removed by other processes on Windows 7 
        in.close();
    }

    private static Hashtable<Integer, String> createKeys() {
        Hashtable<Integer, String> h = new Hashtable<Integer, String>();
        h.put(0x10000001, "Name");
        h.put(0x4000000c, "Name");
        h.put(0x50000001, "Name");
        h.put(0x90000001, "Name");
        h.put(0x90000005, "Detection Channel Name");
        h.put(0xb0000003, "Name");
        h.put(0xd0000001, "Name");
        h.put(0x12000001, "Name");
        h.put(0x14000001, "Name");
        h.put(0x10000002, "Description");
        h.put(0x14000002, "Description");
        h.put(0x10000003, "Notes");
        h.put(0x10000004, "Objective");
        h.put(0x10000005, "Processing Summary");
        h.put(0x10000006, "Special Scan Mode");
        h.put(0x10000007, "Scan Type");
        h.put(0x10000008, "Scan Mode");
        h.put(0x10000009, "Number of Stacks");
        h.put(0x1000000a, "Lines Per Plane");
        h.put(0x1000000b, "Samples Per Line");
        h.put(0x1000000c, "Planes Per Volume");
        h.put(0x1000000d, "Images Width");
        h.put(0x1000000e, "Images Height");
        h.put(0x1000000f, "Number of Planes");
        h.put(0x10000010, "Number of Stacks");
        h.put(0x10000011, "Number of Channels");
        h.put(0x10000012, "Linescan XY Size");
        h.put(0x10000013, "Scan Direction");
        h.put(0x10000014, "Time Series");
        h.put(0x10000015, "Original Scan Data");
        h.put(0x10000016, "Zoom X");
        h.put(0x10000017, "Zoom Y");
        h.put(0x10000018, "Zoom Z");
        h.put(0x10000019, "Sample 0X");
        h.put(0x1000001a, "Sample 0Y");
        h.put(0x1000001b, "Sample 0Z");
        h.put(0x1000001c, "Sample Spacing");
        h.put(0x1000001d, "Line Spacing");
        h.put(0x1000001e, "Plane Spacing");
        h.put(0x1000001f, "Plane Width");
        h.put(0x10000020, "Plane Height");
        h.put(0x10000021, "Volume Depth");
        h.put(0x10000034, "Rotation");
        h.put(0x10000035, "Precession");
        h.put(0x10000036, "Sample 0Time");
        h.put(0x10000037, "Start Scan Trigger In");
        h.put(0x10000038, "Start Scan Trigger Out");
        h.put(0x10000039, "Start Scan Event");
        h.put(0x10000040, "Start Scan Time");
        h.put(0x10000041, "Stop Scan Trigger In");
        h.put(0x10000042, "Stop Scan Trigger Out");
        h.put(0x10000043, "Stop Scan Event");
        h.put(0x10000044, "Stop Scan Time");
        h.put(0x10000045, "Use ROIs");
        h.put(0x10000046, "Use Reduced Memory ROIs");
        h.put(0x10000047, "User");
        h.put(0x10000048, "Use B/C Correction");
        h.put(0x10000049, "Position B/C Contrast 1");
        h.put(0x10000050, "Position B/C Contrast 2");
        h.put(0x10000051, "Interpolation Y");
        h.put(0x10000052, "Camera Binning");
        h.put(0x10000053, "Camera Supersampling");
        h.put(0x10000054, "Camera Frame Width");
        h.put(0x10000055, "Camera Frame Height");
        h.put(0x10000056, "Camera Offset X");
        h.put(0x10000057, "Camera Offset Y");
        h.put(0x40000001, "Multiplex Type");
        h.put(0x40000002, "Multiplex Order");
        h.put(0x40000003, "Sampling Mode");
        h.put(0x40000004, "Sampling Method");
        h.put(0x40000005, "Sampling Number");
        h.put(0x40000006, "Acquire");
        h.put(0x50000002, "Acquire");
        h.put(0x7000000b, "Acquire");
        h.put(0x90000004, "Acquire");
        h.put(0xd0000017, "Acquire");
        h.put(0x40000007, "Sample Observation Time");
        h.put(0x40000008, "Time Between Stacks");
        h.put(0x4000000d, "Collimator 1 Name");
        h.put(0x4000000e, "Collimator 1 Position");
        h.put(0x4000000f, "Collimator 2 Name");
        h.put(0x40000010, "Collimator 2 Position");
        h.put(0x40000011, "Is Bleach Track");
        h.put(0x40000012, "Bleach After Scan Number");
        h.put(0x40000013, "Bleach Scan Number");
        h.put(0x40000014, "Trigger In");
        h.put(0x12000004, "Trigger In");
        h.put(0x14000003, "Trigger In");
        h.put(0x40000015, "Trigger Out");
        h.put(0x12000005, "Trigger Out");
        h.put(0x14000004, "Trigger Out");
        h.put(0x40000016, "Is Ratio Track");
        h.put(0x40000017, "Bleach Count");
        h.put(0x40000018, "SPI Center Wavelength");
        h.put(0x40000019, "Pixel Time");
        h.put(0x40000020, "ID Condensor Frontlens");
        h.put(0x40000021, "Condensor Frontlens");
        h.put(0x40000022, "ID Field Stop");
        h.put(0x40000023, "Field Stop Value");
        h.put(0x40000024, "ID Condensor Aperture");
        h.put(0x40000025, "Condensor Aperture");
        h.put(0x40000026, "ID Condensor Revolver");
        h.put(0x40000027, "Condensor Revolver");
        h.put(0x40000028, "ID Transmission Filter 1");
        h.put(0x40000029, "ID Transmission 1");
        h.put(0x40000030, "ID Transmission Filter 2");
        h.put(0x40000031, "ID Transmission 2");
        h.put(0x40000032, "Repeat Bleach");
        h.put(0x40000033, "Enable Spot Bleach Pos");
        h.put(0x40000034, "Spot Bleach Position X");
        h.put(0x40000035, "Spot Bleach Position Y");
        h.put(0x40000036, "Bleach Position Z");
        h.put(0x50000003, "Power");
        h.put(0x90000002, "Power");
        h.put(0x70000003, "Detector Gain");
        h.put(0x70000005, "Amplifier Gain");
        h.put(0x70000007, "Amplifier Offset");
        h.put(0x70000009, "Pinhole Diameter");
        h.put(0x7000000c, "Detector Name");
        h.put(0x7000000d, "Amplifier Name");
        h.put(0x7000000e, "Pinhole Name");
        h.put(0x7000000f, "Filter Set Name");
        h.put(0x70000010, "Filter Name");
        h.put(0x70000013, "Integrator Name");
        h.put(0x70000014, "Detection Channel Name");
        h.put(0x70000015, "Detector Gain B/C 1");
        h.put(0x70000016, "Detector Gain B/C 2");
        h.put(0x70000017, "Amplifier Gain B/C 1");
        h.put(0x70000018, "Amplifier Gain B/C 2");
        h.put(0x70000019, "Amplifier Offset B/C 1");
        h.put(0x70000020, "Amplifier Offset B/C 2");
        h.put(0x70000021, "Spectral Scan Channels");
        h.put(0x70000022, "SPI Wavelength Start");
        h.put(0x70000023, "SPI Wavelength End");
        h.put(0x70000026, "Dye Name");
        h.put(0xd0000014, "Dye Name");
        h.put(0x70000027, "Dye Folder");
        h.put(0xd0000015, "Dye Folder");
        h.put(0x90000003, "Wavelength");
        h.put(0x90000006, "Power B/C 1");
        h.put(0x90000007, "Power B/C 2");
        h.put(0xb0000001, "Filter Set");
        h.put(0xb0000002, "Filter");
        h.put(0xd0000004, "Color");
        h.put(0xd0000005, "Sample Type");
        h.put(0xd0000006, "Bits Per Sample");
        h.put(0xd0000007, "Ratio Type");
        h.put(0xd0000008, "Ratio Track 1");
        h.put(0xd0000009, "Ratio Track 2");
        h.put(0xd000000a, "Ratio Channel 1");
        h.put(0xd000000b, "Ratio Channel 2");
        h.put(0xd000000c, "Ratio Const. 1");
        h.put(0xd000000d, "Ratio Const. 2");
        h.put(0xd000000e, "Ratio Const. 3");
        h.put(0xd000000f, "Ratio Const. 4");
        h.put(0xd0000010, "Ratio Const. 5");
        h.put(0xd0000011, "Ratio Const. 6");
        h.put(0xd0000012, "Ratio First Images 1");
        h.put(0xd0000013, "Ratio First Images 2");
        h.put(0xd0000016, "Spectrum");
        h.put(0x12000003, "Interval");
        return h;
    }

    private Integer readEntry() throws IOException {
        return in.readInt();
    }

    private Object readValue() throws IOException {
        int blockType = in.readInt();
        int dataSize = in.readInt();

        switch (blockType) {
            case TYPE_LONG:
                return (long) in.readInt();
            case TYPE_RATIONAL:
                return in.readDouble();
            case TYPE_ASCII:
                String s = in.readString(dataSize).trim();
                StringBuffer sb = new StringBuffer();
                for (int i=0; i<s.length(); i++) {
                    if (s.charAt(i) >= 10) sb.append(s.charAt(i));
                    else break;
                }

                return sb.toString();
            case TYPE_SUBBLOCK:
                return null;
        }
        in.skipBytes(dataSize);
        return "";
    }

    private void parseApplicationTags() throws IOException {
        int blockSize = in.readInt();
        int numEntries = in.readInt();

        for (int i=0; i<numEntries; i++) {
            long fp = in.getFilePointer();
            int entrySize = in.readInt();
            int entryNameLength = in.readInt();
            String entryName = in.readString(entryNameLength);

            int dataType = in.readInt();
            int dataSize = in.readInt();

            Object data = null;

            switch (dataType) {
                case TYPE_ASCII:
                    data = in.readString(dataSize);
                    break;
                case TYPE_LONG:
                    data = in.readInt();
                    break;
                case TYPE_RATIONAL:
                    data = in.readDouble();
                    break;
                case TYPE_DATE:
                    data = in.readLong();
                    break;
                case TYPE_BOOLEAN:
                    data = in.readInt() == 0;
                    break;
            }

            addGlobalMeta(entryName, data);

            if (in.getFilePointer() == fp + entrySize) {
                continue;
            }

            int nDimensions = in.readInt();
//            int[] coordinate = new int[nDimensions];

            for (int n=0; n<nDimensions; n++) {
//                coordinate[n] =
                        in.readInt();
            }
        }
    }

    // -- Helper classes --

    class SubBlock {
        public Hashtable<Integer, Object> blockData;
        public boolean acquire = true;

        public SubBlock() {
            try {
                read();
            }
            catch (IOException e) {
                LOG.error("Failed to read sub-block data", e);
            }
        }

        protected int getIntValue(int key) {
            Object o = blockData.get(new Integer(key));
            if (o == null) return -1;
            return !(o instanceof Number) ? -1 : ((Number) o).intValue();
        }

//        protected float getFloatValue(int key) {
//            Object o = blockData.get(new Integer(key));
//            if (o == null) return -1f;
//            return !(o instanceof Number) ? -1f : ((Number) o).floatValue();
//        }

        protected double getDoubleValue(int key) {
            Object o = blockData.get(new Integer(key));
            if (o == null) return -1d;
            return !(o instanceof Number) ? -1d : ((Number) o).doubleValue();
        }

        protected String getStringValue(int key) {
            Object o = blockData.get(new Integer(key));
            return o == null ? null : o.toString();
        }

        protected void read() throws IOException {
            blockData = new Hashtable<Integer, Object>();
            Integer entry = readEntry();
            Object value = readValue();
            while (value != null && in.getFilePointer() < in.length()) {
                if (!blockData.containsKey(entry)) blockData.put(entry, value);
                entry = readEntry();
                value = readValue();
            }
        }

        public void addToHashtable() {
            String prefix = this.getClass().getSimpleName() + " #";
            int index = 1;
            while (getSeriesMeta(prefix + index + " Acquire") != null) index++;
            prefix += index;
            Integer[] keys =
                    blockData.keySet().toArray(new Integer[blockData.keySet().size()]);
            for (Integer key : keys) {
                if (METADATA_KEYS.get(key) != null) {
                    addSeriesMeta(prefix + " " + METADATA_KEYS.get(key),
                                  blockData.get(key));

                    if (METADATA_KEYS.get(key).equals("Bits Per Sample")) {
                        core.bitsPerPixel =
                                Integer.parseInt(blockData.get(key).toString());
                    }
                    // TODO: save userName
//                    else if (METADATA_KEYS.get(key).equals("User")) {
//                        String userName = blockData.get(key).toString();
//                    }
                }
            }
            addGlobalMeta(prefix + " Acquire", acquire);
        }
    }

    class Recording extends SubBlock {
        public String description;
        public String name;
        public String binning;
        public String startTime;
        // Objective data
        public String correction, immersion;
        public Integer magnification;
        public Double lensNA;
        public Boolean iris;

        protected void read() throws IOException {
            super.read();
            description = getStringValue(RECORDING_DESCRIPTION);
            name = getStringValue(RECORDING_NAME);
            binning = getStringValue(RECORDING_CAMERA_BINNING);
            if (binning != null && binning.indexOf("x") == -1) {
                if (binning.equals("0")) binning = null;
                else binning += "x" + binning;
            }

            // start time in days since Dec 30 1899
//            long stamp = (long) (getDoubleValue(RECORDING_SAMPLE_0TIME) * 86400000);
//            if (stamp > 0) {
//                startTime = DateTools.convertDate(stamp, DateTools.MICROSOFT);
//            }

            // ==========================================
            // ETT: begin hack to format MS Access recording time
            final double recordingSampleTime = getDoubleValue(RECORDING_SAMPLE_0TIME);
            final int daysSinceDec301899 = (int) recordingSampleTime;
            if (daysSinceDec301899 > 0) {
                Calendar c = new GregorianCalendar(1899, 11, 30); // month value is zero based!
                c.add(Calendar.DAY_OF_YEAR, daysSinceDec301899);

                final double fractionOfDay = recordingSampleTime - daysSinceDec301899;
                final double secondsInDay = fractionOfDay * 60.0 * 60.0 * 24.0;

                c.add(Calendar.SECOND, (int) secondsInDay);

                final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                startTime = sdf.format(c.getTime());
                blockData.put(RECORDING_SAMPLE_0TIME, startTime);
            }
            // ETT: end hack to format MS Access recording time
            // ==========================================

            double zoom = getDoubleValue(RECORDING_ZOOM);

            String objective = getStringValue(RECORDING_OBJECTIVE);

            correction = "";

            if (objective == null) objective = "";
            String[] tokens = objective.split(" ");
            int next = 0;
            for (; next<tokens.length; next++) {
                if (tokens[next].indexOf("/") != -1) break;
                correction += tokens[next];
            }
            if (next < tokens.length) {
                String p = tokens[next++];
                int slash = p.indexOf("/");
                if (slash > 0) {
                    try {
                        magnification = new Integer(p.substring(0, slash - 1));
                    }
                    catch (NumberFormatException e) {
                        LOG.warn("ignoring magnification parse exception", e);
                    }
                }
                if (slash >= 0 && slash < p.length() - 1) {
                    try {
                        lensNA = new Double(p.substring(slash + 1));
                    }
                    catch (NumberFormatException e) {
                        LOG.warn("ignoring lensNA parse exception", e);
                    }
                }
            }

            immersion = next < tokens.length ? tokens[next++] : "Unknown";
            iris = Boolean.FALSE;
            final int nextPlusOne = next + 1;
            if (nextPlusOne < tokens.length) {
                iris = tokens[nextPlusOne].trim().equalsIgnoreCase("iris");
            }
        }
    }

    class Laser extends SubBlock {
        public String medium, type, model;
        public Double power;

        protected void read() throws IOException {
            super.read();
            model = getStringValue(LASER_NAME);
            type = getStringValue(LASER_NAME);
            if (type == null) type = "";
            medium = "";

            if (type.startsWith("HeNe")) {
                medium = "HeNe";
                type = "Gas";
            }
            else if (type.startsWith("Argon")) {
                medium = "Ar";
                type = "Gas";
            }
            else if (type.equals("Titanium:Sapphire") || type.equals("Mai Tai")) {
                medium = "TiSapphire";
                type = "SolidState";
            }
            else if (type.equals("YAG")) {
                medium = "";
                type = "SolidState";
            }
            else if (type.equals("Ar/Kr")) {
                medium = "";
                type = "Gas";
            }

            acquire = getIntValue(LASER_ACQUIRE) != 0;
            power = getDoubleValue(LASER_POWER);
        }
    }

    class Track extends SubBlock {
        public Double timeIncrement;

        protected void read() throws IOException {
            super.read();
            timeIncrement = getDoubleValue(TRACK_TIME_BETWEEN_STACKS);
            acquire = getIntValue(TRACK_ACQUIRE) != 0;
        }
    }

    class DetectionChannel extends SubBlock {
        public Double pinhole;
        public Double gain, amplificationGain;
        public String filter, filterSet;
        public String channelName;

        protected void read() throws IOException {
            super.read();
            pinhole = getDoubleValue(CHANNEL_PINHOLE_DIAMETER);
            gain = getDoubleValue(CHANNEL_DETECTOR_GAIN);
            amplificationGain = getDoubleValue(CHANNEL_AMPLIFIER_GAIN);
            filter = getStringValue(CHANNEL_FILTER);
            if (filter != null) {
                filter = filter.trim();
                if (filter.length() == 0 || filter.equals("None")) {
                    filter = null;
                }
            }

            filterSet = getStringValue(CHANNEL_FILTER_SET);
            channelName = getStringValue(CHANNEL_NAME);
            acquire = getIntValue(CHANNEL_ACQUIRE) != 0;
        }
    }

    class IlluminationChannel extends SubBlock {
        public Integer wavelength;
        public Double attenuation;
        public String name;

        protected void read() throws IOException {
            super.read();
            wavelength = getIntValue(ILLUM_CHANNEL_WAVELENGTH);
            attenuation = getDoubleValue(ILLUM_CHANNEL_ATTENUATION);
            acquire = getIntValue(ILLUM_CHANNEL_ACQUIRE) != 0;

            name = getStringValue(ILLUM_CHANNEL_NAME);
            try {
                wavelength = new Integer(name);
            }
            catch (NumberFormatException e) {
                LOG.warn("ignoring wavelength parse exception", e);
            }
        }
    }

    class DataChannel extends SubBlock {
        public String name;

        protected void read() throws IOException {
            super.read();
            name = getStringValue(DATA_CHANNEL_NAME);
            for (int i=0; i<name.length(); i++) {
                if (name.charAt(i) < 10) {
                    name = name.substring(0, i);
                    break;
                }
            }

            acquire = getIntValue(DATA_CHANNEL_ACQUIRE) != 0;
        }
    }

    class BeamSplitter extends SubBlock {
        public String filter, filterSet;

        protected void read() throws IOException {
            super.read();

            filter = getStringValue(BEAM_SPLITTER_FILTER);
            if (filter != null) {
                filter = filter.trim();
                if (filter.length() == 0 || filter.equals("None")) {
                    filter = null;
                }
            }
            filterSet = getStringValue(BEAM_SPLITTER_FILTER_SET);
        }
    }

    class Timer extends SubBlock { }
    class Marker extends SubBlock { }

    private static final Logger LOG = Logger.getLogger(ZeissLSMReader.class);


}
