package com.github.seregamorph.maven.test.core;

import static com.github.seregamorph.maven.test.core.ReflectionUtils.call;

import com.github.seregamorph.maven.test.common.GroupArtifactId;
import com.github.seregamorph.maven.test.util.ProjectModuleUtils;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import javax.inject.Singleton;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.project.MavenProject;

/**
 * @author Sergey Chernov
 */
@Singleton
public class TestTaskCacheHelper {

    private final FileHashCache fileHashCache = new FileHashCache();

    public TestTaskInput getTestTaskInput(MavenProject project, Mojo delegate, Set<GroupArtifactId> cacheExcludes) {
        var modules = ProjectModuleUtils.getProjectModules(project);

        var testTaskInput = new TestTaskInput();
        testTaskInput.addIgnoredProperty("timestamp", Instant.now().toString());
        // todo git commit hash

        var pluginArtifacts = project.getPluginArtifacts();
        for (var pluginArtifact : pluginArtifacts) {
            var file = pluginArtifact.getFile();
            // now the file is always null, enhance the caching key on demand
            var hash = file == null ? null : fileHashCache.getFileHash(file, FileSensitivity.CLASSPATH);
            var groupArtifactId = GroupArtifactId.of(pluginArtifact);
            testTaskInput.addPluginArtifactHash(groupArtifactId, pluginArtifact.getClassifier(),
                pluginArtifact.getVersion(), hash);
        }

        // todo add java version
        // todo system properties

        testTaskInput.setModuleName(project.getGroupId() + ":" + project.getArtifactId());
        var testClasspath = getTestClasspath(project);
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
        testTaskInput.setArgLine(call(delegate, String.class, "getArgLine"));
        testTaskInput.setTest(call(delegate, String.class, "getTest"));
        testTaskInput.setExcludes(call(delegate, List.class, "getExcludes"));
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
