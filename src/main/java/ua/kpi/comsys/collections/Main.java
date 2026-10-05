package ua.kpi.comsys.collections;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        LogAnalyzer analyzer = new LogAnalyzer();

        List<String> logLines = List.of(
                "127.0.0.1 ident alice [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" 200 9481",
                "192.168.0.15 - - [01/May/2025:07:20:11 +0000] \"GET /about.html HTTP/1.1\" 200 2310",
                "10.0.0.7 - bob [01/May/2025:07:20:12 +0000] \"POST /login HTTP/1.1\" 302 -",
                "192.168.0.15 - - [01/May/2025:07:20:13 +0000] \"GET /style.css HTTP/1.1\" 304 0",
                "this line is not a log entry",
                "127.0.0.1 ident alice [01/May/2025:07:20:14 +0000] \"GET /logo.png HTTP/1.1\" 200 5120",
                "192.168.0.15 - - [01/May/2025:07:20:15 +0000] \"GET /missing HTTP/1.1\" 404 153"
        );

        System.out.println("Unique IPs: " + analyzer.getUniqueIps(logLines));
        System.out.println("Top 2 IPs: " + analyzer.getTopIps(logLines, 2));
    }
}
