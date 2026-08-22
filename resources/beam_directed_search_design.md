# Beam-Directed Search — Implementation Plan

Design document for item **A1** of `implementation_plan.md`. This is the structural
fix for the measured 43,054x simulation redundancy, and the reason `Test Slow.json`
takes ~28 seconds to solve something a human solves by eye.

---

## 1. Goal and success criteria

| | Current (`solveLevelOriginal`) | Target (`BeamSolver`) |
|---|---|---|
| `Test Slow.json` | 275,806,357 leaf simulations, ~28 s | < 500,000 search nodes, < 50 ms |
| `Level 015.json` | 3,135,717 leaves, 457 ms | no regression; expected < 100 ms |
| Distinct light outcomes per level | 6,406 out of 275.8 M leaves | ~1 outcome per node, by construction |
| Every other level in `src/saveFiles/` | baseline | identical solved / not-solved verdict |

Hard requirement: **the old solver stays intact and stays the default** until the
differential harness (§9) is green on every save file. BDS ships behind `-Pbeam`.

---

## 2. Why the current search is redundant

`solveLevelOriginal` enumerates *placements*: it assigns every object to a cell, then
calls `projectLight` on the finished grid. But the level's outcome depends only on the
objects the light actually **touches**. On an open level almost every object is off-beam,
so millions of distinct placements produce the byte-identical light field — 43,054 of
them per distinct outcome on `Test Slow`, with `Receiver hits: 1` across the whole run.

BDS inverts the loop. Instead of *place everything, then trace*, it *traces, and places
only where the trace actually arrives*. Off-beam objects are never permuted at all; they
are dropped into any free cell at the end ("parking").

---

## 3. The two lemmas BDS rests on

**L1 — Off-beam invariance.** Light propagation reads `cellStaticItem`,
`cellDynamicItem` and nothing else, and only for cells a beam enters. Therefore two grids
that agree on every cell the beam enters produce identical light fields and identical
receiver states. *Objects on cells no beam enters cannot affect the result.*

**L2 — Light monotonicity.** `Receiver.powerUp` only ever sets `isPowered = true`; no rule
penalises extra light or wrong-coloured light. Therefore if light field `A ⊇ B`, then
`powered(A) ⊇ powered(B)`. *Adding light is never harmful.*

L1 gives correctness of parking. L2 gives correctness of the "never place an object that
merely absorbs a beam" pruning (§7.4) — removing such an object only ever adds light.

Both lemmas are worth asserting in the differential harness rather than trusted on faith;
see §9.

---

## 4. Architecture — three new files, four touched

**New**

| File | Responsibility |
|---|---|
| `src/searchLogic/LightPhysics.java` | Transition table: `(typeId, inDir, inColour) -> up to 3 output beams`. **Derived at runtime from the existing `interactWithLight` implementations**, so it cannot drift from the engine. |
| `src/searchLogic/ObjectMultiset.java` | Distinct object types + remaining counts, replacing the `LinkedList<DynamicGridObject>` permutation queue. |
| `src/searchLogic/BeamSolver.java` | The search: phase 1 source enumeration, phase 2 beam-directed placement, trail-based undo. |

**Touched**

| File | Change |
|---|---|
| `LevelSolver.java` | `public boolean solveLevelBeamDirected(...)` delegating to `BeamSolver`; keeps the `solutionGrid` / `attemptPermutations` contract. |
| `build.gradle` | `-Pbeam` -> `systemProperty 'solver.beam', 'true'`. |
| `tests/levelTest.java` | Dispatch on the flag; add the differential mode of §9. |
| `SolverProfiler.java` | BDS counter block (§10). |

---

## 5. The physics table (`LightPhysics`)

### 5.1 The invariant that makes a table possible

Every emitting object in the codebase writes its output into **the cell adjacent in the
output beam's own direction**. Verified case by case:

