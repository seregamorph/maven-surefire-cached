package com.github.seregamorph.maven.test.core;

import com.github.seregamorph.maven.test.common.GroupArtifactId;
import com.github.seregamorph.maven.test.util.ProjectModuleUtils;
import java.io.File;
import java.time.Instant;
import java.util.Set;
import javax.inject.Singleton;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.surefire.AbstractSurefireMojo;
import org.apache.maven.project.MavenProject;

@Singleton
public class TestTaskCacheHelper {

    private final FileHashCache fileHashCache = new FileHashCache();

    public TestTaskInput getTestTaskInput(AbstractSurefireMojo task, Set<GroupArtifactId> cacheExcludes) {
        var modules = ProjectModuleUtils.getProjectModules(task.getProject());

        var testTaskInput = new TestTaskInput();
        testTaskInput.addIgnoredProperty("timestamp", Instant.now().toString());
        // todo git commit hash

        // todo add java version
        // todo system properties

        testTaskInput.setModuleName(task.getProject().getGroupId() + ":" + task.getProject().getArtifactId());
        var testClasspath = getTestClasspath(task.getProject());
        for (var artifact : testClasspath.artifacts()) {
            if (isIncludeToCacheEntry(artifact, cacheExcludes)) {
                // can be a jar file (when "install" command is executed) or
                // a classes directory (when "test" command is executed)
                var file = artifact.getFile();
                var hash = fileHashCache.getFileHash(file, FileSensitivity.CLASSPATH);
                var groupArtifactId = GroupArtifactId.of(artifact);
                if (modules.contains(groupArtifactId)) {
                    testTaskInput.addModuleArtifactHash(groupArtifactId, hash);
                } else {
                    testTaskInput.addLibraryArtifactHash(groupArtifactId, artifact.getClassifier(),
                            artifact.getVersion(), hash);
                }
            }
        }
        if (testClasspath.classesDir().exists()) {
            testTaskInput.setClassesHashes(HashUtils.hashDirectory(testClasspath.classesDir()));
        }
        if (testClasspath.testClassesDir().exists()) {
            testTaskInput.setTestClassesHashes(HashUtils.hashDirectory(testClasspath.testClassesDir()));
        }
        // todo support additional files like logback.xml not in the classpath
        testTaskInput.setArgLine(task.getArgLine());
        testTaskInput.setTest(task.getTest());
        testTaskInput.setExcludes(task.getExcludes());
        return testTaskInput;
    }

    private static boolean isIncludeToCacheEntry(Artifact artifact, Set<GroupArtifactId> cacheExcludes) {
        return artifact.getArtifactHandler().isAddedToClasspath()
            && !cacheExcludes.contains(GroupArtifactId.of(artifact));
    }

    private static TestClasspath getTestClasspath(MavenProject project) {
        var artifacts = project.getArtifacts();
        var classesDir = new File(project.getBuild().getOutputDirectory());
        var testClassesDir = new File(project.getBuild().getTestOutputDirectory());
        return new TestClasspath(artifacts, classesDir, testClassesDir);
    }

    private record TestClasspath(Set<Artifact> artifacts, File classesDir, File testClassesDir) {
    }
}
