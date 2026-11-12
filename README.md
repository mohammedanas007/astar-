# A* Grid Pathfinder: JaCoCo Test Evaluation

A small but non-trivial Java 21 module (A* on a grid with obstacles) used as the system
under test for evaluating JaCoCo coverage together with JUnit tests.

## Layout

```
pom.xml                         Maven build: JUnit 4, surefire, JaCoCo (agent, report, check)
src/main/java/.../pathfinding/  System under test
  Point, GridMap, PathResult, AStarPathfinder
src/test/java/.../pathfinding/  Tests
  GridMapTest, PathResultTest, AStarPathfinderTest, RandomizedOracleTest
  PathAssert (shared path validator), ReportRunner (pass/fail + timing log)
scripts/run-local.sh            Build + test without Maven (no coverage)
scripts/mutation-check.sh       Injects known bugs into a copy of the SUT to test the tests
results/                        Output of the runs described in REPORT.md
REPORT.md                       Findings
docs/TEST_DESIGN.md             Formal test case table (Test ID, preconditions, inputs, expected, technique)
docs/ROLES.md                  Student A / Student B responsibilities
```

## Prerequisites

JDK 21, Maven 3.9+, and network access to Maven Central for the first build.

## Run with JaCoCo (the main workflow)

```bash
mvn clean verify
```

| Output | Location |
| --- | --- |
| Per-test results | `target/surefire-reports/` |
| Raw coverage data | `target/jacoco.exec` |
| HTML coverage report | `target/site/jacoco/index.html` |
| XML / CSV coverage | `target/site/jacoco/jacoco.xml`, `jacoco.csv` |

`mvn verify` also runs the `check` goal, which fails the build below 90% line or 85% branch
coverage. Those thresholds are a starting point I chose, not measured values; adjust them in
`pom.xml` after your first report.

Open the HTML report and look at, in order: branch coverage per class (red/yellow diamonds in
`AStarPathfinder` mark untested decisions), then line coverage, then missed methods.

## Run with JaCoCo without Maven

Download the JaCoCo release zip, then:

```bash
JACOCO=/path/to/jacoco/lib
scripts/run-local.sh                        # compiles into target/
java -javaagent:$JACOCO/jacocoagent.jar=destfile=target/jacoco.exec \
     -cp target/classes:target/test-classes:junit-4.13.2.jar:hamcrest-core-1.3.jar \
     com.example.pathfinding.ReportRunner
java -jar $JACOCO/jacococli.jar report target/jacoco.exec \
     --classfiles target/classes --sourcefiles src/main/java --html target/jacoco-html
```

## Run tests only, offline (no coverage)

```bash
scripts/run-local.sh            # writes target/test-report.txt
scripts/mutation-check.sh       # writes target/mutation-report.txt
```

`run-local.sh` expects JUnit 4.13.2 and Hamcrest 1.3 jars; set `JUNIT_JAR` and
`HAMCREST_JAR` if they are not at the default paths in the script.

## Behaviour contract of the SUT

- Moves: 4-way, or 8-way with `allowDiagonal`. Orthogonal cost 1, diagonal cost sqrt(2).
- Diagonals may not cut corners: both flanking cells must be walkable.
- Heuristic: Manhattan (4-way) or octile (8-way); both admissible and consistent, so paths are optimal.
- Ties: lowest f, then lowest h, then first discovered. Neighbour order N, E, S, W, NE, SE, SW, NW (N is y-1).
- Start or goal outside the grid: `IllegalArgumentException`. Start or goal on a wall: not found.
- Start equals goal: single-cell path, cost 0. No path: empty list, cost `+Infinity`.
