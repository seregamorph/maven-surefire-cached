package com.github.seregamorph.maven.test.extension;

import com.github.seregamorph.maven.test.core.CachedTestWrapper;
import com.github.seregamorph.maven.test.core.TestTaskCacheHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

public class CachedDelegatingMojo extends AbstractMojo {

    private final MavenSession session;
    private final MavenProject project;
    private final Mojo delegate;
    private final TestTaskCacheHelper testTaskCacheHelper;
    private final String pluginName;

    public CachedDelegatingMojo(MavenSession session, MavenProject project, Mojo delegate,
                                TestTaskCacheHelper testTaskCacheHelper, String pluginName) {
        this.session = session;
        this.project = project;
        this.delegate = delegate;
        this.testTaskCacheHelper = testTaskCacheHelper;
        this.pluginName = pluginName;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String cacheStorage = System.getProperty("user.home") + "/.m2/test-cache";
        String[] cacheExcludes = {};

        var cachedTestWrapper = new CachedTestWrapper(session, project, delegate, testTaskCacheHelper,
            cacheStorage, cacheExcludes, pluginName);
        cachedTestWrapper.execute(delegate::execute);
    }
}
