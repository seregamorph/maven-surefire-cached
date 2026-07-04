package com.github.seregamorph.maven.test.extension;

import static com.github.seregamorph.maven.test.TestFileUtils.getResourceFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.github.seregamorph.maven.test.common.PluginName;
import java.io.File;
import java.math.BigDecimal;
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

public class IntegrationTestCachedMojoTest {

    private MavenProject project;
    private MavenSession session;
    private TestFailsafeMojo delegate;
    private TestTaskCacheHelper testTaskCacheHelper;
    private IntegrationTestCachedMojo integrationTestCachedMojo;

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

        var userProperties = new Properties();
        session = mock(MavenSession.class);
        var request = mock(MavenExecutionRequest.class);
        when(request.getActiveProfiles()).thenReturn(List.of("profile1"));
        when(session.getRequest()).thenReturn(request);
        when(session.getSystemProperties()).thenReturn(new Properties());
        when(session.getUserProperties()).thenReturn(userProperties);
        when(session.getAllProjects()).thenReturn(List.of(project));
        delegate = mock(TestFailsafeMojo.class, withSettings().extraInterfaces(ContextEnabled.class));
        when(((ContextEnabled) delegate).getPluginContext()).thenReturn(Collections.emptyMap());
        when(delegate.getSummaryFile()).thenReturn(getResourceFile("failsafe-reports/failsafe-summary.xml"));
        when(delegate.getReportsDirectory()).thenReturn(getResourceFile("failsafe-reports"));
        testTaskCacheHelper = new TestTaskCacheHelper();
        testTaskCacheHelper.initSession(session);
        testTaskCacheHelper.initProjects(session);
        integrationTestCachedMojo = new IntegrationTestCachedMojo(testTaskCacheHelper, session, project, delegate);
    }

    @Test
    public void shouldReturnTaskInput() {
        var config = integrationTestCachedMojo.loadEffectiveTestPluginConfig(PluginName.SUREFIRE_CACHED);
        var testTaskInput = testTaskCacheHelper.getTestTaskInput(session, project, delegate, config);
        assertEquals("1.0", testTaskInput.getIgnoredProperties().get("project.version"));
        assertNotNull(testTaskInput.getIgnoredProperties().get("timestamp"));
    }

    @Test
    public void shouldReturnTaskOutput() {
        var endTime = Instant.now();
        var startTime = endTime.minusSeconds(10);
        var taskOutput = integrationTestCachedMojo.getTaskOutput(startTime, endTime);
        assertEquals(startTime, taskOutput.getStartTime());
        assertEquals(endTime, taskOutput.getEndTime());
        assertEquals(new BigDecimal("10.000"), taskOutput.getTotalTimeSeconds());
        assertEquals(new BigDecimal("10.000"), taskOutput.getTotalTestTimeSeconds());
        assertEquals(1, taskOutput.getTotalTests());
        assertEquals(2, taskOutput.getTotalErrors());
        assertEquals(3, taskOutput.getTotalFailures());
        assertThat(taskOutput.getFailureMessage())
            .startsWith("org.apache.maven.surefire.booter.SurefireBooterForkException: The forked VM terminated without properly saying goodbye. VM crash or System.exit called?");
        assertEquals("[FlakyFailure{testClassName='failsafe-summary.xml', testName='flakes'}]", taskOutput.getTestcaseFlakyErrors().toString());
        assertEquals("[]", taskOutput.getTestcaseFlakyFailures().toString());
        assertEquals("[]", taskOutput.getTestcaseErrors().toString());
        assertEquals("[]", taskOutput.getTestcaseFailures().toString());
    }

    private interface TestFailsafeMojo extends Mojo {

        File getSummaryFile();

        File getReportsDirectory();

        String getArgLine();

        String getTest();

        String getGroups();

        String getExcludedGroups();

        List<String> getExcludes();
    }
}
