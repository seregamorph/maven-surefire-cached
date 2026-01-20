package com.github.seregamorph.maven.test.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.github.seregamorph.maven.test.common.PluginName;
import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SurefireCachedMojoTest {

    private MavenProject project;
    private MavenSession session;
    private TestSurefireMojo delegate;
    private TestTaskCacheHelper testTaskCacheHelper;
    private SurefireCachedMojo surefireCachedMojo;

    @BeforeEach
    public void setUp() {
        var testPomFile = getResourceFile("module/module-pom.xml");
        project = new MavenProject();
        project.setVersion("1.0");
        project.setFile(testPomFile);

        var build = new Build();
        build.setDirectory(new File(project.getBasedir(), "target").getAbsolutePath());
        build.setOutputDirectory(new File(project.getBasedir(), "target/classes").getAbsolutePath());
        build.setTestOutputDirectory(new File(project.getBasedir(), "target/test-classes").getAbsolutePath());
        project.setBuild(build);

        var parentProject = new MavenProject();
        parentProject.setFile(getResourceFile("parent-pom.xml"));

        project.setParent(parentProject);

        var userProperties = new Properties();
        session = mock(MavenSession.class);
        var request = mock(MavenExecutionRequest.class);
        when(request.getActiveProfiles()).thenReturn(List.of("profile1"));
        when(session.getRequest()).thenReturn(request);
        when(session.getSystemProperties()).thenReturn(new Properties());
        when(session.getUserProperties()).thenReturn(userProperties);
        when(session.getAllProjects()).thenReturn(List.of(project));
        delegate = mock(TestSurefireMojo.class, withSettings().extraInterfaces(ContextEnabled.class));
        when(((ContextEnabled) delegate).getPluginContext()).thenReturn(Collections.emptyMap());
        when(delegate.getReportsDirectory()).thenReturn(getResourceFile("surefire-reports"));
        testTaskCacheHelper = new TestTaskCacheHelper();
        testTaskCacheHelper.init(session);
        surefireCachedMojo = new SurefireCachedMojo(testTaskCacheHelper, session, project, delegate);
    }

    @Test
    public void shouldLoadEffectiveTestPluginConfig() {
        var config = surefireCachedMojo.loadEffectiveTestPluginConfig(PluginName.SUREFIRE_CACHED);
        assertEquals(List.of("com.acme:core"), config.getExcludeModules());
        assertEquals(List.of("META-INF/MANIFEST.MF", "META-INF/maven/plugin.xml",
            "META-INF/maven/**/plugin-help.xml"), config.getExcludeClasspathResources());
        assertEquals(List.of("surefire-reports/TEST-*.xml"),
            config.getArtifacts().get("surefire-reports").getIncludes());
        assertEquals(List.of("jacoco-*.exec"), config.getArtifacts().get("jacoco").getIncludes());}

    @Test
    public void shouldReturnTaskInput() {
        var config = surefireCachedMojo.loadEffectiveTestPluginConfig(PluginName.SUREFIRE_CACHED);
        var testTaskInput = testTaskCacheHelper.getTestTaskInput(session, project, delegate, config);
        assertEquals("1.0", testTaskInput.getIgnoredProperties().get("project.version"));
        assertNotNull(testTaskInput.getIgnoredProperties().get("timestamp"));
    }

    @Test
    public void shouldReturnTaskOutput() {
        var endTime = Instant.now();
        var startTime = endTime.minusSeconds(10);
        var taskOutput = surefireCachedMojo.getTaskOutput(startTime, endTime);
        assertEquals(startTime, taskOutput.getStartTime());
        assertEquals(endTime, taskOutput.getEndTime());
        assertEquals(new BigDecimal("10.000"), taskOutput.getTotalTimeSeconds());
        assertEquals(new BigDecimal("0.034"), taskOutput.getTotalTestTimeSeconds());
        assertEquals(4, taskOutput.getTotalTests());
        assertEquals(0, taskOutput.getTotalErrors());
        assertEquals(0, taskOutput.getTotalFailures());
        assertNull(taskOutput.getFailureMessage());
        assertEquals("[FlakyFailure{testClassName='com.github.seregamorph.testsmartcontext.demo.FlakyTest', testName='testGetSubscriptionId_MicroserviceEnabled'}]", taskOutput.getTestcaseFlakyErrors().toString());
        assertEquals("[FlakyFailure{testClassName='com.github.seregamorph.testsmartcontext.demo.FlakyFailureTest', testName='transactionalMethods_shouldNotSelfCall'}]", taskOutput.getTestcaseFlakyFailures().toString());
        assertEquals("[FlakyFailure{testClassName='com.github.seregamorph.testsmartcontext.demo.FlakyTest', testName=''}]", taskOutput.getTestcaseErrors().toString());
        assertEquals("[FlakyFailure{testClassName='com.github.seregamorph.testsmartcontext.demo.RetriedFailureTest', testName='after space shutdown no new tasks is going to be executed[1]'}]", taskOutput.getTestcaseFailures().toString());
    }

    private static File getResourceFile(String name) {
        return new File(getResourceURI(name));
    }

    private static URI getResourceURI(String name) {
        URL resource = SurefireCachedMojoTest.class.getClassLoader().getResource(name);
        if (resource == null) {
            throw new RuntimeException("Resource not found: " + name);
        }
        try {
            return resource.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private interface TestSurefireMojo extends Mojo {

        File getReportsDirectory();

        String getArgLine();

        String getTest();

        String getGroups();

        String getExcludedGroups();

        List<String> getExcludes();
    }
}
