/* Linnea Kirby
 * 21 June 2016
 * 
 * Solution for Cipher Tech Solutions Challenge 3
 */

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.Exception;
import java.io.StringReader;
import java.util.Scanner;
import java.util.HashMap;

public class Challenge3 {

	private Boolean debug = false; //make this true to see intermediary steps

	private Boolean bigEndian = false;
    private String filename;

    private HashMap<String, Double> info;

    /*
    * Constructor
    */
    Challenge3(String fn) throws Exception{
        try{
            info = new HashMap<String, Double>();

            filename = fn;
            FileInputStream input = new FileInputStream(filename);
            parseBin(input);
            printInfo();
            input.close();
        }
        catch (FileNotFoundException e){
            System.out.println("I'm sorry, but your filename is invalid!");
        }
        catch (IOException e) {
            System.out.println("I/O Problem!");
            e.printStackTrace();
        }
    }

    /*
    * getValue
    * returns the big- or little-endian value represented by two bytes
    */
    int getValue(int b1, int b2){
    	if(bigEndian){
    		return (b1<<8)+b2;
    	}
    	return b1+(b2<<8);
    }

    /*
    * getValue
    * returns the big- or little-endian value represented by four bytes
    */
    int getValue(int b1, int b2, int b3, int b4){
        if(bigEndian){
            return (b1<<24)+(b2<<16)+(b3<<8)+b4;
        }
        return (b4<<24)+(b3<<16)+(b2<<8)+b1;
    }

    /*
    * getValueLeftJustify
    * returns the value of a left-justified value represented by four bytes
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
            System.out.println("I/O Problem!");
            e.printStackTrace();
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
            System.out.println("I/O Problem!");
            e.printStackTrace();
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
        if(debug){
            System.out.println("Big Endian? "+bigEndian);
        }

    	//Bytes 2-3 - Make sure file is a TIFF file
        b1 = nextByte(input);
        b2 = nextByte(input);
        if(debug){
            System.out.println("TIFF? "+(getValue(b1, b2) == 42));
        }
        if(getValue(b1, b2) != 42){
            throw new Exception("I'm sorry, but this file is not a TIFF file!");
        }

    	//Bytes 4-7 - Find offset of first IFD
        b1 = nextByte(input);
        b2 = nextByte(input);
        int b3 = nextByte(input);
        int b4 = nextByte(input);
        int offset = getValue(b1, b2, b3, b4);

        if(debug){
            System.out.println("Offset: "+offset);
        }

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
        if(debug){
            System.out.println("Field type: "+Integer.toHexString(fieldType));
        }
    	//Bytes 4-7 - number of values, Count of specified Type
        b1 = nextByte(ifeInput);
        b2 = nextByte(ifeInput);
        int b3 = nextByte(ifeInput);
        int b4 = nextByte(ifeInput);

        offsetCount[1] = getValue(b1, b2, b3, b4);

        if(debug){
            System.out.println("Count: "+Integer.toHexString(offsetCount[1]));
        }

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

        if(debug){
            System.out.println("b1: "+b1+"\nb2: "+b2+"\nb3: "+b3+"\nb4: "+b4);
            System.out.println("Value offset: "+Integer.toHexString(offsetCount[0]));
        }

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

            if(debug){
                System.out.print("Degrees/Minutes/Seconds: ");
                for(int j=0; j<offsetCount[1]; j++){
                    System.out.print(dms[j]+" ");
                }
                System.out.println();
            }

            latLongInput.close();

            return dms;
        }
        catch (IOException e) {
            System.out.println("I/O Problem!");
            e.printStackTrace();
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
        if(debug){
            System.out.println("Latitude/Longitude Ref: "+((char)value));
            System.out.println("count/2: "+valueCount[1]/2);
        }
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
        if(debug){
            System.out.println("Gps Latitude Ref");
        }
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
        if(debug){
            System.out.println("Gps Latitude");
        }
        int[] v = parseIFE(tagInput); //GpsLatitude
        float[] latDms = parseFieldType5(v);
        double latitude = latDms[0] + latDms[1]/60.0 + latDms[2]/3600.0;
        info.put("Latitude", info.get("Latitude Ref")*latitude);
        if(debug){
            System.out.println("Latitude: "+info.get("Latitude"));
        }
    }

    /*
    * parseGpsLongRef
    * parses a Gps Longitude Ref
    */
    void parseGpsLongRef(FileInputStream tagInput){
        if(debug){
            System.out.println("Gps Longitude Ref");
        }
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
        if(debug){
            System.out.println("Gps Longitude");
        }
        int[] v = parseIFE(tagInput); //GpsLongitude
        float[] longDms = parseFieldType5(v);
        double longitude = longDms[0] + longDms[1]/60.0 + longDms[2]/3600.0;
        info.put("Longitude", info.get("Longitude Ref")*longitude);
        if(debug){
            System.out.println("Longitude: "+info.get("Longitude"));
        }
    }

