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

        Map<String, String> configuredPlaceholders = ext.getPlaceholders();
        String applicationId = variant.getApplicationId();
        String oldValue = configuredPlaceholders.put("applicationId", applicationId);
        if (oldValue != null) {
            configuredPlaceholders.put("applicationId", oldValue);
        }

        Collection<BaseVariantOutput> outputs;
        try {
            outputs = (Collection<BaseVariantOutput>) method_getOutputs.invoke(variant);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new AssertionError("Failed to invoke BaseVariantOutput#getOutputs()", e);
        }

        for (BaseVariantOutput output : outputs) {
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
                method_setResDir.invoke(processResources, placeholderTask.getOutputDirectory());
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Error invoking ProcessAndroidResources#setResDir", e);
            }
        }
    }
}
