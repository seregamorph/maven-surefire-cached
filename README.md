# maven-surefire-cached
Maven surefire/failsafe plugins supporting local and remote build caching.

## Comparison with Maven Build Cache Extension
The [Maven Build Cache Extension](https://maven.apache.org/extensions/maven-build-cache-extension/) is an open-source
project adding support of artifact caching to maven, also allowing to skip goal executions via cache.
It can cover a wide range of typical scenarios, however it's not a good choice for pipelines separating build and 
test phases. It does not properly handle test reports, does not support flexible test filtering (caching them 
separately depending on filtered test subset) for parallel execution.
Also it does not cache so called CLI executions like `mvn surefire:test`, only lifecycle executions
like `mvn clean test`, which is also not always convenient.

## Adoption
To adopt the plugin, the standard maven-surefire-plugin and maven-failsafe-plugin should be replaced accordingly.

For `maven-surefire-plugin`:
```xml
<plugin>
    <!-- Replaced with surefire-cached-maven-plugin -->
    <artifactId>maven-surefire-plugin</artifactId>
    <executions>
        <execution>
            <id>default-test</id>
            <phase>none</phase>
        </execution>
    </executions>
</plugin>
<plugin>
    <groupId>com.github.seregamorph</groupId>
    <artifactId>surefire-cached-maven-plugin</artifactId>
    <version>${surefire-cached.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>test</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <!-- Take configuration from regular maven-surefire-plugin -->
    </configuration>
</plugin>
```

For `maven-failsafe-plugin`:
```xml
<plugin>
    <groupId>com.github.seregamorph</groupId>
    <artifactId>failsafe-cached-maven-plugin</artifactId>
    <version>${surefire-cached.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <!-- Take configuration from regular maven-failsafe-plugin -->
    </configuration>
</plugin>
<plugin>
    <artifactId>maven-failsafe-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

And also add to the root `pom.xml` `<build>` or `.mvn/extensions.xml`:
```xml
<extensions>
    <extension>
        <groupId>com.github.seregamorph</groupId>
        <artifactId>surefire-cached-extension</artifactId>
        <version>${surefire-cached.version}</version>
    </extension>
</extensions>
```
This extension will print the cache statistics after the build.

See sample migration to cached plugins:
* [rest-api-framework](https://github.com/seregamorph/rest-api-framework/pull/2/files)
* [spring-test-smart-context](https://github.com/seregamorph/spring-test-smart-context/pull/6/files)

First build without tests
```shell
mvn clean install -DskipTests=true
```

Then run unit tests
```shell
mvn surefire-cached:test
```

Or via phase
```shell
mvn test
```

Then run integration tests
```shell
mvn failsafe-cached:integration-test -Dit.test=SampleIT
```
or via phase
```shell
mvn verify
```

Using remote cache
```shell
mvn clean install -DcacheStorageUrl=http://localhost:8080/cache
```
