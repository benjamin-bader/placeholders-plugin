package com.bendb.placeholders;

import com.android.ide.common.res2.FileStatus;
import com.android.utils.FileUtils;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.ParallelizableTask;
import org.gradle.api.tasks.TaskAction;
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
import java.util.Map;
import java.util.stream.Collectors;

@ParallelizableTask
@CacheableTask
public class PlaceholderReplacementTask extends DefaultTask {
    private File preProcessedResourceDirectory;
    private File outputDirectory;
    private Map<String, String> placeholders;
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

    @OutputFiles
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
    public Map<String, String> getPlaceholders() {
        return placeholders;
    }

    public void setPlaceholders(Map<String, String> placeholders) {
        this.placeholders = placeholders;
    }

    @TaskAction
    public void replaceResourcePlaceholders(IncrementalTaskInputs inputs) throws Exception {
        inputPath  = preProcessedResourceDirectory.toPath();
        outputPath = outputDirectory.toPath();

        placeholderArray = new String[placeholders.size()];
        replacementArray = new String[placeholders.size()];

        int index = 0;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            placeholderArray[index] = entry.getKey();
            replacementArray[index] = entry.getValue();
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

        if (inputFilePath.endsWith(".xml")) {
            getLogger().debug("{} is probably an XML resource; replacing placeholders.", inputFilePath);
            processXmlResourceFile(inputFilePath, toWrite);
        } else {
            getLogger().debug("{} is probably not an XML resource; copying it verbatim.", inputFilePath);
            Files.copy(inputFilePath, toWrite, StandardCopyOption.ATOMIC_MOVE);
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
}
