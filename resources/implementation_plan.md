# Performance Improvement Plan for LightTheWaySolver

## Problem Statement

The current [solveLevelOriginal()](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/LevelSolver.java#74-110) in [LevelSolver.java](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/LevelSolver.java) uses a recursive backtracking approach that explores permutations exhaustively. With **S** filtered spots per object and **N** dynamic objects, the search space is **O(S^N)** — growing catastrophically:

| Objects | Filtered Spots | Permutations | Est. Time @ 1M/sec |
|---------|---------------|--------------|---------------------|
| 6 | 30 | 729M | ~12 min worst case |
| 8 | 35 | 2.25 trillion | ~26 days |
| 10 | 40 | 10.5 quadrillion | ~333 years |

The user reports [projectLight](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/LevelSolver.java#112-148) consumes ~45% of solve time. The remaining ~55% is split between filtering, object placement, backtracking overhead, and grid resets.

---

## Strategy 1: Early Light Simulation (Pruning mid-placement) — ⭐ Highest Impact

### The Insight

Currently, light is **only projected after ALL objects are placed**, meaning the solver blindly places all N objects before discovering the configuration is hopeless. This is the single biggest source of wasted computation.

### Proposed Change

**Run partial light simulation after placing critical objects (light sources, prisms, T-junctions)** and prune entire subtrees if the partial simulation reveals impossibility:

1. **After placing a LightSource**: Immediately simulate its light beam. If the beam hits a wall or goes out of bounds without encountering any useful interaction cell, this entire placement subtree is dead — prune it.

2. **After placing all LightSources**: Run full light projection on the partially-placed grid. If the light cannot physically reach certain receivers (they are in unreachable grid regions), prune.

3. **After placing a Prism/TJunction**: The light beam changes direction. Re-simulate the affected light path. If the redirected beam immediately dead-ends (hits wall/boundary with no useful cells between), prune.

4. **Receiver reachability check**: Before each placement iteration, compute which receivers can possibly be reached by any light path given the current partial placement. If any receiver is unreachable, prune immediately.

### Implementation

```
// In solveLevelOriginal, after placing a DGO:
cell.cellDynamicItem = dgo;

// NEW: Early pruning check
if (dgo instanceof LightSource || dgo instanceof Prism || dgo instanceof TJunction) {
    if (!earlyLightCheck(grid)) {
        // This placement leads to a dead end - skip entire subtree
        cell.cellDynamicItem = null;
        continue; // next spot
    }
}

boolean isSolutionFound = solveLevelOriginal(grid, emptySpots, dgoQueue);
```

The `earlyLightCheck` method would:
- Project light from all currently-placed light sources
- Check if light reaches at least one cell adjacent to each receiver (or can potentially reach via unplaced objects)
- Reset light state after check

### Estimated Speedup

**10x–1000x** depending on how many subtrees get pruned early. For 8-10 object levels, this can eliminate the vast majority of futile branches. This is the single most impactful change because it attacks the exponential growth at its root.

### Complexity to Implement: Medium

Requires a fast "partial light simulation" that works on incomplete grids, plus logic to reset light state after checking.

---

## Strategy 2: Smarter DGO Placement Order (Heuristic Reordering)

### The Insight

The current code processes DGOs in whatever order they appear in the `dgoQueue`. But **placement order dramatically affects pruning effectiveness**:

- **LightSources should be placed FIRST**: They are the origin of all light. Placing them first means you can immediately project light and detect dead-end paths.
- **Prisms/TJunctions next**: They redirect light, so placing them second lets you extend the partial light simulation.
- **Colour-sensitive objects (Filters, ColourShifters) last**: They only modify light properties, so they have the most constraints and fewest valid placements when other objects are already placed.

### Proposed Ordering

```
1. LightSources        — establish light origins
2. Prisms              — split/redirect light
3. TJunctions          — split light
4. ForwardMirrors       — redirect light
5. BackwardMirrors      — redirect light  
6. ColourShifters       — modify colour + redirect
7. Filters (R/B/Y)     — filter by colour (most constrained)
```

### Additional Heuristic: Most-Constrained-First within groups

Within each group, place the DGO that has the **fewest valid filtered spots first**. This is a classic CSP (Constraint Satisfaction Problem) heuristic — "Minimum Remaining Values (MRV)". It minimizes branching factor at the top of the search tree.

```java
// Before recursive call, sort remaining DGOs by filtered spot count
dgoQueue.sort((a, b) -> {
    int aSpots = a.filter(grid, emptySpots).size();
    int bSpots = b.filter(grid, emptySpots).size();
    return Integer.compare(aSpots, bSpots);
});
```

### Estimated Speedup

**2x–10x** from better ordering alone. Combined with Strategy 1 (early simulation), the benefit compounds dramatically since early-placed light sources enable early pruning.

### Complexity to Implement: Low

Just reorder the `dgoQueue` before starting the solve.

---

## Strategy 3: CPU Multi-threading (Parallelized Search)

### The Insight

The recursive backtracking is embarrassingly parallelizable at the top level: each placement of the **first** DGO into a different filtered spot creates an **independent subtree** that can be solved concurrently.

### Proposed Architecture

```
Main Thread:
  1. Place first DGO into spot #1 → spawn Task for subtree
  2. Place first DGO into spot #2 → spawn Task for subtree
  3. ... (for all filtered spots of first DGO)

Thread Pool (ForkJoinPool or ExecutorService):
  - Each task runs solveLevelOriginal on its subtree
  - First task to find a solution signals cancellation to others
  - Each task operates on its own DEEP COPY of the grid
```

### Key Design Decisions

1. **Grid isolation**: Each thread needs its own copy of the `GridCell[][]` array. The current [GridCell](file:///d:/Documents/Projects/LightTheWaySolver/src/model/GridCell.java#12-87) uses mutable state (`cellDynamicItem`, `light`, `receiver.isPowered`), so each thread must work on an independent copy.

2. **Depth of parallelism**: Parallelizing only at the **first DGO level** gives you ~30 tasks (number of filtered spots for the first DGO). This is sufficient for 6-12 CPU cores. Going deeper creates too many tasks with too much overhead.

3. **Cancellation**: Use `AtomicBoolean solutionFound` shared across tasks. Each task checks it periodically and returns early if another task found the solution.

4. **State management**: The `sourceSpots` HashMap, `lightProcessingQueue`, and timing counters must all be thread-local (each task has its own LevelSolver instance).

### Implementation Sketch

```java
public GridCell[][] solveLevelParallel(GridCell[][] grid, 
        ArrayList<Pair<Integer, Integer>> emptySpots,
        LinkedList<DynamicGridObject> dgoQueue) {
    
    DynamicGridObject firstDgo = dgoQueue.remove();
    ArrayList<Pair<Integer, Integer>> filteredSpots = firstDgo.filter(grid, this.emptySpots);
    
    AtomicBoolean found = new AtomicBoolean(false);
    AtomicReference<GridCell[][]> solution = new AtomicReference<>();
    
    ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    List<Future<?>> futures = new ArrayList<>();
    
    for (Pair<Integer, Integer> spot : filteredSpots) {
        futures.add(pool.submit(() -> {
            if (found.get()) return;
            // Deep copy grid, create fresh solver instance
            GridCell[][] gridCopy = GridLayout.copyGridCellArray(grid);
            LevelSolver localSolver = new LevelSolver(receiverSpots, emptySpots);
            // Place first DGO
            gridCopy[spot.getKey()][spot.getValue()].cellDynamicItem = firstDgo;
            // Solve remaining in this subtree
            LinkedList<DynamicGridObject> remainingDgos = new LinkedList<>(dgoQueue);
            if (localSolver.solveLevelOriginal(gridCopy, emptySpots, remainingDgos)) {
                found.set(true);
                solution.set(localSolver.solutionGrid);
            }
        }));
    }
    // Wait for first solution or all tasks to complete
    ...
}
```

### Estimated Speedup

**4x–8x** on a modern CPU (6-12 cores, accounting for overhead). Near-linear scaling with core count since subtrees are independent.

### Complexity to Implement: Medium-High

Requires careful grid deep-copying, thread-safe state management, and proper cancellation logic. The shared mutable state in [GridCell](file:///d:/Documents/Projects/LightTheWaySolver/src/model/GridCell.java#12-87) (especially `light` and `receiver.isPowered`) must be isolated per thread.

---

## Strategy 4: Light Projection Micro-Optimizations

Since [projectLight](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/LevelSolver.java#112-148) is ~45% of total time, even small constant-factor improvements yield significant gains.

### 4a. Eliminate Object Allocation in Hot Path

**Current**: Every light step creates a `new Light(...)` and `new Pair<>(...)` object. At millions of iterations, this creates enormous GC pressure.

**Fix**: Use primitive arrays or pre-allocated object pools instead.

```java
// Instead of: new Light(colour, orientation, x, y)
// Use a pre-allocated ring buffer of Light objects
Light reusedLight = lightPool[poolIndex++];
reusedLight.colour = colour;
reusedLight.orientation = orientation;
reusedLight.xPos = x;
reusedLight.yPos = y;
```

Alternatively, represent light state as packed integers:
```java
// Pack light state into a single int: colour(2 bits) | orientation(2 bits) | x(12 bits) | y(12 bits)  
int lightState = (colour << 26) | (orientation << 24) | (x << 12) | y;
```

### 4e. Batch Light Reset

[resetLightInGridCellArray](file:///d:/Documents/Projects/LightTheWaySolver/src/model/GridLayout.java#82-89) iterates the entire grid to null out light. For large grids, this is wasteful. Instead, track which cells received light and only reset those:

```java
ArrayList<int[]> litCells = new ArrayList<>(); // track during projection
// During light setting:
grid[x][y].light = light;
litCells.add(new int[]{x, y});
// Reset only lit cells:
for (int[] cell : litCells) {
    grid[cell[0]][cell[1]].light = null;
}
```

### 4f. Remove Timing Code in Hot Path

The `System.currentTimeMillis()` calls in [spreadLight](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/LevelSolver.java#166-197), [incrementLight](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/LevelSolver.java#228-268), [emitLight](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/LevelSolver.java#198-220), and [allReceiversArePowered](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/LevelSolver.java#149-165) add measurable overhead when called millions of times. Move these to a debug mode or remove them entirely during solving.

### Estimated Combined Speedup

**2x–4x** from all micro-optimizations combined.

### Complexity to Implement: Low-Medium

Each optimization is small and independent. Can be applied incrementally.

---

## Strategy 5: Constraint Propagation (CSP Solver Approach)

### The Insight

The puzzle is fundamentally a **Constraint Satisfaction Problem**. The current approach treats it as brute-force search with filtering. A proper CSP solver uses techniques that can prune the search space far more aggressively:

### 5a. Arc Consistency / Forward Checking

After placing each DGO, immediately filter the remaining DGOs' valid spots based on the new constraint. The current code already does this partially (each [filter()](file:///d:/Documents/Projects/LightTheWaySolver/src/model/interactionObjects/BackwardMirror.java#22-67) checks the grid state), but it could be more aggressive:

- When a mirror is placed, any DGO that **must** be in the mirror's reflection path has its domain narrowed
- When a filter is placed, colour-mismatched DGOs in the light path are eliminated

### 5b. Light Path Connectivity Analysis

Before any placement, analyze the grid topology:
1. From each light source orientation, trace all **possible** light paths (considering all possible mirror/prism placements)
2. For each receiver, compute which grid cells could potentially route light to it
3. Intersect these — cells that are on NO useful path can never contain a useful object placement

This "reachability map" can eliminate 30-50% of filtered spots before search even begins.

---

