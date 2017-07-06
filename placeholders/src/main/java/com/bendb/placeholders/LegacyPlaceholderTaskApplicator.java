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

import org.gradle.api.Project;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

class LegacyPlaceholderTaskApplicator extends AbstractPlaceholderTaskApplicator {
    private final Method method_getOutputs;
    private final Method method_setResDir;

    LegacyPlaceholderTaskApplicator(Project project) throws Exception {
        super(project);

        method_getOutputs = ApplicationVariant.class.getMethod("getOutputs");
        method_getOutputs.setAccessible(true);

        method_setResDir = ProcessAndroidResources.class.getMethod("setResDir", File.class);
        method_setResDir.setAccessible(true);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void applyToVariant(ApplicationVariant variant) {
        // Specifying 'Collection' instead of 'List' so we can compile under AGP 2.x and 3.x

        MergeResources mergeResources = variant.getMergeResources();
        File mergedResourcesDir = mergeResources.getOutputDir();

        Collection<BaseVariantOutput> outputs;
        try {
            outputs = (Collection<BaseVariantOutput>) method_getOutputs.invoke(variant);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new AssertionError("Failed to invoke BaseVariantOutput#getOutputs()", e);
        }

        Map<String, Object> placeholders = new LinkedHashMap<>(
                variant.getMergedFlavor().getManifestPlaceholders());
        placeholders.put("applicationId", variant.getApplicationId());

        File intermediateOutputDir = new File(getIntermediateDir(), variant.getDirName());

        for (BaseVariantOutput output : outputs) {
            File outputDir = intermediateOutputDir;
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
                method_setResDir.invoke(processResources, placeholderTask.getOutputDirectory());
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Error invoking ProcessAndroidResources#setResDir", e);
            }
        }
    }
}
