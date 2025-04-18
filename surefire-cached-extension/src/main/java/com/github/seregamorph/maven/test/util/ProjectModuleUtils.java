package com.github.seregamorph.maven.test.util;

import com.github.seregamorph.maven.test.common.GroupArtifactId;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

/**
 * @author Sergey Chernov
 */
public final class ProjectModuleUtils {

    public static SortedSet<GroupArtifactId> getProjectModules(MavenProject project) {
        var projectModules = new TreeSet<GroupArtifactId>(Comparator.comparing(Object::toString));
        projectModules.add(GroupArtifactId.of(project.getArtifact()));
        addProjectReferences(projectModules, project.getProjectReferences().values());
        return projectModules;
    }

    private static void addProjectReferences(Set<GroupArtifactId> projectReferences, Collection<MavenProject> mavenProjects) {
        for (MavenProject mavenProject : mavenProjects) {
            Artifact artifact = mavenProject.getArtifact();
            var groupArtifactId = GroupArtifactId.of(artifact);
            if (projectReferences.add(groupArtifactId)) {
                addProjectReferences(projectReferences, mavenProject.getProjectReferences().values());
            }
        }
    }

    private ProjectModuleUtils() {
    }
}
