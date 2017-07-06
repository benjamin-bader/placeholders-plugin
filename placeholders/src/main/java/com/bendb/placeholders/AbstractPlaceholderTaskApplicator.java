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

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.ApplicationVariant;

import org.gradle.api.Project;

import java.io.File;
import java.util.Locale;

abstract class AbstractPlaceholderTaskApplicator implements PlaceholderTaskApplicator {
    protected final Project project;

    protected AbstractPlaceholderTaskApplicator(Project project) {
        this.project = project;
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

    protected File getIntermediateDir() {
        return new File(project.getBuildDir(), "intermediates/res/post-processed");
    }
}
