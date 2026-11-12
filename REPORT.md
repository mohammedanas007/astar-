# JaCoCo Evaluation on an A* Grid Pathfinder: Report

Date: 2026-11-10. Java 21.0.12, JUnit 4.13.2.

## 1. Objective and scope

Evaluate JaCoCo (Java code-coverage tool) against a standalone Java module. The system under
test (SUT) is a grid pathfinder using A* with obstacles, in four-way and eight-way movement
modes. Deliverables: tool setup, test design, execution and reporting.

**Important limitation.** JaCoCo itself was **not executed** for this report. The environment
used to build this project has no network access, so the JaCoCo agent, the JaCoCo Maven plugin
and surefire could not be downloaded, and only JUnit 4 was available on disk. Everything below
about tests and defect detection was run and measured. Everything about JaCoCo is configuration
that has not been exercised. No coverage percentages appear in this report because none were measured.

## 2. System under test

| Class | Role |
| --- | --- |
| `GridMap` | Rectangular grid, `#` wall and `.` open, out-of-bounds counts as blocked |
| `AStarPathfinder` | A* search; Manhattan heuristic (4-way) or octile heuristic (8-way) |
| `PathResult` | Immutable result: found, path, cost, nodes expanded |
| `Point` | Cell coordinate record |

Contract, as implemented and tested:

- Orthogonal step costs 1; diagonal step costs sqrt(2) and may not cut corners.
- Ties resolve by lowest f, then lowest h, then first discovered, with neighbour order N, E, S, W, NE, SE, SW, NW.
- Start or goal off the grid throws `IllegalArgumentException`; start or goal on a wall returns "not found".
- No path gives an empty list and cost `+Infinity`.

## 3. Tool setup

| Item | Status |
| --- | --- |
| `pom.xml` with JUnit 4.13.2, surefire 3.5.2, JaCoCo 0.8.13 (`prepare-agent`, `report`, `check`) | Written, **not run** |
| Coverage thresholds in `check`: 90% line, 85% branch | My choice, **unverified** |
| JaCoCo CLI workflow (agent + `jacococli.jar report`) in README | Documented, **not run** |
| `scripts/run-local.sh`: javac + JUnit 4 jars from the local Gradle install | **Run, works** |
| `scripts/mutation-check.sh` | **Run, works** |

To produce the real coverage report on a machine with network access: `mvn clean verify`, then
open `target/site/jacoco/index.html`. See `README.md`.

## 4. Test design

47 tests in four classes.

| Class | Tests | Covers |
| --- | --- | --- |
| `AStarPathfinderTest` | 32 | Functional 8, boundary 6, edge/error 11, ties 4, speed/scale 3 |
| `GridMapTest` | 11 | Parsing, bounds, blocked cells, invalid construction |
| `PathResultTest` | 3 | Immutability, no-path result, `Point` formatting |
| `RandomizedOracleTest` | 1 | 2000 seeded random grids x 2 movement modes, compared with an independent Dijkstra |

Requested scenarios and where they are tested:

- **Blocked paths:** fully walled-off goal, enclosed goal (asserts the whole reachable region of 16 cells is expanded), blocked start, blocked goal, diagonal squeezes between walls, a 300x300 grid split by a wall.
- **Edge of map:** start and goal in corners, a route forced along the map edge, 1x1 and 1xN and Nx1 grids, out-of-bounds start and goal on all four sides, and a no-wrap check (a left region of 4 cells must not connect to the right side).
- **Ties:** four-way 3x3 diagonal-corner tie resolves east-first; eight-way tie between a diagonal-first and an orthogonal-first route resolves diagonal-first; equal-cost routes around an obstacle give identical results over repeated runs and across fresh instances.

Every found path in the random test is also checked by a shared validator (`PathAssert`):
starts at start, ends at goal, no repeated cells, no wall cells, legal steps only, no corner
cutting, and reported cost equals the path's own length.

Assertion metrics (static count): 125 JUnit assertion call sites (114 in test bodies, 11 inside
the shared path validator), plus 29 invocations of the validator or the no-path helper. This
undercounts executed assertions, because the validator and the oracle comparison run inside
loops thousands of times.

