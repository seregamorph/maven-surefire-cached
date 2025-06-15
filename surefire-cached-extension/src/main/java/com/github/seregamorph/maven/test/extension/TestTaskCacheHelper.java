package com.github.seregamorph.maven.test.extension;

import static com.github.seregamorph.maven.test.util.ReflectionUtils.call;

import com.github.seregamorph.maven.test.common.GroupArtifactId;
import com.github.seregamorph.maven.test.config.TestPluginConfig;
import com.github.seregamorph.maven.test.core.FileHashCache;
import com.github.seregamorph.maven.test.core.TestTaskInput;
import com.github.seregamorph.maven.test.storage.CacheService;
import com.github.seregamorph.maven.test.storage.CacheServiceMetrics;
import com.github.seregamorph.maven.test.storage.CacheStorage;
import com.github.seregamorph.maven.test.util.AntPathMatcher;
import com.github.seregamorph.maven.test.util.HashUtils;
import com.github.seregamorph.maven.test.util.MavenPropertyUtils;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
        this.cacheStorage = new CacheStorageFactory(session).createCacheStorage();
        // todo configurable failureThreshold
        this.cacheService = new CacheService(cacheStorage, metrics, 4);
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

    public TestTaskInput getTestTaskInput(
        MavenSession session,
        MavenProject project,
        Mojo delegate,
        TestPluginConfig testPluginConfig
    ) {
        List<String> activeProfiles = session.getRequest().getActiveProfiles();

        TestTaskInput testTaskInput = new TestTaskInput();

        testTaskInput.addIgnoredProperty("timestamp", Instant.now().toString());
        for (String ignoredProperty : testPluginConfig.getInputIgnoredProperties()) {
            String value = MavenPropertyUtils.getProperty(session, project, ignoredProperty);
            if (value != null) {
                testTaskInput.addIgnoredProperty(ignoredProperty, value);
            }
        }

        for (String property : testPluginConfig.getInputProperties()) {
            String value = MavenPropertyUtils.getProperty(session, project, property);
            if (value != null) {
                testTaskInput.addProperty(property, value);
            }
        }

        Map<?, ?> pluginContext = ((ContextEnabled) delegate).getPluginContext();
        PluginDescriptor pluginDescriptor = (PluginDescriptor) pluginContext.get("pluginDescriptor");
        if (pluginDescriptor != null) {
            List<Artifact> pluginArtifacts = pluginDescriptor.getArtifacts();
            for (Artifact pluginArtifact : pluginArtifacts) {
                File file = pluginArtifact.getFile();
                String hash = file == null ? null : fileHashCache.getClasspathElementHash(file,
                    testPluginConfig.getExcludeClasspathResources());
                GroupArtifactId groupArtifactId = groupArtifactId(pluginArtifact);
                testTaskInput.addPluginArtifactHash(groupArtifactId, pluginArtifact.getClassifier(),
                    pluginArtifact.getVersion(), hash);
            }
        }

        testTaskInput.setModuleName(project.getGroupId() + ":" + project.getArtifactId());
        TestClasspath testClasspath = getTestClasspath(project);
        for (Artifact artifact : testClasspath.artifacts()) {
            if (isIncludeToCacheEntry(testPluginConfig.getExcludeModules(), artifact)) {
                // Can be a jar file (when "install" command is executed) or
                // a classes directory (when "test" command is executed).
                // The trick is we calculate hash of files which is the same in both cases (jar manifest is ignored)
                File file = artifact.getFile();
                String hash = fileHashCache.getClasspathElementHash(file, testPluginConfig.getExcludeClasspathResources());
                GroupArtifactId groupArtifactId = groupArtifactId(artifact);
                String classifier = artifact.getClassifier();
                String classifierSuffix = classifier == null || classifier.isEmpty() ? "" : ":" + classifier;
                String fileTypeSuffix = file.isDirectory() ? "@dir" : "@" + artifact.getType();
                if (modules.contains(groupArtifactId)) {
                    testTaskInput.addModuleArtifactHash(groupArtifactId + classifierSuffix + fileTypeSuffix, hash);
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

    static boolean isIncludeToCacheEntry(List<String> excludeModules, Artifact artifact) {
        if (artifact.getArtifactHandler().isAddedToClasspath()) {
            AntPathMatcher antPathMatcher = new AntPathMatcher(":");
            for (String excludeModule : excludeModules) {
                String gai = artifact.getGroupId() + ":" + artifact.getArtifactId();
                if (antPathMatcher.match(excludeModule, gai)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static TestClasspath getTestClasspath(MavenProject project) {
        // todo respect getClasspathDependencyExcludes, getClasspathDependencyScopeExclude,
        // getAdditionalClasspathElements
        Set<Artifact> artifacts = project.getArtifacts();
        File classesDir = new File(project.getBuild().getOutputDirectory());
        File testClassesDir = new File(project.getBuild().getTestOutputDirectory());
        return new TestClasspath(artifacts, classesDir, testClassesDir);
    }

    private static GroupArtifactId groupArtifactId(Artifact artifact) {
        return new GroupArtifactId(artifact.getGroupId(), artifact.getArtifactId());
    }

    private static final class TestClasspath {

        private final Set<Artifact> artifacts;
        private final File classesDir;
        private final File testClassesDir;

        private TestClasspath(Set<Artifact> artifacts, File classesDir, File testClassesDir) {
            this.artifacts = artifacts;
            this.classesDir = classesDir;
            this.testClassesDir = testClassesDir;
        }

        public Set<Artifact> artifacts() {
            return artifacts;
        }

        public File classesDir() {
            return classesDir;
        }

        public File testClassesDir() {
            return testClassesDir;
        }
    }
}
