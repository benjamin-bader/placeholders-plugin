package com.bendb.placeholders;

import com.bendb.placeholders.aapt2.StringPool;
import com.google.protobuf.ByteString;
import okio.Buffer;
import org.gradle.internal.impldep.com.google.common.io.ByteStreams;
import org.junit.Test;

import java.io.InputStream;

public class StringPoolTest {
    @Test
    public void foo() throws Exception {
        InputStream asrc = getClass().getClassLoader().getResourceAsStream("values_values.arsc.flat");
        byte[] bytes = ByteStreams.toByteArray(asrc);
        aapt.pb.Format.ResourceTable rt = aapt.pb.Format.ResourceTable.parseFrom(bytes);

        ByteString stringPoolBytes = rt.getStringPool().getData();

        Buffer buffer = new Buffer();
        buffer.write(stringPoolBytes.toByteArray());

        StringPool.ResStringPoolHeader header =  StringPool.ResStringPoolHeader.parse(buffer);
        System.out.println("rt = " + rt);
        System.out.println("rt.getPackagesList() = " + rt.getPackagesList());
    }
}
