package tests;

import javafx.util.Pair;
import model.GridCell;
import model.GridLayout;
import model.Level;
import model.Stats;
import model.interactionObjects.DynamicGridObject;
import model.interactionObjects.Receiver;
import model.interactionObjects.StaticGridObject;
import searchLogic.LevelSolver;

import java.util.ArrayList;
import java.util.LinkedList;

import static model.interactionObjects.Colour.*;
import static model.interactionObjects.StaticGridObject.*;

public class levelTest {

    /**
     * Loads a level by filename, sets up the grid (receivers, empty positions,
     * DGO queue) without any GUI, then runs the solver and prints stats.
     *
     * @param levelFileName the JSON filename of the level, e.g. "Level 015.json"
     */
    public static void testSolveLevel(String levelFileName) {
        // Strip the .json extension if present,since Level.load appends it
        String levelName = levelFileName;
        if (levelName.endsWith(".json")) {
            levelName = levelName.substring(0, levelName.length() - 5);
        }

        // 1. Load the level from its save file
        Level level = Level.load(levelName);
        if (level == null) {
            System.out.println("Failed to load level: " + levelFileName);
            return;
        }
        GridLayout gridLayout = level.gridLayout;

        // 2. Walk the grid to build emptyPositions, receiverPositions, and create Receiver objects
        ArrayList<Pair<Integer, Integer>> emptyPositions = new ArrayList<>();
        ArrayList<Pair<Integer, Integer>> receiverPositions = new ArrayList<>();

        for (int x = 0; x < gridLayout.gridCellArray.length; x++) {
            for (int y = 0; y < gridLayout.gridCellArray[x].length; y++) {
                StaticGridObject sgo = gridLayout.gridCellArray[x][y].cellStaticItem;
                switch (sgo) {
                    case EMPTY:
                        emptyPositions.add(new Pair<>(x, y));
                        break;
                    case WHITE_RECEIVER:
                        gridLayout.gridCellArray[x][y].receiver = new Receiver(WHITE);
                        receiverPositions.add(new Pair<>(x, y));
                        break;
                    case RED_RECEIVER:
                        gridLayout.gridCellArray[x][y].receiver = new Receiver(RED);
                        receiverPositions.add(new Pair<>(x, y));
                        break;
                    case BLUE_RECEIVER:
                        gridLayout.gridCellArray[x][y].receiver = new Receiver(BLUE);
                        receiverPositions.add(new Pair<>(x, y));
                        break;
                    case YELLOW_RECEIVER:
                        gridLayout.gridCellArray[x][y].receiver = new Receiver(YELLOW);
                        receiverPositions.add(new Pair<>(x, y));
                        break;
                    default:
                        break;
                }
            }
        }

        // 3. Build the DGO queue in the same order as LevelRender
        LinkedList<DynamicGridObject> dgoQueue = new LinkedList<>();
        addAllDGOs(dgoQueue, gridLayout.prisms);
        addAllDGOs(dgoQueue, gridLayout.redFilters);
        addAllDGOs(dgoQueue, gridLayout.blueFilters);
        addAllDGOs(dgoQueue, gridLayout.yellowFilters);
        addAllDGOs(dgoQueue, gridLayout.colourShifters);
        addAllDGOs(dgoQueue, gridLayout.lights);
        addAllDGOs(dgoQueue, gridLayout.tJunctions);
        addAllDGOs(dgoQueue, gridLayout.frontMirrors);
        addAllDGOs(dgoQueue, gridLayout.backMirrors);

        // 4. Create solver and print stats
        LevelSolver solver = new LevelSolver(receiverPositions, emptyPositions,
                gridLayout.gridCellArray.length, gridLayout.gridCellArray[0].length);
        solver.createStats(emptyPositions.size(), dgoQueue.size());

        // 5. Solve the level
        System.out.println("Solving level: " + levelName);
        long startTime = System.currentTimeMillis();
        solver.solveLevelOriginal(gridLayout.gridCellArray, emptyPositions, dgoQueue, 0);
        long totalTime = System.currentTimeMillis() - startTime;

        // 6. Print results
        if (solver.solutionGrid != null) {
            Stats statistics = new Stats(solver.solutionGrid, totalTime,
                    solver.totalPermutations, solver.attemptPermutations);
            System.out.println("=== Solution found! ===");
            GridCell.printGridCell(solver.solutionGrid);
            System.out.println("Time taken: " + (totalTime / 1000) + "s (" + totalTime + "ms)");
            System.out.println("Attempted permutations: " + solver.attemptPermutations);
            System.out.println("Total permutations: " + solver.totalPermutations);
            System.out.println("Permutation ratio: " + statistics.permutationRatio + "%");
        } else {
            System.out.println("!!! No solution found for level: " + levelName + " !!!");
        }
    }

    /** Helper to safely add all items from a list of DGOs to the queue. */
    @SuppressWarnings("unchecked")
    private static void addAllDGOs(LinkedList<DynamicGridObject> queue, ArrayList<?> dgoList) {
        if (dgoList != null) {
            for (Object dgo : dgoList) {
                queue.add((DynamicGridObject) dgo);
            }
        }
    }

    /**
     * CLI entry point. Pass the level filename as the first argument.
     * Usage: java tests.test "Level 015.json"
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java tests.test <levelFileName>");
            System.out.println("Example: java tests.test \"Level 015.json\"");
            return;
        }
        testSolveLevel(args[0]);
    }
}
