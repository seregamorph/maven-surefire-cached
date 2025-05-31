package com.github.seregamorph.maven.test.core;

import com.github.seregamorph.maven.test.common.PluginName;
import com.github.seregamorph.maven.test.util.JsonSerializers;
import com.github.seregamorph.maven.test.util.MoreFileUtils;
import java.io.File;
import java.util.ArrayList;
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

    /**
     * Loads effective test plugin config which is merged from all parent projects.
     *
     * @param project
     * @param pluginName
     * @return
     */
    public static TestPluginConfig loadEffectiveTestPluginConfig(MavenProject project, PluginName pluginName) {
        var configs = new ArrayList<SurefireCachedConfig>();
        MavenProject currentProject = project;
        do {
            for (String fileName : List.of(CONFIG_FILE_NAME, ".mvn/" + CONFIG_FILE_NAME)) {
                var surefireCachedConfigFile = new File(currentProject.getBasedir(), fileName);
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
        for (var config : configs) {
            if (mergedConfig == null) {
                mergedConfig = mergeCommon(config, pluginName);
            } else {
                mergedConfig = merge(mergeCommon(config, pluginName), mergedConfig);
            }
        }

        if (mergedConfig == null) {
            throw new IllegalStateException("Unable to find surefire cached config file in "
                + new File(project.getBasedir(), CONFIG_FILE_NAME) + " or parent Maven project");
        }
        return merge(mergedConfig, TestPluginConfig.DEFAULT_CONFIG);
    }

    private static TestPluginConfig mergeCommon(SurefireCachedConfig surefireCachedConfig, PluginName pluginName) {
        var pluginConfig = pluginName == PluginName.SUREFIRE_CACHED ?
            surefireCachedConfig.getSurefire() : surefireCachedConfig.getFailsafe();
        var common = surefireCachedConfig.getCommon();

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
