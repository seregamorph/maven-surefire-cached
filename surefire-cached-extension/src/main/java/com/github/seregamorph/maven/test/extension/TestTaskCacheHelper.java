package com.github.seregamorph.maven.test.extension;

import static com.github.seregamorph.maven.test.util.ReflectionUtils.call;

import com.github.seregamorph.maven.test.common.GroupArtifactId;
import com.github.seregamorph.maven.test.core.FileHashCache;
import com.github.seregamorph.maven.test.core.TestPluginConfig;
import com.github.seregamorph.maven.test.core.TestTaskInput;
import com.github.seregamorph.maven.test.storage.CacheService;
import com.github.seregamorph.maven.test.storage.CacheServiceMetrics;
import com.github.seregamorph.maven.test.storage.CacheStorage;
import com.github.seregamorph.maven.test.util.HashUtils;
import com.github.seregamorph.maven.test.util.MavenPropertyUtils;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

/**
 * @author Sergey Chernov
 */
@Singleton
public class TestTaskCacheHelper {

    private static final String PROP_CACHE_STORAGE_URL = "cacheStorageUrl";

    private FileHashCache fileHashCache;
    private SortedSet<GroupArtifactId> modules;
    private CacheServiceMetrics metrics;
    private CacheStorage cacheStorage;
    private CacheService cacheService;
    private CacheReport cacheReport;

    public void init(MavenSession session) {
        fileHashCache = new FileHashCache();
        modules = session.getAllProjects().stream()
            .map(p -> new GroupArtifactId(p.getGroupId(), p.getArtifactId()))
            .collect(Collectors.toCollection(TreeSet::new));

        this.metrics = new CacheServiceMetrics();
        this.cacheStorage = createCacheStorage(session);
        this.cacheService = new CacheService(cacheStorage, metrics, 2);
        this.cacheReport = new CacheReport();
    }

    public void destroy() {
        cacheReport = null;
        cacheService = null;
        cacheStorage = null;
        metrics = null;
        modules = null;
        fileHashCache = null;
    }

    public CacheServiceMetrics getMetrics() {
        if (metrics == null) {
            throw new IllegalStateException("metrics is not initialized");
        }
        return metrics;
    }

    public CacheStorage getCacheStorage() {
        if (cacheStorage == null) {
            throw new IllegalStateException("cacheStorage is not initialized");
        }
        return cacheStorage;
    }

    public CacheService getCacheService() {
        if (cacheService == null) {
            throw new IllegalStateException("cacheStorage is not initialized");
        }
        return cacheService;
    }

    public CacheReport getCacheReport() {
        if (cacheReport == null) {
            throw new IllegalStateException("cacheReport is not initialized");
        }
        return cacheReport;
    }

    private static CacheStorage createCacheStorage(MavenSession session) {
        String cacheStorageUrl = session.getUserProperties().getProperty(PROP_CACHE_STORAGE_URL);
        if (cacheStorageUrl == null) {
            cacheStorageUrl = System.getProperty("user.home") + "/.m2/test-cache";
        }
        return CacheStorageFactory.createCacheStorage(cacheStorageUrl);
    }

