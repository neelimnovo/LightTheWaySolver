package searchLogic;

import javafx.util.Pair;
import model.GridCell;
import model.GridLayout;
import model.interactionObjects.StaticGridObject;
import model.interactionObjects.DynamicGridObject;
import model.interactionObjects.LightSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import static model.GridLayout.copyGridCellArray;
import static model.interactionObjects.StaticGridObject.*;
import static model.interactionObjects.Colour.*;
import static model.interactionObjects.FaceOrientation.*;

public class LevelSolver {
    // Initialised in LevelRender
    ArrayList<Pair<Integer, Integer>> receiverSpots;

    // Initialised in the solveLevel method
    HashMap<LightSource, Pair<Integer, Integer>> sourceSpots;
    LinkedList<Light> lightProcessingQueue;
    
    public GridCell[][] solutionGrid;

    public double permutationRatio;
    public long attemptPermutations = 0;
    public long totalPermutations;


    public LevelSolver(ArrayList<Pair<Integer, Integer>> receiverSpots) {
        this.receiverSpots = receiverSpots;
        lightProcessingQueue = new LinkedList<>();
        sourceSpots = new HashMap<>();
    }

    public void createStats(long emptySpots, long dynamicObjects) {
        totalPermutations = createStatsHelper(emptySpots, dynamicObjects);
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
    public boolean solveLevel(GridCell[][] grid, ArrayList<Pair<Integer, Integer>> emptySpots,
                              LinkedList<DynamicGridObject> dgoQueue) {
        if (!dgoQueue.isEmpty()) {
            // System.out.println(!dgoQueue.isEmpty());
            DynamicGridObject dgo = dgoQueue.remove();
            ArrayList<Pair<Integer, Integer>> filteredEmptySpots = dgo.filter(grid, emptySpots);
            // System.out.println(dgoQueue + " " + dgoQueue.size());
            System.out.println(dgo + " " + filteredEmptySpots.size());
            // Collections.shuffle(filteredEmptySpots);
            for (Pair<Integer, Integer> spot : filteredEmptySpots) {
                int spotX = spot.getKey();
                int spotY = spot.getValue();
                if (grid[spotX][spotY].cellDynamicItem == null) {
                    trackLightSources(dgo, spotX, spotY);
                    GridCell[][] copyGrid = copyGridCellArray(grid);
                    copyGrid[spotX][spotY].cellDynamicItem = dgo;
                    LinkedList<DynamicGridObject> copyQueue = new LinkedList<>(dgoQueue);
                    boolean isSolutionFound = solveLevel(copyGrid, emptySpots, copyQueue);
                    if (isSolutionFound) {
                        return true;
                    }
                }
            }
        } else {
            return projectLight(grid);
        }
        // unable to convince compiler that this statement is unreachable because of dynamic recursion
        return false;
    }

    // EFFECTS: Starts light projecting for the level until it is complete
    // Then indicates whether level is solved or not
    public boolean projectLight(GridCell[][] grid) {
        emitLight(grid);
        while (!lightProcessingQueue.isEmpty()) {
            Light light = lightProcessingQueue.remove();
            spreadLight(light, grid);
        }
        // GridCell.printGridCell(grid);
        if (allReceiversArePowered(receiverSpots, grid)) {
            return true;
        } else {
            for (Pair<Integer, Integer> spot : receiverSpots) {
                int spotX = spot.getKey();
                int spotY = spot.getValue();
                grid[spotX][spotY].receiver.isPowered = false;
            }
         return false;
        }
    }

    private boolean allReceiversArePowered(ArrayList<Pair<Integer, Integer>> receiverSpots, GridCell[][] grid) {
        attemptPermutations++;
//        if (attemptPermutations % 100000 == 0) {
//            System.out.println(attemptPermutations);
//        }
        for(Pair<Integer, Integer> spot : receiverSpots) {
            int spotX = spot.getKey();
            int spotY = spot.getValue();
            if (!grid[spotX][spotY].receiver.isPowered) {
                return false;
                }
            }
        System.out.println("Number of attempts: " + attemptPermutations);
        this.solutionGrid = grid;
        // setupPermutationRatio(attemptPermutations, totalPermutations);
        System.out.println("found solution");
        //GridCell.printGridCell(solutionGrid);
        return true;
    }

    // EFFECTS: Called on each light in the light processing queue, this method does one of 4 things
    // 1) If the current gridCell has a dynamicGridObject, interacts with it
    // 2) If the current gridCell is void space, increments light in the appropriate direction
    // 3) If the current gridCell is a wall, stops the spread of light by doing nothing
    // 4) If the current gridCell is a receiver, attempt to power it up with the light
    private void spreadLight(Light light, GridCell[][] grid) {
        StaticGridObject sgo = grid[light.xPos][light.yPos].cellStaticItem;
        if (sgo.equals(EMPTY)) {
            DynamicGridObject dgo = grid[light.xPos][light.yPos].cellDynamicItem;
            if (dgo != null) {
                dgo.interactWithLight(light, grid, lightProcessingQueue);
            } else {
                incrementLight(light, grid);
            }
        } else if (sgo.equals(WALL)) {
            // Do nothing
        } else {    // If it is not empty, or a wall, it must be a receiver
            grid[light.xPos][light.yPos].receiver.powerUp(light);
        }
    }

    // EFFECTS: Starts emitting light from the light sources and adds them to the light processing queue
    private void emitLight(GridCell[][] grid) {
        for (Pair<Integer, Integer> sourceSpot : sourceSpots.values()) {
            int spotX = sourceSpot.getKey();
            int spotY = sourceSpot.getValue();
            LightSource lightSource = (LightSource) grid[spotX][spotY].cellDynamicItem;
            Light startingLight = null;
            switch (lightSource.orientation) {
                case UP:
                    if (GridLayout.isWithinBounds(grid, spotX, spotY - 1)) {
                        startingLight = new Light(WHITE, UP, spotX, spotY - 1);
                        grid[spotX][spotY - 1].light = startingLight;
                    }
                    break;
                case DOWN:
                    if (GridLayout.isWithinBounds(grid, spotX, spotY + 1)) {
                        startingLight = new Light(WHITE, DOWN, spotX, spotY + 1);
                        grid[spotX][spotY + 1].light = startingLight;
                    }
                    break;
                case LEFT:
                    if (GridLayout.isWithinBounds(grid, spotX - 1, spotY)) {
                        startingLight = new Light(WHITE, LEFT, spotX - 1, spotY);
                        grid[spotX - 1][spotY].light = startingLight;
                    }
                    break;
                case RIGHT:
                    if (GridLayout.isWithinBounds(grid, spotX + 1, spotY)) {
                        startingLight = new Light(WHITE, RIGHT, spotX + 1, spotY);
                        grid[spotX + 1][spotY].light = startingLight;
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + lightSource.orientation);
            }
            lightProcessingQueue.add(startingLight);
        }
    }

    // EFFECTS: Records the position of light sources for each iteration of solving
    private void trackLightSources(DynamicGridObject dgo, int spotX, int spotY) {
        if (dgo.getClass() == LightSource.class) {
            sourceSpots.put((LightSource) dgo, new Pair<>(spotX, spotY));
        }
    }

    // EFFECTS: Increments the light one grid cell at a time if it is possible to do so
    // in the original direction of the light
    private void incrementLight(Light light, GridCell[][] grid) {
        Light incrementedLight = null;
        switch (light.orientation) {
            case UP:
                if (GridLayout.isWithinBounds(grid, light.xPos, light.yPos - 1)) {
                    incrementedLight = new Light(light.colour, light.orientation, light.xPos, light.yPos - 1);
                    grid[light.xPos][light.yPos - 1].light = incrementedLight;
                }
                break;
            case DOWN:
                if (GridLayout.isWithinBounds(grid, light.xPos, light.yPos + 1)) {
                    incrementedLight = new Light(light.colour, light.orientation, light.xPos, light.yPos + 1);
                    grid[light.xPos][light.yPos + 1].light = incrementedLight;
                }
                break;
            case LEFT:
                if (GridLayout.isWithinBounds(grid, light.xPos - 1, light.yPos)) {
                    incrementedLight = new Light(light.colour, light.orientation, light.xPos - 1, light.yPos);
                    grid[light.xPos - 1][light.yPos].light = incrementedLight;
                }
                break;
            case RIGHT:
                if (GridLayout.isWithinBounds(grid, light.xPos + 1, light.yPos)) {
                    incrementedLight = new Light(light.colour, light.orientation, light.xPos + 1, light.yPos);
                    grid[light.xPos + 1][light.yPos].light = incrementedLight;
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + light.orientation);
        }
        if (incrementedLight != null) {
            lightProcessingQueue.add(incrementedLight);
        }
    }

}
