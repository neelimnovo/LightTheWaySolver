package searchLogic;

import javafx.util.Pair;
import model.GridCell;
import model.GridLayout;
import model.interactionObjects.filters.*;
import model.interactionObjects.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.math.BigInteger;
import java.io.*;
import java.util.concurrent.*;

import static model.interactionObjects.StaticGridObject.*;
import static model.interactionObjects.Colour.*;

public class LevelSolver {
    // Direction delta lookup tables indexed by FaceOrientation.ordinal()
    // UP=0, DOWN=1, LEFT=2, RIGHT=3
    private static final int[] DX = {0, 0, -1, 1};
    private static final int[] DY = {-1, 1, 0, 0};

    private final int gridWidth, gridHeight;

    // Initialised in LevelRender
    ArrayList<Pair<Integer, Integer>> receiverSpots;
    ArrayList<Pair<Integer, Integer>> emptySpots;

    // Initialised in the solveLevel method
    HashMap<LightSource, Pair<Integer, Integer>> sourceSpots;
    ShortQueue lightProcessingQueue;
    
    // Arrays for tracking which coordinates are lit, for fast O(L) resetting of the grid
    int[] litSpotX = new int[50];
    int[] litSpotY = new int[50];
    int litCount = 0;

    // Cache for O(1) lookup of empty spot indices
    private final int[][] spotIndexGrid;

    // Cache for static-filtered spots for each unique DGO in the level
    private final HashMap<DynamicGridObject, ArrayList<Pair<Integer, Integer>>> staticFilteredSpotsCache = new HashMap<>();
    
    public GridCell[][] solutionGrid;

    public double permutationRatio;
    public long attemptPermutations = 0;
    public BigInteger totalPermutations;

    /**
     * Solve level
     *  Filter DGO empty spots
     *  Copy GridCell Array
     *  Project Light
     *     Emit Light
     *     Spread Light
     *         Interact with Light
     *         Increment Light
     *     Receivers are powered
     *
     */

    // Hot-path instrumentation. Entirely compiled out when solver.profile is unset,
    // because SolverProfiler.ENABLED is a static final constant.
    public final SolverProfiler profiler = new SolverProfiler();

    /**
     * Diagnostic: bypass ALL placement filtering and consider every empty spot for
     * every DGO. Enormously slower, but it is ground truth for "does this level have
     * a solution at all?". If a level reports no solution normally but IS solvable
     * with -Pnofilter, the filtering heuristics are over-strict and are rejecting a
     * legitimate placement. Usage: gradle runTest -PlevelName="..." -Pnofilter
     */
    public static final boolean NO_FILTER = Boolean.getBoolean("solver.nofilter");

    // Total DGOs in the level, used to derive recursion depth for the profiler
    private int totalDgoCount;


