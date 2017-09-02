/*Linnea Kirby
20 June 2016
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define M 0xa5
#define A 0xc9
#define N 256

unsigned char *LCG(unsigned char *data, int dataLength, unsigned char initialValue);

int main(int argc, char *argv[]){
	unsigned char* result;

	//goes through all strings from command line, runs LCG, and prints the result
	for(int i=1; i<argc; i++){
		result = LCG((unsigned char *)argv[i], strlen(argv[i]), 0x55);
		for(int j = 0; j< (int)strlen(result); j++){
			printf("%s%2x", "\\x",result[j]);
		}
		printf("\n");
		free(result);
	}

	//runs LCG on a string represented by hex numbers and prints the result
	result = LCG("\xF3\x93\x68\x2D\xCB", 5, 0x55);
	printf("%s\n", result);
	free(result);
}

//steps and returns the next key in the stream
unsigned char nextValue(unsigned char prevValue){
	return (M * prevValue + A) % N;
}

//takes in data, data length, and initial value, and returns the data xor'd with the LCG key stream
unsigned char *LCG(unsigned char *data, int dataLength, unsigned char initialValue){
	unsigned char *LCG = malloc(dataLength);

	unsigned char currValue = initialValue;

	for(int i=0; i<dataLength; i++){
		currValue = nextValue(currValue);

		LCG[i] = currValue ^ data[i];
	}

	return LCG;
}