## 5. Execution results

Source: `results/test-report.txt` (full per-test log).

| Metric | Value |
| --- | --- |
| Tests run / passed / failed / ignored | 47 / 47 / 0 / 0 |
| Wall time | 525.0 ms |
| Sum of per-test times | 423.5 ms |
| Slowest | Randomized oracle test, 249.49 ms (4000 searches plus 4000 repeat runs and 4000 oracle searches) |
| Next slowest | 300x300 no-path search, 91.31 ms; 500x500 eight-way, 19.85 ms |

The suite was run twice with identical pass/fail results. Wall time was 405 ms on the other run
against 525 ms here, so timings vary by about 30% between runs (JIT warm-up affects the first tests).
Treat the millisecond figures as indicative, not benchmarks.

## 6. Defect reporting

**Defects found in the SUT: none.** All 47 tests pass.

Because a suite that has never failed has not shown it can fail, I injected 11 known bugs, one
at a time, into a copy of the SUT (`scripts/mutation-check.sh`; log in `results/mutation-report.txt`).
The control run on unmodified code passed.

| Mutant | Result | Caught by |
| --- | --- | --- |
| M1 allow diagonal corner cutting | Killed | 4 tests, e.g. `diagonalCannotSqueezeBetweenTwoDiagonalWalls` |
| M2 diagonal step costs 1 | Killed | 8 tests, e.g. `eightWayTiePrefersTheDiagonalFirst` |
| M3 inadmissible heuristic (doubled) | Killed | **1 test only**: the randomized oracle |
| M4 bounds check off by one | Killed | 18 tests |
| M5 blocked start ignored | Killed | 2 tests |
| M6 h tie-break removed | Killed | **1 test only**: `eightWayTiePrefersTheDiagonalFirst` |
| M7 closed-set check removed | **Survived** | none |
| M8 four-way search moves diagonally | Killed | 7 tests |
| M9 heuristic returns 0 | Killed | 2 tests, including `heuristicKeepsSearchFocusedOnTheGoal` |
| M10 out-of-bounds cells treated as open | Killed | 27 tests |
| M11 path list not copied | Killed | `pathIsDefensivelyCopiedAndImmutable` |

Mutation score: 10 of 11 killed (90%). This is a hand-made sample of 11, not a systematic
mutation tool run, so treat it as a sanity check on the suite rather than a coverage figure.

Observations:

- **M7 survived.** I believe it is an equivalent mutant: with a consistent heuristic, a cell that is already closed has its optimal cost recorded, so the `tentative < g` test rejects it anyway and the extra check only avoids a comparison. I have not proven this.
- **M3 and M6 each depend on a single test.** An inadmissible heuristic is caught only by the randomized oracle, and a broken h tie-break only by one specific tie test. Adding a fixed scenario for the first and a second tie scenario for the second would add redundancy.
- **Test defects caught in review, before the first run:** one test meant to check an allowed diagonal actually exercised an orthogonal move, and another contained an assertion that compared a value to itself and could never fail. Both were rewritten with independently derived expectations.

SUT design notes (not failures): the open-list comparator uses an epsilon (1e-9) for float
ties. That is safe for the grid sizes tested, since costs are small integer combinations of 1
and sqrt(2), but an epsilon comparator is not strictly transitive in theory.

## 7. JaCoCo coverage results

Not measured. Fill in after running `mvn clean verify`:

| Counter | Covered / total | % |
| --- | --- | --- |
| Instructions | | |
| Branches | | |
| Lines | | |
| Methods | | |
| Classes | | |

Things worth checking in the HTML report: partially covered branches in
`AStarPathfinder.findPath` (the diagonal and bounds conditions), and `GridMap.parse` error paths.
Remember that JaCoCo reports which code ran, not whether the assertions are strong; the
mutation results above are the evidence for assertion strength.

## 8. Reproduce

```bash
scripts/run-local.sh        # tests, no coverage      -> target/test-report.txt
scripts/mutation-check.sh   # injected-bug check      -> target/mutation-report.txt
mvn clean verify            # tests + JaCoCo coverage -> target/site/jacoco/index.html
```
