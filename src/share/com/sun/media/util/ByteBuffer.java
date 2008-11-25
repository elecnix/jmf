/*
 * @(#)ByteBuffer.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

public class ByteBuffer {

    public byte [] buffer;
    public int     offset;
    public int     length;
    public int     size;

    public ByteBuffer(int size) {
	this.size = size;
	buffer = new byte[size];
    }

    public final void clear() {
	offset = 0;
	length = 0;
    }

    public final void writeBytes(String s) {
	byte [] bytes = s.getBytes();
	writeBytes(bytes);
    }

    public final void writeBytes(byte [] bytes) {
	System.arraycopy(bytes, 0,
			 buffer, offset, bytes.length);
	offset += bytes.length;
	length += bytes.length;
    }

    public final void writeInt(int value) {
	buffer[offset + 0] = (byte)((value >> 24) & 0xFF);
	buffer[offset + 1] = (byte)((value >> 16) & 0xFF);
	buffer[offset + 2] = (byte)((value >>  8) & 0xFF);
	buffer[offset + 3] = (byte)((value >>  0) & 0xFF);
	offset += 4;
	length += 4;
    }

    public final void writeIntLittleEndian(int value) {
	buffer[offset + 3] = (byte)((value >>> 24) & 0xFF);
	buffer[offset + 2] = (byte)((value >>> 16) & 0xFF);
	buffer[offset + 1] = (byte)((value >>>  8) & 0xFF);
	buffer[offset + 0] = (byte)((value >>>  0) & 0xFF);
	offset += 4;
	length += 4;
    }

    public final void writeShort(short value) {
	buffer[offset + 0] = (byte)((value >> 8) & 0xFF);
	buffer[offset + 1] = (byte)((value >> 0) & 0xFF);
	offset += 2;
	length += 2;
    }

    public final void writeShortLittleEndian(short value) {
	buffer[offset + 1] = (byte)((value >> 8) & 0xFF);
	buffer[offset + 0] = (byte)((value >> 0) & 0xFF);
	offset += 2;
	length += 2;
    }

    public final void writeByte(byte value) {
	buffer[offset] = value;
	offset++;
	length++;
    }
}
