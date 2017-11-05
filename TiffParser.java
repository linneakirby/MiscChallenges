import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.lang.Exception;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parses and extracts metadata from a TIFF Header.
 */
public class TiffParser {
  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private Boolean debug = false; //make this true to see intermediary steps

  private Boolean bigEndian = false;
  private String filename;

  private HashMap<String, Double> info;

  /**
   * Constructor
   */
  TiffParser(String fn) throws Exception{
    try{
      info = new HashMap<String, Double>();

      filename = fn;
      FileInputStream input = new FileInputStream(filename);
      parseBin(input);
      printInfo();
      input.close();
    }
    catch (FileNotFoundException e){
      logger.severe("I'm sorry, but your filename is invalid!");
    }
    catch (IOException e) {
      logger.log(Level.SEVERE, "I/O Problem!", e);
    }
  }

  /**
   * Returns the big- or little-endian value represented by two bytes
   */
  int getValue(int b1, int b2){
    if(bigEndian){
      return (b1<<8)+b2;
    }
    return b1+(b2<<8);
  }

  /**
   * Returns the big- or little-endian value represented by four bytes
   */
  int getValue(int b1, int b2, int b3, int b4){
    if(bigEndian){
      return (b1<<24)+(b2<<16)+(b3<<8)+b4;
    }
    return (b4<<24)+(b3<<16)+(b2<<8)+b1;
  }

  /**
   * Returns the value of a left-justified value represented by four bytes
   */
  int getValueLeftJustify(int b1, int b2, int b3, int b4){
    if(bigEndian){
      if(b4 != 0){
        return (b1<<24)+(b2<<16)+(b3<<8)+b4;
      }
      if(b3 != 0){
        return (b1<<16)+(b2<<8)+b3;
      }
      if(b2 != 0){
        return (b1<<8)+b2;
      }
      return b1;

    }
    if(b1 != 0){
      return (b4<<24)+(b3<<16)+(b2<<8)+b1;
    }
    if(b2 != 0){
      return (b4<<16)+(b3<<8)+b2;
    }
    if(b3 !=0){
      return (b4<<8)+b3;
    }
    return b4;
  }

  /*
   * nextByte
   * returns the value of the next byte from the binary file and increments the currentOffset by one
   */
  int nextByte(FileInputStream input){
    try{
      return input.read();
    }
    catch (IOException e) {
      logger.log(Level.SEVERE, "I/O Problem!", e);
    }
    return -1;
  }

  /*
   * skip
   * skips the indicated number of bytes in the file and increments the currentOffset accordingly
   */
  void skip(int skipAmount, FileInputStream input){
    try{
      input.skip(skipAmount);
    }
    catch (IOException e) {
      logger.log(Level.SEVERE, "I/O Problem!", e);
    }
  }

  /*
   * parseFileHeader
   * Parses the file header of the binary file
   */
  int parseFileHeader(FileInputStream input) throws Exception{
    int b1 = nextByte(input);
    int b2 = nextByte(input);

    //Bytes 0-1 - Check endianness 
    if(b1 == 0x4d && b2 == 0x4d){
      bigEndian = true;

    }
    logger.info(String.format("Big Endian? %b", bigEndian));

    //Bytes 2-3 - Make sure file is a TIFF file
    b1 = nextByte(input);
    b2 = nextByte(input);

    logger.info(String.format("TIFF? %b", getValue(b1, b2) == 42));

    if(getValue(b1, b2) != 42){
      throw new Exception("I'm sorry, but this file is not a TIFF file!");
    }

    //Bytes 4-7 - Find offset of first IFD
    b1 = nextByte(input);
    b2 = nextByte(input);
    int b3 = nextByte(input);
    int b4 = nextByte(input);
    int offset = getValue(b1, b2, b3, b4);

    logger.info("Offset: " + offset);

    return offset;
  }

  /*
   * parseIFE
   * Parses and ImageFileEntry
   */
  int[] parseIFE(FileInputStream ifeInput){
    int[] offsetCount = new int[2];

    //Bytes 2-3 - field Type
    int b1 = nextByte(ifeInput);
    int b2 = nextByte(ifeInput);
    int fieldType = getValue(b1, b2);

    logger.info(
        String.format("Field type: %s", Integer.toHexString(fieldType)));

    //Bytes 4-7 - number of values, Count of specified Type
    b1 = nextByte(ifeInput);
    b2 = nextByte(ifeInput);
    int b3 = nextByte(ifeInput);
    int b4 = nextByte(ifeInput);

    offsetCount[1] = getValue(b1, b2, b3, b4);

    logger.info(
        String.format("Count: %s", Integer.toHexString(offsetCount[1])));

    //Bytes 8-11 - value offset (in bytes) of the Value for the field
    b1 = nextByte(ifeInput);
    b2 = nextByte(ifeInput);
    b3 = nextByte(ifeInput);
    b4 = nextByte(ifeInput);
    if(fieldType != 2 && b4 != 0){
      offsetCount[0] = getValue(b1, b2, b3, b4);
    }
    else{
      offsetCount[0] = getValueLeftJustify(b1, b2, b3, b4);
    }

    logger.info("b1: " + b1);
    logger.info("b2: " + b2);
    logger.info("b3: " + b3);
    logger.info("b4: " + b4);
    logger.info("Value offset: " + Integer.toHexString(offsetCount[0]));

    return offsetCount;
  }

