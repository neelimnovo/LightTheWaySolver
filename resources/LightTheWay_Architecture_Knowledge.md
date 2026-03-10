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

The solver uses a brute-force algorithmic approach embedded inside `LevelSolver.solveLevelOriginal()`, heavily enhanced by heuristic filtering, recursive backtracking, and symmetry breaking.

### Core Workflow
1. **Setup**: The solver algorithm is provided with the level's grid layout, a list of empty positions on that grid, a list of receiver positions, and a queue of dynamic grid objects.
2.  **Spot Filtering**: Before attempting to place a DGO, the solver filters a smaller list of `EMPTY` spots by calling `dgo.filter()`. This filters out logically invalid placements. For example, a [LightSource](file:///d:/Documents/Projects/LightTheWaySolver/src/model/interactionObjects/LightSource.java#13-166) will not be placed directly facing a wall, and a [BackwardMirror](file:///d:/Documents/Projects/LightTheWaySolver/src/model/interactionObjects/BackwardMirror.java#14-129) will not be placed where both its reflective sides face blocked paths, minimizing dead-end permutations early.
3.  **Recursive Backtracking**: 
    *   The solver pulls the next DGO from the queue.
    *   It iterates over the valid, filtered spots, tentatively assigning the DGO to the grid cell.
    *   It then recurses deeper to place the remaining DGOs, until they are all placed
    *   It then tries to verify if the solution is valid by projecting light, and checking if all the receivers are powered on.
    *   If a recursion branch fails to result in a powered grid, the solver backtracks, lifts the object off the grid, and tries the next filtered spot.
4.  **Symmetry Breaking**: Permutations scale exponentially (e.g. 30 spots for 6 objects equals $30^6$ or 729 million permutations). If the DGO queue contains identical objects (e.g., two Red Filters with no orientation), swapping the placed coordinates of two identical objects is logically equivalent. To prevent processing identical states, the algorithm enforces a canonical placement order: the second identical DGO is strictly limited to empty spots that logically occur *after* the spot chosen for the first identical DGO.
5.  **Verification**: When all DGOs are successfully placed, [projectLight()](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/LevelSolver.java#207-265) is invoked to spread light on the grid. If [allReceiversArePowered()](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/LevelSolver.java#266-281) evaluates to true, the combination is recorded as a solution.

---

## 4. Light Spreading Algorithm

Because light propagation is simulated millions of times per second in the tight inner loop of the permutation solver, the architecture deliberately avoids Object-Oriented paradigms (like `new Light()`) to prevent Garbage Collection (GC) pauses and memory allocation overhead.

### Core Workflow (`LevelSolver.projectLight`)
1.  **Bitwise `short` Packing**: Light is encapsulated into a 16-bit `short`. This single primitive securely tracks:
    *   X Coordinate (4 bits)
    *   Y Coordinate (4 bits)
    *   Color (2 bits)
    *   FaceOrientation (2 bits)
2.  **Primitive Traversal Queue**: The propagation uses a Breadth-First-Search style queue. [emitLight()](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/LevelSolver.java#309-326) finds all [LightSource](file:///d:/Documents/Projects/LightTheWaySolver/src/model/interactionObjects/LightSource.java#13-166)s, creates `short` light primitives, updates the grid, and queues them into a [ShortQueue](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/ShortQueue.java#3-53). [ShortQueue](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/ShortQueue.java#3-53) is a custom-built array-backed primitive queue that dodges Java Generics auto-boxing.
3.  **Propagation Loop ([spreadLight](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/LevelSolver.java#282-308))**: 
    *   While the [ShortQueue](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/ShortQueue.java#3-53) is not empty, a light `short` is popped out.
    *   The simulation inspects the [GridCell](file:///d:/Documents/Projects/LightTheWaySolver/src/model/GridCell.java#12-117) at the light's X/Y coordinates.
    *   If **EMPTY**: Light increments functionally forward by 1 cell ([incrementLight()](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/LevelSolver.java#334-349)) and the new state is queued.
    *   If **DYNAMIC**: The specific [DynamicGridObject](file:///d:/Documents/Projects/LightTheWaySolver/src/model/interactionObjects/DynamicGridObject.java#9-19) invokes [interactWithLight](file:///d:/Documents/Projects/LightTheWaySolver/src/model/interactionObjects/DynamicGridObject.java#15-16). The DGO unpacks the `short`, executes its unique logic (e.g. splitting, reflecting, filtering), repacks new `short` lights, records them in the grid, and pushes them to the [ShortQueue](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/ShortQueue.java#3-53).
    *   If **RECEIVER**: The receiver checks if the light's encoded color satisfies its requirements.
    *   If **WALL**: Propagation strictly halts for that beam.
4.  **Sparse-Matrix Fast Rest**: To tear down the light simulation when a permutation fails, the entire grid array is *not* wiped. As elements are popped off the [ShortQueue](file:///d:/Documents/Projects/LightTheWaySolver/src/searchLogic/ShortQueue.java#3-53), their X and Y coordinates are tracked in fast, primitive parallel arrays (`litX` and `litY`). When resetting, the loop only targets precisely the [GridCell](file:///d:/Documents/Projects/LightTheWaySolver/src/model/GridCell.java#12-117) coordinates listed in `litX/Y`, switching their light state back to `-1`. This dramatically reduces tear-down complexity from $O(\text{GridArea})$ to $O(\text{PathLength})$, saving massive CPU cycles on 1-million+ permutation counts.
