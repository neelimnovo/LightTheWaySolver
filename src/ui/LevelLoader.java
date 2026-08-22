package ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import model.Level;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ui.LevelRender.createRenderLevelScene;
import static ui.MainMenu.*;

public class LevelLoader {

    final static String SOLVED_GREEN = "#69f0ae";
    final static String UNSOLVED_RED = "#ff5252";

    private final static int TILES_PER_ROW = 5;

    static Scene createLevelLoaderScene() {
        File solutionsFolder = new File("src/solutionFiles/");
        List<String> levelsSolutions = listFolder(solutionsFolder).stream()
                .map(level -> level.replace(" solution.json", ""))
                .collect(Collectors.toList());

        levelLoadScene = createChooserScene(
                savedLevelNames(),
                levelName -> levelsSolutions.contains(levelName) ? SOLVED_GREEN : UNSOLVED_RED,
                levelName -> mainWindow.setScene(createRenderLevelScene(levelName, Level.load(levelName))));

        return levelLoadScene;
    }

    // EFFECTS: Names of every level in the saveFiles folder, without the .json extension
    static List<String> savedLevelNames() {
        List<String> names = new ArrayList<>();
        for (String file : listFolder(new File("src/saveFiles/"))) {
            if (file.endsWith(".json")) {
                names.add(file.substring(0, file.length() - ".json".length()));
            }
        }
        return names;
    }

    /**
     * EFFECTS: Builds a scrolling grid of tiles, one per entry, that hands the chosen entry
     * to onChoose. Shared by the level browser, the solution drafter and the reference
     * solution browser so they all look and behave the same.
     *
     * @param entries  the names to show, one tile each
     * @param colourOf the tile colour for a given entry
     * @param onChoose what to do with the entry the user clicks
     */
    static Scene createChooserScene(List<String> entries, Function<String, String> colourOf,
                                    Consumer<String> onChoose) {
        GridPane gridPane = new GridPane();
        // Rounded corners and subtle shadow to gridPane
        gridPane.setStyle("-fx-background-color:" + SCENE_BLUE + ";"
            + "-fx-background-radius: 16;"
            + "-fx-effect: dropshadow(gaussian, #00000022, 8, 0.2, 0, 2);");

        for (int i = 0; i < entries.size(); i++) {
            String entry = entries.get(i);
            Button entryButton = makeTile(entry, colourOf.apply(entry), () -> onChoose.accept(entry));
            gridPane.add(entryButton, i % TILES_PER_ROW, i / TILES_PER_ROW);
        }

        Button backButton = makeBackButton(mainMenuScene);
        backButton.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 12;");
        changeButtonColour(backButton, BUTTON_BLUE);
        backButton.setMinWidth(140);
        backButton.setMinHeight(48);
        int lastRow = entries.isEmpty() ? 0 : ((entries.size() - 1) / TILES_PER_ROW) + 1;
        gridPane.add(backButton, 0, lastRow);

        gridPane.setPadding(new Insets(30,30,30,30));
        gridPane.setVgap(20);
        gridPane.setHgap(20);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setStyle("-fx-background: " + SCENE_BLUE + ";");
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setContent(gridPane);
        // Also set the viewport background for full coverage
        scrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
            scrollPane.lookup(".viewport").setStyle("-fx-background-color: " + SCENE_BLUE + ";");
        });

        return new Scene(scrollPane, SCENE_WIDTH, SCENE_HEIGHT);
    }

    // EFFECTS: One large, rounded tile of a chooser grid
    private static Button makeTile(String text, String colour, Runnable onClick) {
        Button tile = new Button(text);
        // Large, rounded, bold font, subtle shadow
        String baseStyle = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 12;";
        String normalEffect = "-fx-effect: dropshadow(gaussian, #00000022, 4, 0.2, 0, 1);";
        String hoverEffect = "-fx-effect: dropshadow(gaussian, #00000055, 12, 0.3, 0, 4);";
        tile.setStyle(baseStyle + normalEffect);
        tile.setMinWidth(140);
        tile.setMinHeight(48);
        tile.setPadding(new Insets(8, 16, 8, 16));
        tile.setOnMouseEntered(e -> {
            // Replace only the -fx-effect property
            String updated = tile.getStyle().replaceAll("-fx-effect:[^;]*;?", "") + hoverEffect;
            tile.setStyle(updated);
        });
        tile.setOnMouseExited(e -> {
            String updated = tile.getStyle().replaceAll("-fx-effect:[^;]*;?", "") + normalEffect;
            tile.setStyle(updated);
        });
        tile.setOnAction(event -> onClick.run());
        changeButtonColour(tile, colour);
        return tile;
    }

    // EFFECTS: Sorted contents of a folder, empty if the folder is missing
    private static List<String> listFolder(File folder) {
        String[] files = folder.list();
        if (files == null) return new ArrayList<>();
        Arrays.sort(files);
        return new ArrayList<>(Arrays.asList(files));
    }
}
