package com.bendb.placeholders.aapt2;

public class ResourceTableConfig {
    /**
     * Number of bytes in the struct; uint32.
     */
    public long size;

    /**
     * Mobile country code, from SIM.  0 means "any".
     *
     * uint16, mutually exclusive with {@link #imsi}.
     */
    public int mcc;

    /**
     * Mobile network code, from SIM.  0 means "any".
     *
     * uint16, mutally exclusive with {@link #imsi}.
     */
    public int mnc;

    /**
     * uint32, utually exclusive with {@link #mnc} and {@link #mcc}.
     */
    public long imsi;

    /**
     * uint32; could be a struct or a set of four characters,
     * but we don't care to distinguish here.
     */
    public long locale;


    /**
     * uint32
     */
    public long screenType;

    /**
     * uint32
     */
    public long input;

    /**
     * uint32
     */
    public long screenSize;

    /**
     * uint32
     */
    public long version;

    public long screenConfig;

    public long screenSizeDp;

    /**
     * char[4]
     */
    public char[] localeScript;


}