- `BackwardMirror` `\`: UP→LEFT@(x-1,y), DOWN→RIGHT@(x+1,y), LEFT→UP@(x,y-1), RIGHT→DOWN@(x,y+1)
- `ForwardMirror` `/`: UP→RIGHT@(x+1,y), DOWN→LEFT@(x-1,y), LEFT→DOWN@(x,y+1), RIGHT→UP@(x,y-1)
- `Filter`: same direction, adjacent in that direction; colour becomes the filter colour
- `ColourShifter`: always emits in **its own** orientation, adjacent in that orientation
- `Prism` (all 4 orientations): the RED/BLUE/YELLOW triple each land adjacent in their own direction
- `TJunction` (all 4): both outputs land adjacent in their own direction
- `LightSource`: no output (blocks)

So an output beam is fully described by `(outDir, outColour)`; the target cell is implied.
That collapses the whole physics into a small lookup table.

### 5.2 Derive it, do not re-implement it

Re-typing those switch statements into a table by hand is exactly how the `TJunction`
tautology (`!= LEFT || != RIGHT`) survived. Instead, build the table by **calling the real
objects**:

```java
// 3x3 scratch grid, object at the centre: every output lands in bounds.
for (DynamicGridObject proto : distinctPrototypesInThisLevel) {
    int t = internTypeId(proto.getTypeId());
    for (FaceOrientation d : FaceOrientation.CACHED_VALUES) {
        for (Colour c : Colour.CACHED_VALUES) {
            scratchQueue.clear();
            proto.interactWithLight(Light.create(1, 1, c, d), scratchGrid, scratchQueue);
            // whatever landed in the queue IS the truth
            TABLE[t][d.ordinal()][c.ordinal()] = packOutputs(scratchQueue);
        }
    }
}
```

`packOutputs` stores `count | out0<<8 | out1<<16 | out2<<24`, each `outN` being
`(dirOrdinal << 2) | colourOrdinal`. While packing, assert that each queued light really
does sit adjacent in its own direction — that turns the §5.1 invariant into something
checked rather than assumed.

Table size: `types (≤29) x 4 dirs x 4 colours` = 464 ints. Permanently cache-resident.

### 5.3 Type identity for free

`DynamicGridObject.getTypeId()` already returns a stable string per (class, orientation,
colour): `"backMirror"`, `"upPrism"`, `"leftYellowShift"`. Case by case it agrees exactly
with `areIdenticalDGOs`. Intern those strings to small ints once at setup and BDS gets:

- the physics-table key,
- the multiset key — so identical objects collapse structurally instead of via
  `iterationSpotIndex`,
- and a drop-in replacement for the hot `instanceof` chain (micro-opt **M3**, 302 M calls).

---

## 6. Data structures

All grid-indexed state is flat `byte[]`/`int[]` of size `gridWidth * gridHeight`, indexed
`cell = x * gridHeight + y`.

```java
byte[]    cellState;    // FREE=0, PASSED=1, OBJECT=2  (walls/receivers read from the static grid)
int[]     objectAt;     // typeId placed on that cell, or -1
short[]   frontier;     // pending beams, packed exactly like Light
int       frontierSize;
int[]     remaining;    // remaining[typeId] = how many are still unplaced
int       remainingTotal;
int       freeCells;    // EMPTY, unoccupied and unPASSED — i.e. parking capacity
boolean[] powered;      // per receiver index
int       poweredCount;
long[]    visitedBits;  // 4096-bit set over the 12-bit packed light state, for cycle cutting
int[]     trail;        // tagged undo log
int       trailTop;
```

**Trail entries** (tag in the low 3 bits, payload above):

| Tag | Payload | Undo |
|---|---|---|
| `T_PASS` | cell index | `cellState[c]=FREE; freeCells++` |
| `T_PLACE` | cell index, typeId | `cellState[c]=FREE; objectAt[c]=-1; remaining[t]++; remainingTotal++; freeCells++` |
| `T_POWER` | receiver index | `powered[r]=false; poweredCount--` |
| `T_VISIT` | 12-bit light state | clear the bit |

Every recursion level saves `trailTop` and `frontierSize` on entry and rewinds both on
exit. No allocation anywhere in the search loop.

---

## 7. The algorithm

### 7.1 Phase 1 — light sources are enumerated positionally

A `LightSource` is the one object whose placement matters *off* the beam, because it
creates a beam. So sources are not beam-directed; they are enumerated the old way, over
`staticFilteredSpotsCache` with the existing symmetry-breaking index. Everything else is
handed to phase 2.

```
placeSources(k):
    if k == sourceCount:
        seed the frontier from every source (mirroring emitLight)
        return beamSearch()
    for each candidate spot for source k (ordered, see 7.6):
        if occupied: continue
        place; if placeSources(k+1): return true; unplace
    return false
```

For `Test Slow` this is a single loop over ~56 filtered spots.

### 7.2 Phase 2 — the beam search

```
beamSearch():
    if frontierSize == 0:
        return terminalCheck()
    save frontierSize, trailTop
    beam = frontier[--frontierSize]
    result = walk(Light.getX(beam), Light.getY(beam),
                  Light.getOrientation(beam), Light.getColour(beam))
    rewind(frontierSize, trailTop)
    return result