    /*
    * parseAltRef
    * parses an Altitude Ref
    */
    void parseAltRef(FileInputStream tagInput){
        if(debug){
            System.out.println("Gps Altitude Ref");
        }
        int[] v = parseIFE(tagInput); //GpsAltitudeRef
        info.put("Altitude Ref", (double)parseFieldType1(v));
    }

    /*
    * parseAltitude
    * parses an Altitude
    */
    void parseAltitude(FileInputStream tagInput){
        if(debug){
            System.out.println("Gps Altitude");
        }
        int[] v = parseIFE(tagInput); //GpsAltitude
        float[] alt = parseFieldType5(v);
        info.put("Altitude", info.get("Altitude Ref")+alt[0]);
        if(debug){
            System.out.println("Altitude: "+info.get("Altitude"));
        }
    }

    /*
    * parseGpsTime
    * parses a Gps Time
    */
    void parseGpsTime(FileInputStream tagInput){
        if(debug){
            System.out.println("Gps Time");
        }
        int[] v = parseIFE(tagInput); //GpsTime
        float[] timeDms = parseFieldType5(v);
        double time = timeDms[0] + timeDms[1]/60.0 + timeDms[2]/3600.0;
        info.put("Gps Time", time);
        if(debug){
            System.out.println("Gps Time: "+info.get("Gps Time"));
        }
    }

    /*
    * parseGpsIFD
    * parses a Gps IFD
    */
    void parseGpsIFD(FileInputStream tagInput){
        if(debug){
            System.out.println("Gps IFD");
        }
        int[] v = parseIFE(tagInput); //GpsIFD
        parseIFD(v[0]);
    }

    /*
    * parseExifIFD
    * parses an Exif IFD
    */
    void parseExifIFD(FileInputStream tagInput){
        if(debug){
            System.out.println("Exif IFD");
        }
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

            if(debug){
                System.out.println("ifdOffset: "+(ifdOffset));
            }

        	//find number of directory entries
            int b1 = nextByte(ifdInput);
            int b2 = nextByte(ifdInput);
            ifdOffset += 2;
            int numEntries = getValue(b1, b2);
            if(debug){
                //System.out.println("b1: "+b1+"\nb2: "+b2);
                System.out.println("Num entries: "+numEntries);
            }

    		//for each entry
            for(int i=0; i<numEntries; i++){
    			//find tag
                b1 = nextByte(ifdInput);
                b2 = nextByte(ifdInput);
                ifdOffset += 2;
                int tag = getValue(b1, b2);
                if(debug){
                    //System.out.println("b1: "+b1+"\nb2: "+b2);
                   System.out.println("Tag: "+Integer.toHexString(tag));
                }
                if(!tagOfInterest(tag, ifdOffset, ifdInput)){
                   skip(10, ifdInput);
                }
            }
            ifdInput.close();
        }
        catch (IOException e) {
            System.out.println("I/O Problem!");
            e.printStackTrace();
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
        System.out.println("GPS INFORMATION");
        System.out.printf("Coordinates: (%f, %f)\n",info.get("Latitude"),info.get("Longitude"));
        System.out.printf("Altitude: %.2f m\n", info.get("Altitude"));
        double gpsTime = info.get("Gps Time");
        int h = (int)gpsTime/1;
        double m = gpsTime%1*60;
        double s = m%1*60;
        System.out.printf("Gps Time: %02d:%02d:%02d\n", h, (int)m, (int)s);
    }

	public static void main(String[] args) throws Exception{
		Challenge3 tiffParser;
        try{
            tiffParser = new Challenge3(args[0]);
        }
        catch(ArrayIndexOutOfBoundsException e){
            Scanner inputFilename = new Scanner(System.in);
            System.out.print("Please enter a filename: ");
            tiffParser = new Challenge3(inputFilename.next());
            inputFilename.close();
        }
    }

}