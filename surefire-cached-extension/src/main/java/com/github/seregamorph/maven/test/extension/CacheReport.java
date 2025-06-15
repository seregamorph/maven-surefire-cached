package com.github.seregamorph.maven.test.extension;

import static java.util.Collections.emptyList;

import com.github.seregamorph.maven.test.common.GroupArtifactId;
import com.github.seregamorph.maven.test.common.PluginName;
import com.github.seregamorph.maven.test.core.TaskOutcome;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author Sergey Chernov
 */
public class CacheReport {

    /**
     * There can be more than one execution per module and plugin.
     */
    private final SortedMap<GroupArtifactId, SortedMap<PluginName, List<ModuleTestResult>>> executionResults;

    public CacheReport() {
        executionResults = new TreeMap<>();
    }

    public void addExecutionResult(
        GroupArtifactId groupArtifactId,
        PluginName pluginName,
        TaskOutcome result,
        BigDecimal totalTimeSeconds,
        int deletedCacheEntries
    ) {
        synchronized (executionResults) {
            executionResults.computeIfAbsent(groupArtifactId, k -> new TreeMap<>())
                .computeIfAbsent(pluginName, k -> new ArrayList<>())
                .add(new ModuleTestResult(result, totalTimeSeconds, deletedCacheEntries));
        }
    }

    public List<ModuleTestResult> getExecutionResults(PluginName pluginName) {
        synchronized (executionResults) {
            return executionResults.values().stream()
                .flatMap(m -> m.getOrDefault(pluginName, emptyList()).stream())
                .collect(Collectors.toList());
        }
    }

    public static final class ModuleTestResult {

        private final TaskOutcome result;
        private final BigDecimal totalTimeSeconds;
        private final int deletedCacheEntries;

        public ModuleTestResult(TaskOutcome result, BigDecimal totalTimeSeconds, int deletedCacheEntries) {
            this.result = result;
            this.totalTimeSeconds = totalTimeSeconds;
            this.deletedCacheEntries = deletedCacheEntries;
        }

        public TaskOutcome getResult() {
            return result;
        }

        public BigDecimal getTotalTimeSeconds() {
            return totalTimeSeconds;
        }

        public int getDeletedCacheEntries() {
            return deletedCacheEntries;
        }

        @Override
        public String toString() {
            return "ModuleTestResult{" +
                "result=" + result +
                ", totalTimeSeconds=" + totalTimeSeconds +
                '}';
        }
    }
}
