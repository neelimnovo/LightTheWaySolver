# Performance Improvement Plan for LightTheWaySolver (Revision 3)

Revision 3 replaces the speculative strategy list of Revision 2. Every claim
below is now backed by counters from the profiler added in
[SolverProfiler.java](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/SolverProfiler.java).
Where a number is estimated rather than measured, it says so.

---

## 0. How to run the profiler

```bash
# counters only (near-zero distortion)
gradle runTest -PlevelName="Level 015.json" -Pprofile

# also count DISTINCT physical light outcomes (proves redundancy; slower, ~33MB)
gradle runTest -PlevelName="Level 015.json" -Pprofile -Pdedupe

# ground truth for solvability: bypass ALL placement filtering
gradle runTest -PlevelName="Level 029-easier.json" -Pnofilter
```

`SolverProfiler.ENABLED` is a `static final` read from a system property, so with
profiling off the JIT constant-folds every instrumentation branch away and the
shipped hot loop is unchanged.

**On the timing lines specifically**: the sections being timed run in a few
hundred nanoseconds, and a `System.nanoTime()` pair costs ~36ns on this machine.
The profiler calibrates and subtracts that, but the residual relative error is
still large — the `projectLight()` estimate comes out at ~200% of wall time,
which is impossible. The report prints an explicit warning when an estimate
exceeds wall time. **Trust the counters, not the time-attribution lines.** The
counters are what the conclusions below rest on.

---

## 1. Measured baselines

### Level 015 — small, tightly constrained, already fast
```
35 empty spots, 7 DGOs, naive P(35,7)   = 33,891,580,800
wall time                                = 497 ms
internal nodes visited                   = 322,894
placements descended into                = 3,458,610
leaf evaluations                         = 3,135,717
filter() calls                           = 322,894   (scanned 4,848,253, kept 99.4%)
light queue pops                         = 19,882,826  (6.34 per leaf)
receiver hits                            = 1,456,400
DISTINCT light outcomes                  = 11,353
  -> 99.64% of leaves redundant, each outcome re-simulated 276x
throughput                               = 6.2M leaves/sec (161 ns/leaf)
```

### Test Slow — open room, the user's pathological case
```
64 empty spots, 6 DGOs (1 DOWN light + 5 backward mirrors)
naive P(64,6)                            = 53,981,544,960
wall time                                = 39,035 ms
internal nodes visited                   = 26,951,616
placements descended into                = 302,757,972
leaf evaluations                         = 275,806,357
filter() calls                           = 26,951,616
  candidate spots scanned                = 1,617,096,956
  spots kept                             = 1,617,096,956   <-- 100.0% keep rate
  estimated share of wall time           = ~36%
light queue pops                         = 1,206,409,342 (4.37 per leaf)
single-cell light advances               = 837,211,909
RECEIVER HITS                            = 1
DISTINCT light outcomes                  = 6,406
  -> 100.00% of leaves redundant, each outcome re-simulated 43,054x
throughput                               = 7.1M leaves/sec (142 ns/leaf)
```

> The level file was edited during this analysis (a mirror was removed). An
> earlier run of the 7-DGO version took 308s / 2.48B leaves. The 6-DGO numbers
> above are the current ones.

### Level 029-easier — reports "no solution" (a correctness bug, see §4)
```
42 empty spots, 6 DGOs, wall ~5s, leaf evaluations = 47,628,095
filter keep rate = 98.9%, receiver hits = 15,006,122
RESULT: no solution found
```

---

## 2. The core diagnosis

Three numbers explain essentially everything:

**(a) 43,054x redundancy.** On Test Slow, 275,806,357 leaf evaluations produce
only **6,406 distinct physical light outcomes**. The solver enumerates
*placements* but only ever tests *light outcomes*, and placements outnumber
outcomes by four to five orders of magnitude. Almost every mirror in almost
every permutation sits somewhere the beam never reaches, so moving it changes
nothing — yet each move triggers a full re-simulation from scratch.

