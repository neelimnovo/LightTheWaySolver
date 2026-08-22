# Light The Way: Solver Architecture and Algorithms

This document details the architectural components, object interactions, and solving algorithms powering the `LightTheWaySolver` core engine.

## 1. Grid Setup and Architecture

The game environment is modeled as a 2D grid of [GridCell](file:///d:/Documents/Projects/LightTheWaySolver/src/model/GridCell.java#12-117) objects. Each cell in the grid manages up to four core states:

*   **`StaticGridObject`**: The immutable, structural foundation of the cell (e.g., Walls, Empty space, Receivers).
*   **[DynamicGridObject](file:///d:/Documents/Projects/LightTheWaySolver/src/model/interactionObjects/DynamicGridObject.java#9-19)**: A mutable, interactable item (e.g., Mirror, Prism) explicitly placed into the cell by the solver algorithm.
*   **[Receiver](file:///d:/Documents/Projects/LightTheWaySolver/src/model/interactionObjects/Receiver.java#5-20) Instance**: If the cell's static object is a Receiver, this object tracks the runtime power state (whether it has been hit by the correct light).
*   **[Light](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/Light.java#6-46) State**: A 16-bit packed primitive `short` representing the light currently occupying the cell. ` -1` denotes the absence of light.

## 2. Static and Dynamic Objects

The puzzle mechanics heavily rely on how light propagates through empty space and interacts with the various objects.

### Static Objects
Static objects are pre-defined by the level and cannot be moved or altered:
*   **`WALL`**: Completely blocks light and occupies a cell, preventing DGO placement.
*   **`EMPTY`**: Void space where light travels freely and DGOs can be safely placed.
*   **Receivers (`WHITE`, `RED`, `BLUE`, `YELLOW`)**: Goal targets. A puzzle is only solved when every receiver on the grid is powered by a light beam of a matching color.

### Dynamic Grid Objects (DGOs)
DGOs are interactable tools that alter light trajectories. They are placed into `EMPTY` spots by the solver. Crucially, their orientation is fixed upon level loading and cannot be rotated by the solver.
*   **[LightSource](file:///d:/Documents/Projects/LightTheWaySolver/src/model/interactionObjects/LightSource.java#13-166)**: Emits a beam of WHITE light in its fixed facing direction.
*   **[ForwardMirror](file:///d:/Documents/Projects/LightTheWaySolver/src/model/interactionObjects/ForwardMirror.java#13-105) (`/`) & [BackwardMirror](file:///d:/Documents/Projects/LightTheWaySolver/src/model/interactionObjects/BackwardMirror.java#14-129) (`\`)**: Reflects incoming light exactly 90 degrees. The new direction depends on the incoming light's trajectory and the tilt of the mirror.
*   **[Prism](file:///d:/Documents/Projects/LightTheWaySolver/src/model/interactionObjects/Prism.java#14-187)**: Refracts incoming WHITE light into three separate colored beams (RED, BLUE, YELLOW) emitted orthogonally and opposite to the prism's forward orientation.
*   **[Filter](file:///d:/Documents/Projects/LightTheWaySolver/src/model/interactionObjects/filters/Filter.java#16-184) (Red, Blue, Yellow)**: A semi-permeable object. It blocks incorrectly colored light, but allows WHITE light or identically colored light to pass through. WHITE light passing through takes on the filter's color.
*   **[ColourShifter](file:///d:/Documents/Projects/LightTheWaySolver/src/model/interactionObjects/ColourShifter.java#17-177)**: Absorbs incoming light of any valid color and re-emits it out its front face, completely changing the beam to its own fixed color.
*   **[TJunction](file:///d:/Documents/Projects/LightTheWaySolver/src/model/interactionObjects/TJunction.java#13-315)**: Splits an incoming beam of light into two identical beams, pushing them out perpendicularly in opposite directions.

---

## 3. Level Solving Algorithm (Brute-Force Permutations)

**There are now two solver implementations, and they are not at parity.** The
mature, default implementation is [LevelSolver.solveLevelOriginal()](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/LevelSolver.java#L141-L211)
(pure Java, in-process). As of commit `80cd9b6` ("Modify raycasting,
implement first draft of CPP solver and tester"), there is also
[LevelSolver.solveLevelCPP()](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/LevelSolver.java#L393-L441),
which shells out to a native C++ subprocess. Both share the same recursive
backtracking *shape*, but the C++ side is an early prototype with materially
weaker pruning — see §5.

### 3a. Java solver (`solveLevelOriginal`) — the production path

This is what `LevelRender` (GUI) and `levelTest` (CLI, `gradle runTest`)
actually call. It uses brute-force recursive backtracking, heavily enhanced
by heuristic filtering and symmetry breaking.

1. **Setup**: The solver is provided with the level's grid layout, a list of
   empty positions, a list of receiver positions, and a queue of dynamic grid
   objects, ordered by `LevelRender`/`levelTest` as `lights → tJunctions →
   prisms → colourShifters → filters → mirrors` before the queue is ever
   handed to the solver.
2. **Static pre-filtering (once, up front)**: [precomputeStaticFilters()](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/LevelSolver.java#L118-L136)
   calls each unique DGO's `staticFilter()` exactly once against the level's
   immutable walls/receivers, and caches the result in
   `staticFilteredSpotsCache`, keyed so that functionally-identical DGOs
   (same class + orientation/colour, per `areIdenticalDGOs`) share one cached
   list rather than recomputing it per instance.
3. **Dynamic spot filtering (per recursive node)**: Before placing a DGO,
   `dgo.filter(grid, baseSpots)` re-checks only the already-static-filtered
   subset against the *current* dynamic grid state (other DGOs already
   placed this branch). This is purely local/adjacency-based — every
   `filter()` implementation only inspects the up-to-4 immediate neighbor
   cells of a candidate spot (e.g. a [LightSource](file:///d:/Documents/Projects/LightTheWaySolver/src/model/interactionObjects/LightSource.java)
   won't be placed facing a wall or another light source; a
   [BackwardMirror](file:///d:/Documents/Projects/LightTheWaySolver/src/model/interactionObjects/BackwardMirror.java)
   won't be placed where both reflective sides are blocked). It cannot detect
   that a beam crosses open empty space and never reaches anything — there is
   no reachability/connectivity check anywhere in this solver.
4. **Recursive backtracking**: pulls the next DGO off the queue, iterates the
   filtered spots, tentatively assigns the DGO to the cell, recurses. On
   failure it nulls `cellDynamicItem` and tries the next spot (in-place
   mutation, not a grid copy — see the note on `Archive.java` below).
5. **Symmetry breaking**: identical DGOs (per `areIdenticalDGOs`) are given a
   canonical placement order via `spotIndexGrid` (an O(1) coordinate→index
   lookup) and `iterationSpotIndex` — once one instance of an identical pair
   is placed at spot index *k*, the next identical instance is restricted to
   spots at index ≥ *k*, eliminating permutation-equivalent duplicate states.
6. **Verification**: once the queue is empty, [projectLight()](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/LevelSolver.java#L257-L300)
   runs; if [allReceiversArePowered()](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/LevelSolver.java#L302-L315)
   is true, the grid is recorded as `solutionGrid` and the search unwinds
   with `true`.

**Historical note** (visible in `git log` / `src/searchLogic/Archive.java`):
an earlier version of this solver deep-copied the entire `GridCell[][]` on
every recursive call (`copyGridCellArray`/`updateGridCellArray`). Commit
`5e07b64` ("Fix big time consuming bug related to deep-copying") replaced
that with the current in-place-mutate-then-backtrack approach — any future
parallelization of this solver should copy the grid once per top-level
branch, not per node, to avoid reintroducing that cost.

### 3b. C++ solver prototype (`solveLevelCPP`) — early draft, not yet at parity

Invoked from Java via `ProcessBuilder`: `LevelSolver.solveLevelCPP()`
hand-serializes `emptySpots`/`receiverSpots`/`dgoQueue` to a small JSON
document, writes it to the stdin of a compiled `src/cpp/solver/solver`
executable, and hand-parses the JSON it writes back to stdout
(`serializeToJSON`/`deserializeFromJSON`, both bespoke string-scanning code —
there is no JSON library on either side of this bridge). `main.cpp` mirrors
this with its own hand-rolled parser. The `Solver`/`Grid` C++ classes
(`src/cpp/solver/Solver.{h,cpp}`, `Grid.{h,cpp}`) reimplement the packed-light
representation, `ShortQueue`, and the DGO class hierarchy in C++.

**Its recursive search (`Solver::solveRecursive`) has only two pruning
mechanisms: "is this spot already occupied?" and symmetry breaking (via the
same `iterationSpotIndex` scheme as Java).** It does **not** call any
equivalent of the Java solver's per-DGO `filter()`/`staticFilter()`
geometric heuristics — there is no C++ counterpart to
"LightSource can't face a wall" or "Filter can't be occluded on two sides."
The only compensating check found is in `Grid::emitLight`, which skips
emitting into a cell that directly contains another `LightSource` (a narrow,
special-cased substitute for one specific thing the Java `filter()` would
have caught more generally). **Practically, this means the C++ prototype
currently explores a far larger fraction of the naive permutation space per
placement than the Java solver does** — the two are not a fair like-for-like
speed comparison in their current state; the C++ side would need the
geometric filtering ported before its (likely faster, constant-factor-wise)
native execution translates into a faster *solve*.

`compileCppSolver()` in `levelTestCpp.java` recompiles the executable
(`g++ -std=c++17 -O2 -w -o solver *.cpp`) on every invocation of
`gradle runTestCpp`; `build.gradle` also exposes `compileCpp`,
`runCppSolver`, and `cleanCpp` as standalone tasks.

---

## 4. Light Spreading Algorithm — Java (`LevelSolver.projectLight`)

Because light propagation is simulated millions of times per second in the
tight inner loop of the permutation solver, the architecture deliberately
avoids Object-Oriented paradigms (like `new Light()`) to prevent Garbage
Collection (GC) pauses and memory allocation overhead.

1. **Bitwise `short` Packing**: Light is encapsulated into a 16-bit `short`
   ([Light.java](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/Light.java)).
   This single primitive tracks X (4 bits), Y (4 bits), Colour (2 bits), and
   FaceOrientation (2 bits). **Caveat**: 4 bits per axis caps addressable
   coordinates at 0-15 — a grid wider or taller than 16 cells would silently
   wrap coordinates in this packed format with no error raised.
2. **Primitive Traversal Queue**: BFS-style, backed by
   [ShortQueue](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/ShortQueue.java)
   (a custom array-backed circular queue of raw `short`, avoiding boxing).
   [emitLight()](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/LevelSolver.java#L345-L358)
   iterates `sourceSpots` (a `HashMap<LightSource, Pair<Integer,Integer>>`
   populated/cleared by `trackLightSources` as sources are placed/backtracked
   during search), creates one starting `short` per light source, writes it
   to the grid, and enqueues it.
3. **Propagation loop ([spreadLight](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/LevelSolver.java#L322-L342))**:
   pops one `short` per iteration and dispatches on the cell's *current*
   contents:
   *   **EMPTY, no DGO** → [incrementLight()](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/LevelSolver.java#L369-L381)
       advances **exactly one cell** and re-enqueues (see note below — this
       is a single-step advance, not a multi-cell raycast).
   *   **EMPTY, has DGO** → the DGO's `interactWithLight()` is invoked, which
       unpacks the `short`, applies its own logic (reflect/split/filter/
       shift), and enqueues whatever new beam(s) result.
   *   **not EMPTY, not WALL** (i.e. a receiver cell) → `grid[x][y].receiver.powerUp(light)`.
   *   **WALL** → the beam is dropped; nothing is enqueued for it (this check
       in `spreadLight` is now effectively a dead-code safety net, since
       `incrementLight` already refuses to enqueue *into* a wall cell — see
       below).
4. **`incrementLight` is a single-step advance, not a multi-cell raycast —
   despite its own comment.** The method's comment still reads *"Ray‑cast the
   light forward until it hits a WALL, a RECEIVER, or a DynamicGridObject"*,
   but as of commit `80cd9b6` the implementation only advances one cell per
   call:
   ```java
   private void incrementLight(short light, GridCell[][] grid) {
       // Ray‑cast the light forward until it hits a WALL, a RECEIVER, or a DynamicGridObject.
       int ord = Light.getOrientation(light).ordinal();
       int nx = Light.getX(light) + DX[ord];
       int ny = Light.getY(light) + DY[ord];
       if (GridLayout.isWithinBounds(this.gridWidth, this.gridHeight, nx, ny) && grid[nx][ny].cellStaticItem != WALL) {
           short incrementedLight = Light.create(nx, ny, Light.getColour(light), Light.getOrientation(light));
           grid[nx][ny].light = incrementedLight;
           lightProcessingQueue.add(incrementedLight);
       }
   }
   ```
   Every cell a beam crosses now round-trips through `ShortQueue` once,
   rather than being walked directly inside a tight loop the way an earlier
   revision did (that version advanced through consecutive empty cells
   in-loop, recording them for reset without touching the queue at all, and
   only enqueued once upon reaching an actual obstruction — see `git show
   27af3d3` for that version). Both are semantically equivalent (same final
   light distribution, same solved/unsolved outcome) — the difference is
   purely queue-operation volume per beam-path-length, so this reads as an
   intentional simplification (plausibly made to keep the Java and C++
   implementations structurally closer to each other) rather than a
   regression bug, but **the comment is now stale and should either be
   corrected or the loop restored** — a future reader will reasonably expect
   the multi-cell behavior the comment describes.
5. **Sparse reset via lit-cell tracking**: rather than wiping the whole grid,
   every `short` popped from the queue (line 268-270 of `projectLight`) has
   its X/Y recorded into parallel primitive arrays `litSpotX`/`litSpotY`
   (grown via `resizeLitSpotArrays()` as needed). On a failed permutation,
   only those recorded cells have `.light` reset to `-1`, and `litCount`
   resets to 0 — turning teardown from *O(grid area)* into *O(cells actually
   lit this attempt)*.

---

## 5. Light Spreading Algorithm — C++ prototype (`Grid::spreadLight`)

The C++ port (`src/cpp/solver/Grid.cpp`) does **not** mirror the current
Java single-step design — it reimplements something closer to the *older*
Java raycasting style, walking multiple empty cells per call, but combines
that with inline dispatch of the terminal interaction in a way that appears
to double-fire that interaction. This is worth flagging explicitly as an
open issue in what is a first-draft port, not a validated implementation:

```cpp
void Grid::spreadLight(uint16_t light, ShortQueue& lightQueue) {
    ...
    while (true) {
        int nx = x + DX[ord]; int ny = y + DY[ord];
        if (!isWithinBounds(nx, ny)) break;
        GridCell& nextCell = grid[nx][ny];
        if (nextCell.cellStaticItem == StaticGridObject::WALL) break;
        if (nextCell.receiver != nullptr && !nextCell.receiver->isPowered) {
            nextCell.receiver->powerUp(Light::getColour(light));
            break;                                    // interaction #1 (inline)
        }
        if (nextCell.cellDynamicItem != nullptr) {
            nextCell.cellDynamicItem->interactWithLight(light, grid, lightQueue, width, height);
            break;                                     // interaction #1 (inline)
        }
        nextCell.light = light; lastX = nx; lastY = ny; x = nx; y = ny; moved = true;
    }
    if (moved) {
        // re-enqueues a light ONE STEP BEHIND the obstruction just handled above
        uint16_t newLight = Light::create(lastX, lastY, Light::getColour(light), Light::getOrientation(light));
        grid[lastX][lastY].light = newLight;
        lightQueue.add(newLight);
    }
}
```

Walk-through of the apparent bug: when a beam crosses **one or more** empty
cells before reaching a wall/receiver/DGO, the obstruction is resolved
*inline* inside this same call (`powerUp` or `interactWithLight`, both of
which may themselves enqueue further beams). But because `moved` is then
`true`, the function *also* re-enqueues a light state at `(lastX, lastY)` —
the last empty cell, one step short of that same obstruction. When that
re-enqueued light is later popped and passed back into `spreadLight`, the
very first loop iteration recomputes `(nx, ny)` as the *same* obstruction
cell and dispatches the *same* interaction a second time. On that second
call `moved` stays `false` (zero cells crossed), so the duplication doesn't
cascade indefinitely — it's bounded to exactly one extra dispatch per beam
segment that travels ≥1 empty cell before its first obstruction, not an
unbounded loop.

**Effect on correctness vs. performance**: `Receiver.powerUp` is idempotent
(`isPowered` only ever gets set to `true`), so a duplicate call there can't
flip a correct "unsolved" into an incorrect "solved," and duplicate light
purely *adds* extra beams rather than removing real ones — so this should
not by itself produce a wrong solved/unsolved verdict. But it does mean
real, unnecessary duplicate work: every mirror/prism/T-junction/filter/
colour-shifter interaction reachable via a beam with ≥1 empty cells of
travel gets invoked twice, and each duplicate's downstream beams are
themselves subject to the same doubling if they too cross open space before
their next obstruction — compounding along any chain of reflections
separated by empty runs. This has not been benchmarked against the Java
solver's output on a shared level to confirm parity; that would be the
natural next step before relying on `solveLevelCPP` results.

---

## 6. Implemented Performance Optimizations (as of the current `master`)

Confirmed present in the code today (not aspirational — each has been
verified by reading the current source):

*   **Packed `short` light state** instead of an allocated `Light` object
    (eliminates the largest historical GC-pressure source in the light loop).
*   **Custom `ShortQueue`** (array-backed circular buffer of raw `short`)
    in place of a boxed-generic `Queue<Light>`.
*   **Batched lit-cell reset** (`litSpotX`/`litSpotY`) — *O(cells lit)*
    teardown instead of a full-grid scan.
*   **Static-filter precomputation and caching** (`precomputeStaticFilters`,
    `staticFilteredSpotsCache`), shared across functionally-identical DGOs.
*   **Symmetry breaking** via `spotIndexGrid` + `iterationSpotIndex`, avoiding
    redundant permutation-equivalent search branches.
*   **In-place grid mutation with backtracking**, replacing an earlier
    per-node deep-copy design (`Archive.java`, abandoned per commit
    `5e07b64`).
*   **DGO placement group-ordering** (`lights → tJunctions → prisms →
    colourShifters → filters → mirrors`) fixed in `LevelRender`/`levelTest`,
    front-loading the objects most useful for early pruning.
*   **A native C++ solver prototype** exists as an alternate, in-progress
    path (§3b) — not yet a drop-in performance win, since it currently lacks
    the geometric filtering that gives the Java solver most of its pruning
    power, and its light-spreading routine has the open double-dispatch
    question noted in §5.
