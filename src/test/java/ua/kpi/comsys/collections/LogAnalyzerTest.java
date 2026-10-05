package ua.kpi.comsys.collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogAnalyzerTest {

    private final LogAnalyzer analyzer = new LogAnalyzer();

    private static String line(String ip) {
        return ip + " - - [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" 200 9481";
    }

    private static List<String> repeat(String ip, int times) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            lines.add(line(ip));
        }
        return lines;
    }

    @Nested
    class GetUniqueIps {

        @Test
        @DisplayName("null замість списку -> IllegalArgumentException з текстом зі специфікації")
        void nullListIsRejected() {
            IllegalArgumentException thrown =
                    assertThrows(IllegalArgumentException.class, () -> analyzer.getUniqueIps(null));

            assertEquals("logLines cannot be null", thrown.getMessage());
        }

        @Test
        @DisplayName("порожній список -> порожня множина")
        void emptyListGivesEmptySet() {
            assertTrue(analyzer.getUniqueIps(List.of()).isEmpty());
        }

        @Test
        @DisplayName("приклад зі специфікації розбирається")
        void parsesExampleFromSpecification() {
            String example = "127.0.0.1 ident alice [01/May/2025:07:20:10 +0000] "
                    + "\"GET /index.html HTTP/1.1\" 200 9481";

            assertEquals(Set.of("127.0.0.1"), analyzer.getUniqueIps(List.of(example)));
        }

        @Test
        @DisplayName("повтори однієї адреси згортаються в один елемент")
        void duplicatesAreCollapsed() {
            List<String> lines = List.of(
                    line("10.0.0.1"), line("10.0.0.2"), line("10.0.0.1"), line("10.0.0.3"), line("10.0.0.2"));

            assertEquals(Set.of("10.0.0.1", "10.0.0.2", "10.0.0.3"), analyzer.getUniqueIps(lines));
        }

        @Test
        @DisplayName("розмір відповіді '-' і лапки в запиті не ламають розбір")
        void acceptsDashSizeAndEscapedQuotes() {
            List<String> lines = List.of(
                    "10.0.0.7 - bob [01/May/2025:07:20:12 -0300] \"POST /login HTTP/1.1\" 302 -",
                    "10.0.0.8 - - [01/May/2025:07:20:13 +0200] \"GET /search?q=\\\"java\\\" HTTP/1.1\" 200 12",
                    "  10.0.0.9 - - [01/May/2025:07:20:14 +0000] \"\" 400 0  ");

            assertEquals(Set.of("10.0.0.7", "10.0.0.8", "10.0.0.9"), analyzer.getUniqueIps(lines));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "",
                "   ",
                "this line is not a log entry",
                "127.0.0.1",
                "127.0.0.1 ident alice",
                "127.0.0.1 ident alice [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" 200",
                "127.0.0.1 ident alice [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" OK 9481",
                "127.0.0.1 ident alice [01/May/2025:07:20:10 +0000] GET /index.html HTTP/1.1 200 9481",
                "127.0.0.1 ident alice 01/May/2025:07:20:10 +0000 \"GET /index.html HTTP/1.1\" 200 9481",
                "127.0.0.1 ident alice [2025-05-01 07:20:10] \"GET /index.html HTTP/1.1\" 200 9481",
                "127.0.0.1 ident [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" 200 9481",
                "127.0.0.1 ident alice [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" 200 9481 extra",
                "256.0.0.1 ident alice [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" 200 9481",
                "127.0.1 ident alice [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" 200 9481",
                "localhost ident alice [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" 200 9481"
        })
        @DisplayName("рядок не у Common Log Format ігнорується")
        void malformedLineIsIgnored(String malformed) {
            List<String> lines = List.of(line("10.0.0.1"), malformed);

            assertEquals(Set.of("10.0.0.1"), analyzer.getUniqueIps(lines));
        }

        @Test
        @DisplayName("null усередині списку ігнорується")
        void nullElementIsIgnored() {
            List<String> lines = Arrays.asList(line("10.0.0.1"), null, line("10.0.0.2"));

            assertEquals(Set.of("10.0.0.1", "10.0.0.2"), analyzer.getUniqueIps(lines));
        }

        @Test
        @DisplayName("усі рядки некоректні -> порожня множина")
        void onlyMalformedLinesGiveEmptySet() {
            assertTrue(analyzer.getUniqueIps(List.of("foo", "bar baz")).isEmpty());
        }
    }

    @Nested
    class GetTopIps {

        @Test
        @DisplayName("null замість списку -> IllegalArgumentException з текстом зі специфікації")
        void nullListIsRejected() {
            IllegalArgumentException thrown =
                    assertThrows(IllegalArgumentException.class, () -> analyzer.getTopIps(null, 3));

            assertEquals("logLines cannot be null", thrown.getMessage());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
        @DisplayName("topN <= 0 -> IllegalArgumentException з текстом зі специфікації")
        void nonPositiveTopNIsRejected(int topN) {
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> analyzer.getTopIps(List.of(line("10.0.0.1")), topN));

            assertEquals("topN must be greater than 0", thrown.getMessage());
        }

        @Test
        @DisplayName("null-список перевіряється раніше за topN")
        void nullListIsCheckedBeforeTopN() {
            IllegalArgumentException thrown =
                    assertThrows(IllegalArgumentException.class, () -> analyzer.getTopIps(null, 0));

            assertEquals("logLines cannot be null", thrown.getMessage());
        }

        @Test
        @DisplayName("порожній список -> порожній результат")
        void emptyListGivesEmptyResult() {
            assertTrue(analyzer.getTopIps(List.of(), 5).isEmpty());
        }

        @Test
        @DisplayName("адреси йдуть за спаданням частоти")
        void sortsByFrequencyDescending() {
            List<String> lines = new ArrayList<>();
            lines.addAll(repeat("10.0.0.1", 1));
            lines.addAll(repeat("10.0.0.2", 3));
            lines.addAll(repeat("10.0.0.3", 2));

            assertEquals(List.of("10.0.0.2", "10.0.0.3", "10.0.0.1"), analyzer.getTopIps(lines, 3));
        }

        @Test
        @DisplayName("результат обрізається до topN")
        void limitsResultToTopN() {
            List<String> lines = new ArrayList<>();
            lines.addAll(repeat("10.0.0.1", 4));
            lines.addAll(repeat("10.0.0.2", 3));
            lines.addAll(repeat("10.0.0.3", 2));
            lines.addAll(repeat("10.0.0.4", 1));

            assertEquals(List.of("10.0.0.1", "10.0.0.2"), analyzer.getTopIps(lines, 2));
            assertEquals(List.of("10.0.0.1"), analyzer.getTopIps(lines, 1));
        }

        @Test
        @DisplayName("однакова частота -> лексикографічний порядок за зростанням")
        void tiesAreBrokenLexicographically() {
            List<String> lines = List.of(
                    line("192.168.0.2"), line("10.0.0.9"), line("172.16.0.1"), line("10.0.0.10"));

            assertEquals(List.of("10.0.0.10", "10.0.0.9", "172.16.0.1", "192.168.0.2"),
                    analyzer.getTopIps(lines, 4));
        }

        @Test
        @DisplayName("нічия на межі topN розв'язується на користь меншого рядка")
        void tieOnTheBoundaryKeepsSmallerIp() {
            List<String> lines = new ArrayList<>();
            lines.addAll(repeat("10.0.0.5", 3));
            lines.addAll(repeat("10.0.0.3", 2));
            lines.addAll(repeat("10.0.0.1", 2));
            lines.addAll(repeat("10.0.0.2", 2));

            assertEquals(List.of("10.0.0.5", "10.0.0.1", "10.0.0.2"), analyzer.getTopIps(lines, 3));
        }

        @Test
        @DisplayName("унікальних адрес менше за topN -> повертаються всі наявні")
        void returnsAllWhenFewerThanTopN() {
            List<String> lines = new ArrayList<>();
            lines.addAll(repeat("10.0.0.2", 2));
            lines.addAll(repeat("10.0.0.1", 1));

            assertEquals(List.of("10.0.0.2", "10.0.0.1"), analyzer.getTopIps(lines, 10));
            assertEquals(List.of("10.0.0.2", "10.0.0.1"), analyzer.getTopIps(lines, Integer.MAX_VALUE));
        }

        @Test
        @DisplayName("некоректні рядки і null не впливають на підрахунок")
        void malformedLinesDoNotAffectCounting() {
            List<String> lines = Arrays.asList(
                    line("10.0.0.1"),
                    "10.0.0.2 broken line",
                    null,
                    line("10.0.0.2"),
                    "10.0.0.2",
                    "",
                    line("10.0.0.1"));

            assertEquals(List.of("10.0.0.1", "10.0.0.2"), analyzer.getTopIps(lines, 2));
        }

        @Test
        @DisplayName("вхідний список не змінюється")
        void inputListIsNotModified() {
            List<String> lines = new ArrayList<>(List.of(line("10.0.0.2"), line("10.0.0.1"), "garbage"));
            List<String> snapshot = List.copyOf(lines);

            analyzer.getTopIps(lines, 1);
            analyzer.getUniqueIps(lines);

            assertEquals(snapshot, lines);
        }
    }
}
