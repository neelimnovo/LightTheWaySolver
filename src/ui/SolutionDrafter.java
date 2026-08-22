package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Pair;
import model.GridCell;
import model.GridLayout;
import model.Level;
import model.ReferenceSolution;
import model.interactionObjects.DynamicGridObject;
import model.interactionObjects.LightSource;
import model.interactionObjects.Receiver;
import searchLogic.LevelSolver;
import searchLogic.Light;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static model.interactionObjects.Colour.*;
import static model.interactionObjects.StaticGridObject.*;
import static ui.LevelGrid.newResourceImageView;
import static ui.MainMenu.*;

/**
 * Lets a person hand draft a solution for a level instead of searching for one: click an empty
 * spot, pick one of the objects the level provides, then project light to see whether the draft
 * powers every receiver. A draft can be saved to the referenceSolutionFiles folder, which is
 * kept separate from the solutionFiles folder written by the solver.
 *
 * Only the objects the level actually provides can be placed, since a draft using pieces the
 * level does not have would not be a solution to that level.
 */
public class SolutionDrafter {

    private final static String CELL_PURPLE = "#492149";
    private final static String POWERED_GREEN = "#43a047";
    private final static String VALID_TEXT = "#1b5e20";
    private final static String INVALID_TEXT = "#b71c1c";
    private final static String NEUTRAL_TEXT = "#37474f";
    private final static int CELL_SIZE = 25;

    private final String levelName;
    // Where the Go back button returns to, since the drafter can be opened from two places
    private final Scene returnScene;
    private final GridCell[][] grid;
    private final ArrayList<Pair<Integer, Integer>> emptyPositions = new ArrayList<>();
    private final ArrayList<Pair<Integer, Integer>> receiverPositions = new ArrayList<>();

    // Objects the level provides that are not on the board yet, keyed by DynamicGridObject type id
    private final Map<String, Integer> unplacedObjects = new LinkedHashMap<>();

    private final GridPane renderedGrid = new GridPane();
    private final VBox inventoryBox = new VBox(6);
    private final Label statusLabel = new Label();
    private Button[][] cellButtons;

    // EFFECTS: The scene listing every level that a solution can be drafted for
    static Scene createSolutionDraftLoaderScene() {
        solutionDraftLoadScene = LevelLoader.createChooserScene(
                LevelLoader.savedLevelNames(),
                levelName -> ReferenceSolution.exists(levelName)
                        ? LevelLoader.SOLVED_GREEN : LevelLoader.UNSOLVED_RED,
                levelName -> mainWindow.setScene(
                        createSolutionDraftScene(levelName, solutionDraftLoadScene)));
        return solutionDraftLoadScene;
    }

    // EFFECTS: The drafting board for one level, pre-filled with its saved reference solution if
    // it has one. Returns returnScene untouched if the level itself can no longer be loaded
    static Scene createSolutionDraftScene(String levelName, Scene returnScene) {
        Level level = Level.load(levelName);
        if (level == null) {
            showAlert(Alert.AlertType.ERROR, "Couldn't load " + levelName + " from src/saveFiles/");
            return returnScene;
        }
        return new SolutionDrafter(levelName, level, returnScene).buildScene();
    }

    private SolutionDrafter(String levelName, Level level, Scene returnScene) {
        this.levelName = levelName;
        this.returnScene = returnScene;
        this.grid = level.gridLayout.gridCellArray;
        indexGrid(grid, emptyPositions, receiverPositions);
        countAvailableObjects(level.gridLayout);
        loadExistingDraft();
    }

