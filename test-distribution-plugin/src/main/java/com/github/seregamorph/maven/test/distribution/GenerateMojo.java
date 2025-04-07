package com.github.seregamorph.maven.test.distribution;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.surefire.util.DirectoryScanner;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.apache.maven.surefire.api.util.DefaultScanResult;

/**
 * This goal generates test class distribution according to chosen Provider.
 *
 * @author Sergey Chernov
 */
@Mojo(
        name = "generate",
        requiresDependencyResolution = ResolutionScope.TEST,
        defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES,
        threadSafe = true)
public class GenerateMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private File classesDirectory;

    @Parameter(defaultValue = "${project.build.testOutputDirectory}")
    private File testClassesDirectory;

    @Parameter(required = true, property = "testdistribution.includes")
    private List<String> includes;

    @Parameter(property = "testdistribution.excludes")
    private List<String> excludes;

    @Parameter(property = "testdistribution.distributionGenerator")
    private String distributionGenerator;

    @Parameter(required = true, property = "testdistribution.numGroups")
    private int numGroups;

    @Override
    public void execute() throws MojoExecutionException {
        if (includes.isEmpty()) {
            throw new MojoExecutionException("Plugin configuration should declare `includes` parameter with class name filtering");
        }

        if (numGroups <= 0) {
            throw new MojoExecutionException("Plugin configuration should declare `numGroups` parameter with value greater than 0");
        }

        getLog().info("Distributing test classes from " + testClassesDirectory + ", includes " + includes);

        var scanResult = scanTestClassesDirectory();

        try {
            var testClassesGroups = generateTestClassDistribution(scanResult.getClasses(), numGroups);
            // todo store
            testClassesGroups.forEach(System.out::println);
        } catch (IOException | ReflectiveOperationException e) {
            throw new MojoExecutionException("Failed to generate test classes", e);
        }
    }

    private List<List<String>> generateTestClassDistribution(List<String> classes, int numGroups) throws IOException, ReflectiveOperationException {
        if (distributionGenerator == null || distributionGenerator.isEmpty()) {
            return new SimpleDistributionGenerator().generate(classes, numGroups);
        }

        Collection<URL> urls = new ArrayList<>();
        for (Artifact artifact : project.getArtifacts()) {
            urls.add(artifact.getFile().toPath().toUri().toURL());
        }
        urls.add(classesDirectory.toURI().toURL());
        urls.add(testClassesDirectory.toURI().toURL());

        var classLoader = new URLClassLoader(urls.toArray(new URL[0]));
        var distributionGeneratorClass = classLoader.loadClass(distributionGenerator);
        var generateMethod = distributionGeneratorClass.getMethod("generate", List.class, int.class);
        Thread.currentThread().setContextClassLoader(classLoader);
        var distributionGenerator = distributionGeneratorClass.getConstructor().newInstance();
        //noinspection unchecked
        return (List<List<String>>) generateMethod.invoke(distributionGenerator, classes, numGroups);
    }

    private DefaultScanResult scanTestClassesDirectory() {
        var scanner = new DirectoryScanner(testClassesDirectory, new TestListResolver(includes, excludes));
        return scanner.scan();
    }
}