  /*
   * parseFieldType5
   * Parses a field of type 5
   */
  float[] parseFieldType5(int[] offsetCount){
    float[] dms = new float[offsetCount[1]];

    try{
      FileInputStream latLongInput = new FileInputStream(filename);
      //skip to offset of GPS Latitude/Longitude
      skip(offsetCount[0], latLongInput);

      for(int i=0; i<offsetCount[1]; i++){
        int[] numeratorValues = {nextByte(latLongInput), nextByte(latLongInput), nextByte(latLongInput), nextByte(latLongInput)};
        float numerator = (float)getValue(numeratorValues[0], numeratorValues[1], numeratorValues[2], numeratorValues[3]);

        int[] denominatorValues = {nextByte(latLongInput), nextByte(latLongInput), nextByte(latLongInput), nextByte(latLongInput)};
        float denominator = (float)getValue(denominatorValues[0], denominatorValues[1], denominatorValues[2], denominatorValues[3]);

        dms[i] = numerator/denominator;
      }

      logger.info("Degrees/Minutes/Seconds:");
      for(int j = 0; j < offsetCount[1]; j++) {
        logger.info(String.format("%f", dms[j]));
      }

      latLongInput.close();

      return dms;
    }
    catch (IOException e) {
      logger.log(Level.SEVERE, "I/O Problem!", e);
    }

    return dms;
  }

  /*
   * parseFieldType2
   * Parses a field of type 2 (null-terminated ASCII)
   */
  char[] parseFieldType2(int[] valueCount){
    char[] c = new char[valueCount[1]];
    int value = valueCount[0];

    logger.info("Latitude/Longitude Ref: " + (char)value);
    logger.info("count/2: " + valueCount[1]/2);

    c[0] = (char)value;

    return c;
  }

  /*
   * parseFieldType1
   * Parses a field of type 1 (byte)
   */
  int parseFieldType1(int[] valueCount){
    return valueCount[0];
  }

  /*
   * parseGpsLatRef
   * parses a Gps Latitude Ref
   */
  void parseGpsLatRef(FileInputStream tagInput){
    logger.info("Gps Latitude Ref");
    int[] v = parseIFE(tagInput); //GpsLatitudeRef
    char[] latr = parseFieldType2(v);
    if(latr[0] == 'N'){
      info.put("Latitude Ref", 1.0);
    }
    else{
      info.put("Latitude Ref", -1.0);
    }
  }

  /*
   * parseGpsLatitude
   * parses a Gps Latitude
   */
  void parseGpsLatitude(FileInputStream tagInput){
    logger.info("Gps Latitude");
    int[] v = parseIFE(tagInput); //GpsLatitude
    float[] latDms = parseFieldType5(v);
    double latitude = latDms[0] + latDms[1]/60.0 + latDms[2]/3600.0;
    info.put("Latitude", info.get("Latitude Ref")*latitude);
    logger.info("Latitude: " + info.get("Latitude"));
  }

  /*
   * parseGpsLongRef
   * parses a Gps Longitude Ref
   */
  void parseGpsLongRef(FileInputStream tagInput){
    logger.info("Gps Longitude Ref");
    int[] v = parseIFE(tagInput); //GpsLongitudeRef
    char[] longr = parseFieldType2(v);
    if(longr[0] == 'E'){
      info.put("Longitude Ref", 1.0);
    }
    else{
      info.put("Longitude Ref", -1.0);
    }
  }

  /*
   * parseGpsLongitude
   * parses a Gps Longitude
   */
  void parseGpsLongitude(FileInputStream tagInput){
    logger.info("Gps Longitude");
    int[] v = parseIFE(tagInput); //GpsLongitude
    float[] longDms = parseFieldType5(v);
    double longitude = longDms[0] + longDms[1]/60.0 + longDms[2]/3600.0;
    info.put("Longitude", info.get("Longitude Ref")*longitude);
    logger.info("Longitude: " + info.get("Longitude"));
  }

  /*
   * parseAltRef
   * parses an Altitude Ref
   */
  void parseAltRef(FileInputStream tagInput){
    logger.info("Gps Altitude Ref");
    int[] v = parseIFE(tagInput); //GpsAltitudeRef
    info.put("Altitude Ref", (double)parseFieldType1(v));
  }

