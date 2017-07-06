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

import com.android.ide.common.res2.FileStatus;
import com.android.utils.FileUtils;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@ParallelizableTask
@CacheableTask
public class PlaceholderReplacementTask extends DefaultTask {
    private File preProcessedResourceDirectory;
    private File outputDirectory;
    private Map<String, Object> placeholders;
    private ConfigurableFileCollection processedResourceFiles = getProject().files().builtBy(this);

    // temporary state
    Path inputPath;
    Path outputPath;

    String[] placeholderArray;
    String[] replacementArray;

    public void setPreProcessedResourceDirectory(File directory) {
        this.preProcessedResourceDirectory = directory;
    }

    @InputDirectory
    public File getPreProcessedResourceDirectory() {
        return preProcessedResourceDirectory;
    }

    @OutputDirectories
    public FileCollection getProcessedResourceFiles() {
        return processedResourceFiles;
    }

    @OutputDirectory
    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
        processedResourceFiles.setFrom(outputDirectory);
    }

    @Input
    public Map<String, Object> getPlaceholders() {
        return placeholders;
    }

    public void setPlaceholders(Map<String, Object> placeholders) {
        this.placeholders = placeholders;
    }

    @TaskAction
    public void replaceResourcePlaceholders(IncrementalTaskInputs inputs) throws Exception {
        inputPath  = preProcessedResourceDirectory.toPath();
        outputPath = outputDirectory.toPath();

        placeholderArray = new String[placeholders.size()];
        replacementArray = new String[placeholders.size()];

        int index = 0;
        for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
            placeholderArray[index] = getPlaceholderSyntaxFor(entry.getKey());
            replacementArray[index] = String.valueOf(entry.getValue());
            index++;
        }

        if (inputs.isIncremental()) {
            Map<File, FileStatus> changes = new LinkedHashMap<>();

            inputs.outOfDate(details -> {
                FileStatus status = details.isAdded() ? FileStatus.NEW : FileStatus.CHANGED;
                changes.put(details.getFile(), status);
            });

            inputs.removed(details -> {
                changes.put(details.getFile(), FileStatus.REMOVED);
            });

            Map<Path, FileStatus> changedPaths = new LinkedHashMap<>(changes.size());
            for (Map.Entry<File, FileStatus> entry : changes.entrySet()) {
                changedPaths.put(entry.getKey().toPath(), entry.getValue());
            }

            processIncrementalChanges(changedPaths);
        } else {
            processAllResources();
        }
    }

    private void processIncrementalChanges(Map<Path, FileStatus> changes) throws Exception {
        for (Map.Entry<Path, FileStatus> entry : changes.entrySet()) {
            switch (entry.getValue()) {
                case REMOVED:
                    handleRemovedFile(entry.getKey());
                    break;

                default:
                    processSingleFile(entry.getKey());
                    break;
            }
        }
    }

    private void processAllResources() throws Exception {
        FileUtils.cleanOutputDir(outputDirectory);

        List<Path> inputs = Files.walk(inputPath).collect(Collectors.toList());

        for (Path path : inputs) {
            processSingleFile(path);
        }
    }

    private void handleRemovedFile(Path inputFilePath) throws Exception {
        Path relative = inputPath.relativize(inputFilePath);
        Path toRemove = outputPath.resolve(relative);

        getLogger().debug("Removing cached resource file: {}", toRemove);

        Files.deleteIfExists(toRemove);
    }

    private void processSingleFile(Path inputFilePath) throws Exception {
        Path relative = inputPath.relativize(inputFilePath);
        Path toWrite = outputPath.resolve(relative);

        Files.createDirectories(toWrite.getParent());

        String inputFilePathString = inputFilePath.toString().toLowerCase(Locale.US);

        if (Files.isDirectory(inputFilePath)) {
            Files.createDirectories(toWrite);
        } else if (inputFilePathString.endsWith(".xml")) {
            getLogger().debug("{} is probably an XML resource; replacing placeholders.", inputFilePath);
            Files.deleteIfExists(toWrite);
            processXmlResourceFile(inputFilePath, toWrite);
        } else {
            getLogger().debug("{} is probably not an XML resource; copying it verbatim.", inputFilePath);
            Files.deleteIfExists(toWrite);
            Files.copy(inputFilePath, toWrite, StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

    private void processXmlResourceFile(Path inputFilePath, Path outputFilePath) throws Exception {
        try (BufferedReader reader = Files.newBufferedReader(inputFilePath, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(outputFilePath,
                                                             StandardCharsets.UTF_8,
                                                             StandardOpenOption.WRITE,
                                                             StandardOpenOption.CREATE,
                                                             StandardOpenOption.TRUNCATE_EXISTING)) {
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                String outputLine = replacePlaceholdersInString(inputLine);
                writer.write(outputLine);
                writer.newLine();
            }

            writer.flush();
        }
    }

    private String replacePlaceholdersInString(String line) {
        for (int i = 0; i < placeholderArray.length; ++i) {
            line = line.replace(placeholderArray[i], replacementArray[i]);
        }
        return line;
    }

    private static String getPlaceholderSyntaxFor(String placeholder) {
        if (placeholder.startsWith("${") && placeholder.endsWith("}")) {
            return placeholder;
        } else if (placeholder.startsWith("${") || placeholder.endsWith("}")) {
            throw new IllegalArgumentException("Invalid placeholder syntax: " + placeholder);
        }
        return "${" + placeholder + "}";
    }
}
