CC = gcc
CFLAGS = -O0 -g -std=c99 -pedantic -Wall -Wextra

all: lcg

challenge1: lcg.c
	$(CC) $(CFLAGS) -o lcg lcg.c

clean:
	rm -f *.o lcg
