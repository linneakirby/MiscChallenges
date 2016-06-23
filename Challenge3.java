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
import java.util.Stack;

public class Challenge3 {

	private Boolean debug = true;
	private Boolean bigEndian = false;
    private FileInputStream input;
    private int currentOffset;
    private Stack<Integer> offsetStack;
    private String filename;

    /*
    * Constructor
    */
    Challenge3() throws Exception{
        try{
            currentOffset = 0;
            offsetStack = new Stack<Integer>();
            filename = "challenge03.bin";
            input = new FileInputStream(filename);
            parseBin();
            input.close();
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
    int nextByte(){
        try{
            currentOffset++;
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
    void skip(int skipAmount, FileInputStream is){
        try{
            is.skip(skipAmount);
            currentOffset += skipAmount;
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
    int parseFileHeader() throws Exception{
        int b1 = nextByte();
        int b2 = nextByte();

		//Bytes 0-1 - Check endianness 
        if(b1 == 0x4d && b2 == 0x4d){
            bigEndian = true;

        }
        if(debug){
            System.out.println("Big Endian? "+bigEndian);
        }

    	//Bytes 2-3 - Make sure file is a TIFF file
        b1 = nextByte();
        b2 = nextByte();
        if(debug){
            System.out.println("TIFF? "+(getValue(b1, b2) == 42));
        }
        if(getValue(b1, b2) != 42){
            throw new Exception("I'm sorry, but this file is not a TIFF file!");
        }

    	//Bytes 4-7 - Find offset of first IFD
        b1 = nextByte();
        b2 = nextByte();
        int b3 = nextByte();
        int b4 = nextByte();
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
    int parseIFE(){
	    //Bytes 2-3 - field Type
        int b1 = nextByte();
        int b2 = nextByte();
        int fieldType = getValue(b1, b2);
        if(debug){
            System.out.println("Field type: "+Integer.toHexString(fieldType));
        }
    	//Bytes 4-7 - number of values, currentOffset of specified Type
        b1 = nextByte();
        b2 = nextByte();
        int b3 = nextByte();
        int b4 = nextByte();

        int currentOffset = getValue(b1, b2, b3, b4);

        if(debug){
            System.out.println("currentOffset: "+Integer.toHexString(currentOffset));
        }

        //Bytes 8-11 - value offset (in bytes) of the Value for the field
        b1 = nextByte();
        b2 = nextByte();
        b3 = nextByte();
        b4 = nextByte();
        int valueOffset = getValue(b1, b2, b3, b4);

        if(debug){
            System.out.println("Value offset: "+Integer.toHexString(valueOffset));
        }

        return valueOffset;
    }

    void parseGpsLatitudeRef(int valueOffset){
        try{
            FileInputStream is = new FileInputStream(filename);
            //skip to offset of GPS Latitude Ref
            skip(valueOffset-currentOffset, is);
            int b1 = nextByte();
            System.out.println("Latitude Ref: "+b1);
            is.close();
        }
        catch (IOException e) {
            System.out.println("I/O Problem!");
            e.printStackTrace();
        }
    }

    /*
    * tagOfInterest
    * checks to see if the tag contains GPS information; if so, calls parseIFE() and return true
    */
    boolean tagOfInterest(int tag){
        int valueOffset;
    	switch(tag){
    		case 0x0001: 
				if(debug){
    				System.out.println("Gps Latitude Ref");
    			}
                valueOffset = parseIFE(); //GpsLatitudeRef
                //parseGpsLatitudeRef(valueOffset);
    			return true;

   			case 0x0002: 
   				if(debug){
    				System.out.println("Gps Latitude");
    			}
   				parseIFE(); //GpsLatitude
    			return true;

    		case 0x0003: 
    			if(debug){
    				System.out.println("Gps Longitude Ref");
    			}
    			parseIFE(); //GpsLongitudeRef
    			return true;

    		case 0x0004: 
    			if(debug){
    				System.out.println("Gps Longitude");
    			}
    			parseIFE(); //GpsLongitude
    			return true;

   			case 0x8825: 
   				if(debug){
    				System.out.println("Gps IFD");
    			}
   				valueOffset = parseIFE(); //GpsIFD
                offsetStack.push(currentOffset);
                parseIFD(valueOffset);
                currentOffset = offsetStack.pop();
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
    	//skip to offset of 0th IFD
        skip(ifdOffset-currentOffset, input);

        if(debug){
            System.out.println("ifdOffset-currentOffset: "+(ifdOffset-currentOffset));
        }

    	//find number of directory entries
        int b1 = nextByte();
        int b2 = nextByte();
        int numEntries = getValue(b1, b2);
        if(debug){
            //System.out.println("b1: "+b1+"\nb2: "+b2);
            System.out.println("Num entries: "+numEntries);
        }

		//for each entry
        for(int i=0; i<numEntries; i++){
			//find tag
            b1 = nextByte();
            b2 = nextByte();
            int tag = getValue(b1, b2);
            if(debug){
                //System.out.println("b1: "+b1+"\nb2: "+b2);
               System.out.println("Tag: "+Integer.toHexString(tag));
            }
            if(!tagOfInterest(tag)){
               skip(10, input);
            }
       }
    }

	/*
	 * parseBin
     * Parses the bin file
     */
	void parseBin() throws Exception{
		int ifdOffset = parseFileHeader();
		parseIFD(ifdOffset);
	}

	public static void main(String[] args) throws Exception{
		Challenge3 tiffParser = new Challenge3();
    }

}