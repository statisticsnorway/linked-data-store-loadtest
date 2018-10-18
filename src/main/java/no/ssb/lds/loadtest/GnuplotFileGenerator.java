package no.ssb.lds.loadtest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class GnuplotFileGenerator {

    public static void writeStatisticsGnuplotAndDatafile(Statistics statistics, String xlabel, File gnuplotScriptFile, File dataFile, String imageFileBasename) {
        Map<String, Map<Number, Number>> stat_datasets = statistics.stat_datasets;
        Map<String, Integer> indexByStatistic = new LinkedHashMap<>();
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dataFile), StandardCharsets.ISO_8859_1))) {
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

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(gnuplotScriptFile), StandardCharsets.ISO_8859_1))) {
            bw.write("set terminal svg size 1920,1080 font 'Verdana,20'\n");
            bw.write("set output '" + imageFileBasename + ".svg'\n");
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
            bw.write("set ylabel \"messages / second\"\n");
            bw.write("plot '" + dataFile.getName() + "' \\\n");
            writePlotLine(bw, 0, Statistics.STAT_MSG_SEC, String.valueOf(indexByStatistic.get(Statistics.STAT_MSG_SEC)), 1, "linespoints ls 1");
            bw.write("set ylabel \"milliseconds\"\n");
            bw.write("set xlabel \"" + xlabel + "\"\n");
            bw.write("unset format x\n");
            bw.write("plot '" + dataFile.getName() + "' \\\n");
            writePlotLine(bw, 0, "r-" + Statistics.STAT_MEAN, String.valueOf(indexByStatistic.get("r-" + Statistics.STAT_MEAN)), 2, "linespoints ls 1");
            writePlotLine(bw, 1, "w-" + Statistics.STAT_MEAN, String.valueOf(indexByStatistic.get("w-" + Statistics.STAT_MEAN)), 2, "linespoints ls 2");
            bw.write("unset multiplot\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void writePlotLine(BufferedWriter bw, int i, String stat, String datasetIndex, int size, String w) throws IOException {
        bw.write("  ");
        if (i > 0) {
            bw.write("'' ");
        }
        bw.write("i ");
        bw.write(datasetIndex);
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