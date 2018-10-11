package no.ssb.lds.loadtest;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.LinkedHashMap;
import java.util.Map;

public class Statistics {

    public static final String STAT_N = "N";
    public static final String STAT_FAILED = "N-Failed";
    public static final String STAT_MSG_SEC = "msg/sec";
    public static final String STAT_MIN = "min";
    public static final String STAT_MEAN = "mean";
    public static final String STAT_MAX = "max";
    public static final String STAT_STDDEV = "stddev";
    public static final String STAT_MEDIAN = "median";
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
    private final DescriptiveStatistics r_stat = new DescriptiveStatistics();
    private final DescriptiveStatistics w_stat = new DescriptiveStatistics();

    public Statistics() {
    }

    public void recordReadLatency(double latencyMs) {
        synchronized (r_stat) {
            r_stat.addValue(latencyMs);
        }
    }

    public void recordWriteLatency(double latencyMs) {
        synchronized (w_stat) {
            w_stat.addValue(latencyMs);
        }
    }

    public void sample(int x, int sampleWindowSeconds) {
        DescriptiveStatistics combinedStats;
        synchronized (r_stat) {
            sample(r_stat, "r-", x, sampleWindowSeconds);
            combinedStats = r_stat.copy();
            r_stat.clear();
        }
        synchronized (w_stat) {
            sample(w_stat, "w-", x, sampleWindowSeconds);
            for (double value : w_stat.getValues()) {
                combinedStats.addValue(value);
            }
            w_stat.clear();
        }
        sample(combinedStats, "", x, sampleWindowSeconds);
    }

    private void sample(DescriptiveStatistics stats, String prefix, int x, int sampleWindowSeconds) {
        stat_datasets.computeIfAbsent(prefix + STAT_N, k -> new LinkedHashMap<>()).put(x, stats.getN());
        stat_datasets.computeIfAbsent(prefix + STAT_MSG_SEC, k -> new LinkedHashMap<>()).put(x, (double) stats.getN() / sampleWindowSeconds);
        stat_datasets.computeIfAbsent(prefix + STAT_MIN, k -> new LinkedHashMap<>()).put(x, stats.getMin());
        stat_datasets.computeIfAbsent(prefix + STAT_GMEAN, k -> new LinkedHashMap<>()).put(x, stats.getGeometricMean());
        stat_datasets.computeIfAbsent(prefix + STAT_MEAN, k -> new LinkedHashMap<>()).put(x, stats.getMean());
        stat_datasets.computeIfAbsent(prefix + STAT_MEDIAN, k -> new LinkedHashMap<>()).put(x, stats.getPercentile(50));
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