    /**
     * EFFECTS: Attaches a Receiver to every receiver cell and collects the empty and receiver
     * coordinates, which is the state the solver needs before it can project light.
     * Shared with the reference solution browser.
     */
    static void indexGrid(GridCell[][] grid, List<Pair<Integer, Integer>> emptyPositions,
                          List<Pair<Integer, Integer>> receiverPositions) {
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[x].length; y++) {
                switch (grid[x][y].cellStaticItem) {
                    case EMPTY:
                        emptyPositions.add(new Pair<>(x, y));
                        break;
                    case WHITE_RECEIVER:
                        grid[x][y].receiver = new Receiver(WHITE);
                        receiverPositions.add(new Pair<>(x, y));
                        break;
                    case RED_RECEIVER:
                        grid[x][y].receiver = new Receiver(RED);
                        receiverPositions.add(new Pair<>(x, y));
                        break;
                    case BLUE_RECEIVER:
                        grid[x][y].receiver = new Receiver(BLUE);
                        receiverPositions.add(new Pair<>(x, y));
                        break;
                    case YELLOW_RECEIVER:
                        grid[x][y].receiver = new Receiver(YELLOW);
                        receiverPositions.add(new Pair<>(x, y));
                        break;
                    default:
                        break;
                }
            }
        }
    }

    // EFFECTS: Tallies up every dynamic object the level makes available
    private void countAvailableObjects(GridLayout layout) {
        addToInventory(layout.lights);
        addToInventory(layout.tJunctions);
        addToInventory(layout.prisms);
        addToInventory(layout.colourShifters);
        addToInventory(layout.redFilters);
        addToInventory(layout.blueFilters);
        addToInventory(layout.yellowFilters);
        addToInventory(layout.frontMirrors);
        addToInventory(layout.backMirrors);
    }

    private void addToInventory(List<? extends DynamicGridObject> objects) {
        if (objects == null) return;
        for (DynamicGridObject dgo : objects) {
            unplacedObjects.merge(dgo.getTypeId(), 1, Integer::sum);
        }
    }

    // EFFECTS: Re-opens the saved reference solution for this level, so drafts can be edited
    private void loadExistingDraft() {
        if (!ReferenceSolution.exists(levelName)) return;
        ReferenceSolution existing = ReferenceSolution.load(levelName);
        if (existing == null) return;
        existing.applyTo(grid);
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[x].length; y++) {
                DynamicGridObject dgo = grid[x][y].cellDynamicItem;
                if (dgo != null) takeFromInventory(dgo.getTypeId());
            }
        }
    }

    private Scene buildScene() {
        Label title = new Label("Draft a solution - " + levelName);
        title.setFont(Font.font("System", FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#083344"));

        Label hint = new Label("Click an empty spot to place one of the objects this level provides, "
                + "then project light to test the draft.");

        renderedGrid.setStyle("-fx-border-style: solid inside;"
                + "-fx-border-width: 5;"
                + "-fx-border-color: #4ba3c7;");
        buildGridButtons();

        Label inventoryTitle = new Label("Objects left to place");
        inventoryTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        VBox inventoryPanel = new VBox(10, inventoryTitle, inventoryBox);
        inventoryPanel.setPadding(new Insets(12));
        inventoryPanel.setMinWidth(280);
        inventoryPanel.setStyle("-fx-background-color: #e6fbff; -fx-background-radius: 12;");

        HBox board = new HBox(30, renderedGrid, inventoryPanel);
        board.setAlignment(Pos.TOP_LEFT);

        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        setStatus("Nothing tested yet - place objects, then press Project Light.", NEUTRAL_TEXT);

        Button projectButton = new Button("Project Light");
        projectButton.setOnAction(event -> testDraft());

        Button saveButton = new Button("Save reference solution");
        saveButton.setOnAction(event -> saveReferenceSolution());

        Button clearButton = new Button("Clear board");
        clearButton.setOnAction(event -> clearBoard());

        Button backButton = makeBackButton(returnScene);

        for (Button button : List.of(projectButton, saveButton, clearButton)) {
            changeButtonColour(button, BUTTON_BLUE);
            materialiseButton(button);
        }

        HBox buttonRow = new HBox(18, projectButton, saveButton, clearButton, backButton);

        VBox root = new VBox(16, title, hint, board, statusLabel, buttonRow);
        root.setPadding(new Insets(24, 30, 24, 40));
        root.setStyle("-fx-background-color:" + SCENE_BLUE);

        redraw();
        return new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
    }

    // EFFECTS: Creates one button per grid cell; only the empty ones open a placement menu
    private void buildGridButtons() {
        cellButtons = new Button[grid.length][grid[0].length];
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[x].length; y++) {
                Button cell = new Button();
                cell.setPadding(Insets.EMPTY);
                cell.setMinSize(CELL_SIZE, CELL_SIZE);
                cell.setMaxSize(CELL_SIZE, CELL_SIZE);
                if (grid[x][y].cellStaticItem == EMPTY) {
                    final int spotX = x;
                    final int spotY = y;
                    cell.setOnAction(event -> showPlacementMenu(spotX, spotY));
                }
                cellButtons[x][y] = cell;
                renderedGrid.add(cell, x, y);
            }
        }
    }

    // EFFECTS: Pops up the list of objects that can go on this spot, plus a way to clear it
    private void showPlacementMenu(int x, int y) {
        ContextMenu menu = new ContextMenu();

        DynamicGridObject placed = grid[x][y].cellDynamicItem;
        if (placed != null) {
            MenuItem removeItem = new MenuItem("Remove " + placed,
                    newResourceImageView(placed.getCorrectImageString()));
            removeItem.setOnAction(event -> removeObject(x, y));
            menu.getItems().addAll(removeItem, new SeparatorMenuItem());
        }

        for (Map.Entry<String, Integer> available : unplacedObjects.entrySet()) {
            if (available.getValue() <= 0) continue;
            String typeId = available.getKey();
            DynamicGridObject prototype = DynamicGridObject.fromTypeId(typeId);
            if (prototype == null) continue;
            MenuItem item = new MenuItem(prototype + "   (" + available.getValue() + " left)",
                    newResourceImageView(prototype.getCorrectImageString()));
            item.setOnAction(event -> placeObject(x, y, typeId));
            menu.getItems().add(item);
        }

        if (menu.getItems().isEmpty()) {
            MenuItem nothingLeft = new MenuItem("Every object in this level is already placed");
            nothingLeft.setDisable(true);
            menu.getItems().add(nothingLeft);
        }
        menu.show(cellButtons[x][y], Side.BOTTOM, 0, 0);
    }

    private void placeObject(int x, int y, String typeId) {
        DynamicGridObject replaced = grid[x][y].cellDynamicItem;
        if (replaced != null) returnToInventory(replaced.getTypeId());
        grid[x][y].cellDynamicItem = DynamicGridObject.fromTypeId(typeId);
        takeFromInventory(typeId);
        clearProjectedLight();
        redraw();
    }

    private void removeObject(int x, int y) {
        DynamicGridObject removed = grid[x][y].cellDynamicItem;
        if (removed == null) return;
        grid[x][y].cellDynamicItem = null;
        returnToInventory(removed.getTypeId());
        clearProjectedLight();
        redraw();
    }

    private void clearBoard() {
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[x].length; y++) {
                DynamicGridObject removed = grid[x][y].cellDynamicItem;
                if (removed != null) {
                    grid[x][y].cellDynamicItem = null;
                    returnToInventory(removed.getTypeId());
                }
            }
        }
        clearProjectedLight();
        redraw();
    }

    private void takeFromInventory(String typeId) {
        int left = unplacedObjects.getOrDefault(typeId, 0);
        // Stay at zero if a saved draft uses an object the level no longer provides
        unplacedObjects.put(typeId, Math.max(0, left - 1));
    }

    private void returnToInventory(String typeId) {
        unplacedObjects.merge(typeId, 1, Integer::sum);
    }

    // EFFECTS: Wipes the previously projected light so the board reflects the edited draft
    private void clearProjectedLight() {
        GridLayout.resetLightInGridCellArray(grid);
        for (Pair<Integer, Integer> spot : receiverPositions) {
            grid[spot.getKey()][spot.getValue()].receiver.isPowered = false;
        }
        setStatus("Draft changed - project light again to re-test it.", NEUTRAL_TEXT);
    }

    // EFFECTS: Runs the level light projection over the drafted board and reports the result
    private boolean testDraft() {
        boolean valid = projectAndRender();
        if (countPlacedLightSources() == 0) {
            setStatus("No light source placed yet, so nothing was lit.", INVALID_TEXT);
            return valid;
        }
        int powered = countPoweredReceivers();
        int unplaced = countUnplacedObjects();
        String unplacedNote = unplaced == 0 ? "" : "   (" + unplaced + " object(s) still unplaced)";
        if (valid) {
            setStatus("VALID - all " + receiverPositions.size() + " receivers powered." + unplacedNote,
                    VALID_TEXT);
        } else {
            setStatus("NOT VALID - " + powered + " of " + receiverPositions.size()
                    + " receivers powered." + unplacedNote, INVALID_TEXT);
        }
        return valid;
    }

    // EFFECTS: Projects light using the solver own simulation, then redraws the light trail
    private boolean projectAndRender() {
        LevelSolver solver = new LevelSolver(receiverPositions, emptyPositions,
                grid.length, grid[0].length);
        boolean valid = solver.projectDraftedLight(grid);
        redraw();
        return valid;
    }

    private void saveReferenceSolution() {
        boolean valid = testDraft();

        ReferenceSolution solution = new ReferenceSolution(levelName, valid);
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[x].length; y++) {
                DynamicGridObject dgo = grid[x][y].cellDynamicItem;
                if (dgo != null) solution.addPlacement(x, y, dgo);
            }
        }

        if (solution.placements.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Nothing to save - no objects have been placed.");
            return;
        }

        String path = solution.save();
        if (path == null) {
            showAlert(Alert.AlertType.ERROR, "Couldn't write the reference solution to "
                    + ReferenceSolution.FOLDER);
        } else if (valid) {
            showAlert(Alert.AlertType.INFORMATION, "Valid solution saved to " + path);
        } else {
            showAlert(Alert.AlertType.WARNING, "Saved to " + path
                    + "\n\nNote: this draft does not power every receiver.");
        }
    }

    // EFFECTS: Repaints every cell from the current state of the grid
    private void redraw() {
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[x].length; y++) {
                cellButtons[x][y].setGraphic(newResourceImageView(cellImage(grid[x][y])));
                cellButtons[x][y].setStyle(cellStyle(x, y));
            }
        }
        refreshInventoryPanel();
    }

    // EFFECTS: The image representing a cell: the object on it, the light passing through it,
    // or the static level element underneath. Shared with the reference solution browser
    static String cellImage(GridCell cell) {
        if (cell.cellStaticItem == EMPTY) {
            if (cell.cellDynamicItem != null) return cell.cellDynamicItem.getCorrectImageString();
            if (cell.light != -1) return Light.getCorrectLightString(cell.light);
        }
        return getCorrectImageString(cell.cellStaticItem);
    }

    // EFFECTS: Cell background, which turns green once a receiver has been powered
    static String cellBackgroundStyle(GridCell cell) {
        boolean isPoweredReceiver = cell.receiver != null && cell.receiver.isPowered;
        return "-fx-background-color: " + (isPoweredReceiver ? POWERED_GREEN : CELL_PURPLE)
                + "; -fx-background-radius: 0;";
    }

    private String cellStyle(int x, int y) {
        GridCell cell = grid[x][y];
        String style = cellBackgroundStyle(cell);
        return cell.cellStaticItem == EMPTY ? style + " -fx-cursor: hand;" : style;
    }

    private void refreshInventoryPanel() {
        inventoryBox.getChildren().clear();
        for (Map.Entry<String, Integer> available : unplacedObjects.entrySet()) {
            if (available.getValue() <= 0) continue;
            DynamicGridObject prototype = DynamicGridObject.fromTypeId(available.getKey());
            if (prototype == null) continue;
            inventoryBox.getChildren().add(new Label(prototype + "   x " + available.getValue(),
                    newResourceImageView(prototype.getCorrectImageString())));
        }
        if (inventoryBox.getChildren().isEmpty()) {
            inventoryBox.getChildren().add(new Label("Every object has been placed."));
        }
    }

    private int countUnplacedObjects() {
        int total = 0;
        for (int left : unplacedObjects.values()) {
            total += left;
        }
        return total;
    }

    private int countPlacedLightSources() {
        int sources = 0;
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[x].length; y++) {
                if (grid[x][y].cellDynamicItem instanceof LightSource) sources++;
            }
        }
        return sources;
    }

    private int countPoweredReceivers() {
        int powered = 0;
        for (Pair<Integer, Integer> spot : receiverPositions) {
            if (grid[spot.getKey()][spot.getValue()].receiver.isPowered) powered++;
        }
        return powered;
    }

    private void setStatus(String message, String colour) {
        statusLabel.setText(message);
        statusLabel.setTextFill(Color.web(colour));
    }

    private static void showAlert(Alert.AlertType type, String message) {
        new Alert(type, message, ButtonType.OK).showAndWait();
    }
}
