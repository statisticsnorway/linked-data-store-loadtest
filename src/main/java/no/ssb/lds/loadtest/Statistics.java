package no.ssb.lds.loadtest;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.LinkedHashMap;
import java.util.Map;

public class Statistics {

    public static final String STAT_N = "N";
    public static final String STAT_SECONDS = "sec";
    public static final String STAT_N_PER_SEC = "N/sec";
    public static final String STAT_MIN = "min";
    public static final String STAT_MEAN = "mean";
    public static final String STAT_MAX = "max";
    public static final String STAT_STDDEV = "stddev";
    public static final String STAT_MEDIAN = "median";
    public static final String STAT_P25 = "25%";
    public static final String STAT_P10 = "10%";
    public static final String STAT_P5 = "5%";
    public static final String STAT_P1 = "1%";
    public static final String STAT_P0_1 = "0.1%";
    public static final String STAT_P75 = "75%";
    public static final String STAT_P90 = "90%";
    public static final String STAT_P95 = "95%";
    public static final String STAT_P99 = "99%";
    public static final String STAT_P99_9 = "99.9%";
    public static final String STAT_KURTOSIS = "kurtosis";
    public static final String STAT_SKEWNESS = "skewness";
    public static final String STAT_GMEAN = "gmean";
    public static final String STAT_SUM = "sum";
    public static final String STAT_SAMPLE_VARIANCE = "sample-variance";

    final Map<String, Map<Number, Number>> stat_datasets = new LinkedHashMap<>();
    private final DescriptiveStatistics readLatencyStatistics = new DescriptiveStatistics();
    private final DescriptiveStatistics writeLatencyStatistics = new DescriptiveStatistics();
    private final DescriptiveStatistics readFailedLatencyStatistics = new DescriptiveStatistics();
    private final DescriptiveStatistics writeFailedLatencyStatistics = new DescriptiveStatistics();
    private final DescriptiveStatistics workerConcurrencyStatistics = new DescriptiveStatistics();
    private final DescriptiveStatistics commandConcurrencyStatistics = new DescriptiveStatistics();

    public Statistics() {
    }

    public void recordReadLatency(double latencyMs) {
        synchronized (readLatencyStatistics) {
            readLatencyStatistics.addValue(latencyMs);
        }
    }

    public void recordWriteLatency(double latencyMs) {
        synchronized (writeLatencyStatistics) {
            writeLatencyStatistics.addValue(latencyMs);
        }
    }

    public void recordReadFailedLatency(double latencyMs) {
        synchronized (readFailedLatencyStatistics) {
            readFailedLatencyStatistics.addValue(latencyMs);
        }
    }

    public void recordWriteFailedLatency(double latencyMs) {
        synchronized (writeFailedLatencyStatistics) {
            writeFailedLatencyStatistics.addValue(latencyMs);
        }
    }

    public void recordWorkerConcurrency(double workerConcurrency) {
        synchronized (workerConcurrencyStatistics) {
            workerConcurrencyStatistics.addValue(workerConcurrency);
        }
    }

    public void recordCommandConcurrency(double commandConcurrency) {
        synchronized (commandConcurrencyStatistics) {
            commandConcurrencyStatistics.addValue(commandConcurrency);
        }
    }

    public void sample(int x, int sampleWindowSeconds) {
        DescriptiveStatistics combinedSuccessStats;
        synchronized (readLatencyStatistics) {
            sample(readLatencyStatistics, "r-", x, sampleWindowSeconds);
            combinedSuccessStats = readLatencyStatistics.copy();
            readLatencyStatistics.clear();
        }
        synchronized (writeLatencyStatistics) {
            sample(writeLatencyStatistics, "w-", x, sampleWindowSeconds);
            for (double value : writeLatencyStatistics.getValues()) {
                combinedSuccessStats.addValue(value);
            }
            writeLatencyStatistics.clear();
        }
        sample(combinedSuccessStats, "", x, sampleWindowSeconds);

        DescriptiveStatistics combinedFailedStats;
        synchronized (readFailedLatencyStatistics) {
            sample(readFailedLatencyStatistics, "rf-", x, sampleWindowSeconds);
            combinedFailedStats = readFailedLatencyStatistics.copy();
            readFailedLatencyStatistics.clear();
        }
        synchronized (writeFailedLatencyStatistics) {
            sample(writeFailedLatencyStatistics, "wf-", x, sampleWindowSeconds);
            for (double value : writeFailedLatencyStatistics.getValues()) {
                combinedFailedStats.addValue(value);
            }
            writeFailedLatencyStatistics.clear();
        }
        sample(combinedFailedStats, "f-", x, sampleWindowSeconds);

        synchronized (workerConcurrencyStatistics) {
            sample(workerConcurrencyStatistics, "wc-", x, sampleWindowSeconds);
            for (double value : workerConcurrencyStatistics.getValues()) {
                combinedSuccessStats.addValue(value);
            }
            workerConcurrencyStatistics.clear();
        }
        synchronized (commandConcurrencyStatistics) {
            sample(commandConcurrencyStatistics, "cc-", x, sampleWindowSeconds);
            for (double value : commandConcurrencyStatistics.getValues()) {
                combinedSuccessStats.addValue(value);
            }
            commandConcurrencyStatistics.clear();
        }
    }

