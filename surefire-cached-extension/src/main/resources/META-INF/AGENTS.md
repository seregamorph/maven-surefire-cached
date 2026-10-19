# surefire-cached-extension: notes for AI agents

This file is for AI coding agents working in a project that uses `surefire-cached-extension`.
Full documentation: https://github.com/seregamorph/maven-surefire-cached

## What it does
A Maven core extension that wraps `maven-surefire-plugin` and `maven-failsafe-plugin`. Before tests run, it
hashes the task inputs: classes, test classes, dependency jars, plugin artifacts, selected properties, active
profiles, `argLine` and test filters (`test`, `groups`, `excludes`, ...). If a cached result exists for that hash,
it restores the reports and skips the test run. Otherwise it runs the tests and stores the result only if
everything passed.

Both lifecycle runs (`mvn verify`) and direct plugin calls (`mvn surefire:test`, `mvn failsafe:integration-test`)
are cached.

## Adoption
Add it to `.mvn/extensions.xml`. Use `surefire-cached-extension-s3` instead of `surefire-cached-extension` to
cache directly in Amazon S3. Use only one of the two artifacts.
```xml
<extensions>
    <extension>
        <groupId>com.github.seregamorph</groupId>
        <artifactId>surefire-cached-extension</artifactId>
        <version><!-- same version as this jar --></version>
    </extension>
</extensions>
```

## Global configuration parameters
Pass these as `-Dname=value` on the command line, or put them in `.mvn/maven.config`. They are read from Maven
system/user properties, not from `pom.xml` `<properties>` (`skipCache` is the only exception). All of them are
optional.

| Parameter                    | Default                                    | Description                                                                                                                                                                                                                  |
|------------------------------|--------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `cacheStorageUrl`            | `$HOME/.m2/test-cache`                     | Where the cache lives. Can be a local directory (file storage), an `http://` or `https://` URL (remote cache server), or `s3://bucketname` (requires `surefire-cached-extension-s3`, and the bucket name must not end with `/`). |
| `skipCache`                  | `false`                                    | Turns caching off: tests always run and nothing is read from or written to the cache. Set it globally with `-DskipCache`, or for one module with `<skipCache>true</skipCache>` in that module's `pom.xml` `<properties>`.        |
| `maxCacheEntries`            | `16` if env `CI=true`, otherwise `4`       | File storage only. Maximum number of cache entries kept per module (`groupId:artifactId`). Older entries are removed.                                                                                                       |
| `cacheExpirationHours`       | `6`                                        | S3 storage only. How long a cache entry stays valid, in hours.                                                                                                                                                              |
| `s3EndpointOverride`         | (none)                                     | S3 storage only. A custom S3 endpoint URL, for example LocalStack. Setting it also turns on path-style access.                                                                                                             |
| `cacheConnectTimeoutSec`     | `10`                                       | HTTP storage only. Connect timeout in seconds.                                                                                                                                                                              |
| `cacheReadTimeoutSec`        | `10`                                       | HTTP storage only. Read timeout in seconds.                                                                                                                                                                                 |
| `cacheWriteTimeoutSec`       | `10`                                       | HTTP storage only. Write timeout in seconds.                                                                                                                                                                                |
| `cacheCheckServerVersion`    | `false`                                    | HTTP storage only. If `true`, checks that the cache server's protocol version is supported.                                                                                                                                  |
| `cacheHashPrefix`            | (none)                                     | HTTP storage only. A prefix added to the cache entry hash in URLs, used to keep different caches apart on a shared server.                                                                                                  |
| `cacheFailureThreshold`      | `4`                                        | After this many failed cache reads (or writes), the remaining reads (or writes) in the build are skipped. Storage outages then slow the build down only a little and do not fail it.                                       |
| `cacheIfTestcaseFlakyErrors` | `false`                                    | If `true`, results are cached even when some tests failed and then passed on rerun (flaky tests, e.g. with `rerunFailingTestsCount`).                                                                                       |

S3 credentials and region come from the standard AWS SDK provider chain (`AWS_REGION`, `AWS_ACCESS_KEY_ID`,
`AWS_SECRET_ACCESS_KEY`, `~/.aws/credentials`, and so on). Never put credentials in `.mvn/maven.config` or `pom.xml`.

## Module configuration: `surefire-cached.json`
You can place a `surefire-cached.json` (or `.mvn/surefire-cached.json`) in a module directory or in any parent
module directory. The extension collects these files from the root module down to the current module and merges
them: a value set closer to the module wins. Within a file, the `surefire` and `failsafe` sections take priority
over `common`.

How merging works: each list or map REPLACES the default value or the parent's value. Nothing is appended. If you
override a list, copy in the default entries you want to keep.

Default configuration:
```json
{
  "common": {
    "inputIgnoredProperties": [
      "java.version", "os.arch", "os.name", "env.CI", "env.GITHUB_BASE_REF", "env.GITHUB_REF",
      "env.GITHUB_RUN_ID", "env.GITHUB_JOB", "env.GITHUB_SHA", "project.version"
    ],
    "inputProperties": ["java.specification.version"],
    "excludeModules": [],
    "excludeClasspathResources": [
      "META-INF/MANIFEST.MF",
      "META-INF/maven/**/pom.properties",
      "META-INF/maven/**/pom.xml"
    ]
  },
  "surefire": {
    "artifacts": {
      "surefire-reports": {"includes": ["surefire-reports/TEST-*.xml"]}
    }
  },
  "failsafe": {
    "artifacts": {
      "failsafe-reports": {
        "includes": ["failsafe-reports/TEST-*.xml", "failsafe-reports/failsafe-summary.xml"]
      }
    }
  }
}
```

Fields (allowed in `common`, `surefire` and `failsafe`):
* `inputProperties`: properties included in the cache key hash. Use the `env.` prefix for environment variables.
  The value is looked up in Maven system properties, then user properties, then the module's `pom.xml` properties.
* `inputIgnoredProperties`: properties recorded in `target/<plugin>-cached-input.json` as metadata, but left out
  of the hash. Useful for finding out which build produced a cache entry.
* `excludeModules`: `groupId:artifactId` patterns (wildcards such as `groupId:*` work) for classpath modules that
  are left out of the hash. Use it for modules whose contents change on every build, such as build timestamps or
  `git.properties`.
* `excludeClasspathResources`: Ant-style patterns for classpath resources left out of the hash.
* `artifacts`: named groups of `includes` patterns, relative to `target/`, for files that are cached and restored
  in addition to the result (for example `jacoco-surefire.exec`).

Use the `"//"` key for comments.

## Build outputs
* `target/surefire-cached-input.json` / `target/failsafe-cached-input.json`: the inputs used to compute the hash.
  Diff these between two builds to find out why a cache hit was expected but did not happen.
* `target/surefire-cached-output.json` / `target/failsafe-cached-output.json`: the test results summary.
* `target/surefire-cached-report.json` (in the root module): the cache statistics for the build. A text summary
  is also printed at the end of the build.

## Tips for agents
* To force tests to run (for example while debugging a test), add `-DskipCache`. Cache hits are reported as
  `Cache hit ...` in the log, and the test JVM is not started.
* Failed or flaky runs are never cached, so a failing test will always run again.
* A cache miss can come from any change to the test classpath (including dependency jars), to `inputProperties`,
  or to `argLine`, active profiles or test filters. Compare the `*-cached-input.json` files to find the cause.