    public TestTaskInput getTestTaskInput(
        MavenSession session,
        MavenProject project,
        Mojo delegate,
        TestPluginConfig testPluginConfig
    ) {
        var activeProfiles = session.getRequest().getActiveProfiles();

        var testTaskInput = new TestTaskInput();

        testTaskInput.addIgnoredProperty("timestamp", Instant.now().toString());
        for (var ignoredProperty : testPluginConfig.getInputIgnoredProperties()) {
            var value = MavenPropertyUtils.getProperty(session, project, ignoredProperty);
            if (value != null) {
                testTaskInput.addIgnoredProperty(ignoredProperty, value);
            }
        }

        for (var property : testPluginConfig.getInputProperties()) {
            var value = MavenPropertyUtils.getProperty(session, project, property);
            if (value != null) {
                testTaskInput.addProperty(property, value);
            }
        }

        var pluginContext = ((ContextEnabled) delegate).getPluginContext();
        var pluginDescriptor = (PluginDescriptor) pluginContext.get("pluginDescriptor");
        if (pluginDescriptor != null) {
            var pluginArtifacts = pluginDescriptor.getArtifacts();
            for (var pluginArtifact : pluginArtifacts) {
                var file = pluginArtifact.getFile();
                var hash = file == null ? null : fileHashCache.getClasspathElementHash(file, List.of());
                var groupArtifactId = groupArtifactId(pluginArtifact);
                testTaskInput.addPluginArtifactHash(groupArtifactId, pluginArtifact.getClassifier(),
                    pluginArtifact.getVersion(), hash);
            }
        }

        testTaskInput.setModuleName(project.getGroupId() + ":" + project.getArtifactId());
        var testClasspath = getTestClasspath(project);
        var excludeModules = testPluginConfig.getExcludeModules().stream()
            .map(GroupArtifactId::fromString)
            .collect(Collectors.toSet());
        for (var artifact : testClasspath.artifacts()) {
            if (isIncludeToCacheEntry(artifact, excludeModules)) {
                // Can be a jar file (when "install" command is executed) or
                // a classes directory (when "test" command is executed).
                // The trick is we calculate hash of files which is the same in both cases (jar manifest is ignored)
                var file = artifact.getFile();
                var hash = fileHashCache.getClasspathElementHash(file, testPluginConfig.getExcludeClasspathResources());
                var groupArtifactId = groupArtifactId(artifact);
                var suffix = file.isDirectory() ? "@dir" : "@" + artifact.getType();
                if (modules.contains(groupArtifactId)) {
                    testTaskInput.addModuleArtifactHash(groupArtifactId + suffix, hash);
                } else {
                    testTaskInput.addLibraryArtifactHash(groupArtifactId, artifact.getClassifier(),
                        artifact.getVersion(), hash);
                }
            }
        }
        if (testClasspath.classesDir().exists()) {
            testTaskInput.setClassesHashes(HashUtils.hashDirectory(testClasspath.classesDir(),
                testPluginConfig.getExcludeClasspathResources()));
        }
        if (testClasspath.testClassesDir().exists()) {
            testTaskInput.setTestClassesHashes(HashUtils.hashDirectory(testClasspath.testClassesDir(),
                testPluginConfig.getExcludeClasspathResources()));
        }
        testTaskInput.setActiveProfiles(activeProfiles);
        testTaskInput.setArgLine(call(delegate, String.class, "getArgLine"));
        testTaskInput.setTest(call(delegate, String.class, "getTest"));
        testTaskInput.setExcludeClasspathResources(testPluginConfig.getExcludeClasspathResources());
        testTaskInput.setArtifactConfigs(testPluginConfig.getArtifacts());
        testTaskInput.setGroups(call(delegate, String.class, "getGroups"));
        testTaskInput.setExcludedGroups(call(delegate, String.class, "getExcludedGroups"));
        testTaskInput.setExcludes(call(delegate, List.class, "getExcludes"));
        // todo filtered getProperties
        return testTaskInput;
    }

    private static boolean isIncludeToCacheEntry(Artifact artifact, Set<GroupArtifactId> excludeModules) {
        return artifact.getArtifactHandler().isAddedToClasspath()
            && !excludeModules.contains(groupArtifactId(artifact));
    }

    private static TestClasspath getTestClasspath(MavenProject project) {
        // todo respect getClasspathDependencyExcludes, getClasspathDependencyScopeExclude
        var artifacts = project.getArtifacts();
        var classesDir = new File(project.getBuild().getOutputDirectory());
        var testClassesDir = new File(project.getBuild().getTestOutputDirectory());
        return new TestClasspath(artifacts, classesDir, testClassesDir);
    }

    private static GroupArtifactId groupArtifactId(Artifact artifact) {
        return new GroupArtifactId(artifact.getGroupId(), artifact.getArtifactId());
    }

    private record TestClasspath(Set<Artifact> artifacts, File classesDir, File testClassesDir) {
    }
}
