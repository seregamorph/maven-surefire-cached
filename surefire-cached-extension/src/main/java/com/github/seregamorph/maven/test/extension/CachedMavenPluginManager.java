package com.github.seregamorph.maven.test.extension;

import static com.github.seregamorph.maven.test.common.TestTaskOutput.PLUGIN_FAILSAFE_CACHED;
import static com.github.seregamorph.maven.test.common.TestTaskOutput.PLUGIN_SUREFIRE_CACHED;

import com.github.seregamorph.maven.test.core.TestTaskCacheHelper;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.ExtensionRealmCache;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginContainerException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginIncompatibleException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.internal.DefaultMavenPluginManager;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.sisu.Priority;

@Named
@Singleton
@Priority(10)
public class CachedMavenPluginManager implements MavenPluginManager {

    private final Provider<DefaultMavenPluginManager> defaultMavenPluginManagerProvider;
    private final TestTaskCacheHelper testTaskCacheHelper;

    @Inject
    public CachedMavenPluginManager(
        Provider<DefaultMavenPluginManager> defaultMavenPluginManagerProvider,
        TestTaskCacheHelper testTaskCacheHelper
    ) {
        this.defaultMavenPluginManagerProvider = defaultMavenPluginManagerProvider;
        this.testTaskCacheHelper = testTaskCacheHelper;
    }

    @Override
    public PluginDescriptor getPluginDescriptor(Plugin plugin, List<RemoteRepository> repositories, RepositorySystemSession session) throws PluginResolutionException, PluginDescriptorParsingException, InvalidPluginDescriptorException {
        return delegate().getPluginDescriptor(plugin, repositories, session);
    }

    @Override
    public MojoDescriptor getMojoDescriptor(Plugin plugin, String goal, List<RemoteRepository> repositories, RepositorySystemSession session) throws MojoNotFoundException, PluginResolutionException, PluginDescriptorParsingException, InvalidPluginDescriptorException {
        return delegate().getMojoDescriptor(plugin, goal, repositories, session);
    }

    @Override
    public void checkRequiredMavenVersion(PluginDescriptor pluginDescriptor) throws PluginIncompatibleException {
        delegate().checkRequiredMavenVersion(pluginDescriptor);
    }

    @Override
    public void setupPluginRealm(PluginDescriptor pluginDescriptor, MavenSession session, ClassLoader parent, List<String> imports, DependencyFilter filter) throws PluginResolutionException, PluginContainerException {
        delegate().setupPluginRealm(pluginDescriptor, session, parent, imports, filter);
    }

    @Override
    public ExtensionRealmCache.CacheRecord setupExtensionsRealm(MavenProject project, Plugin plugin, RepositorySystemSession session) throws PluginManagerException {
        return delegate().setupExtensionsRealm(project, plugin, session);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getConfiguredMojo(Class<T> mojoInterface, MavenSession session, MojoExecution mojoExecution) throws PluginConfigurationException, PluginContainerException {
        T mojo = delegate().getConfiguredMojo(mojoInterface, session, mojoExecution);
        if ("org.apache.maven.plugin.surefire.SurefireMojo".equals(mojo.getClass().getName())
            || "org.apache.maven.plugin.surefire.SurefirePlugin".equals(mojo.getClass().getName())) {
            // we should normally generate a proxy here, but in the maven-core there is only one known call of
            // getConfiguredMojo method with mojoInterface=Mojo.class
            return (T) new CachedDelegatingMojo(session, session.getCurrentProject(), (Mojo) mojo, testTaskCacheHelper, PLUGIN_SUREFIRE_CACHED);
        } else if ("org.apache.maven.plugin.failsafe.IntegrationTestMojo".equals(mojo.getClass().getName())) {
            return (T) new CachedDelegatingMojo(session, session.getCurrentProject(), (Mojo) mojo, testTaskCacheHelper, PLUGIN_FAILSAFE_CACHED);
        }
        return mojo;
    }

    @Override
    public void releaseMojo(Object mojo, MojoExecution mojoExecution) {
        delegate().releaseMojo(mojo, mojoExecution);
    }

    private MavenPluginManager delegate() {
        return defaultMavenPluginManagerProvider.get();
    }
}
