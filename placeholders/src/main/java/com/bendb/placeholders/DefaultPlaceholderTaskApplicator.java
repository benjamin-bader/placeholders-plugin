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

import com.android.build.gradle.api.ApplicationVariant;
import com.android.build.gradle.api.BaseVariantOutput;
import com.android.build.gradle.tasks.MergeResources;
import com.android.build.gradle.tasks.ProcessAndroidResources;

import com.android.ide.common.res2.MergingException;
import com.android.ide.common.res2.ResourceSet;
import org.gradle.api.DomainObjectCollection;
import org.gradle.api.Project;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;

class DefaultPlaceholderTaskApplicator extends AbstractPlaceholderTaskApplicator {
    private final Field field_inputResourcesDir;

    private final Field field_sourceFolderInputs;

    DefaultPlaceholderTaskApplicator(Project project) throws Exception {
        super(project);
        field_inputResourcesDir = ProcessAndroidResources.class.getDeclaredField("inputResourcesDir");
        field_inputResourcesDir.setAccessible(true);

        field_sourceFolderInputs = MergeResources.class.getDeclaredField("sourceFolderInputs");
        field_sourceFolderInputs.setAccessible(true);
    }

    @Override
    @SuppressWarnings({"RedundantCast", "unchecked"}) // Weird casting necessary to compile on both AGP 2.x and 3.x
    protected void applyToVariant(ApplicationVariant variant) {
        DomainObjectCollection<BaseVariantOutput> outputs = (DomainObjectCollection<BaseVariantOutput>) (Object) variant.getOutputs();
        MergeResources mergeResources = variant.getMergeResources();
        File mergedResourcesDir = mergeResources.getOutputDir();

        Supplier<List<ResourceSet>> sourceFolderInputs;
        try {
            sourceFolderInputs = (Supplier<List<ResourceSet>>) field_sourceFolderInputs.get(mergeResources);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }

        for (ResourceSet resourceSet : sourceFolderInputs.get()) {
            try {
                resourceSet.loadFromFiles(null);
            } catch (MergingException e) {
                throw new AssertionError(e);
            }
        }

        Set<File> inputs = mergeResources.getSourceFolderInputs();
        project.getLogger().lifecycle("Folders: {}", inputs);

        if (!"AAPT_V1".equals(mergeResources.getAaptGeneration())) {
            project.getLogger().warn(
                    "placeholders works with aapt1 only; current aapt version is {}",
                    mergeResources.getAaptGeneration());
            return;
        }

        Map<String, Object> placeholders = new LinkedHashMap<>(
                variant.getMergedFlavor().getManifestPlaceholders());
        placeholders.put("applicationId", variant.getApplicationId());

        File intermediateDir = new File(getIntermediateDir(), variant.getDirName());

        outputs.all(output -> {
            File outputDir = intermediateDir;
            String taskNameSlug = capitalize(variant.getName());
            if (outputs.size() > 1) {
                outputDir = new File(getIntermediateDir(), output.getDirName());
                taskNameSlug += capitalize(output.getName());
            }

            String taskName = String.format(Locale.US, "process%sResourcePlaceholders", taskNameSlug);
            PlaceholderReplacementTask placeholderTask = project
                    .getTasks()
                    .create(taskName, PlaceholderReplacementTask.class);

            placeholderTask.dependsOn(mergeResources);
            placeholderTask.setPreProcessedResourceDirectory(mergedResourcesDir);
            placeholderTask.setPlaceholders(placeholders);
            placeholderTask.setOutputDirectory(outputDir);

            ProcessAndroidResources processResources = output.getProcessResources();
            processResources.dependsOn(placeholderTask);

            try {
                field_inputResourcesDir.set(processResources, placeholderTask.getProcessedResourceFiles());
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Error setting ProcessAndroidResources#inputResourcesDir");
            }
        });
    }
}
