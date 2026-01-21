package com.github.seregamorph.maven.test.config;

import static com.github.seregamorph.maven.test.TestFileUtils.getResourceFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.seregamorph.maven.test.common.PluginName;
import java.util.List;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class TestPluginConfigLoaderTest {

    private MavenProject project;

    @BeforeEach
    public void setUp() {
        var testPomFile = getResourceFile("module/module-pom.xml");
        project = new MavenProject();
        project.setVersion("1.0");
        project.setFile(testPomFile);

        var parentProject = new MavenProject();
        parentProject.setFile(getResourceFile("parent-pom.xml"));
        project.setParent(parentProject);
    }

    @Test
    public void shouldLoadEffectiveSurefireTestPluginConfig() {
        var config = TestPluginConfigLoader.loadEffectiveTestPluginConfig(project, PluginName.SUREFIRE_CACHED);
        assertEquals(List.of("com.acme:core"), config.getExcludeModules());
        assertEquals(List.of("META-INF/MANIFEST.MF", "META-INF/maven/plugin.xml",
            "META-INF/maven/**/plugin-help.xml"), config.getExcludeClasspathResources());
        assertEquals(List.of("surefire-reports/TEST-*.xml"),
            config.getArtifacts().get("surefire-reports").getIncludes());
        assertEquals(List.of("jacoco-*.exec"), config.getArtifacts().get("jacoco").getIncludes());
    }

    @Test
    @Disabled // todo fix
    public void shouldLoadEffectiveFailsafeTestPluginConfig() {
        var config = TestPluginConfigLoader.loadEffectiveTestPluginConfig(project, PluginName.FAILSAFE_CACHED);
        assertEquals(List.of("com.acme:core"), config.getExcludeModules());
        assertEquals(List.of("META-INF/MANIFEST.MF", "META-INF/maven/**/pom.properties",
            "META-INF/maven/**/pom.xml"), config.getExcludeClasspathResources());
        assertEquals(List.of("failsafe-reports/TEST-*.xml"),
            config.getArtifacts().get("failsafe-reports").getIncludes());
        assertEquals(List.of("jacoco-*.exec"), config.getArtifacts().get("jacoco").getIncludes());
    }
}
