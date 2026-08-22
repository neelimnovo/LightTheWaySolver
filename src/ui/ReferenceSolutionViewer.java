package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Pair;
import model.GridCell;
import model.Level;
import model.ReferenceSolution;
import searchLogic.LevelSolver;

import java.util.ArrayList;
import java.util.List;

import static ui.LevelGrid.newResourceImageView;
import static ui.MainMenu.*;
import static ui.SolutionDrafter.cellBackgroundStyle;
import static ui.SolutionDrafter.cellImage;
import static ui.SolutionDrafter.indexGrid;

/**
 * Browses the hand drafted solutions in the referenceSolutionFiles folder. Each one is rebuilt by
 * re-loading its level, dropping the saved placements back onto it and re-projecting light, so
 * what is shown is verified against the current level rather than taken on trust from the file.
 */
public class ReferenceSolutionViewer {

    private final static String VALID_TEXT = "#1b5e20";
    private final static String INVALID_TEXT = "#b71c1c";
    private final static int CELL_SIZE = 25;

    // EFFECTS: The scene listing every level that has a saved reference solution
    static Scene createReferenceSolutionLoaderScene() {
        List<String> levelNames = ReferenceSolution.savedLevelNames();
        referenceSolutionLoadScene = LevelLoader.createChooserScene(
                levelNames,
                ReferenceSolutionViewer::tileColour,
                levelName -> mainWindow.setScene(createReferenceSolutionScene(levelName)));
        return referenceSolutionLoadScene;
    }

    private static String tileColour(String levelName) {
        ReferenceSolution solution = ReferenceSolution.load(levelName);
        return solution != null && solution.valid ? LevelLoader.SOLVED_GREEN : LevelLoader.UNSOLVED_RED;
    }

    // EFFECTS: Renders one saved reference solution with its light projection
    static Scene createReferenceSolutionScene(String levelName) {
        Label title = new Label("Reference solution - " + levelName);
        title.setFont(Font.font("System", FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#083344"));

        Label statusLabel = new Label();
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        GridPane renderedGrid = new GridPane();
        renderedGrid.setStyle("-fx-border-style: solid inside;"
                + "-fx-border-width: 5;"
                + "-fx-border-color: #4ba3c7;");

        ReferenceSolution solution = ReferenceSolution.load(levelName);
        Level level = Level.load(levelName);
        if (solution == null || level == null) {
            statusLabel.setText("Couldn't rebuild this reference solution - "
                    + "its level or solution file is missing.");
            statusLabel.setTextFill(Color.web(INVALID_TEXT));
        } else {
            GridCell[][] grid = level.gridLayout.gridCellArray;
            ArrayList<Pair<Integer, Integer>> emptyPositions = new ArrayList<>();
            ArrayList<Pair<Integer, Integer>> receiverPositions = new ArrayList<>();
            indexGrid(grid, emptyPositions, receiverPositions);
            solution.applyTo(grid);

            LevelSolver solver = new LevelSolver(receiverPositions, emptyPositions,
                    grid.length, grid[0].length);
            boolean valid = solver.projectDraftedLight(grid);

            renderGrid(renderedGrid, grid);
            if (valid) {
                statusLabel.setText("VALID - all " + receiverPositions.size() + " receivers powered.");
                statusLabel.setTextFill(Color.web(VALID_TEXT));
            } else {
                statusLabel.setText("NOT VALID - " + poweredReceivers(grid, receiverPositions) + " of "
                        + receiverPositions.size() + " receivers powered.");
                statusLabel.setTextFill(Color.web(INVALID_TEXT));
            }
        }

        Button editButton = new Button("Edit this draft");
        editButton.setOnAction(event -> mainWindow.setScene(
                SolutionDrafter.createSolutionDraftScene(levelName, referenceSolutionLoadScene)));
        changeButtonColour(editButton, BUTTON_BLUE);
        materialiseButton(editButton);

        Button backButton = makeBackButton(referenceSolutionLoadScene);
        HBox buttonRow = new HBox(18, editButton, backButton);

        VBox root = new VBox(16, title, renderedGrid, statusLabel, buttonRow);
        root.setAlignment(Pos.TOP_LEFT);
        root.setPadding(new Insets(24, 30, 24, 40));
        root.setStyle("-fx-background-color:" + SCENE_BLUE);

        return new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
    }

    // EFFECTS: Draws the rebuilt grid, light trail included, as a read-only board
    private static void renderGrid(GridPane renderedGrid, GridCell[][] grid) {
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[x].length; y++) {
                Label cell = new Label("", newResourceImageView(cellImage(grid[x][y])));
                cell.setMinSize(CELL_SIZE, CELL_SIZE);
                cell.setMaxSize(CELL_SIZE, CELL_SIZE);
                cell.setStyle(cellBackgroundStyle(grid[x][y]));
                renderedGrid.add(cell, x, y);
            }
        }
    }

    private static int poweredReceivers(GridCell[][] grid, List<Pair<Integer, Integer>> receiverPositions) {
        int powered = 0;
        for (Pair<Integer, Integer> spot : receiverPositions) {
            if (grid[spot.getKey()][spot.getValue()].receiver.isPowered) powered++;
        }
        return powered;
    }
}
