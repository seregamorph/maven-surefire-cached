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
