package searchLogic;

import javafx.util.Pair;
import model.GridCell;
import model.GridLayout;
import model.interactionObjects.filters.*;
import model.interactionObjects.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
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

    // EFFECTS: "Efficiently" iterates through all possible placement permutations to find a solution grid
    // iterationSpotIndex: for symmetry breaking — when placing an identical DGO to the previous one,
    // only consider spots at indices >= iterationSpotIndex to avoid redundant permutations
    public boolean solveLevelOriginal(GridCell[][] grid, ArrayList<Pair<Integer, Integer>> emptySpots,
                              LinkedList<DynamicGridObject> dgoQueue, int iterationSpotIndex) {
        if (!dgoQueue.isEmpty()) {
            DynamicGridObject dgo = dgoQueue.remove();
            ArrayList<Pair<Integer, Integer>> filteredEmptySpots = dgo.filter(grid, this.emptySpots);

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

    // EFFECTS: Returns the index of the given spot in the global emptySpots list, which is the same for all DGOs
    // This provides a canonical ordering for symmetry breaking.
    private int emptySpotIndex(Pair<Integer, Integer> spot) {
        int sx = spot.getKey();
        int sy = spot.getValue();
        for (int i = 0; i < emptySpots.size(); i++) {
            Pair<Integer, Integer> es = emptySpots.get(i);
            if (es.getKey() == sx && es.getValue() == sy) {
                return i;
            }
        }
        return -1; // Should never happen
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
        // long emitTime = System.currentTimeMillis();
        // long projectTime = System.currentTimeMillis();
        emitLight(grid);
        // emitTime = System.currentTimeMillis() - emitTime;
        // timeSpentEmittingLight += emitTime;
        
        while (!lightProcessingQueue.isEmpty()) {
            short light = lightProcessingQueue.remove();
            
            // Resize litSpot arrays if needed
            // Ideally should not need this if we pre-allocate enough space
            if (litCount >= litSpotX.length) {
                System.out.println("Resizing litSpot arrays");
                int[] nX = new int[litSpotX.length * 2];
                int[] nY = new int[litSpotY.length * 2];
                System.arraycopy(litSpotX, 0, nX, 0, litSpotX.length);
                System.arraycopy(litSpotY, 0, nY, 0, litSpotY.length);
                litSpotX = nX;
                litSpotY = nY;
            }
            litSpotX[litCount] = Light.getX(light);
            litSpotY[litCount] = Light.getY(light);
            litCount++;

            // long spreadTime = System.currentTimeMillis();
            spreadLight(light, grid);
            // spreadTime = System.currentTimeMillis() - spreadTime;
            // timeSpentSpreadingLight += spreadTime;
        }
        attemptPermutations++;
        if (allReceiversArePowered(receiverSpots, grid)) {
            // timeSpentProjectingLight += System.currentTimeMillis() - projectTime;

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

            // timeSpentProjectingLight += System.currentTimeMillis() - projectTime;
            return false;
        }
    }

    private boolean allReceiversArePowered(ArrayList<Pair<Integer, Integer>> receiverSpots, GridCell[][] grid) {
        // long checkTime = System.currentTimeMillis();
        for(Pair<Integer, Integer> spot : receiverSpots) {
            int spotX = spot.getKey();
            int spotY = spot.getValue();
            if (!grid[spotX][spotY].receiver.isPowered) {
                // timeSpentCheckingReceiversPowered += System.currentTimeMillis() - checkTime;
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
                // TODO: Implement ray-cast processing, where light is projected in a straight line until it hits a wall or receiver
                // or a DGO
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
        // timeSpentEmittingLight += System.currentTimeMillis() - emitTime;
    }

    // EFFECTS: Records the position of light sources for each iteration of solving
    private void trackLightSources(DynamicGridObject dgo, int spotX, int spotY) {
        if (dgo.getClass() == LightSource.class) {
            sourceSpots.put((LightSource) dgo, new Pair<>(spotX, spotY));
        }
    }

    // EFFECTS: Increments the light one grid cell at a time if it is possible to do so
    // in the original direction of the light
    // Skips increment if it hits a wall, or goes out of bounds
    private void incrementLight(short light, GridCell[][] grid) {
        // long incrementTime = System.currentTimeMillis();
        int ord = Light.getOrientation(light).ordinal();
        int nx = Light.getX(light) + DX[ord];
        int ny = Light.getY(light) + DY[ord];
        if (GridLayout.isWithinBounds(this.gridWidth, this.gridHeight, nx, ny) && grid[nx][ny].cellStaticItem != WALL) {
            short incrementedLight = Light.create(nx, ny, Light.getColour(light), Light.getOrientation(light));
            grid[nx][ny].light = incrementedLight;
            lightProcessingQueue.add(incrementedLight);
        }
        // timeSpentIncrementingLight += System.currentTimeMillis() - incrementTime;
    }

}
