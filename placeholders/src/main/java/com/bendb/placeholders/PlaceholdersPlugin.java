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

import com.android.build.gradle.AppPlugin;
import com.android.builder.Version;
import com.android.repository.Revision;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class PlaceholdersPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        AppPlugin appPlugin = project.getPlugins().findPlugin(AppPlugin.class);
        if (appPlugin == null) {
            throw new IllegalStateException("The 'com.android.application' plugin must be applied first");
        }

        PlaceholderTaskApplicator applicator;
        try {
            applicator = createApplicator(project);
        } catch (Exception e) {
            throw new IllegalStateException("Error instantiating PlaceholderTaskApplicator", e);
        }

        applicator.applyPlaceholderTask(project);
    }

    private PlaceholderTaskApplicator createApplicator(Project project) throws Exception {
        Revision currentRevision = Revision.parseRevision(Version.ANDROID_GRADLE_PLUGIN_VERSION);
        Revision threePointOhRevision = Revision.parseRevision("3.0.0");

        PlaceholderTaskApplicator applicator;
        if (currentRevision.compareTo(threePointOhRevision, Revision.PreviewComparison.IGNORE) >= 0) {
            applicator = new DefaultPlaceholderTaskApplicator(project);
        } else {
            applicator = new LegacyPlaceholderTaskApplicator(project);
        }
        return applicator;
    }
}
