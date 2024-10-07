package com.github.seregamorph.maven.test.builder;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionRunner;
import org.apache.maven.plugin.MojosExecutionStrategy;
import org.eclipse.sisu.Priority;

@Named
@Singleton
@Priority(10)
public class TurboMojosExecutionStrategy implements MojosExecutionStrategy {

/*
    original phases of the default lifecycle [
        "validate",
        "initialize",
        "generate-sources",
        "process-sources",
        "generate-resources",
        "process-resources",
        "compile",
        "process-classes",

        "generate-test-sources",
        "process-test-sources",
        "generate-test-resources",
        "process-test-resources",
        "test-compile",
        "process-test-classes",
        "test",

        // moved before "*test*" phases
        "prepare-package",
        "package",

        "pre-integration-test",
        "integration-test",
        "post-integration-test",
        "verify",
        "install",
        "deploy"
    ]
*/

    private static boolean isPackage(MojoExecution mojo) {
        return List.of("prepare-package", "package")
            .contains(mojo.getLifecyclePhase());
    }

    private static boolean isTest(MojoExecution mojo) {
        // "generate-test-sources", "process-test-sources", "generate-test-resources", "process-test-resources",
        // "test-compile", "process-test-classes", "test", "pre-integration-test", "integration-test",
        // "post-integration-test"
        return mojo.getLifecyclePhase().contains("test");
    }

    @Override
    public void execute(
        List<MojoExecution> mojos,
        MavenSession session,
        MojoExecutionRunner mojoRunner
    ) throws LifecycleExecutionException {
        var packageMojos = mojos.stream()
            .filter(TurboMojosExecutionStrategy::isPackage)
            .toList();
        int firstTestMojoIndex = -1;
        for (int i = 0; i < mojos.size(); i++) {
            if (isTest(mojos.get(i))) {
                firstTestMojoIndex = i;
                break;
            }
        }
        var reorderedMojos = new ArrayList<>(mojos);
        if (!packageMojos.isEmpty() && firstTestMojoIndex != -1) {
            reorderedMojos.removeAll(packageMojos);
            reorderedMojos.addAll(firstTestMojoIndex, packageMojos);
        }

        var executedPackageMojos = new ArrayList<MojoExecution>();
        for (MojoExecution mojoExecution : reorderedMojos) {
            mojoRunner.run(mojoExecution);
            if (packageMojos.contains(mojoExecution)) {
                executedPackageMojos.add(mojoExecution);
                if (packageMojos.equals(executedPackageMojos)) {
                    SignalingExecutorCompletionService.signal(session.getCurrentProject());
                }
            }
        }
    }
}
