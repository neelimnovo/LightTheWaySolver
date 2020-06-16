package ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import model.GridCell;
import model.GridLayout;
import model.Level;
import model.Stats;
import model.interactionObjects.StaticGridObject;
import model.interactionObjects.DynamicGridObject;
import model.interactionObjects.Receiver;
import searchLogic.LevelSolver;
import searchLogic.Light;
import java.util.ArrayList;
import java.util.LinkedList;

import static model.interactionObjects.StaticGridObject.*;
import static model.interactionObjects.Colour.*;
import static ui.LevelGrid.*;
import static ui.MainMenu.*;

public class LevelRender {

    static long totalTime;
    static double percentagePermutations;
    static GridCell[][] solutionGrid;
    static Stats statistics;

    static Scene createRenderLevelScene(String levelName, Level level) {
        LinkedList<DynamicGridObject> dynamicGridObjectsQueue = new LinkedList<>();
        LinkedList<Pair<Integer, Integer>> viableCoordinates = new LinkedList<>();
        ArrayList<Pair<Integer, Integer>> emptyPositions = new ArrayList<>();
        ArrayList<Pair<Integer, Integer>> receiverPositions = new ArrayList<>();
        GridPane gridPane = new GridPane();
        gridPane.setVgap(15);
        gridPane.setPadding(new Insets(30,30,30,50));
        Label levelTitle = new Label(levelName);
        GridPane.setConstraints(levelTitle, 0 , 0);

        GridPane renderedGrid = new GridPane();
        renderAndSetupUnsolvedGrid(renderedGrid, level.gridLayout, viableCoordinates,
                emptyPositions, receiverPositions, dynamicGridObjectsQueue);
        GridPane.setConstraints(renderedGrid, 0, 1);

        renderedGrid.setStyle("-fx-border-style: solid inside;" +
                "-fx-border-width: 5;" +
                "-fx-border-color: #4ba3c7;");

        Button solveButton = new Button("Solve Level");
        solveButton.setOnAction(event -> {
            solveThenRenderGrid(renderedGrid, level.gridLayout, emptyPositions,
                    receiverPositions, dynamicGridObjectsQueue);
            if (statistics != null) {
                renderStatsLabels(gridPane);
            }
        });

        Button storeStatsButton = new Button("Store level solution");
        storeStatsButton.setOnAction(event -> {
            statistics.levelName = level.title;
            statistics.save(level.title);
        });
        changeButtonColour(storeStatsButton, BUTTON_BLUE);
        GridPane.setConstraints(storeStatsButton, 0, 3);
        changeButtonColour(solveButton, BUTTON_BLUE);
        GridPane.setConstraints(solveButton, 0, 2);
        Button backLevelRender = makeBackButton(levelLoadScene);
        changeButtonColour(backLevelRender, BUTTON_BLUE);;
        GridPane.setConstraints(backLevelRender, 0 ,4);

        gridPane.getChildren().addAll(levelTitle, renderedGrid, solveButton, storeStatsButton, backLevelRender);
        gridPane.setStyle("-fx-background-color:" + SCENE_BLUE);
        // gridPane.setGridLinesVisible(true);
        levelRenderScene = new Scene(gridPane, SCENE_WIDTH, SCENE_HEIGHT);

        return levelRenderScene;
    }

    private static void renderStatsLabels(GridPane gridPane) {
        Label timeLabel = new Label("Time taken to solve: " + (statistics.totalTime / 1000) + "s");
        Label percentageLabel = new Label("Percentage of all permutations attempted: "
                + statistics.permutationRatio + "%");

        GridPane.setConstraints(timeLabel, 1 , 2);
        GridPane.setConstraints(percentageLabel, 1 , 3);
        gridPane.getChildren().addAll(timeLabel, percentageLabel);
    }

    private static void solveThenRenderGrid(GridPane renderedGrid,
                                            GridLayout gridLayout,
                                            ArrayList<Pair<Integer, Integer>> emptyPositions,
                                            ArrayList<Pair<Integer, Integer>> receiverPositions,
                                            LinkedList<DynamicGridObject> dgoQueue) {
        LevelSolver solver = new LevelSolver(receiverPositions, emptyPositions);
        solver.createStats(emptyPositions.size(), dgoQueue.size());
        long startTime = System.currentTimeMillis();
        solver.solveLevel(gridLayout.gridCellArray, emptyPositions, dgoQueue);
        if (solver.solutionGrid != null) {
            totalTime = (System.currentTimeMillis() - startTime);
            statistics = new Stats(solver.solutionGrid, totalTime, solver.totalPermutations,
                    solver.attemptPermutations);
            renderSolvedGrid(renderedGrid, solver.solutionGrid);
        } else {
            System.out.println("No solution found");
        }
    }