  /*
   * parseAltitude
   * parses an Altitude
   */
  void parseAltitude(FileInputStream tagInput){
    logger.info("Gps Altitude");
    int[] v = parseIFE(tagInput); //GpsAltitude
    float[] alt = parseFieldType5(v);
    info.put("Altitude", info.get("Altitude Ref")+alt[0]);
    logger.info("Altitude: " + info.get("Altitude"));
  }

  /*
   * parseGpsTime
   * parses a Gps Time
   */
  void parseGpsTime(FileInputStream tagInput){
    logger.info("Gps Time");
    int[] v = parseIFE(tagInput); //GpsTime
    float[] timeDms = parseFieldType5(v);
    double time = timeDms[0] + timeDms[1]/60.0 + timeDms[2]/3600.0;
    info.put("Gps Time", time);
    logger.info("Gps Time: " + info.get("Gps Time"));
  }

  /*
   * parseGpsIFD
   * parses a Gps IFD
   */
  void parseGpsIFD(FileInputStream tagInput){
    logger.info("Gps IFD");
    int[] v = parseIFE(tagInput); //GpsIFD
    parseIFD(v[0]);
  }

  /*
   * parseExifIFD
   * parses an Exif IFD
   */
  void parseExifIFD(FileInputStream tagInput){
    logger.info("Exif IFD");
    int[] v = parseIFE(tagInput); //ExifIFD
    parseIFD(v[0]);
  }


  /*
   * tagOfInterest
   * checks to see if the tag contains target information; if so, calls parseIFE() and return true
   */
  boolean tagOfInterest(int tag, int offset, FileInputStream tagInput){
    switch(tag){
      case 0x0001: 
        parseGpsLatRef(tagInput);
        return true;

      case 0x0002: 
        parseGpsLatitude(tagInput);
        return true;

      case 0x0003: 
        parseGpsLongRef(tagInput);
        return true;

      case 0x0004: 
        parseGpsLongitude(tagInput);
        return true;

      case 0x0005:
        parseAltRef(tagInput);
        return true;

      case 0x0006:
        parseAltitude(tagInput);
        return true;

      case 0x0007:
        parseGpsTime(tagInput);
        return true;

      case 0x8769:
        parseExifIFD(tagInput);
        return true;

      case 0x8825: 
        parseGpsIFD(tagInput);
        return true;

      default:
        return false;
    }
  }

  /*
   * parseIFD
   * Parses an ImageFileDirectory in the file
   */
  void parseIFD(int ifdOffset){
    try{
      FileInputStream ifdInput = new FileInputStream(filename);
      //skip to offset of 0th IFD
      skip(ifdOffset, ifdInput);

      logger.info("ifdOffset: " + ifdOffset);

      //find number of directory entries
      int b1 = nextByte(ifdInput);
      int b2 = nextByte(ifdInput);
      ifdOffset += 2;
      int numEntries = getValue(b1, b2);
      logger.info("Num entries: " + numEntries);

      //for each entry
      for(int i=0; i<numEntries; i++){
        //find tag
        b1 = nextByte(ifdInput);
        b2 = nextByte(ifdInput);
        ifdOffset += 2;
        int tag = getValue(b1, b2);
        logger.info("Tag: " + Integer.toHexString(tag));
        if(!tagOfInterest(tag, ifdOffset, ifdInput)){
          skip(10, ifdInput);
        }
      }
      ifdInput.close();
    }
    catch (IOException e) {
      logger.log(Level.SEVERE, "I/O Problem!", e);
    }
  }

  /*
   * parseBin
   * Parses the bin file
   */
  void parseBin(FileInputStream input) throws Exception{
    int ifdOffset = parseFileHeader(input);
    parseIFD(ifdOffset);

  }

  /*
   * printInfo
   * Prints the discovered information
   */
  void printInfo(){
    logger.info("GPS INFORMATION");
    logger.info(String.format("Coordinates: (%f, %f)\n", info.get("Latitude"), info.get("Longitude")));
    logger.info(String.format("Altitude: %.2f m\n", info.get("Altitude")));

    double gpsTime = info.get("Gps Time");
    int h = (int)gpsTime/1;
    double m = gpsTime%1*60;
    double s = m%1*60;

    logger.info(String.format("Gps Time: %02d:%02d:%02d\n", h, (int)m, (int)s));
  }

  public static void main(String[] args) throws Exception {
    String filename;
    if(args.length == 1){
      filename = args[0];
    }
    else{
      Scanner inputFilename = new Scanner(System.in);
      System.out.print("Please enter a filename: ");
      filename = inputFilename.next();
      inputFilename.close();
    }
    TiffParser tiffParser = new TiffParser(filename);
  }

}