This is the whole problem, stated precisely. **It is not a constant-factor
problem.** No amount of micro-optimization fixes a 43,000x redundancy factor;
the duplicates must stop being *generated*.

**(b) `Receiver hits: 1`.** Across 275 million complete light simulations, light
touched a receiver **exactly once** — the actual solution. Every other one of
those 275M simulations was provably pointless: the beam could not reach any
receiver at all. A single cheap "can this beam configuration reach a receiver?"
test would have eliminated essentially 100% of the work.

**(c) Filter keep rate of 100.0%.** On Test Slow the filters scanned 1.6 *billion*
candidate spots and rejected **none** of them, while consuming ~36% of wall time.
On an open level the geometric heuristics have nothing to bite on, so filtering
degenerates into pure overhead — it allocates a fresh `ArrayList` plus a fresh
boxed `Pair<Integer,Integer>` per surviving spot, 1.6 billion times, to produce a
list identical to its input.

### Why Test Slow is pathological, concretely

The receiver sits at (9,9). Its **only** non-wall neighbour is (9,8) — which is
empty-spot index **63 of 64, the very last one in scan order**. The solution is
"DOWN light source at (9,8)", using zero mirrors.

But the DGO queue is `[LightSource, BM, BM, BM, BM, BM]`, so the light source is
placed *first* and iterated in scan order. For each of the 63 wrong light-source
positions, the solver exhaustively permutes all five mirrors — tens of millions
of leaves each — before advancing the light source by one spot. It spends
~98% of its runtime proving, one irrelevant mirror arrangement at a time, that a
light source pointing into open space still doesn't reach the receiver.

---

## 3. Algorithmic improvements, ranked by expected impact

### A1. Beam-directed (ray-guided) search — the structural fix ⭐

> **A full implementation plan for this item now lives in
> [`beam_directed_search_design.md`](beam_directed_search_design.md)** — data structures,
> pseudocode, the derived physics table, edge cases, the validation harness, and
> milestones M1–M9. The sketch below is the summary.

Stop enumerating positions for each object. Instead **follow the light and decide
at each cell the beam enters whether to put something there.**

```
solve(beams, unplacedDGOs):
    if beams is empty:
        if all receivers powered:
            park remaining DGOs in cells no beam touched; return SUCCESS
        return FAIL
    beam = beams.pop()
    walk beam forward cell by cell until wall/receiver/placed-DGO:
        for each empty cell c on that path:
            option 1: place nothing, keep walking
            option 2: for each DISTINCT unplaced DGO type t placeable at c:
                          place t at c, push resulting beam(s), recurse
```

Branching becomes *O(path length x distinct remaining types)* instead of
*O(empty spots ^ objects)*. Objects that never touch the beam are **never
enumerated at all** — which is exactly the 43,054x redundancy factor, removed at
the root rather than filtered after the fact.

**Parking is provably safe**: a DGO placed in a cell no beam reaches cannot alter
the light, so the lit-cell set is unchanged and the configuration stays a
fixpoint. If there are fewer free off-beam cells than leftover DGOs, that branch
fails.

On Test Slow this turns "permute 5 mirrors across 63 cells for every light
position" into "trace one beam per light position, place zero mirrors on it,
park all five" — from 275M leaf evaluations to a few thousand node visits.

*Complexity: high (a genuine rewrite of the search). Impact: orders of magnitude.
This is the one that matters.*

### A2. Receiver back-chaining (goal-directed constraint propagation) ⭐

Receivers are few, and in hand-designed levels they sit in alcoves with exactly
one open side. Precompute, for each receiver, its **entry requirement**: the
(cell, direction, colour) triples that could power it.

- Test Slow: receiver (9,9) is enterable **only** from (9,8) heading DOWN with
  WHITE light. That single constraint identifies the solution immediately.
