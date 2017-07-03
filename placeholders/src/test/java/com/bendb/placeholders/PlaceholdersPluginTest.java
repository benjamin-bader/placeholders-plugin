/*
 * Copyright 2017 Benjamin Bader
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bendb.placeholders;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@Ignore("Not ready yet; still figuring out how to get a full Android project working this way.")
public class PlaceholdersPluginTest {
    private static List<File> pluginClasspath;

    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

    File buildFile;

    @BeforeClass
    public static void setupPluginClasspath() throws Exception {
        URL url = ((URLClassLoader) PlaceholdersPluginTest.class.getClassLoader())
                .findResource("plugin-classpath.txt");
        if (url == null) {
            fail("Could not find 'plugin-classpath.txt'; run the `testClasses` build task.");
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
        pluginClasspath = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null) {
            pluginClasspath.add(new File(line));
        }
    }

    @Before
    public void setup() throws Exception {
        buildFile = tempFolder.newFile("build.gradle");
    }

    @Test
    public void canBeApplied() throws Exception {
        String gradle = "" +
                "apply plugin: 'com.android.application'\n" +
                "apply plugin: 'com.bendb.placeholders'\n" +
                "\n" +
                "buildscript {\n" +
                "repositories {\n" +
                "  jcenter()\n" +
                "}\n" +
                "dependencies {\n" +
                "  classpath 'com.android.tools.build:gradle:3.0.0-alpha4'\n" +
                "}\n" +
                "}" +
                "android {\n" +
                "}";

        Files.write(buildFile.toPath(), gradle.getBytes("UTF-8"));

        BuildResult result = GradleRunner.create()
                .withProjectDir(tempFolder.getRoot())
                .withArguments("assemble")
                .withPluginClasspath(pluginClasspath)
                .build();

        assertThat(result.task(":processDebugResourcePlaceholders").getOutcome(), is(TaskOutcome.SUCCESS));
    }
}