```

### 7.3 `walk` — advance a ray, branching at every free cell it enters

```
walk(x, y, dir, colour):
    loop:
        state = pack(x, y, dir, colour)
        if visited(state): return beamSearch()     // cycle: this beam adds nothing
        markVisited(state)                         // trailed

        sgo = staticGrid[x][y]
        if sgo == WALL:     return beamSearch()    // beam dies
        if sgo is RECEIVER: powerUp(receiverIndexAt(x,y), colour);  // trailed
                            return beamSearch()

        if cellState[cell] == OBJECT:              // deterministic, no choice
            push outputs of LightPhysics[objectAt[cell]][dir][colour]
            return beamSearch()

        if cellState[cell] == FREE:
            // --- branches 1..n : place one of each remaining distinct type here ---
            for t in typesWithRemaining:
                out = LightPhysics[t][dir][colour]
                if count(out) == 0: continue                 // absorbing placement, see 7.4
                if !placementAllowedAt(t, cell): continue    // optional filter, see 7.5
                place(t, cell)                               // trailed
                push out
                if beamSearch(): return true
                rewind to this decision point
            // --- branch n+1 : let the beam pass through ---
            markPassed(cell)                       // trailed; freeCells--
        // cellState == PASSED falls through here too: light crosses, nothing may be placed

        x += DX[dir]; y += DY[dir]                 // walls surround every level, so no bounds check
```

Note the `PASSED` fall-through. A cell an earlier beam crossed is *light-transparent but
placement-locked*. That single rule is what keeps a multi-beam grid self-consistent: no
branch can retroactively invalidate a ray another branch already traced.

### 7.4 Terminal check and parking

```
terminalCheck():
    if poweredCount < receiverCount: return false
    if remainingTotal > freeCells:   return false   // nowhere left to park
    park every remaining object on arbitrary FREE cells
    return true                                     // L1: parked objects are off-beam
