package com.github.seregamorph.maven.test;

import static com.github.seregamorph.maven.test.common.TestTaskOutput.PLUGIN_SUREFIRE_CACHED;

import com.github.seregamorph.maven.test.core.CachedTestWrapper;
import com.github.seregamorph.maven.test.core.TestTaskCacheHelper;
import javax.inject.Inject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.surefire.SurefirePlugin;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
        name = "test",
        requiresDependencyResolution = ResolutionScope.TEST,
        defaultPhase = LifecyclePhase.TEST,
        threadSafe = true)
public class TestCachedMojo extends SurefirePlugin {

    private final TestTaskCacheHelper testTaskCacheHelper;

    @Parameter(property = "cacheStorage", defaultValue = "${user.home}/.m2/test-cache")
    private String cacheStorage;

    @Parameter(property = "cacheExcludes")
    private String[] cacheExcludes;

    @Inject
    public TestCachedMojo(TestTaskCacheHelper testTaskCacheHelper) {
        this.testTaskCacheHelper = testTaskCacheHelper;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        var cachedTestWrapper = new CachedTestWrapper(this, testTaskCacheHelper,
            cacheStorage, cacheExcludes, PLUGIN_SUREFIRE_CACHED);
        cachedTestWrapper.execute(super::execute);
    }
}
