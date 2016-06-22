CC = gcc
CFLAGS = -O0 -g -std=c99 -pedantic -Wall -Wextra

all: challenge1

challenge1: challenge1.c
	$(CC) $(CFLAGS) -o challenge1 challenge1.c

clean:
	rm -f *.o challenge1
