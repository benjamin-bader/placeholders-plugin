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
