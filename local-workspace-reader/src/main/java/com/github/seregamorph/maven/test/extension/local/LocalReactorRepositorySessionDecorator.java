package com.github.seregamorph.maven.test.extension.local;

import java.io.File;
import java.util.List;
import java.util.Map;
import javax.inject.Named;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.RepositorySessionDecorator;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sergey Chernov
 */
@Named
public class LocalReactorRepositorySessionDecorator implements RepositorySessionDecorator {

    private static final Logger logger = LoggerFactory.getLogger(LocalReactorRepositorySessionDecorator.class);

    @Override
    public RepositorySystemSession decorate(MavenProject project, RepositorySystemSession session) {
        var originalWorkspaceReader = session.getWorkspaceReader();
        logger.info("Decorating {} with local reactor repository", project);
        var delegate = new DefaultRepositorySystemSession(session);
        var modules = ProjectModuleExtUtils.getProjectModules(project);
        delegate.setWorkspaceReader(new LocalReactorWorkspaceReader(originalWorkspaceReader, modules));
        return delegate;
    }

    static class LocalReactorWorkspaceReader implements WorkspaceReader {

        private final WorkspaceReader delegate;
        private final Map<GroupArtifactId, MavenProject> modules;

        public LocalReactorWorkspaceReader(WorkspaceReader delegate, Map<GroupArtifactId, MavenProject> modules) {
            this.delegate = delegate;
            this.modules = modules;
        }

        @Override
        public WorkspaceRepository getRepository() {
            return delegate.getRepository();
        }

        @Override
        public File findArtifact(Artifact artifact) {
            var groupArtifactId = new GroupArtifactId(artifact.getGroupId(), artifact.getArtifactId());
            var project = modules.get(groupArtifactId);
            if (project == null) {
                return delegate.findArtifact(artifact);
            } else {
                if ("pom".equals(artifact.getExtension())) {
                    return project.getFile();
                } else if ("jar".equals(artifact.getExtension())) {
                    return new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".jar");
                } else {
                    throw new UnsupportedOperationException("Unsupported artifact extension: " + artifact.getExtension());
                }
            }
        }

        @Override
        public List<String> findVersions(Artifact artifact) {
            throw new UnsupportedOperationException("Unsupported findVersions() " + artifact);
        }
    }
}
