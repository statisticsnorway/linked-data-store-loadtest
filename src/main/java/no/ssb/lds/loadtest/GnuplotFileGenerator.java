package no.ssb.lds.loadtest;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class GnuplotFileGenerator {

    public static void writeStatisticsGnuplotAndDatafile(Statistics statistics, String xlabel, Path plotfilesFolder, String plotfilesBasename) {
        Map<String, Map<Number, Number>> stat_datasets = statistics.stat_datasets;
        Map<String, Integer> indexByStatistic = new LinkedHashMap<>();
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(plotfilesFolder.resolve(plotfilesBasename + ".dat").toFile()), StandardCharsets.ISO_8859_1))) {
            int i = 0;
            for (Map.Entry<String, Map<Number, Number>> e : stat_datasets.entrySet()) {
                String stat = e.getKey();
                indexByStatistic.put(stat, i);
                bw.write("#");
                bw.write(String.valueOf(i));
                bw.write(" \"");
                bw.write(stat);
                bw.write("\"\n");
                Map<Number, Number> statYbyX = e.getValue();
                TreeMap<Number, Number> sortedYbyX = new TreeMap<>((n1, n2) -> (int) (10000 * (n1.doubleValue() - n2.doubleValue())));
                sortedYbyX.putAll(statYbyX);
                for (Map.Entry<Number, Number> xy : sortedYbyX.entrySet()) {
                    bw.write(String.valueOf(xy.getKey()));
                    bw.write(" ");
                    bw.write(String.valueOf(xy.getValue()));
                    bw.write("\n");
                }
                bw.write("\n");
                bw.write("\n");
                i++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(plotfilesFolder.resolve(plotfilesBasename + ".gnu").toFile()), StandardCharsets.ISO_8859_1))) {
            bw.write("set terminal svg size 1920,1080 font 'Verdana,20'\n");
            bw.write("set output '" + plotfilesBasename + ".svg'\n");
            bw.write("set xrange [0:*]\n");
            bw.write("set yrange [0:*]\n");
            bw.write("set xtics rotate\n");
            bw.write("set ytics nomirror\n");
            bw.write("set tics out scale 1.5,0.5 font \",16\" nomirror\n");
            bw.write("set mxtics\n");
            bw.write("set mytics\n");
            bw.write("set grid ytics\n");
            bw.write("set style line 1 lc rgb '#0060ad' lt 1 lw 1 pt 7 pi 0 ps 0.5\n");
            bw.write("set style line 2 lc rgb '#dd181f' lt 1 lw 1 pt 7 pi 0 ps 0.5\n");
            bw.write("set lmargin 12 # align x-axes at the left side across plots\n");
            bw.write("set bmargin 5 # ensure plots have same height\n");
            bw.write("set multiplot layout 2,1\n");
            bw.write("set format x \"\"\n");
            bw.write("set style data boxes\n");
            bw.write("set key box opaque inside bottom right\n");
            bw.write("set ylabel \"Throughput (requests/second)\"\n");
            bw.write("plot '" + plotfilesBasename + ".dat' \\\n");
            writePlotLine(bw, 0, "success", String.valueOf(indexByStatistic.get(Statistics.STAT_N_PER_SEC)), 2, "linespoints ls 1");
            writePlotLine(bw, 1, "failed", String.valueOf(indexByStatistic.get("f-" + Statistics.STAT_N_PER_SEC)), 2, "linespoints ls 2");
            bw.write("set ylabel \"Latency (ms)\"\n");
            bw.write("set xlabel \"" + xlabel + "\"\n");
            bw.write("unset format x\n");
            bw.write("plot '" + plotfilesBasename + ".dat' \\\n");
            writePlotLine(bw, 0, "r-" + Statistics.STAT_MEAN, String.valueOf(indexByStatistic.get("r-" + Statistics.STAT_MEAN)), 2, "linespoints ls 1");
            writePlotLine(bw, 1, "w-" + Statistics.STAT_MEAN, String.valueOf(indexByStatistic.get("w-" + Statistics.STAT_MEAN)), 2, "linespoints ls 2");
            bw.write("unset multiplot\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(plotfilesFolder.resolve(plotfilesBasename + "_total_throughput.gnu").toFile()), StandardCharsets.ISO_8859_1))) {
            bw.write("set terminal svg size 1920,1080 font 'Verdana,20'\n");
            bw.write("set output '" + plotfilesBasename + "_total_throughput.svg'\n");
            bw.write("set xrange [0:*]\n");
            bw.write("set yrange [0:*]\n");
            bw.write("set xtics rotate\n");
            bw.write("set ytics nomirror\n");
            bw.write("set tics out scale 1.5,0.5 font \",16\" nomirror\n");
            bw.write("set mxtics\n");
            bw.write("set mytics\n");
            bw.write("set grid ytics\n");
            bw.write("set style line 1 lc rgb '#0060ad' lt 1 lw 1 pt 7 pi 0 ps 0.5\n");
            bw.write("set style line 2 lc rgb '#dd181f' lt 1 lw 1 pt 13 pi 0 ps 0.5\n");
            bw.write("set format x \"\"\n");
            bw.write("set style data boxes\n");
            bw.write("set key box opaque inside bottom right\n");
            bw.write("set ylabel \"Throughput (requests/second)\"\n");
            bw.write("set xlabel \"" + xlabel + "\"\n");
            bw.write("unset format x\n");
            bw.write("plot '" + plotfilesBasename + ".dat' \\\n");
            writePlotLine(bw, 0, "success", String.valueOf(indexByStatistic.get(Statistics.STAT_N_PER_SEC)), 2, "linespoints ls 1");
            writePlotLine(bw, 1, "failed", String.valueOf(indexByStatistic.get("f-" + Statistics.STAT_N_PER_SEC)), 2, "linespoints ls 2");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(plotfilesFolder.resolve(plotfilesBasename + "_read_latency.gnu").toFile()), StandardCharsets.ISO_8859_1))) {
            bw.write("set terminal svg size 1920,1080 font 'Verdana,20'\n");
            bw.write("set output '" + plotfilesBasename + "_read_latency.svg'\n");
            bw.write("set xrange [0:*]\n");
            bw.write("set yrange [0:*]\n");
            bw.write("set xtics rotate\n");
            bw.write("set ytics nomirror\n");
            bw.write("set tics out scale 1.5,0.5 font \",16\" nomirror\n");
            bw.write("set mxtics\n");
            bw.write("set mytics\n");
            bw.write("set grid ytics mytics\n");
            bw.write("set style line 1 lc rgb '#0060ad' lt 1 lw 1 pt 7 pi 0 ps 0.5\n");
            bw.write("set style line 2 lc rgb '#ffc400' lt 1 lw 1 pt 9 pi 0 ps 0.5\n");
            bw.write("set style line 3 lc rgb '#00ff92' lt 1 lw 1 pt 11 pi 0 ps 0.5\n");
            bw.write("set style line 4 lc rgb '#00caff' lt 1 lw 1 pt 13 pi 0 ps 0.5\n");
            bw.write("set style line 5 lc rgb '#dd181f' lt 1 lw 1 pt 15 pi 0 ps 0.5\n");
            bw.write("set lmargin 12 # align x-axes at the left side across plots\n");
            bw.write("set bmargin 5 # ensure plots have same height\n");
            bw.write("set format x \"\"\n");
            bw.write("set style data boxes\n");
            bw.write("set key box opaque inside bottom right\n");
            bw.write("set logscale y\n");
            bw.write("set ylabel \"Latency (ms)\"\n");
            bw.write("set xlabel \"" + xlabel + "\"\n");
            bw.write("unset format x\n");
            bw.write("plot '" + plotfilesBasename + ".dat' \\\n");
            writePlotLine(bw, 0, "r-" + Statistics.STAT_MEAN, String.valueOf(indexByStatistic.get("r-" + Statistics.STAT_MEAN)), 5, "linespoints ls 1");
            writePlotLine(bw, 1, "r-" + Statistics.STAT_MEDIAN, String.valueOf(indexByStatistic.get("r-" + Statistics.STAT_MEDIAN)), 5, "linespoints ls 2");
            writePlotLine(bw, 2, "r-" + Statistics.STAT_P90, String.valueOf(indexByStatistic.get("r-" + Statistics.STAT_P90)), 5, "linespoints ls 3");
            writePlotLine(bw, 3, "r-" + Statistics.STAT_P95, String.valueOf(indexByStatistic.get("r-" + Statistics.STAT_P95)), 5, "linespoints ls 4");
            writePlotLine(bw, 4, "r-" + Statistics.STAT_P99, String.valueOf(indexByStatistic.get("r-" + Statistics.STAT_P99)), 5, "linespoints ls 5");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(plotfilesFolder.resolve(plotfilesBasename + "_write_latency.gnu").toFile()), StandardCharsets.ISO_8859_1))) {
            bw.write("set terminal svg size 1920,1080 font 'Verdana,20'\n");
            bw.write("set output '" + plotfilesBasename + "_write_latency.svg'\n");
            bw.write("set xrange [0:*]\n");
            bw.write("set yrange [0:*]\n");
            bw.write("set xtics rotate\n");
            bw.write("set ytics nomirror\n");
            bw.write("set tics out scale 1.5,0.5 font \",16\" nomirror\n");
            bw.write("set mxtics\n");
            bw.write("set mytics\n");
            bw.write("set grid ytics mytics\n");
            bw.write("set style line 1 lc rgb '#0060ad' lt 1 lw 1 pt 7 pi 0 ps 0.5\n");
            bw.write("set style line 2 lc rgb '#ffc400' lt 1 lw 1 pt 9 pi 0 ps 0.5\n");
            bw.write("set style line 3 lc rgb '#00ff92' lt 1 lw 1 pt 11 pi 0 ps 0.5\n");
            bw.write("set style line 4 lc rgb '#00caff' lt 1 lw 1 pt 13 pi 0 ps 0.5\n");
            bw.write("set style line 5 lc rgb '#dd181f' lt 1 lw 1 pt 15 pi 0 ps 0.5\n");
            bw.write("set lmargin 12 # align x-axes at the left side across plots\n");
            bw.write("set bmargin 5 # ensure plots have same height\n");
            bw.write("set format x \"\"\n");
            bw.write("set style data boxes\n");
            bw.write("set key box opaque inside bottom right\n");
            bw.write("set logscale y\n");
            bw.write("set ylabel \"Latency (ms)\"\n");
            bw.write("set xlabel \"" + xlabel + "\"\n");
            bw.write("unset format x\n");
            bw.write("plot '" + plotfilesBasename + ".dat' \\\n");
            writePlotLine(bw, 0, "w-" + Statistics.STAT_MEAN, String.valueOf(indexByStatistic.get("w-" + Statistics.STAT_MEAN)), 5, "linespoints ls 1");
            writePlotLine(bw, 1, "w-" + Statistics.STAT_MEDIAN, String.valueOf(indexByStatistic.get("w-" + Statistics.STAT_MEDIAN)), 5, "linespoints ls 2");
            writePlotLine(bw, 2, "w-" + Statistics.STAT_P90, String.valueOf(indexByStatistic.get("w-" + Statistics.STAT_P90)), 5, "linespoints ls 3");
            writePlotLine(bw, 3, "w-" + Statistics.STAT_P95, String.valueOf(indexByStatistic.get("w-" + Statistics.STAT_P95)), 5, "linespoints ls 4");
            writePlotLine(bw, 4, "w-" + Statistics.STAT_P99, String.valueOf(indexByStatistic.get("w-" + Statistics.STAT_P99)), 5, "linespoints ls 5");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(plotfilesFolder.resolve(plotfilesBasename + "_load.gnu").toFile()), StandardCharsets.ISO_8859_1))) {
            bw.write("set terminal svg size 720,720 font 'Verdana,20'\n");
            bw.write("set output '" + plotfilesBasename + "_load.svg'\n");
            bw.write("set xrange [0:*]\n");
            bw.write("set yrange [0:*]\n");
            bw.write("set xtics rotate\n");
            bw.write("set ytics nomirror\n");
            bw.write("set tics out scale 1.5,0.5 font \",16\" nomirror\n");
            bw.write("set mxtics\n");
            bw.write("set mytics\n");
            bw.write("set grid ytics mytics\n");
            bw.write("set style line 1 lc rgb '#3f704d' lt 1 lw 1\n");
            bw.write("set style line 2 lc rgb '#0060ad' lt 1 lw 1\n");
            bw.write("set style line 3 lc rgb '#dd181f' lt 1 lw 1\n");
            bw.write("set lmargin 12 # align x-axes at the left side across plots\n");
            bw.write("set bmargin 5 # ensure plots have same height\n");
            bw.write("set format x \"\"\n");
            bw.write("set style data boxes\n");
            bw.write("set key box opaque inside bottom right\n");
            bw.write("set ylabel \"concurrency-degree\"\n");
            bw.write("set xlabel \"" + xlabel + "\"\n");
            bw.write("set size square\n");
            bw.write("unset format x\n");
            bw.write("plot '" + plotfilesBasename + ".dat' \\\n");
            writePlotLine(bw, 0, "u 1:($1)", "ideal", String.valueOf(indexByStatistic.get("wc-" + Statistics.STAT_MEAN)), 3, "lines ls 1");
            writePlotLine(bw, 1, "worker", String.valueOf(indexByStatistic.get("wc-" + Statistics.STAT_MEAN)), 3, "lines ls 2");
            writePlotLine(bw, 2, "command", String.valueOf(indexByStatistic.get("cc-" + Statistics.STAT_MEAN)), 3, "lines ls 3");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void writePlotLine(BufferedWriter bw, int i, String stat, String datasetIndex, int size, String w) throws IOException {
        writePlotLine(bw, i, "", stat, datasetIndex, size, w);
    }

    static void writePlotLine(BufferedWriter bw, int i, String u, String stat, String datasetIndex, int size, String w) throws IOException {
        bw.write("  ");
        if (i > 0) {
            bw.write("'' ");
        }
        bw.write("i ");
        bw.write(datasetIndex);
        bw.write(" ");
        bw.write(u);
        bw.write(" t \"");
        bw.write(stat);
        bw.write("\" w ");
        bw.write(w);
        if (i < size - 1) {
            bw.write(", \\");

        }
        bw.write("\n");
    }
}
