package com.bendb.placeholders;

import com.bendb.placeholders.aapt2.StringPoolParser;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
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

        StringPoolParser.parse(stringPoolBytes);
    }

    @Test
    public void bar() throws Exception {
        InputStream asrc = getClass().getClassLoader().getResourceAsStream("menu_menu_main.xml.flat");
        CodedInputStream cis = CodedInputStream.newInstance(asrc);
        //byte[] bytes = ByteStreams.toByteArray(asrc);
        aapt.pb.Format.CompiledFile cf = aapt.pb.Format.CompiledFile.parseFrom(cis);

        cf.toString();
    }
}
