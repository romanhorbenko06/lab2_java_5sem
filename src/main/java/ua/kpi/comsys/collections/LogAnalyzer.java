package ua.kpi.comsys.collections;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogAnalyzer {

    private static final String OCTET = "(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)";

    private static final Pattern COMMON_LOG_FORMAT = Pattern.compile(
            "^(" + OCTET + "(?:\\." + OCTET + "){3})"
                    + " \\S+ \\S+"
                    + " \\[\\d{2}/[A-Za-z]{3}/\\d{4}:\\d{2}:\\d{2}:\\d{2} [+-]\\d{4}]"
                    + " \"(?:[^\"\\\\]|\\\\.)*\""
                    + " \\d{3}"
                    + " (?:\\d+|-)$");

    public Set<String> getUniqueIps(List<String> logLines) {
        requireLogLines(logLines);

        Set<String> ips = new TreeSet<>();
        for (String line : logLines) {
            extractIp(line).ifPresent(ips::add);
        }
        return ips;
    }

    public List<String> getTopIps(List<String> logLines, int topN) {
        requireLogLines(logLines);
        if (topN <= 0) {
            throw new IllegalArgumentException("topN must be greater than 0");
        }

        Map<String, Integer> frequencies = new HashMap<>();
        for (String line : logLines) {
            extractIp(line).ifPresent(ip -> frequencies.merge(ip, 1, Integer::sum));
        }

        return frequencies.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(topN)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static void requireLogLines(List<String> logLines) {
        if (logLines == null) {
            throw new IllegalArgumentException("logLines cannot be null");
        }
    }

    private static Optional<String> extractIp(String line) {
        if (line == null) {
            return Optional.empty();
        }
        Matcher matcher = COMMON_LOG_FORMAT.matcher(line.strip());
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
