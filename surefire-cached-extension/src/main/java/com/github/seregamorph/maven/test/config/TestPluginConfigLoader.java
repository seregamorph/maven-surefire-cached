package com.github.seregamorph.maven.test.config;

import com.github.seregamorph.maven.test.common.PluginName;
import com.github.seregamorph.maven.test.util.JsonSerializers;
import com.github.seregamorph.maven.test.util.MoreFileUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.apache.maven.project.MavenProject;

/**
 * We iterate from root project towards current project checking for
 * <pre>surefire-cached.json</pre> and <pre>.mvn/surefire-cached.json.</pre> files.
 * <p>
 * And then merge them in this order.
 *
 * @author Sergey Chernov
 */
public class TestPluginConfigLoader {

    private static final String CONFIG_FILE_NAME = "surefire-cached.json";

    private static final TestPluginConfig DEFAULT_COMMON_CONFIG = new TestPluginConfig()
        .setInputIgnoredProperties(Arrays.asList(
            "java.version",
            "os.arch",
            "os.name",
            "env.CI",
            "env.GITHUB_BASE_REF",
            "env.GITHUB_REF",
            "env.GITHUB_RUN_ID",
            "env.GITHUB_JOB",
            "env.GITHUB_SHA"
        ))
        .setInputProperties(Arrays.asList("java.specification.version"))
        .setExcludeModules(Arrays.asList())
        // remove MANIFEST.MF and default maven descriptors with version
        // from the hash to unify hash calculation for jar files and classes directories
        .setExcludeClasspathResources(Arrays.asList(
            "META-INF/MANIFEST.MF",
            "META-INF/maven/**/pom.properties",
            "META-INF/maven/**/pom.xml"
        ));

    private static final TestPluginConfig DEFAULT_SUREFIRE_CONFIG = merge(
        new TestPluginConfig()
            .setArtifacts(Collections.singletonMap(
                "surefire-reports", new ArtifactsConfig()
                    .setIncludes(Arrays.asList(
                        "surefire-reports/TEST-*.xml"
                    )))),
        DEFAULT_COMMON_CONFIG);

    private static final TestPluginConfig DEFAULT_FAILSAFE_CONFIG = merge(
        new TestPluginConfig()
            .setArtifacts(Collections.singletonMap(
                "failsafe-reports", new ArtifactsConfig()
                    .setIncludes(Arrays.asList(
                        "failsafe-reports/TEST-*.xml",
                        "failsafe-reports/failsafe-summary.xml"
                    )))),
        DEFAULT_COMMON_CONFIG);

    /**
     * Loads effective test plugin config which is merged from all parent projects.
     *
     * @param project
     * @param pluginName
     * @return
     */
    public static TestPluginConfig loadEffectiveTestPluginConfig(MavenProject project, PluginName pluginName) {
        List<SurefireCachedConfig> configs = new ArrayList<>();
        MavenProject currentProject = project;
        do {
            for (String fileName : Arrays.asList(CONFIG_FILE_NAME, ".mvn/" + CONFIG_FILE_NAME)) {
                File surefireCachedConfigFile = new File(currentProject.getBasedir(), fileName);
                if (surefireCachedConfigFile.exists()) {
                    configs.add(JsonSerializers.deserialize(
                        MoreFileUtils.read(surefireCachedConfigFile), SurefireCachedConfig.class,
                        surefireCachedConfigFile.toString()));
                }
            }
            currentProject = currentProject.getParent();
        } while (currentProject != null && currentProject.getBasedir() != null);

        Collections.reverse(configs);
        TestPluginConfig mergedConfig = null;
        for (SurefireCachedConfig config : configs) {
            if (mergedConfig == null) {
                mergedConfig = mergeCommon(config, pluginName);
            } else {
                mergedConfig = merge(mergeCommon(config, pluginName), mergedConfig);
            }
        }

        TestPluginConfig defaultConfig = pluginName == PluginName.SUREFIRE_CACHED ?
            DEFAULT_SUREFIRE_CONFIG : DEFAULT_FAILSAFE_CONFIG;
        if (mergedConfig == null) {
            return defaultConfig;
        }
        return merge(mergedConfig, DEFAULT_COMMON_CONFIG);
    }

    private static TestPluginConfig mergeCommon(SurefireCachedConfig surefireCachedConfig, PluginName pluginName) {
        TestPluginConfig pluginConfig = pluginName == PluginName.SUREFIRE_CACHED ?
            surefireCachedConfig.getSurefire() : surefireCachedConfig.getFailsafe();
        TestPluginConfig common = surefireCachedConfig.getCommon();

        return merge(pluginConfig, common);
    }

    private static TestPluginConfig merge(TestPluginConfig primaryConfig, TestPluginConfig defaultConfig) {
        return new TestPluginConfig()
            .setInputProperties(resolveProperty(primaryConfig, defaultConfig,
                TestPluginConfig::getInputProperties))
            .setInputIgnoredProperties(resolveProperty(primaryConfig, defaultConfig,
                TestPluginConfig::getInputIgnoredProperties))
            .setExcludeModules(resolveProperty(primaryConfig, defaultConfig,
                TestPluginConfig::getExcludeModules))
            .setExcludeClasspathResources(resolveProperty(primaryConfig, defaultConfig,
                TestPluginConfig::getExcludeClasspathResources))
            .setArtifacts(resolveProperty(primaryConfig, defaultConfig,
                TestPluginConfig::getArtifacts));
    }

    @Nullable
    private static <T> T resolveProperty(
        TestPluginConfig primarySource,
        TestPluginConfig secondarySource,
        Function<TestPluginConfig, T> extractor
    ) {
        T value = primarySource == null ? null : extractor.apply(primarySource);
        if (value == null) {
            value = secondarySource == null ? null : extractor.apply(secondarySource);
        }
        return value;
    }

    private TestPluginConfigLoader() {
    }
}
