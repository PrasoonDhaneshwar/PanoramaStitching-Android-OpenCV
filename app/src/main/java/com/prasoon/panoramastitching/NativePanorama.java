package com.prasoon.panoramastitching;

public class NativePanorama {
    public native static int processPanorama(long[] imageAddressArray, long outputAddress);
    public native static String getMessageFromJni();
}
