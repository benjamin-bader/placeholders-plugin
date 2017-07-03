package com.bendb.placeholders.aapt2;

import com.google.protobuf.CodedInputStream;

import java.io.File;
import java.io.FileInputStream;

public class FlatFile {
    private final File sourceFile;

    public FlatFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public void parse() throws Exception {
        CodedInputStream in = CodedInputStream.newInstance(new FileInputStream(sourceFile));
        long sz = in.readRawLittleEndian64();
    }
}
