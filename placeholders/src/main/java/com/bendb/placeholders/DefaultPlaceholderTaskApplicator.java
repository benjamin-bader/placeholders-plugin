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
        field_inputResourcesDir = ProcessAndroidResources.class.getField("inputResourcesDir");
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

            String taskName = String.format(Locale.US, "process%sResourcePlaceholders", taskNameSlug);
            PlaceholderReplacementTask placeholderTask = project
                    .getTasks()
                    .create(taskName, PlaceholderReplacementTask.class);

            placeholderTask.dependsOn(mergeResources);
            placeholderTask.setPreProcessedResourceDirectory(mergedResourcesDir);
            placeholderTask.setPlaceholders(configuredPlaceholders);

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