    public LevelSolver(ArrayList<Pair<Integer, Integer>> receiverSpots, ArrayList<Pair<Integer, Integer>> emptySpots,
                        int gridWidth, int gridHeight) {
        this.receiverSpots = receiverSpots;
        lightProcessingQueue = new ShortQueue(gridWidth * gridHeight);
        sourceSpots = new HashMap<>();
        this.emptySpots = emptySpots;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;

        // Initialize spotIndexGrid for O(1) lookups
        this.spotIndexGrid = new int[gridWidth][gridHeight];
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                spotIndexGrid[x][y] = -1;
            }
        }
        for (Pair<Integer, Integer> spot : emptySpots) {
            spotIndexGrid[spot.getKey()][spot.getValue()] = emptySpots.indexOf(spot);
        }
    }

    public void createStats(long emptySpots, long dynamicObjects) {
        totalPermutations = createStatsHelper(emptySpots, dynamicObjects);
        System.out.println("Number of DynamicObjects: " + dynamicObjects);
        System.out.println("Total empty spots: " + emptySpots);
        System.out.println("Total permutations: " + totalPermutations);
    }

    private BigInteger createStatsHelper(long emptySpots, long dynamicObjects) {
        if (dynamicObjects == 0) {
            return BigInteger.ONE;
        } else {
            return BigInteger.valueOf(emptySpots).multiply(createStatsHelper(--emptySpots, --dynamicObjects));
        }
    }

    /**
     * Precomputes the list of valid empty spots for each unique DynamicGridObject
     * based solely on static grid elements. This cache is used to speed up the
     * filtering process during the search, by not doing repetitive, static filtering computation on recursive calls.
     *
     * @param dgoList The list of all DynamicGridObjects in the level.
     * @param initialGrid The initial grid state (only static elements matter for this precomputation).
     */
    public void precomputeStaticFilters(LinkedList<DynamicGridObject> dgoList, GridCell[][] initialGrid) {
        this.totalDgoCount = dgoList.size();
        Set<DynamicGridObject> processedDGOs = new HashSet<>();
        for (DynamicGridObject currentDgo : dgoList) {
            boolean alreadyProcessed = false;
            for (DynamicGridObject processedDgo : processedDGOs) {
                if (areIdenticalDGOs(currentDgo, processedDgo)) {
                    staticFilteredSpotsCache.put(currentDgo, staticFilteredSpotsCache.get(processedDgo));
                    alreadyProcessed = true;
                    break;
                }
            }

            if (!alreadyProcessed) {
                ArrayList<Pair<Integer, Integer>> filteredSpots = currentDgo.staticFilter(initialGrid, this.emptySpots);
                staticFilteredSpotsCache.put(currentDgo, filteredSpots);
                processedDGOs.add(currentDgo);
            }
        }
    }

    // EFFECTS: "Efficiently" iterates through all possible placement permutations to find a solution grid
    // iterationSpotIndex: for symmetry breaking — when placing an identical DGO to the previous one,
    // only consider spots at indices >= iterationSpotIndex to avoid redundant permutations
    public boolean solveLevelOriginal(GridCell[][] grid, ArrayList<Pair<Integer, Integer>> emptySpots,
                              LinkedList<DynamicGridObject> dgoQueue, int iterationSpotIndex) {
        if (!dgoQueue.isEmpty()) {
            DynamicGridObject dgo = dgoQueue.remove();
            if (SolverProfiler.ENABLED) profiler.nodeVisited(totalDgoCount - dgoQueue.size() - 1);

            // Use the pre-computed static filtered spots as the base for the dynamic filter
            ArrayList<Pair<Integer, Integer>> baseSpots = staticFilteredSpotsCache.get(dgo);
            if (baseSpots == null) baseSpots = this.emptySpots; // Fallback, though shouldn't happen if precomputation is called

            // Perform further dynamic filtering on the base filtered spots
            ArrayList<Pair<Integer, Integer>> filteredEmptySpots;
            if (NO_FILTER) {
                filteredEmptySpots = this.emptySpots;
            } else {
                long filterStart = SolverProfiler.ENABLED ? profiler.filterStart() : 0L;
                filteredEmptySpots = dgo.filter(grid, baseSpots);
                if (SolverProfiler.ENABLED) {
                    profiler.filterEnd(filterStart, baseSpots.size(), filteredEmptySpots.size());
                }
            }

            int filteredSpotsStartIndex = 0;
            // Symmetry breaking: skip spots before iterationSpotIndex
            // iterationSpotIndex > 0 means the previous DGO was identical and placed at that index in the emptySpots list
            if (iterationSpotIndex > 0) {
                for (int i = 0; i < filteredEmptySpots.size(); i++) {
                    // Find the first filtered, empty spot that is past or equal to the iterationSpotIndex in the global, master emptySpots list
                    // We want to start iterating from there, as prior iterations have been attempted
                    // This works because filteredEmptySpots are somewhat equivalent for the same DGOs
                    if (emptySpotIndex(filteredEmptySpots.get(i)) >= iterationSpotIndex) {
                        filteredSpotsStartIndex = i;
                        break;
                    }
                    // If we reach the end without finding a valid index, no further spots are valid
                    if (i == filteredEmptySpots.size() - 1) {
                        // backtrack
                        if (SolverProfiler.ENABLED) profiler.symmetryPrunes++;
                        dgoQueue.addFirst(dgo);
                        return false;
                    }
                }
            }

            for (int i = filteredSpotsStartIndex; i < filteredEmptySpots.size(); i++) {
                Pair<Integer, Integer> spot = filteredEmptySpots.get(i);
                int spotX = spot.getKey();
                int spotY = spot.getValue();
                GridCell cell = grid[spotX][spotY];
                if (cell.cellDynamicItem == null) {
                    if (SolverProfiler.ENABLED) profiler.placementsTried++;
                    trackLightSources(dgo, spotX, spotY);
                    cell.cellDynamicItem = dgo;

                    int nextIterationSpotIndex = 0;
                    // If a subsequent DGO exists and the current.DGO is the same as the next.DGO
                    if (!dgoQueue.isEmpty() && areIdenticalDGOs(dgo, dgoQueue.peek())) {
                        // The interchanged positions of these two DGOs are functionally equivalent
                        // So when the next.DGO is placed, it can skip the permutation where it is placed at current.DGO spot
                        // Hence, get the index of the current.DGO spot from the emptySpots list
                        // and provide it to the next recursive call
                        nextIterationSpotIndex = emptySpotIndex(spot) + 1;
                    }

                    boolean isSolutionFound = solveLevelOriginal(grid, this.emptySpots, dgoQueue, nextIterationSpotIndex);
                    if (isSolutionFound) {
                        return true;
                    }

                    // Backtrack
                    if (SolverProfiler.ENABLED) profiler.backtracks++;
                    cell.cellDynamicItem = null;
                    if (dgo instanceof LightSource) {
                        sourceSpots.remove(dgo);
                    }

                } else if (SolverProfiler.ENABLED) {
                    profiler.spotsRejectedOccupied++;
                }
            }
            dgoQueue.addFirst(dgo);
        } else {
            return projectLight(grid);
        }
        return false;        
    }

    // EFFECTS: Returns the index of the given spot in the global emptySpots list
    // This provides a canonical ordering for symmetry breaking.
    private int emptySpotIndex(Pair<Integer, Integer> spot) {
        return spotIndexGrid[spot.getKey()][spot.getValue()];
    }

    // EFFECTS: Checks if two DynamicGridObjects are functionally identical
    // (same class and same properties), meaning their placements are interchangeable
    private static boolean areIdenticalDGOs(DynamicGridObject a, DynamicGridObject b) {
        if (a.getClass() != b.getClass()) return false;

        // After this point, we know that a and b are of the same class

        // No orienation or colour differentiation for BackwardMirror and ForwardMirror
        if (a instanceof BackwardMirror || a instanceof ForwardMirror) return true;

        // LightSource: identical if same orientation
        if (a instanceof LightSource) {
            return ((LightSource) a).orientation == ((LightSource) b).orientation;
        }
        // Prism: identical if same orientation
        if (a instanceof Prism) {
            return ((Prism) a).orientation == ((Prism) b).orientation;
        }
        // TJunction: identical if same orientation
        if (a instanceof TJunction) {
            return ((TJunction) a).orientation == ((TJunction) b).orientation;
        }
        // Filters: identical if same colour
        if (a instanceof Filter) return ((Filter) a).colour == ((Filter) b).colour;

        // ColourShifter: identical if same orientation and colour
        if (a instanceof ColourShifter) {
            ColourShifter csA = (ColourShifter) a;
            ColourShifter csB = (ColourShifter) b;
            return csA.orientation == csB.orientation && csA.colour == csB.colour;
        }
        
        return false;
    }


    // EFFECTS: Starts light projecting for the level until it is complete
    // Then indicates whether level is solved or not
    public boolean projectLight(GridCell[][] grid) {
        long profileStart = SolverProfiler.ENABLED ? profiler.projectStart() : 0L;
        emitLight(grid);

        while (!lightProcessingQueue.isEmpty()) {
            short light = lightProcessingQueue.remove();
            if (SolverProfiler.ENABLED) profiler.lightQueuePops++;

            // Resize litSpot arrays if needed
            // Ideally should not need this if we pre-allocate enough space
            if (litCount >= litSpotX.length) {
                resizeLitSpotArrays();
            }
            litSpotX[litCount] = Light.getX(light);
            litSpotY[litCount] = Light.getY(light);
            litCount++;

            spreadLight(light, grid);
        }
        attemptPermutations++;
        boolean solved = allReceiversArePowered(receiverSpots, grid);
        if (SolverProfiler.ENABLED) {
            if (SolverProfiler.DEDUPE) {
                profiler.recordOutcome(litSpotX, litSpotY, litCount, solved);
            }
            profiler.litCellsReset += litCount;
            profiler.projectEnd(profileStart);
        }
        if (solved) {
            return true;
        } else {
            // Reset the powered state of all receivers for the next permutation
            for (Pair<Integer, Integer> spot : receiverSpots) {
                int spotX = spot.getKey();
                int spotY = spot.getValue();
                grid[spotX][spotY].receiver.isPowered = false;
            }

            // Reset only places that were lit
            for (int i = 0; i < litCount; i++) {
                grid[litSpotX[i]][litSpotY[i]].light = -1;
            }
            litCount = 0;

            return false;
        }
    }

    /**
     * Projects light through a manually drafted grid, i.e. one where a person has placed the
     * dynamic objects instead of the search. Every light source currently sitting on the grid
     * emits, and the resulting light trail is deliberately LEFT IN PLACE so the caller can
     * render it regardless of whether the draft works.
     *
     * Unlike {@link #projectLight}, an identical light state is only spread once. A hand drafted
     * layout can send light around a closed loop of mirrors, which would otherwise queue lights
     * forever. The packed light short fully describes the state (position, colour, direction) and
     * spreading is idempotent, so skipping repeats is safe and guarantees termination.
     *
     * @param grid the drafted grid, with the drafted dynamic objects already placed on it
     * @return true if every receiver in the level ends up powered
     */
    public boolean projectDraftedLight(GridCell[][] grid) {
        // Start from a clean slate so the draft can be re-tested after each edit
        GridLayout.resetLightInGridCellArray(grid);
        for (Pair<Integer, Integer> spot : receiverSpots) {
            grid[spot.getKey()][spot.getValue()].receiver.isPowered = false;
        }
        lightProcessingQueue.clear();
        litCount = 0;

        sourceSpots.clear();
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                trackLightSources(grid[x][y].cellDynamicItem, x, y);
            }
        }

        // A light short only ever uses its low 12 bits (4 for x, 4 for y, 2 for colour, 2 for direction)
        boolean[] alreadySpread = new boolean[1 << 12];
        emitLight(grid);
        while (!lightProcessingQueue.isEmpty()) {
            short light = lightProcessingQueue.remove();
            int lightState = light & 0x0FFF;
            if (alreadySpread[lightState]) continue;
            alreadySpread[lightState] = true;
            spreadLight(light, grid);
        }

        for (Pair<Integer, Integer> spot : receiverSpots) {
            if (!grid[spot.getKey()][spot.getValue()].receiver.isPowered) return false;
        }
        return true;
    }

    private boolean allReceiversArePowered(ArrayList<Pair<Integer, Integer>> receiverSpots, GridCell[][] grid) {
        // long checkTime = System.currentTimeMillis();
        for(Pair<Integer, Integer> spot : receiverSpots) {
            int spotX = spot.getKey();
            int spotY = spot.getValue();
            if (!grid[spotX][spotY].receiver.isPowered) {
                return false;
                }
            }
        System.out.println("Number of attempts: " + attemptPermutations);
        this.solutionGrid = grid;
        System.out.println("Found solution!");
        return true;
    }

    // EFFECTS: Called on each light in the light processing queue, this method does one of 4 things
    // 1) If the current gridCell has a dynamicGridObject, interacts with it
    // 2) If the current gridCell is void space, increments light in the appropriate direction
    // 3) If the current gridCell is a wall, stops the spread of light by doing nothing
    // 4) If the current gridCell is a receiver, attempt to power it up with the light
    private void spreadLight(short light, GridCell[][] grid) {
        int x = Light.getX(light);
        int y = Light.getY(light);
        StaticGridObject sgo = grid[x][y].cellStaticItem;
        if (sgo == EMPTY) {
            DynamicGridObject dgo = grid[x][y].cellDynamicItem;
            if (dgo != null) {
                if (SolverProfiler.ENABLED) profiler.dgoInteractions++;
                dgo.interactWithLight(light, grid, lightProcessingQueue);
                return;
            } else {
                incrementLight(light, grid);
                return;
            }
        } else if (sgo == WALL) {
            if (SolverProfiler.ENABLED) profiler.wallStops++;
            return; // Do nothing
        }
        else {
            // If it is not empty, or a wall, it must be a receiver
            if (SolverProfiler.ENABLED) profiler.receiverHits++;
            grid[x][y].receiver.powerUp(light);
        }
    }

    // EFFECTS: Starts emitting light from the light sources and adds them to the light processing queue
    private void emitLight(GridCell[][] grid) {
        // long emitTime = System.currentTimeMillis();
        for (Pair<Integer, Integer> sourceSpot : sourceSpots.values()) {
            int spotX = sourceSpot.getKey();
            int spotY = sourceSpot.getValue();
            LightSource lightSource = (LightSource) grid[spotX][spotY].cellDynamicItem;
            int ord = lightSource.orientation.ordinal();
            int newX = spotX + DX[ord];
            int newY = spotY + DY[ord];
            short startingLight = Light.create(newX, newY, WHITE, lightSource.orientation);
            grid[newX][newY].light = startingLight;
            lightProcessingQueue.add(startingLight);
        }
    }

    // EFFECTS: Records the position of light sources for each iteration of solving
    private void trackLightSources(DynamicGridObject dgo, int spotX, int spotY) {
        if (dgo instanceof LightSource) {
            sourceSpots.put((LightSource) dgo, new Pair<>(spotX, spotY));
        }
    }

    // EFFECTS: Increments the light until it hits a wall, receiver, or DGO, in the original direction of the light
    // Skips increment if it hits a wall, or goes out of bounds
    private void incrementLight(short light, GridCell[][] grid) {
        // Ray‑cast the light forward until it hits a WALL, a RECEIVER, or a DynamicGridObject.
        int ord = Light.getOrientation(light).ordinal();
        Colour col = Light.getColour(light);
        int nx = Light.getX(light) + DX[ord];
        int ny = Light.getY(light) + DY[ord];
        if (GridLayout.isWithinBounds(this.gridWidth, this.gridHeight, nx, ny) && grid[nx][ny].cellStaticItem != WALL) {
            if (SolverProfiler.ENABLED) profiler.lightSingleSteps++;
            short incrementedLight = Light.create(nx, ny, Light.getColour(light), Light.getOrientation(light));
            grid[nx][ny].light = incrementedLight;
            lightProcessingQueue.add(incrementedLight);
        }
        // timeSpentIncrementingLight += System.currentTimeMillis() - incrementTime;
    }

    /**
     * Solve level using C++ solver via JSON over stdin/stdout
     * Uses ProcessBuilder to spawn C++ solver as subprocess
     *
     * @param grid The grid to solve
     * @param emptySpots List of empty spots where DGOs can be placed
     * @param dgoQueue List of dynamic grid objects to place
     * @param iterationSpotIndex For symmetry breaking
     * @return true if solution found
     */
    public boolean solveLevelCPP(GridCell[][] grid, ArrayList<Pair<Integer, Integer>> emptySpots,
                                  LinkedList<DynamicGridObject> dgoQueue, int iterationSpotIndex) {
        try {
            // Serialize input to JSON
            String inputJSON = serializeToJSON(emptySpots, dgoQueue);

            // Spawn C++ solver process (native executable built in src/cpp/solver)
            ProcessBuilder pb = new ProcessBuilder("./solver");
            pb.directory(new File("src/cpp/solver"));
            pb.redirectErrorStream(true);

            Process proc = pb.start();

            // Write input to process stdin
            try (OutputStream os = proc.getOutputStream()) {
                os.write(inputJSON.getBytes("UTF-8"));
                os.flush();
            }

            // Read output from process stdout
            StringBuilder output = new StringBuilder();
            try (InputStream is = proc.getInputStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                int ch;
                while ((ch = br.read()) != -1) {
                    output.append((char) ch);
                }
            }

            // Wait for process to complete
            int exitCode = proc.waitFor();

            if (exitCode != 0) {
                System.err.println("C++ solver process failed with exit code: " + exitCode);
                System.err.println("Output: " + output.toString());
                return false;
            }

            // Deserialize output JSON
            String outStr = output.toString();
            System.out.println("C++ solver raw output: \n" + outStr);
            return deserializeFromJSON(outStr, grid);

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Error communicating with C++ solver: " + e.getMessage());
            return false;
        }
    }

    private void resizeLitSpotArrays() {
        int[] nX = new int[litSpotX.length * 2];
        int[] nY = new int[litSpotY.length * 2];
        System.arraycopy(litSpotX, 0, nX, 0, litSpotX.length);
        System.arraycopy(litSpotY, 0, nY, 0, litSpotY.length);
        litSpotX = nX;
        litSpotY = nY;
    }

    /**
     * Serialize solver state to JSON for C++ solver
     */
    private String serializeToJSON(ArrayList<Pair<Integer, Integer>> emptySpots,
                                    LinkedList<DynamicGridObject> dgoQueue) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("   \"gridWidth\": ").append(gridWidth).append(",\n");
        sb.append("   \"gridHeight\": ").append(gridHeight).append(",\n");

        // emptySpots
        sb.append("   \"emptySpots\": [");
        boolean first = true;
        for (Pair<Integer, Integer> spot : emptySpots) {
            if (!first) sb.append(", ");
            sb.append("[").append(spot.getKey()).append(", ").append(spot.getValue()).append("]");
            first = false;
        }
        sb.append("],\n");

        // receiverSpots
        sb.append("   \"receiverSpots\": [");
        first = true;
        for (Pair<Integer, Integer> spot : receiverSpots) {
            if (!first) sb.append(", ");
            sb.append("[").append(spot.getKey()).append(", ").append(spot.getValue()).append("]");
            first = false;
        }
        sb.append("],\n");

        // dgoQueue
        sb.append("   \"dgoQueue\": [");
        first = true;
        for (DynamicGridObject dgo : dgoQueue) {
            if (!first) sb.append(", ");
            sb.append("{\n");
            sb.append("      \"type\": \"").append(dgo.getClass().getSimpleName()).append("\",\n");

            if (dgo instanceof LightSource) {
                LightSource ls = (LightSource) dgo;
                sb.append("      \"orientation\": ").append(ls.orientation.ordinal()).append("\n");
            } else if (dgo instanceof Prism) {
                Prism p = (Prism) dgo;
                sb.append("      \"orientation\": ").append(p.orientation.ordinal()).append("\n");
            } else if (dgo instanceof TJunction) {
                TJunction tj = (TJunction) dgo;
                sb.append("      \"orientation\": ").append(tj.orientation.ordinal()).append("\n");
            } else if (dgo instanceof ForwardMirror) {
                sb.append("      \"orientation\": 0\n");
            } else if (dgo instanceof BackwardMirror) {
                sb.append("      \"orientation\": 0\n");
            } else if (dgo instanceof Filter) {
                Filter f = (Filter) dgo;
                sb.append("      \"orientation\": 0,\n");
                sb.append("      \"colour\": ").append(f.colour.ordinal()).append("\n");
            } else if (dgo instanceof ColourShifter) {
                ColourShifter cs = (ColourShifter) dgo;
                sb.append("      \"orientation\": ").append(cs.orientation.ordinal()).append(",\n");
                sb.append("      \"colour\": ").append(cs.colour.ordinal()).append("\n");
            }

            sb.append("      \"validSpots\": []\n");
            sb.append("   }");
            first = false;
        }
        sb.append("]\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Deserialize C++ solver output JSON
     */
    private boolean deserializeFromJSON(String json, GridCell[][] grid) {
        try {
            // Parse JSON manually but defensively
            int solutionFoundIdx = json.indexOf("\"solutionFound\"");
            if (solutionFoundIdx == -1) {
                System.err.println("Invalid JSON response");
                return false;
            }

            boolean solutionFound = json.substring(solutionFoundIdx).contains("\"solutionFound\": true");

            // attemptPermutations
            attemptPermutations = 0;
            int attemptPermsIdx = json.indexOf("\"attemptPermutations\"");
            if (attemptPermsIdx != -1) {
                int attemptEnd = json.indexOf(",", attemptPermsIdx);
                if (attemptEnd == -1) attemptEnd = json.indexOf("}", attemptPermsIdx);
                if (attemptEnd > attemptPermsIdx) {
                    String attemptStr = json.substring(attemptPermsIdx + "\"attemptPermutations\":".length(), attemptEnd).trim();
                    try { attemptPermutations = Long.parseLong(attemptStr); } catch (NumberFormatException ignored) { }
                }
            }

            // totalPermutations
            totalPermutations = BigInteger.ZERO;
            int totalPermsIdx = json.indexOf("\"totalPermutations\"");
            if (totalPermsIdx != -1) {
                int totalEnd = json.indexOf(",", totalPermsIdx);
                if (totalEnd == -1) totalEnd = json.indexOf("}", totalPermsIdx);
                if (totalEnd > totalPermsIdx) {
                    String totalStr = json.substring(totalPermsIdx + "\"totalPermutations\":".length(), totalEnd).trim();
                    try { totalPermutations = new BigInteger(totalStr); } catch (Exception ignored) { totalPermutations = BigInteger.ZERO; }
                }
            }

            // timeSpent
            long timeSpent = 0;
            int timeIdx = json.indexOf("\"timeSpent\"");
            if (timeIdx != -1) {
                int timeEnd = json.indexOf(",", timeIdx);
                if (timeEnd == -1) timeEnd = json.indexOf("}", timeIdx);
                if (timeEnd > timeIdx) {
                    String timeStr = json.substring(timeIdx + "\"timeSpent\":".length(), timeEnd).trim();
                    try { timeSpent = Long.parseLong(timeStr); } catch (NumberFormatException ignored) { }
                }
            }

            System.out.println("C++ solver time: " + timeSpent + "ms");
            System.out.println("C++ solver permutations: " + attemptPermutations);

            if (solutionFound) {
                int gridStart = json.indexOf("\"solutionGrid\"");
                if (gridStart == -1) {
                    solutionGrid = grid;
                    return true;
                }

                int gridArrStart = json.indexOf("[", gridStart);
                if (gridArrStart == -1) {
                    solutionGrid = grid;
                    return true;
                }

                // Find matching closing bracket for the solutionGrid array
                int depth = 0;
                int gridArrEnd = -1;
                for (int i = gridArrStart; i < json.length(); i++) {
                    char c = json.charAt(i);
                    if (c == '[') depth++;
                    else if (c == ']') {
                        depth--;
                        if (depth == 0) { gridArrEnd = i; break; }
                    }
                }

                if (gridArrEnd == -1 || gridArrEnd <= gridArrStart) {
                    solutionGrid = grid;
                    return true;
                }

                String gridJson = json.substring(gridArrStart, gridArrEnd + 1);

                // Parse grid rows defensively
                int rowStart = 0;
                int row = 0;

                while (true) {
                    int colStart = gridJson.indexOf("[", rowStart);
                    if (colStart == -1) break;
                    int colEnd = gridJson.indexOf("]", colStart);
                    if (colEnd == -1) break;

                    String rowStr = gridJson.substring(colStart + 1, colEnd);
                    String[] cells = rowStr.split(",");

                    for (int col = 0; col < cells.length && row < gridHeight; col++) {
                        String cell = cells[col].trim().replace("\"", "").replace(" ", "");
                        if (!cell.isEmpty() && !"void".equals(cell)) {
                            // Parse cell type
                            if (cell.startsWith("uL") || cell.startsWith("dL") ||
                                cell.startsWith("lL") || cell.startsWith("rL")) {
                                // LightSource
                                FaceOrientation orient = FaceOrientation.UP;
                                if (cell.startsWith("dL")) orient = FaceOrientation.DOWN;
                                else if (cell.startsWith("lL")) orient = FaceOrientation.LEFT;
                                else if (cell.startsWith("rL")) orient = FaceOrientation.RIGHT;
                                grid[col][row].cellDynamicItem = new LightSource(orient);
                            } else if (cell.startsWith("uP") || cell.startsWith("dP") ||
                                        cell.startsWith("lP") || cell.startsWith("rP")) {
                                // Prism
                                FaceOrientation orient = FaceOrientation.UP;
                                if (cell.startsWith("dP")) orient = FaceOrientation.DOWN;
                                else if (cell.startsWith("lP")) orient = FaceOrientation.LEFT;
                                else if (cell.startsWith("rP")) orient = FaceOrientation.RIGHT;
                                grid[col][row].cellDynamicItem = new Prism(orient);
                            } else if (cell.startsWith("uT") || cell.startsWith("dT") ||
                                        cell.startsWith("lT") || cell.startsWith("rT")) {
                                // TJunction
                                FaceOrientation orient = FaceOrientation.UP;
                                if (cell.startsWith("dT")) orient = FaceOrientation.DOWN;
                                else if (cell.startsWith("lT")) orient = FaceOrientation.LEFT;
                                else if (cell.startsWith("rT")) orient = FaceOrientation.RIGHT;
                                grid[col][row].cellDynamicItem = new TJunction(orient);
                            } else if (cell.startsWith("uM") || cell.startsWith("dM") ||
                                        cell.startsWith("lM") || cell.startsWith("rM")) {
                                // Mirror
                                FaceOrientation orient = FaceOrientation.UP;
                                if (cell.startsWith("dM")) orient = FaceOrientation.DOWN;
                                else if (cell.startsWith("lM")) orient = FaceOrientation.LEFT;
                                else if (cell.startsWith("rM")) orient = FaceOrientation.RIGHT;
                                grid[col][row].cellDynamicItem = new BackwardMirror();
                            } else if ("rF".equals(cell)) {
                                grid[col][row].cellDynamicItem = new RedFilter();
                            } else if ("bF".equals(cell)) {
                                grid[col][row].cellDynamicItem = new BlueFilter();
                            } else if ("yF".equals(cell)) {
                                grid[col][row].cellDynamicItem = new YellowFilter();
                            } else if (cell.startsWith("uR") || cell.startsWith("dR") ||
                                        cell.startsWith("lR") || cell.startsWith("rR")) {
                                // ColourShifter
                                FaceOrientation orient = FaceOrientation.UP;
                                Colour colour = Colour.RED;
                                if (cell.startsWith("dR")) orient = FaceOrientation.DOWN;
                                else if (cell.startsWith("lR")) orient = FaceOrientation.LEFT;
                                else if (cell.startsWith("rR")) orient = FaceOrientation.RIGHT;
                                grid[col][row].cellDynamicItem = new ColourShifter(orient, colour);
                            } else if (cell.startsWith("uB") || cell.startsWith("dB") ||
                                        cell.startsWith("lB") || cell.startsWith("rB")) {
                                FaceOrientation orient = FaceOrientation.UP;
                                if (cell.startsWith("dB")) orient = FaceOrientation.DOWN;
                                else if (cell.startsWith("lB")) orient = FaceOrientation.LEFT;
                                else if (cell.startsWith("rB")) orient = FaceOrientation.RIGHT;
                                grid[col][row].cellDynamicItem = new ColourShifter(orient, Colour.BLUE);
                            } else if (cell.startsWith("uY") || cell.startsWith("dY") ||
                                        cell.startsWith("lY") || cell.startsWith("rY")) {
                                FaceOrientation orient = FaceOrientation.UP;
                                if (cell.startsWith("dY")) orient = FaceOrientation.DOWN;
                                else if (cell.startsWith("lY")) orient = FaceOrientation.LEFT;
                                else if (cell.startsWith("rY")) orient = FaceOrientation.RIGHT;
                                grid[col][row].cellDynamicItem = new ColourShifter(orient, Colour.YELLOW);
                            }
                        }
                    }

                    row++;
                    rowStart = colEnd + 1;
                }

                solutionGrid = grid;
            }

            return solutionFound;

        } catch (Exception e) {
            System.err.println("Error parsing C++ solver output: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

}
