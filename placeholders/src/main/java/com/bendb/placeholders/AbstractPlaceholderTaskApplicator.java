package com.bendb.placeholders;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.ApplicationVariant;

import org.gradle.api.Project;

import java.util.Locale;

abstract class AbstractPlaceholderTaskApplicator implements PlaceholderTaskApplicator {
    protected final Project project;
    protected final PlaceholdersExtension ext;

    protected AbstractPlaceholderTaskApplicator(Project project) {
        this.project = project;
        this.ext = project.getExtensions().create("placeholders", PlaceholdersExtension.class);
    }

    @Override
    public void applyPlaceholderTask(Project project) {
        AppExtension appExtension = project.getExtensions().findByType(AppExtension.class);
        if (appExtension == null) {
            throw new IllegalArgumentException("'com.android.application' must be applied first");
        }

        appExtension.getApplicationVariants().all(this::applyToVariant);
    }

    protected abstract void applyToVariant(ApplicationVariant variant);

    protected String capitalize(String string) {
        if (string == null || "".equals(string)) {
            return "";
        }

        return string.substring(0, 1).toUpperCase(Locale.US) + string.substring(1);
    }
}