- Level 029-easier: *all six* receivers have exactly one entry cell —
  (0,2)←(1,2) LEFT, (0,3)←(1,3) LEFT, (0,5)←(1,5) LEFT, (6,0)←(6,1) UP,
  (7,9)←(7,8) DOWN, (9,3)←(8,3) RIGHT. Six hard constraints on a 42-cell grid.

Use these to **order** the search (try placements that directly satisfy an entry
requirement first) and to **prune** (if a receiver's only entry cell is occupied
by something that cannot emit the required colour in the required direction, fail
the branch immediately).

*Complexity: medium. Impact: very large on real levels, which are almost all
alcove-shaped.*

### A3. Early success + off-beam parking (cheap subset of A1)

Even without restructuring the search: after each placement, if all receivers are
already powered, park the remaining DGOs off-beam and return success. Costs one
light simulation per internal node (322K on Level 015 — negligible against 3.1M
leaf simulations) and immediately collapses every "the remaining objects are
irrelevant" tail.

*Complexity: low. Impact: large on Test Slow-shaped levels. Good first step
toward A1.*

### A4. Iterative deepening on **on-path** object count

Most real solutions redirect the beam only a few times. Search for solutions
using 0 on-path objects, then 1, then 2, and so on. Test Slow's solution uses
**zero** on-path mirrors, so this finds it almost immediately. Composes naturally
with A1 (bound the number of "place an object here" choices per branch).

*Complexity: low once A1 exists. Impact: large; also makes the solver find
*simple* solutions first, which is usually what a human wants.*

### A5. Incremental light simulation with undo

Today every leaf re-simulates the entire grid from the light sources, even though
only **one** object moved between sibling leaves. Maintain the light state
incrementally down the DFS: placing object *d+1* only perturbs beams that
actually reach it; on backtrack, undo. Turns per-leaf *O(full sim)* into
*O(delta)*.

Care required: placing an object can *block* an existing beam, forcing
recomputation of everything downstream of it. A practical version recomputes from
the first affected beam segment rather than truly incrementally.

*Complexity: medium-high. Impact: large constant factor (queue pops per leaf are
4.4-6.3 today), and it stacks with A1.*

### A6. Reachability pruning once light sources are placed

After all light sources are down, compute which cells any beam could reach given
at most *K* remaining redirections. If some receiver's entry cell is not in that
set, prune. Directly targets the `Receiver hits: 1` pathology.

*Complexity: medium. Impact: high on sparse levels, low on dense ones.*

### A7. Precomputed static ray tables

