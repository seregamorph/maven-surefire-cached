package com.github.seregamorph.maven.test.extension.local;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

/**
 * @author Sergey Chernov
 */
final class ProjectModuleExtUtils {

    public static SortedMap<GroupArtifactId, MavenProject> getProjectModules(MavenProject project) {
        var projectModules = new TreeMap<GroupArtifactId, MavenProject>(Comparator.comparing(Object::toString));
        projectModules.put(GroupArtifactId.of(project.getArtifact()), project);
        addProjectReferences(projectModules, project.getProjectReferences().values());
        return projectModules;
    }

    private static void addProjectReferences(Map<GroupArtifactId, MavenProject> projectReferences, Collection<MavenProject> mavenProjects) {
        for (MavenProject mavenProject : mavenProjects) {
            Artifact artifact = mavenProject.getArtifact();
            var groupArtifactId = GroupArtifactId.of(artifact);
            if (projectReferences.put(groupArtifactId, mavenProject) == null) {
                addProjectReferences(projectReferences, mavenProject.getProjectReferences().values());
            }
        }
    }

    private ProjectModuleExtUtils() {
    }
}
