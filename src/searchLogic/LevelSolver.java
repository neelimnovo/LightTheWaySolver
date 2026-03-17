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
    public long timeSpentProjectingLight = 0;
    public long timeSpentEmittingLight = 0;
    public long timeSpentSpreadingLight = 0;
    public long timeSpentInteractingWithLight = 0;
    public long timeSpentIncrementingLight = 0;
    public long timeSpentCheckingReceiversPowered = 0;


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
            
            // Use the pre-computed static filtered spots as the base for the dynamic filter
            ArrayList<Pair<Integer, Integer>> baseSpots = staticFilteredSpotsCache.get(dgo);
            if (baseSpots == null) baseSpots = this.emptySpots; // Fallback, though shouldn't happen if precomputation is called
            
            // Perform further dynamic filtering on the base filtered spots
            ArrayList<Pair<Integer, Integer>> filteredEmptySpots = dgo.filter(grid, baseSpots);

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
                    cell.cellDynamicItem = null;
                    if (dgo instanceof LightSource) {
                        sourceSpots.remove(dgo);
                    }
                    
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
        emitLight(grid);
        
        while (!lightProcessingQueue.isEmpty()) {
            short light = lightProcessingQueue.remove();
            
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
        if (allReceiversArePowered(receiverSpots, grid)) {

            System.out.println("Time spent projecting light: " + timeSpentProjectingLight);
            System.out.println("Time spent emitting light: " + timeSpentEmittingLight);
            System.out.println("Time spent spreading light: " + timeSpentSpreadingLight);
            System.out.println("Time spent incrementing light: " + timeSpentIncrementingLight);
            System.out.println("Time spent checking receivers powered: " + timeSpentCheckingReceiversPowered);
            
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
                dgo.interactWithLight(light, grid, lightProcessingQueue);
                return;
            } else {
                incrementLight(light, grid);
                return;
            }
        } else if (sgo == WALL) {
            return; // Do nothing
        }
        else {
            // If it is not empty, or a wall, it must be a receiver
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
        int ord = Light.getOrientation(light).ordinal();
        Colour col = Light.getColour(light);
        int nx = Light.getX(light) + DX[ord];
        int ny = Light.getY(light) + DY[ord];

        while (GridLayout.isWithinBounds(this.gridWidth, this.gridHeight, nx, ny)) {
            GridCell cell = grid[nx][ny];
            if (cell.cellStaticItem == WALL) break;

            short currentLight = Light.create(nx, ny, col, FaceOrientation.CACHED_VALUES[ord]);
            cell.light = currentLight;

            // If it's a collision point (DGO or Receiver), add to queue and stop ray
            if (cell.cellDynamicItem != null || cell.cellStaticItem != EMPTY) {
                lightProcessingQueue.add(currentLight);
                break;
            }

            // It's empty space: record directly for reset since we skip the queue
            if (litCount >= litSpotX.length) {
                resizeLitSpotArrays();
            }
            litSpotX[litCount] = nx;
            litSpotY[litCount] = ny;
            litCount++;

            nx += DX[ord];
            ny += DY[ord];
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

}