Precompute `nextObstruction[cell][direction]` over the static grid once. Since at
most ~7 DGOs are ever placed, a beam segment's endpoint is
`min(nextStaticObstruction, nearest placed DGO along the ray)` — an *O(#DGOs)*
lookup instead of an *O(cells traversed)* walk. Removes the 837M single-cell
advances on Test Slow outright.

*Complexity: medium. Impact: large constant factor; composes with everything.*

### A8. Parallel search (do this last)

Split top-level branches across cores, one grid copy and one `LevelSolver` per
branch (**not** per node — see the `Archive.java` deep-copy regression fixed in
commit `5e07b64`). Worth ~6-8x, but parallelizing before A1-A5 just buys more
hardware to run redundant work on.

---

## 4. Correctness deficiencies in the filtering (found while profiling)

### C1. FIXED: a tautology that rejected every adjacent T-junction

In `TJunction.isDynamicValidJunctionEntrance`, all four orientation cases read:

```java
if (dgo instanceof TJunction
        && (((TJunction) dgo).orientation != LEFT
            || ((TJunction) dgo).orientation != RIGHT)) return false;   // BUG
```

`x != LEFT || x != RIGHT` is **true for every possible value** — no orientation
can equal both. So *any* T-junction adjacent to another junction's entrance was
rejected, regardless of orientation. The method's own comment says the intent was
"the Tjunction's exits must face the entrance", and since a junction emits
perpendicular to its orientation, the correct test is `&&`.

**Fixed** in all four cases. Regression-checked: Level 015 still solves in 465ms
with an identical 3,135,717 permutations, so nothing that previously worked
changed.

### C2. NEW TOOL: `-Pnofilter` ground-truth mode

`LevelSolver.NO_FILTER` bypasses all placement heuristics and considers every
empty spot for every DGO. Vastly slower, but it answers "does this level have a
solution *at all*?" independently of the heuristics. If a level solves with
`-Pnofilter` but not without, a filter is provably over-strict.

### C3. Level 029-easier: filtering is NOT the cause (hypothesis tested and refuted)

The working hypothesis — mine and the one in the original report — was that
over-strict filtering was hiding a real solution. **That is not what is
happening.** Running with every placement heuristic disabled:

```
gradle runTest -PlevelName="Level 029-easier.json" -Pnofilter
  -> exhausted the full P(42,6) = 3,782,940,480 placement space in 4m37s
  -> !!! No solution found !!!
```

With no filtering at all, the solver considered **every** arrangement of all 6
DGOs across all 42 empty cells and still found nothing. So the level is
unsolvable *under the light physics as currently implemented*, and no relaxation
of the filters can change that. Fixing C1 also did not change the outcome.

That leaves two possibilities, and they need to be distinguished before any
filter is touched:

1. **The level genuinely has no solution as encoded.** Hand-analysis supports
   this. The prism is forced to (7,3): a DOWN prism emits RED down, BLUE right,
   YELLOW left, and those must land on Rr(7,9), Br(9,3), Yr(0,3), which pins
   x=7 and y=3 uniquely. That leaves 1 source + 3 T-junctions + 1 mirror to
   deliver WHITE light to **four** endpoints — Wr(0,2), Wr(0,5), Wr(6,0) and the
   prism — needing exactly 3 splits from exactly 3 junctions, with zero slack.
   Every routing I traced dead-ends at the same place: Wr(6,0) can only be lit
   by UP light at (6,1), the only beam that can reach (6,1) arrives travelling
   **LEFT**, and a ForwardMirror `/` maps LEFT→DOWN. A **BackwardMirror `\`**
   maps LEFT→UP and would close the routing. The level ships with
   `frontMirrors x1`, not `backMirrors` — so swapping that one piece is the
   likely intent, and is worth checking before hunting for solver bugs.
2. **The light physics is wrong**, so a real solution isn't recognised. Less
   likely given other levels solve correctly, but it is the only alternative.

Either way the actionable conclusion is: **`-Pnofilter` is the tool that settles
"is this a solver bug or an unsolvable level?" in one run**, and it should be the
first thing run whenever a level unexpectedly reports no solution.

### C3b. Filters that are over-strict anyway (independent of 029-easier)

These do not explain 029-easier, but they still reject placements that are
physically legal, and will cost real solutions on other levels:

1. **`TJunction.isStaticValidJunctionExits` requires BOTH exits to be non-WALL.**
   A junction splits the beam two ways; if one exit faces a wall that beam simply
   dies, but **the other exit still works**.
2. **`Prism.staticFilter` requires all four neighbours valid.** A prism emits
   three coloured beams; one of them terminating in a wall does not invalidate
   the placement.
3. **`LightSource.staticFilter` requires every non-exit neighbour to have
   `receiver == null`.** Defensible where the blocked receiver has no other
   entry, but it is applied unconditionally rather than only when the receiver
   would actually be starved.

Each is a one-line relaxation, and each should be validated with the C4
differential below rather than by inspection.

### C4. Recommended: a filtering differential test

For every level in `src/saveFiles/`, run filtered and unfiltered and assert the
two agree on *solvable / not solvable*. Any disagreement is an over-strict
filter, and disabling one DGO type's filter at a time bisects it. This is a cheap
suite that would have caught C1 and C3 immediately, and it guards the whole class
of bug — over-strict filtering silently turns solvable levels into "no solution",
which is far worse than being slow.

### C4b. Latent: `projectLight` has no cycle detection

`projectLight` loops until the queue drains, and nothing marks a
(cell, direction, colour) state as already visited. A beam that returns to a cell
travelling the same direction would loop forever, growing `litSpotX/Y` through
repeated `resizeLitSpotArrays()` until the heap is exhausted.

This is not reachable with the current level set — Test Slow uses only
BackwardMirrors, and `\` alone maps RIGHT→DOWN→RIGHT, producing a staircase that
always terminates at a wall rather than a closed loop. But a level mixing `/` and
`\` can form a rectangular loop, and nothing in the filters prevents it. A
visited-set keyed on the packed light `short` (which already encodes exactly
cell + direction + colour, so it is a perfect key) would make this safe at
negligible cost — and would double as the memoisation hook for A5.

### C5. Stored solutions do not round-trip their types

`solutionFiles/*.json` records DGO placements, but Gson serialized only fields —
a mirror becomes `{}` and a light source, prism, and T-junction are all
`{"orientation": "UP"}`, indistinguishable. Registering a Gson type adapter (or
writing an explicit `type` field) would let stored solutions be reloaded and used
as regression fixtures.

---

## 5. Java micro-optimizations, ranked by measured payoff

### M1. Make `filter()` allocation-free when it rejects nothing ⭐ (biggest, cheapest)

Test Slow: **1,617,096,956 spots scanned, 100.0% kept, ~36% of wall time.** Every
one of those allocated a `Pair<Integer,Integer>` (plus two boxed `Integer`s, since
coordinates routinely exceed the -128..127 cache) into a freshly allocated
`ArrayList`, only to reproduce the input list exactly.

Fix: build lazily. Scan; only allocate a result list when the *first* rejection
occurs, copying the prefix at that point; if nothing is rejected, `return
baseSpots` unchanged. Pure win, no semantic change, ~10 lines per filter.

Additionally: if a DGO's `staticFilter` kept every spot, its dynamic `filter` can
be skipped entirely for that level.

### M2. Restore the multi-cell raycast in `incrementLight` ⭐

Test Slow: **837,211,909 single-cell advances** and 1.21 *billion* queue pops.
`incrementLight` currently advances **one cell per call**, so every cell a beam
crosses does a full round trip: pack a `short`, write the grid, `ShortQueue.add`,
later `remove`, re-dispatch through `spreadLight`, re-read the cell, bounds-check.

Commit `27af3d3` had a version that walked consecutive empty cells in a tight
loop, recording them for reset and enqueueing **only** on reaching a real
obstruction; commit `80cd9b6` replaced it with the single-step form. The method's
own comment still describes the old behaviour ("Ray-cast the light forward until
it hits a WALL, a RECEIVER, or a DynamicGridObject"), so this reads as an
unintentional carry-over from aligning with the C++ port. Restoring it should
drop queue pops per leaf from ~4.4 to near 1 with identical results.

### M3. Precompute a DGO identity key (kills a hot `instanceof` chain)

`areIdenticalDGOs` runs once per placement — **302,757,972 times** on Test Slow —
and walks a chain of up to six `instanceof` tests plus casts. Give each DGO an
`int identityKey` computed once at load
(`(classId << 4) | (orientation << 2) | colour`) and compare integers.

### M4. Replace `Pair<Integer,Integer>` with packed `int` coordinates

Pack `x | (y << 8)` (mirroring what `Light` already does) or use parallel
`int[]`. Removes boxing from `emptySpots`, `receiverSpots`, `sourceSpots`, every
`filter()`, and `emptySpotIndex`. Wide blast radius (9 DGO subclasses + solver +
call sites) — do it after M1, which captures much of the same win far more
cheaply.

### M5. `sourceSpots` HashMap → small array

`emitLight` iterates `sourceSpots.values()` **once per leaf** — 275M times on
Test Slow — allocating an iterator and hashing, for a map that never holds more
than a few entries. Use `int[] sourceX, sourceY` + `LightSource[]` + a count.

### M6. `GridCell[][]` → flattened structure-of-arrays

`byte[] staticItems`, `Object[] dynamicItems`, `short[] lights`, indexed
`x + y * width`. Removes a pointer chase per access and improves locality in the
innermost loops. The half-finished `src/tests/ArrayLocalityTest.java` is
measuring exactly this — finish it and let the number decide, since JIT and
prefetching often make 2D-array pessimism smaller in practice than synthetic
benchmarks suggest.

### M7. `LinkedList` DGO queue → array indexed by depth

The DGO sequence is fixed; `remove()`/`addFirst()` churn nodes on every node
visit. An array plus the recursion depth removes all of it. (Commit `ef11fa6`
noted ArrayDeque was faster than LinkedList; the signature is `LinkedList` again
today.)

### M8. Merge `litSpotX`/`litSpotY` into one packed `int[]`

1.2 billion paired writes on Test Slow; one array halves the write traffic and
the reset loop's cache footprint.

### M9. Widen the `Light` coordinate packing (robustness, not speed)

`Light.create` packs x and y into **4 bits each**, silently wrapping above 15.
Current levels are ≤11 wide so nothing is broken today, but a 17-wide level would
produce silently wrong light with no error. Widen to 5-6 bits per axis (there are
8 spare bits) or assert on grid size at construction.

### M10. Remove the unused `java.util.concurrent` import

Left over from the C++ solver commit; flagged by the IDE.

---

## 6. Recommended order of work

1. **M1 + M2** — hours of work, no semantic change, and together they target the
   two largest measured constant-factor costs (~36% of wall in no-op filtering,
   837M redundant queue round trips). Do these first regardless of anything else.
2. **C4 (differential test), then C3b** — over-strict filtering makes solvable
   levels report "no solution", which is a worse failure than slowness. Note that
   029-easier turned out **not** to be a filtering bug (C3), so run the
   differential first and let it tell you which filters actually cost solutions
   rather than relaxing them on suspicion.
3. **A3 (early success + parking)** — small, self-contained, and it is the
   cheapest thing that meaningfully attacks Test Slow.
4. **A2 (receiver back-chaining)** — highest impact per unit of effort on real
   levels, given how constrained receiver entry points are.
5. **A1 (beam-directed search)** — the real fix for the 43,000x redundancy.
   Larger effort; A3 and A2 are natural stepping stones toward it.
6. **M3, M5, M7, M8** — mechanical, safe, compound with everything above.
7. **A5 / A7** (incremental simulation, ray tables), then **M4 / M6** (coordinate
   and grid representation), gated on fresh profiler runs.
8. **A8 (parallelism)** last, once per-node cost is genuinely lean.

---

## 7. Status of previously-implemented optimizations

Still accurate as of this revision, verified by reading the current source:

* Packed 16-bit `short` light state instead of an allocated `Light` object.
* Custom `ShortQueue` (array-backed circular buffer, no boxing).
* Batched lit-cell reset via `litSpotX`/`litSpotY` — *O(cells lit)* teardown.
* Static-filter precomputation cached per unique DGO (`staticFilteredSpotsCache`).
* Symmetry breaking via `spotIndexGrid` + `iterationSpotIndex`.
* In-place grid mutation with backtracking (replaced the per-node deep copy that
  `Archive.java` preserves).
* DGO group ordering fixed in `LevelRender`/`levelTest`.
* A native C++ solver prototype exists but is **not** at parity — it has no
  equivalent of the geometric `filter()` heuristics, so it explores far more of
  the space per placement. See the architecture doc for details, including an
  apparent double-dispatch issue in its `Grid::spreadLight`.
