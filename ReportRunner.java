package com.example.pathfinding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * Runs the whole suite and writes a plain-text pass/fail log with per-test timings.
 * Works without Maven: {@code java ReportRunner [report-file]}.
 * Exit code is 1 if any test failed.
 */
public final class ReportRunner {

    private record Entry(String name, String status, double millis, String detail) {}

    private static final class Collector extends RunListener {
        private final Map<String, Long> started = new HashMap<>();
        private final Map<String, String> failures = new HashMap<>();
        final List<Entry> entries = new ArrayList<>();

        private static String id(Description d) {
            String cls = d.getClassName();
            return cls.substring(cls.lastIndexOf('.') + 1) + "." + d.getMethodName();
        }

        @Override
        public void testStarted(Description d) {
            started.put(id(d), System.nanoTime());
        }

        @Override
        public void testFailure(Failure f) {
            String msg = String.valueOf(f.getMessage()).replace('\n', ' ');
            failures.put(id(f.getDescription()), f.getException().getClass().getSimpleName() + ": " + msg);
        }

        @Override
        public void testIgnored(Description d) {
            entries.add(new Entry(id(d), "SKIP", 0.0, ""));
        }

        @Override
        public void testFinished(Description d) {
            String id = id(d);
            double ms = (System.nanoTime() - started.get(id)) / 1e6;
            String fail = failures.get(id);
            entries.add(new Entry(id, fail == null ? "PASS" : "FAIL", ms, fail == null ? "" : fail));
        }
    }

    public static void main(String[] args) throws IOException {
        Class<?>[] suite = {
            GridMapTest.class, PathResultTest.class, AStarPathfinderTest.class, RandomizedOracleTest.class
        };
        Collector collector = new Collector();
        JUnitCore core = new JUnitCore();
        core.addListener(collector);

        long t0 = System.nanoTime();
        Result result = core.run(suite);
        double wallMs = (System.nanoTime() - t0) / 1e6;

        List<String> lines = new ArrayList<>();
        lines.add("A* pathfinder test run");
        lines.add("======================");
        for (Entry e : collector.entries) {
            String line = String.format(Locale.ROOT, "%-4s  %-90s %9.2f ms", e.status(), e.name(), e.millis());
            lines.add(line);
            if (!e.detail().isEmpty()) {
                lines.add("      -> " + e.detail());
            }
        }
        double sumMs = collector.entries.stream().mapToDouble(Entry::millis).sum();
        int passed = result.getRunCount() - result.getFailureCount();
        lines.add("");
        lines.add("Summary");
        lines.add("-------");
        lines.add("Run: " + result.getRunCount());
        lines.add("Passed: " + passed);
        lines.add("Failed: " + result.getFailureCount());
        lines.add("Ignored: " + result.getIgnoreCount());
        lines.add(String.format(Locale.ROOT, "Wall time: %.1f ms", wallMs));
        lines.add(String.format(Locale.ROOT, "Sum of test times: %.1f ms", sumMs));
        lines.add("Slowest tests:");
        collector.entries.stream()
                .sorted((a, b) -> Double.compare(b.millis(), a.millis()))
                .limit(3)
                .forEach(e -> lines.add(String.format(Locale.ROOT, "  %-80s %9.2f ms", e.name(), e.millis())));

        lines.forEach(System.out::println);

        Path out = Path.of(args.length > 0 ? args[0] : "target/test-report.txt");
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        Files.write(out, lines);
        System.exit(result.wasSuccessful() ? 0 : 1);
    }
}
