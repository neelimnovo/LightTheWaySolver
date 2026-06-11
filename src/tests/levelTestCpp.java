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
import java.io.*;

import static model.interactionObjects.Colour.*;
import static model.interactionObjects.StaticGridObject.*;

public class levelTestCpp {

    /**
     * Loads a level by filename, sets up the grid (receivers, empty positions,
     * DGO queue) without any GUI, then runs the C++ solver and prints stats.
     *
     * @param levelFileName the JSON filename of the level, e.g. "Level 015.json"
     */
    public static void testSolveLevelCpp(String levelFileName) {
        // Strip the .json extension if present, since Level.load appends it
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
        addAllDGOs(dgoQueue, gridLayout.lights);
        addAllDGOs(dgoQueue, gridLayout.tJunctions);
        addAllDGOs(dgoQueue, gridLayout.prisms);
        addAllDGOs(dgoQueue, gridLayout.colourShifters);
        addAllDGOs(dgoQueue, gridLayout.redFilters);
        addAllDGOs(dgoQueue, gridLayout.blueFilters);
        addAllDGOs(dgoQueue, gridLayout.yellowFilters);
        addAllDGOs(dgoQueue, gridLayout.frontMirrors);
        addAllDGOs(dgoQueue, gridLayout.backMirrors);

        // 4. Create solver and print stats
        LevelSolver solver = new LevelSolver(receiverPositions, emptyPositions,
                gridLayout.gridCellArray.length, gridLayout.gridCellArray[0].length);
        solver.createStats(emptyPositions.size(), dgoQueue.size());
        solver.precomputeStaticFilters(dgoQueue, gridLayout.gridCellArray);

        // Recompile the C++ solver
        System.out.println("Compiling C++ solver in src/cpp/solver...");
        if (!compileCppSolver()) {
            System.out.println("Failed to compile C++ solver. Aborting.");
            return;
        }

        // 5. Solve the level using the C++ solver
        System.out.println("Solving level with C++ solver: " + levelName);
        long startTime = System.currentTimeMillis();
        solver.solveLevelCPP(gridLayout.gridCellArray, emptyPositions, dgoQueue, 0);
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
     * Compiles the C++ solver found in `src/cpp/solver` using g++.
     * Returns true on success.
     */
    private static boolean compileCppSolver() {
        try {
            File dir = new File("src/cpp/solver");
            if (!dir.exists() || !dir.isDirectory()) {
                System.err.println("C++ solver directory not found: " + dir.getAbsolutePath());
                return false;
            }

            // Use the shell so that globbing (*.cpp) works
            // -w flag suppresses all compiler warnings
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", "g++ -std=c++17 -O2 -w -o solver *.cpp");
            pb.directory(dir);
            pb.redirectErrorStream(true);

            Process proc = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            int exitCode = proc.waitFor();
            if (exitCode != 0) {
                System.err.println("C++ compile failed with exit code: " + exitCode);
                System.err.println("Output: \n" + output.toString());
                return false;
            }

            System.out.println("C++ compile succeeded.");
            return true;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Error compiling C++ solver: " + e.getMessage());
            return false;
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
        testSolveLevelCpp(args[0]);
    }

}
