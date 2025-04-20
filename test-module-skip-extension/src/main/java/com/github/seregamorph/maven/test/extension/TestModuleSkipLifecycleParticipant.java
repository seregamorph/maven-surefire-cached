package com.github.seregamorph.maven.test.extension;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Named;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Listener that cleans up test modules from the session to speed up the build.
 *
 * @author Sergey Chernov
 */
@SessionScoped
@Named
public class TestModuleSkipLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static final Logger logger = LoggerFactory.getLogger(TestModuleSkipLifecycleParticipant.class);

    @Override
    public void afterProjectsRead(MavenSession session) {
        String testModuleSkip = session.getUserProperties().getProperty("testModuleSkip");
        if ("true".equals(testModuleSkip)) {
            logger.info("test-module-skip-extension: cleaning up projects (total projects {})", session.getProjects().size());
            var removedModules = new TreeSet<String>();
            session.getProjects().removeIf(mavenProject -> {
                if (isTestModule(mavenProject)) {
                    removedModules.add(mavenProject.getGroupId() + ':' + mavenProject.getArtifactId());
                    return true;
                } else {
                    return false;
                }
            });
            AtomicInteger removedTestDependenciesCount = new AtomicInteger();
            AtomicInteger removedTestDependenciesAffectedProjectsCount = new AtomicInteger();
            session.getProjects().forEach(project -> {
                if (project.getDependencies().removeIf(dependency -> {
                    if ("test".equals(dependency.getScope())) {
                        removedTestDependenciesCount.incrementAndGet();
                        return true;
                    } else {
                        return false;
                    }
                })) {
                    removedTestDependenciesAffectedProjectsCount.incrementAndGet();
                };
            });
            if (removedModules.isEmpty()) {
                logger.info("No test modules found");
            } else {
                logger.info("Removed {} test modules: {}", removedModules.size(), removedModules);
            }
            if (removedTestDependenciesCount.get() == 0) {
                logger.info("No test dependencies found");
            } else {
                logger.info("Removed {} test scope dependencies from {} projects",
                    removedTestDependenciesCount.get(), removedTestDependenciesAffectedProjectsCount.get());
            }
        }
    }

    private static boolean isTestModule(MavenProject project) {
        return "jar".equals(project.getPackaging())
            && ("testing".equals(project.getArtifactId())
            || project.getArtifactId().endsWith("-testing")
            || project.getArtifactId().endsWith("-test")
            || project.getArtifactId().contains("-test-")
            || project.getArtifactId().endsWith("-tests")
        );
    }
}
