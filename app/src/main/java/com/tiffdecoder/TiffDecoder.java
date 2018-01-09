package com.tiffdecoder;

public class TiffDecoder {

	static {
		System.loadLibrary("tiff");
		System.loadLibrary("tiffdecoder");
	}
	
	public static native int nativeTiffOpen(String name);

	public static native int[] nativeTiffGetBytes();

	public static native int nativeTiffGetLength();

	public static native int nativeTiffGetWidth();

	public static native int nativeTiffGetHeight();

	public static native void nativeTiffClose();

}