    // EFFECTS: Renders the level grid in its initial unsolved state
    // also sets up four lists involved in level setup and solving
    private static void renderAndSetupUnsolvedGrid(GridPane renderedGrid, GridLayout gridLayout,
                                                   LinkedList<Pair<Integer, Integer>> viableCoordinates,
                                                   ArrayList<Pair<Integer, Integer>> emptyPositions,
                                                   ArrayList<Pair<Integer, Integer>> receiverPositions,
                                                   LinkedList<DynamicGridObject> dgoQueue) {
        for (int x = 0; x < gridLayout.gridCellArray.length; x++ ) {
            for (int y = 0; y < gridLayout.gridCellArray[x].length; y++) {
                StaticGridObject sgo = gridLayout.gridCellArray[x][y].cellStaticItem;
                Label gridNode = new Label("",
                        readResourceImage(getCorrectImageString(sgo)));
                switch (sgo) {
                    case EMPTY:
                        viableCoordinates.add(new Pair<>(x,y));
                        emptyPositions.add(new Pair<>(x,y));
                        break;
                    case WALL:
                        break;
                    case WHITE_RECEIVER:
                        gridLayout.gridCellArray[x][y].receiver = new Receiver(WHITE);
                        receiverPositions.add(new Pair<>(x,y));
                        break;
                    case RED_RECEIVER:
                        gridLayout.gridCellArray[x][y].receiver = new Receiver(RED);
                        receiverPositions.add(new Pair<>(x,y));
                        break;
                    case BLUE_RECEIVER:
                        gridLayout.gridCellArray[x][y].receiver = new Receiver(BLUE);
                        receiverPositions.add(new Pair<>(x,y));
                        break;
                    case YELLOW_RECEIVER:
                        gridLayout.gridCellArray[x][y].receiver = new Receiver(YELLOW);
                        receiverPositions.add(new Pair<>(x,y));
                        break;
                }
                renderedGrid.add(gridNode, x, y);
            }
        }

        // The order of these method calls and hence dgoQueue setup is super important
        placeDynamicGridObjects(gridLayout.prisms, renderedGrid, viableCoordinates, dgoQueue);
        placeDynamicGridObjects(gridLayout.redFilters, renderedGrid, viableCoordinates, dgoQueue);
        placeDynamicGridObjects(gridLayout.blueFilters, renderedGrid, viableCoordinates, dgoQueue);
        placeDynamicGridObjects(gridLayout.yellowFilters, renderedGrid, viableCoordinates, dgoQueue);
        placeDynamicGridObjects(gridLayout.colourShifters, renderedGrid, viableCoordinates, dgoQueue);
        placeDynamicGridObjects(gridLayout.lights, renderedGrid, viableCoordinates, dgoQueue);
        placeDynamicGridObjects(gridLayout.tJunctions, renderedGrid, viableCoordinates, dgoQueue);
        placeDynamicGridObjects(gridLayout.frontMirrors, renderedGrid, viableCoordinates, dgoQueue);
        placeDynamicGridObjects(gridLayout.backMirrors, renderedGrid, viableCoordinates, dgoQueue);
        viableCoordinates.clear();
    }


    // EFFECTS: Helper method for rendering unsolved grid
    private static void placeDynamicGridObjects(ArrayList dgoList,
                                                GridPane renderedGrid,
                                                LinkedList<Pair<Integer, Integer>> viableCoordinates,
                                                LinkedList<DynamicGridObject> dgoQueue) {
        for (int i = 0; i < dgoList.size(); i++) {
            Pair emptyCoordinate = viableCoordinates.remove();
            DynamicGridObject dgo = (DynamicGridObject) dgoList.get(i);
            dgoQueue.add(dgo);
            Label labelToModify = new Label("", readResourceImage(dgo.getCorrectImageString()));
            renderedGrid.add(labelToModify, (int) emptyCoordinate.getKey(),
                                            (int) emptyCoordinate.getValue());
        }
    }


    // EFFECTS: Re-renders the empty spots on the grid and the specific dynamic objects placed as solution
    private static void renderSolvedGrid(GridPane renderedGrid, GridCell[][] gridCellArray) {
        for (int x = 0; x < gridCellArray.length; x++ ) {
            for (int y = 0; y < gridCellArray[x].length; y++) {
                if (gridCellArray[x][y].cellStaticItem.equals(EMPTY)) {
                    Label gridNode;
                    if (gridCellArray[x][y].cellDynamicItem != null) {
                        gridNode = new Label("",
                                readResourceImage(gridCellArray[x][y]
                                        .cellDynamicItem.getCorrectImageString()));
                    } else {
                        Light labelLight = gridCellArray[x][y].light;
                        if (labelLight != null) {
                            gridNode = new Label("", readResourceImage(labelLight.getCorrectLightString()));
                        } else {
                            gridNode = new Label("", readResourceImage(getCorrectImageString(EMPTY)));
                        }
                    }
                    renderedGrid.add(gridNode, x, y);
                }
            }
        }
    }
}
