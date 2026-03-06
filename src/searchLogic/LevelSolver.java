package searchLogic;

import javafx.util.Pair;
import model.GridCell;
import model.GridLayout;
import model.interactionObjects.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ArrayDeque;

import static model.interactionObjects.StaticGridObject.*;
import static model.interactionObjects.Colour.*;
import static model.interactionObjects.FaceOrientation.*;

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
    ArrayDeque<Light> lightProcessingQueue;
    
    public GridCell[][] solutionGrid;

    public double permutationRatio;
    public long attemptPermutations = 0;
    public long totalPermutations;

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
    public long timeSpentFilteringForDGO = 0;
    public long timeSpentProjectingLight = 0;
    public long timeSpentEmittingLight = 0;
    public long timeSpentSpreadingLight = 0;
    public long timeSpentInteractingWithLight = 0;
    public long timeSpentIncrementingLight = 0;
    public long timeSpentCheckingReceiversPowered = 0;


    public LevelSolver(ArrayList<Pair<Integer, Integer>> receiverSpots, ArrayList<Pair<Integer, Integer>> emptySpots,
                        int gridWidth, int gridHeight) {
        this.receiverSpots = receiverSpots;
        lightProcessingQueue = new ArrayDeque<>();
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

    private long createStatsHelper(long emptySpots, long dynamicObjects) {
        if (dynamicObjects == 0) {
            return 1;
        } else {
            return emptySpots * createStatsHelper(--emptySpots, --dynamicObjects);
        }
    }

    // EFFECTS: "Efficiently" iterates through all possible placement permutations to find a solution grid
    public boolean solveLevelOriginal(GridCell[][] grid, ArrayList<Pair<Integer, Integer>> emptySpots,
                              LinkedList<DynamicGridObject> dgoQueue) {
        if (!dgoQueue.isEmpty()) {
            long dgoTime = System.currentTimeMillis();
            DynamicGridObject dgo = dgoQueue.remove();
            ArrayList<Pair<Integer, Integer>> filteredEmptySpots = dgo.filter(grid, this.emptySpots);
            dgoTime = System.currentTimeMillis() - dgoTime;
            timeSpentFilteringForDGO += dgoTime;
            for (Pair<Integer, Integer> spot : filteredEmptySpots) {
                int spotX = spot.getKey();
                int spotY = spot.getValue();
                GridCell cell = grid[spotX][spotY];
                if (cell.cellDynamicItem == null) {
                    trackLightSources(dgo, spotX, spotY);
                    cell.cellDynamicItem = dgo;

                    boolean isSolutionFound = solveLevelOriginal(grid, this.emptySpots, dgoQueue);
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


    // EFFECTS: Starts light projecting for the level until it is complete
    // Then indicates whether level is solved or not
    public boolean projectLight(GridCell[][] grid) {
        long emitTime = System.currentTimeMillis();
        long projectTime = System.currentTimeMillis();
        emitLight(grid);
        emitTime = System.currentTimeMillis() - emitTime;
        timeSpentEmittingLight += emitTime;
        while (!lightProcessingQueue.isEmpty()) {
            Light light = lightProcessingQueue.remove();
            long spreadTime = System.currentTimeMillis();
            spreadLight(light, grid);
            spreadTime = System.currentTimeMillis() - spreadTime;
            timeSpentSpreadingLight += spreadTime;
        }
        attemptPermutations++;
        if (allReceiversArePowered(receiverSpots, grid)) {
            timeSpentProjectingLight += System.currentTimeMillis() - projectTime;

            System.out.println("Time spent filtering for DGO: " + timeSpentFilteringForDGO);
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
            GridLayout.resetLightInGridCellArray(grid);
            timeSpentProjectingLight += System.currentTimeMillis() - projectTime;
            return false;
        }
    }

    private boolean allReceiversArePowered(ArrayList<Pair<Integer, Integer>> receiverSpots, GridCell[][] grid) {
        long checkTime = System.currentTimeMillis();
        for(Pair<Integer, Integer> spot : receiverSpots) {
            int spotX = spot.getKey();
            int spotY = spot.getValue();
            if (!grid[spotX][spotY].receiver.isPowered) {
                timeSpentCheckingReceiversPowered += System.currentTimeMillis() - checkTime;
                return false;
                }
            }
        System.out.println("Number of attempts: " + attemptPermutations);
        this.solutionGrid = grid;
        System.out.println("Found solution!");
        timeSpentCheckingReceiversPowered += System.currentTimeMillis() - checkTime;
        return true;
    }

    // EFFECTS: Called on each light in the light processing queue, this method does one of 4 things
    // 1) If the current gridCell has a dynamicGridObject, interacts with it
    // 2) If the current gridCell is void space, increments light in the appropriate direction
    // 3) If the current gridCell is a wall, stops the spread of light by doing nothing
    // 4) If the current gridCell is a receiver, attempt to power it up with the light
    private void spreadLight(Light light, GridCell[][] grid) {
        int x = light.xPos;
        int y = light.yPos;
        StaticGridObject sgo = grid[x][y].cellStaticItem;
        if (sgo == EMPTY) {
            DynamicGridObject dgo = grid[x][y].cellDynamicItem;
            if (dgo != null) {
                dgo.interactWithLight(light, grid, lightProcessingQueue);
                return;
            } else{
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
        long emitTime = System.currentTimeMillis();
        for (Pair<Integer, Integer> sourceSpot : sourceSpots.values()) {
            int spotX = sourceSpot.getKey();
            int spotY = sourceSpot.getValue();
            LightSource lightSource = (LightSource) grid[spotX][spotY].cellDynamicItem;
            // Light source will never be placed next to bounds, so no out of bounds check needed
            int ord = lightSource.orientation.ordinal();
            int newX = spotX + DX[ord];
            int newY = spotY + DY[ord];
            Light startingLight = new Light(WHITE, lightSource.orientation, newX, newY);
            grid[newX][newY].light = startingLight;
            lightProcessingQueue.add(startingLight);
        }
        timeSpentEmittingLight += System.currentTimeMillis() - emitTime;
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
    private void incrementLight(Light light, GridCell[][] grid) {
        long incrementTime = System.currentTimeMillis();
        int ord = light.orientation.ordinal();
        int nx = light.xPos + DX[ord];
        int ny = light.yPos + DY[ord];
        if (GridLayout.isWithinBounds(this.gridWidth, this.gridHeight, nx, ny) && grid[nx][ny].cellStaticItem != WALL) {
            Light incrementedLight = new Light(light.colour, light.orientation, nx, ny);
            grid[nx][ny].light = incrementedLight;
            lightProcessingQueue.add(incrementedLight);
        }
        timeSpentIncrementingLight += System.currentTimeMillis() - incrementTime;
    }

}