```

Parking is unconditionally safe by **L1**, and by construction can never include a
`LightSource` — those were all placed in phase 1.

Skipping absorbing placements (`count(out) == 0`) is safe by **L2**: parking such an
object instead only lets the beam continue, which can only add light and therefore only
power more receivers. Guard it with `solver.beam.blockers` so the assumption can be
flipped off if a level ever contradicts it.

### 7.5 Filters are advisory here, not load-bearing

`implementation_plan.md` §C3b records that several `filter()`/`staticFilter()`
implementations are over-strict. BDS must not inherit that. `placementAllowedAt` starts as
`return true`; the static filters get wired in afterwards as an opt-in (`-Pbeamfilter`)
and only once the differential harness proves they change no verdict. A filter-free BDS
also doubles as a second, independent `-Pnofilter` ground truth.

### 7.6 Source ordering — the cheap heuristic that finishes `Test Slow`

`Test Slow`'s receiver at (9,9) has exactly one non-wall neighbour, (9,8), and that cell
is empty-spot index **63 of 64** — dead last in scan order. Even a fast BDS would grind
through 63 hopeless source positions first.

Precompute each receiver's **entry cells** (adjacent non-wall cells) and order the phase-1
candidates so that cells adjacent to, or on a clear straight line to, a receiver come
first. Pure ordering: no pruning, no completeness risk — and `Test Slow` then succeeds on
roughly the first attempt.

The full version of this is **A2 receiver back-chaining**: a receiver whose entry set has
size 1 *forces* an emitter into that one cell, which prunes rather than reorders. Worth
doing, but only after M1–M5 of §11 are green.

---

## 8. Edge cases and how each is handled

| Case | Handling |
|---|---|
| Beam cycle (`/` + `\` rectangle) | `visitedBits` over the 12-bit packed state; a repeat kills the beam. Also fixes latent bug **C4b** in the old solver. |
| Prism / TJunction splitting | Table returns 2 or 3 outputs; all pushed to the frontier. |
| Prism fed wrong colour or direction | Table returns 0 outputs = absorbing = pruned by §7.4. |
| Multiple light sources | Phase 1 enumerates the combination; identical sources reuse the existing spot-index symmetry break. |
| A source blocking another source's beam | Sources are on the grid before any beam is traced, so the walk meets them as `OBJECT` with 0 outputs. |
| Output beam lands on a `PASSED` cell | Legal — it crosses. `PASSED` forbids *placement*, not light. |
| Output beam lands on an `OBJECT` cell | Deterministic interaction; the next loop iteration handles it. |
| Wrong-coloured light on a receiver | `powerUp` no-ops and the beam still dies there, matching `spreadLight`. |
| Out of bounds | Impossible: every level is wall-surrounded (commit `7759d07`), and `Light` packs 4 bits per axis so grids must stay ≤ 16 wide anyway (**M9**). |
| Zero objects left mid-search | Fine — beams keep propagating, only the pass branch remains. |

---

## 9. Validation — the part that must not be skipped

**9.1 Physics table self-test.** For every `(type, dir, colour)`, compare the table entry
against a live `interactWithLight` call on a scratch grid. The table is derived from that
call, so this asserts the packing and the §5.1 adjacency invariant, not the physics itself.

**9.2 Solution re-verification (always on, even in release).** When BDS reports success,
run the finished grid through the **existing** `projectDraftedLight`. If that does not
report every receiver powered, BDS is wrong — fail loudly rather than print a bad grid.
It costs one simulation per solve and turns any completeness bug into an immediate hard
error instead of a silently wrong answer.

**9.3 Differential harness.** New `tests/SolverDiff.java`: for every file in
`src/saveFiles/`, run both solvers and compare the **verdict** (solved / not solved), not
the grid — BDS legitimately returns a different but equally valid placement. Levels too
slow under the old solver get a time budget and are compared only where the old solver
finishes. Levels that currently report "no solution" (`Level 029-easier`) must still
report "no solution"; if BDS finds one, §9.2 proves it and we have learned something real
about §C3.

**9.4 Known-solution levels.** `src/solutionFiles/` holds stored solutions; assert BDS
solves each corresponding level.

---

## 10. Profiler counters to add

Mirroring the existing block, all behind `SolverProfiler.ENABLED`:

```
beamNodes            // beamSearch() entries
beamWalkCells        // cells advanced through
beamPlacements       // objects placed on-beam
beamPasses           // pass-through decisions
beamAbsorbPrunes     // placements skipped by 7.4
beamCyclesCut        // visited-state hits
beamTerminalChecks   // frontier emptied
beamParkFailures     // terminal checks that failed for lack of parking space
beamSourceSeeds      // phase-1 source combinations tried
```

The headline number is `beamTerminalChecks` versus the old `leafEvaluations`: that ratio
is the redundancy actually eliminated. Keep `-Pdedupe` working so the distinct-outcome
count can be compared directly against the old 6,406.

---

## 11. Milestones

| | Deliverable | Acceptance |
|---|---|---|
| **M1** | `LightPhysics` + type interning | §9.1 self-test passes for every type in every save file |
| **M2** | `BeamSolver`: single source, mirrors only, no pruning, no cycle handling | `Level 002` and `Level 015` solve; §9.2 verification passes |
| **M3** | Parking + `remaining`/`freeCells` accounting | `Test Slow` solves; node count measured |
| **M4** | Prism / TJunction / Filter / ColourShifter via the table | Levels using them match the old verdict |
| **M5** | Cycle cutting | A hand-built `/`+`\` loop level terminates |
| **M6** | Multi-source phase 1 + symmetry break | Multi-source levels match |
| **M7** | Differential harness green (§9.3) | **BDS may now become the default** |
| **M8** | Source ordering heuristic (§7.6) | `Test Slow` under ~5 ms |
| **M9** | Optional: static filters as pruning, transposition table, A2 back-chaining | Each must keep §9.3 green |

M1–M3 alone are what fixes the reported problem. M7 is the gate for switching the default.

---

## 12. Expected numbers for `Test Slow`

The level is a 7x8 open room plus a one-cell corridor: one DOWN `LightSource`, five
`BackwardMirror`s, one WHITE receiver at (9,9), 64 empty spots.

A `\` mirror maps DOWN→RIGHT and RIGHT→DOWN, so from a downward source the beam can only
staircase toward the bottom-right — it can never reach (9,8), whose other neighbours are
all walls. The single solution is therefore the source *at* (9,8) firing straight into the
receiver, with all five mirrors parked.

Per wrong source position, BDS explores the staircase routings with ≤ 5 turns: on the
order of `sum_k C(7,k)*C(8,k)` for k ≤ 5, a few thousand nodes. Across ~56 filtered source
spots that is **a few hundred thousand O(1) nodes**, against 275,806,357 full light
simulations today. With §7.6 ordering the correct source spot is tried among the first few
and the search ends almost immediately.

---

## 13. Risks and open questions

1. **L2 (monotonicity) is only as true as the rules.** If a future rule punishes stray
   light, or requires a receiver to be lit by exactly one beam, §7.4's absorb-pruning
   breaks. Hence the `solver.beam.blockers` escape hatch.
2. **Duplicate work across beam orderings.** Two frontier orders can reach the same final
   grid. That costs time, never correctness. A transposition table keyed on (placement
   bitboard, frontier signature) is the fix if profiling shows it matters.
3. **Tightly-constrained levels may not improve.** Where nearly every object is on-beam,
   BDS approaches the old search's work. It should not be *slower*, but M7 must confirm
   that on `Level 025` / `Level 027` before the default flips.
4. **`attemptPermutations` changes meaning.** BDS counts search nodes, not permutations, so
   `Stats.permutationRatio` and the UI stats column become incomparable across solvers.
   Either label them per-solver or stop writing the ratio for BDS runs.
5. **`Level 029-easier` stays unsolved.** BDS is not expected to change that verdict — the
   exhaustive `-Pnofilter` run already proved no placement solves it. If BDS *does* find
   one, §9.2 will prove it, and the `-Pnofilter` path has a bug worth chasing.