    private void sample(DescriptiveStatistics stats, String prefix, int x, int sampleWindowSeconds) {
        stat_datasets.computeIfAbsent(prefix + STAT_N, k -> new LinkedHashMap<>()).put(x, stats.getN());
        stat_datasets.computeIfAbsent(prefix + STAT_SECONDS, k -> new LinkedHashMap<>()).put(x, sampleWindowSeconds);
        stat_datasets.computeIfAbsent(prefix + STAT_N_PER_SEC, k -> new LinkedHashMap<>()).put(x, (double) stats.getN() / sampleWindowSeconds);
        stat_datasets.computeIfAbsent(prefix + STAT_MIN, k -> new LinkedHashMap<>()).put(x, stats.getMin());
        stat_datasets.computeIfAbsent(prefix + STAT_GMEAN, k -> new LinkedHashMap<>()).put(x, stats.getGeometricMean());
        stat_datasets.computeIfAbsent(prefix + STAT_MEAN, k -> new LinkedHashMap<>()).put(x, stats.getMean());
        stat_datasets.computeIfAbsent(prefix + STAT_P0_1, k -> new LinkedHashMap<>()).put(x, stats.getPercentile(0.1));
        stat_datasets.computeIfAbsent(prefix + STAT_P1, k -> new LinkedHashMap<>()).put(x, stats.getPercentile(1));
        stat_datasets.computeIfAbsent(prefix + STAT_P5, k -> new LinkedHashMap<>()).put(x, stats.getPercentile(5));
        stat_datasets.computeIfAbsent(prefix + STAT_P10, k -> new LinkedHashMap<>()).put(x, stats.getPercentile(10));
        stat_datasets.computeIfAbsent(prefix + STAT_P25, k -> new LinkedHashMap<>()).put(x, stats.getPercentile(25));
        stat_datasets.computeIfAbsent(prefix + STAT_MEDIAN, k -> new LinkedHashMap<>()).put(x, stats.getPercentile(50));
        stat_datasets.computeIfAbsent(prefix + STAT_P75, k -> new LinkedHashMap<>()).put(x, stats.getPercentile(75));
        stat_datasets.computeIfAbsent(prefix + STAT_P90, k -> new LinkedHashMap<>()).put(x, stats.getPercentile(90));
        stat_datasets.computeIfAbsent(prefix + STAT_P95, k -> new LinkedHashMap<>()).put(x, stats.getPercentile(95));
        stat_datasets.computeIfAbsent(prefix + STAT_P99, k -> new LinkedHashMap<>()).put(x, stats.getPercentile(99));
        stat_datasets.computeIfAbsent(prefix + STAT_P99_9, k -> new LinkedHashMap<>()).put(x, stats.getPercentile(99.9));
        stat_datasets.computeIfAbsent(prefix + STAT_MAX, k -> new LinkedHashMap<>()).put(x, stats.getMax());
        stat_datasets.computeIfAbsent(prefix + STAT_STDDEV, k -> new LinkedHashMap<>()).put(x, stats.getStandardDeviation());
        stat_datasets.computeIfAbsent(prefix + STAT_KURTOSIS, k -> new LinkedHashMap<>()).put(x, stats.getKurtosis());
        stat_datasets.computeIfAbsent(prefix + STAT_SKEWNESS, k -> new LinkedHashMap<>()).put(x, stats.getSkewness());
        stat_datasets.computeIfAbsent(prefix + STAT_SUM, k -> new LinkedHashMap<>()).put(x, stats.getSum());
        stat_datasets.computeIfAbsent(prefix + STAT_SAMPLE_VARIANCE, k -> new LinkedHashMap<>()).put(x, stats.getVariance());
    }

}
