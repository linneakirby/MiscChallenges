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

    int getValue(int b1, int b2, int b3, int b4){
        if(bigEndian){
            return (b1<<24)+(b2<<16)+(b3<<8)+b4;
        }
        return (b4<<24)+(b3<<16)+(b2<<8)+b1;
    }

    /*
    * parseFileHeader
    * Parses the file header of the bin file
    */
    int parseFileHeader(FileInputStream input) throws Exception{
    	try {
    		
    		int b1 = input.read();
			int b2 = input.read();

			//Bytes 0-1 - Check endianness 
			if(b1 == 0x4d && b2 == 0x4d){
				bigEndian = true;

			}
			if(debug){
				System.out.println("Big Endian? "+bigEndian);
			}

			//Bytes 2-3 - Make sure file is a TIFF file
			b1 = input.read();
			b2 = input.read();
			if(debug){
				System.out.println("TIFF? "+(getValue(b1, b2) == 42));
			}
			if(getValue(b1, b2) != 42){
				throw new Exception("I'm sorry, but this file is not a TIFF file!");
			}

			//Bytes 4-7 - Find offset of first IFD
			b1 = input.read();
			b2 = input.read();
            int b3 = input.read();
            int b4 = input.read();
			int offset = getValue(b1, b2, b3, b4);

			if(debug){
				System.out.println("Offset: "+offset);
			}

			return offset;

    	} catch (IOException e) {
			System.out.println("I/O Problem!");
			e.printStackTrace();
		}

		return -1;
    }

    void parseIFE(FileInputStream input){
		try{
			//Bytes 2-3 - field Type
    		int b1 = input.read();
    		int b2 = input.read();
    		int fieldType = getValue(b1, b2);
    		if(debug){
    			System.out.println("Field type: "+Integer.toHexString(fieldType));
    		}
    		//Bytes 4-7 - number of values, Count of specified Type
			b1 = input.read();
			b2 = input.read();
            int b3 = input.read();
            int b4 = input.read();

			int count = getValue(b1, b2, b3, b4);
			
            if(debug){
    			System.out.println("Count: "+Integer.toHexString(count));
    		}
    		
    		//Bytes 8-11 - value offset (in bytes) of the Value for the field
			b1 = input.read();
			b2 = input.read();
            b3 = input.read();
            b4 = input.read();
			int valueOffset = getValue(b1, b2, b3, b4);

			if(debug){
    			System.out.println("Value offset: "+Integer.toHexString(valueOffset));
    		}
    	}
    	catch (IOException e) {
			System.out.println("I/O Problem!");
			e.printStackTrace();
		}
    }

    boolean tagOfInterest(int tag, FileInputStream input){
    	switch(tag){
    		case 0x0001: 
				if(debug){
    				System.out.println("Gps Latitude Ref");
    			}
    			parseIFE(input); //GpsLatitudeRef
    			return true;

   			case 0x0002: 
   				if(debug){
    				System.out.println("Gps Latitude");
    			}
   				parseIFE(input); //GpsLatitude
    			return true;

    		case 0x0003: 
    			if(debug){
    				System.out.println("Gps Longitude Ref");
    			}
    			parseIFE(input); //GpsLongitudeRef
    			return true;

    		case 0x0004: 
    			if(debug){
    				System.out.println("Gps Longitude");
    			}
    			parseIFE(input); //GpsLongitude
    			return true;

   			case 0x8825: 
   				if(debug){
    				System.out.println("Gps IFD");
    			}
   				parseIFE(input); //GpsIFD
    			return true;

   			default:
    			return false;
    	}
    }

	/*
	 * parseIFD
     * Parses an IFD in the file
     */
    void parseIFD(FileInputStream input, int ifdOffset){
    	try{
    		//skip to offset of 0th IFD
    		input.skip(ifdOffset);

    		//find number of directory entries
    		int b1 = input.read();
    		int b2 = input.read();
    		int numEntries = getValue(b1, b2);
    		if(debug){
				System.out.println("Num entries: "+numEntries);
			}

			//for each entry
			for(int i=0; i<numEntries; i++){
				//find tag
				b1 = input.read();
				b2 = input.read();
				int tag = getValue(b1, b2);
				if(debug){
                    System.out.println("b1: "+b1+"\nb2: "+b2);
					System.out.println("Tag: "+Integer.toHexString(tag));
				}
				if(!tagOfInterest(tag, input)){
					input.skip(10);
				}
			}
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
	void parseBin() throws Exception{
		try {
			FileInputStream input = new FileInputStream("challenge03.bin");
			int ifdOffset = parseFileHeader(input);
			parseIFD(input, ifdOffset);
			input.close();
		} catch (FileNotFoundException e) {
			System.out.println("File not found!");
			e.printStackTrace();
		}
		catch (IOException e) {
			System.out.println("I/O Problem!");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception{
		Challenge3 tiffParser = new Challenge3();
		tiffParser.parseBin();
    }

}