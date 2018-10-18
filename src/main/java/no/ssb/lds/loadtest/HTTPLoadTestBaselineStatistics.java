package no.ssb.lds.loadtest;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HTTPLoadTestBaselineStatistics {

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Must provide the following three arguments: INFOLDER OUTFOLDER OUTBASENAME");
            return;
        }
        String inFolder = args[0];
        Path resultsPath = Paths.get(inFolder);
        if (!Files.isDirectory(resultsPath)) {
            System.err.println("Invalid INFOLDER argument, not a directory: " + inFolder);
            return;
        }
        String outFolder = args[1];
        Path plotfilesFolder = Path.of(outFolder);
        if (!Files.isDirectory(plotfilesFolder)) {
            System.err.println("Invalid OUTFOLDER argument, not a directory: " + outFolder);
            return;
        }
        if (!Files.isWritable(plotfilesFolder)) {
            System.err.println("Invalid OUTFOLDER argument, invalid permissions: " + outFolder);
            return;
        }

        String outBasename = args[2];

        Statistics statistics = new Statistics();
        Pattern csvFilePattern = Pattern.compile("(.*)\\.csv");
        Pattern readPattern = Pattern.compile("\\(Read-URL:");
        Pattern writePattern = Pattern.compile("\\(Write-URL:");
        ObjectMapper mapper = new ObjectMapper();
        List<Path> csvFiles = Files.list(resultsPath).filter(path -> csvFilePattern.matcher(path.getFileName().toString().toLowerCase()).matches()).collect(Collectors.toList());

        for (Path csvFile : csvFiles) {
            int failed = 0;
            int deviations = 0;
            int undeterminedReadWrite = 0;
            Matcher csvFilenameMatcher = csvFilePattern.matcher(csvFile.getFileName().toString().toLowerCase());
            csvFilenameMatcher.matches();
            String testBasename = csvFilenameMatcher.group(1);

            Path testResultsFile = resultsPath.resolve(testBasename + ".json");
            try (JsonParser jParser = mapper.getFactory().createParser(testResultsFile.toFile())) {
                if (jParser.nextToken() != JsonToken.START_ARRAY) {
                    throw new IllegalStateException("Expected an array");
                }
                while (jParser.nextToken() == JsonToken.START_OBJECT) {
                    ObjectNode node = mapper.readTree(jParser);
                    long duration = node.get("test_duration").asLong();
                    boolean success = node.get("test_success").asBoolean();
                    if (!success) {
                        failed++;
                        continue;
                    }
                    boolean deviation = node.get("test_deviation_flag").asBoolean();
                    if (deviation) {
                        deviations++;
                        continue;
                    }
                    String tags = node.get("test_tags").asText();
                    Matcher readMatcher = readPattern.matcher(tags);
                    if (readMatcher.find()) {
                        statistics.recordReadLatency(duration);
                    } else {
                        Matcher writeMatcher = writePattern.matcher(tags);
                        if (writeMatcher.find()) {
                            statistics.recordWriteLatency(duration);
                        } else {
                            undeterminedReadWrite++;
                        }
                    }
                }
            }

            if (failed > 0) {
                System.err.println(testBasename + " Number of failed tests: " + failed);
            }
            if (deviations > 0) {
                System.err.println(testBasename + " Number of deviations: " + deviations);
            }
            if (undeterminedReadWrite > 0) {
                System.err.println(testBasename + " Number of test_tags that match neither read nor write pattern: " + undeterminedReadWrite);
            }


            Path loadConfigurationFile = resultsPath.resolve(testBasename + "_load_specififation.json");
            ObjectNode loadConfigurationNode = (ObjectNode) mapper.readTree(loadConfigurationFile.toFile());
            loadConfigurationNode.get("test_id").asText();
            int threads = loadConfigurationNode.get("test_no_of_threads").asInt();
            int testDurationSeconds = loadConfigurationNode.get("test_duration_in_seconds").asInt();
            statistics.sample(threads, testDurationSeconds);
        }

        GnuplotFileGenerator.writeStatisticsGnuplotAndDatafile(statistics, "clients",
                plotfilesFolder.resolve(outBasename + ".gnu").toFile(),
                plotfilesFolder.resolve(outBasename + ".dat").toFile(),
                outBasename
        );
    }
}
