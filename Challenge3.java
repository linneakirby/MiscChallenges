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

public class Challenge3 {

	private Boolean debug = true;
	private Boolean bigEndian = false;
    private String filename;

    private int latRef = 1;
    private int longRef = 1;
    private double latitude = 0;
    private double longitude = 0;

    /*
    * Constructor
    */
    Challenge3(String fn) throws Exception{
        try{
            filename = fn;
            FileInputStream input = new FileInputStream(filename);
            parseBin(input);
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
    int parseIFE(FileInputStream ifeInput){
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

        int count = getValue(b1, b2, b3, b4);

        if(debug){
            System.out.println("Count: "+Integer.toHexString(count));
        }

        //Bytes 8-11 - value offset (in bytes) of the Value for the field
        b1 = nextByte(ifeInput);
        b2 = nextByte(ifeInput);
        b3 = nextByte(ifeInput);
        b4 = nextByte(ifeInput);
        int valueOffset = getValue(b1, b2, b3, b4);

        if(debug){
            System.out.println("Value offset: "+Integer.toHexString(valueOffset));
        }

        return valueOffset;
    }

    double parseGpsLatLong(int valueOffset){
        try{
        FileInputStream latLongInput = new FileInputStream(filename);
            //skip to offset of GPS Latitude/Longitude
            skip(valueOffset, latLongInput);

            float[] dms = new float[3];

            for(int i=0; i<3; i++){
                int[] numeratorValues = {nextByte(latLongInput), nextByte(latLongInput), nextByte(latLongInput), nextByte(latLongInput)};
                float numerator = (float)getValue(numeratorValues[0], numeratorValues[1], numeratorValues[2], numeratorValues[3]);

                int[] denominatorValues = {nextByte(latLongInput), nextByte(latLongInput), nextByte(latLongInput), nextByte(latLongInput)};
                float denominator = (float)getValue(denominatorValues[0], denominatorValues[1], denominatorValues[2], denominatorValues[3]);

                dms[i] = numerator/denominator;
            }

            if(debug){
                System.out.print("Degrees/Minutes/Seconds: ");
                for(int j=0; j<3; j++){
                    System.out.print(dms[j]+" ");
                }
                System.out.println();
            }

            latLongInput.close();

            return dms[0] + dms[1]/60.0 + dms[2]/3600.0;
        }
        catch (IOException e) {
            System.out.println("I/O Problem!");
            e.printStackTrace();
        }

        return -1;
    }

    /*
    * parseGpsLatLongRef
    * Parses the 7-bit, null-terminated ASCII value corresponding to a Latitude or Longitude Ref
    */
    int parseGpsLatLongRef(int valueOffset){
        try{
            FileInputStream latLongRefInput = new FileInputStream(filename);
            //skip to offset of GPS Latitude/Longitude Ref
            skip(valueOffset, latLongRefInput);
            int b1 = nextByte(latLongRefInput);
            System.out.println("Latitude/Longitude Ref: "+(Integer.toBinaryString(b1)));
            System.out.println("N: "+Integer.toBinaryString('N'));
            System.out.println("W: "+Integer.toBinaryString('W'));
            latLongRefInput.close();
            return 1;
        }
        catch (IOException e) {
            System.out.println("I/O Problem!");
            e.printStackTrace();
        }
        return 0;
    }

    /*
    * tagOfInterest
    * checks to see if the tag contains GPS information; if so, calls parseIFE() and return true
    */
    boolean tagOfInterest(int tag, int offset, FileInputStream tagInput){
        int valueOffset;
    	switch(tag){
    		case 0x0001: 
				if(debug){
    				System.out.println("Gps Latitude Ref");
    			}
                valueOffset = parseIFE(tagInput); //GpsLatitudeRef
                latRef = parseGpsLatLongRef(valueOffset);
    			return true;

   			case 0x0002: 
   				if(debug){
    				System.out.println("Gps Latitude");
    			}
   				valueOffset = parseIFE(tagInput); //GpsLatitude
                latitude = latRef*parseGpsLatLong(valueOffset);
                if(debug){
                    System.out.println("Latitude: "+latitude);
                }
    			return true;

    		case 0x0003: 
    			if(debug){
    				System.out.println("Gps Longitude Ref");
    			}
    			valueOffset = parseIFE(tagInput); //GpsLongitudeRef
                longRef = parseGpsLatLongRef(valueOffset);
    			return true;

    		case 0x0004: 
    			if(debug){
    				System.out.println("Gps Longitude");
    			}
    			valueOffset = parseIFE(tagInput); //GpsLongitude
                longitude = longRef*parseGpsLatLong(valueOffset);
                if(debug){
                    System.out.println("Longitude: "+longitude);
                }
    			return true;

   			case 0x8825: 
   				if(debug){
    				System.out.println("Gps IFD");
    			}
   				valueOffset = parseIFE(tagInput); //GpsIFD
                parseIFD(valueOffset);
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