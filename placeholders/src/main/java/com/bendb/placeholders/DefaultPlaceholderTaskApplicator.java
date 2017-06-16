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

import org.gradle.api.DomainObjectCollection;
import org.gradle.api.Project;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;

class DefaultPlaceholderTaskApplicator extends AbstractPlaceholderTaskApplicator {
    private final Field field_inputResourcesDir;

    DefaultPlaceholderTaskApplicator(Project project) throws Exception {
        super(project);
        field_inputResourcesDir = ProcessAndroidResources.class.getDeclaredField("inputResourcesDir");
        field_inputResourcesDir.setAccessible(true);
    }

    @Override
    @SuppressWarnings({"RedundantCast", "unchecked"}) // Weird casting necessary to compile on both AGP 2.x and 3.x
    protected void applyToVariant(ApplicationVariant variant) {
        DomainObjectCollection<BaseVariantOutput> outputs = (DomainObjectCollection<BaseVariantOutput>) (Object) variant.getOutputs();
        MergeResources mergeResources = variant.getMergeResources();
        File mergedResourcesDir = mergeResources.getOutputDir();

        Map<String, String> configuredPlaceholders = ext.getPlaceholders();
        String applicationId = variant.getApplicationId();
        String oldValue = configuredPlaceholders.put("applicationId", applicationId);
        if (oldValue != null) {
            configuredPlaceholders.put("applicationId", oldValue);
        }

        outputs.all(output -> {
            String taskNameSlug = capitalize(variant.getName());
            if (outputs.size() > 1) {
                taskNameSlug += capitalize(output.getName());
            }

            File outputBuildDir = new File(project.getBuildDir(), output.getDirName());
            File processedResourcesOutputDir = new File(outputBuildDir, "res-placeholders");

            String taskName = String.format(Locale.US, "process%sResourcePlaceholders", taskNameSlug);
            PlaceholderReplacementTask placeholderTask = project
                    .getTasks()
                    .create(taskName, PlaceholderReplacementTask.class);

            placeholderTask.dependsOn(mergeResources);
            placeholderTask.setPreProcessedResourceDirectory(mergedResourcesDir);
            placeholderTask.setPlaceholders(configuredPlaceholders);
            placeholderTask.setOutputDirectory(processedResourcesOutputDir);

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
