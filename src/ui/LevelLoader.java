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
import java.util.stream.Collectors;

import static ui.LevelRender.createRenderLevelScene;
import static ui.MainMenu.*;

public class LevelLoader {

    final static String SOLVED_GREEN = "#69f0ae";
    final static String UNSOLVED_RED = "#ff5252";

    static Scene createLevelLoaderScene() {
        ScrollPane scrollPane = new ScrollPane();
        GridPane gridPane = new GridPane();
        // Rounded corners and subtle shadow to gridPane
        gridPane.setStyle("-fx-background-color:" + SCENE_BLUE + ";"
            + "-fx-background-radius: 16;"
            + "-fx-effect: dropshadow(gaussian, #00000022, 8, 0.2, 0, 2);");
        File levelsFolder = new File("src\\saveFiles\\");
        File solutionsFolder = new File("src\\solutionFiles\\");
        String[] levels = levelsFolder.list();
        List<String> levelsSolutions = new ArrayList<>(Arrays.asList(solutionsFolder.list()));
        levelsSolutions = levelsSolutions.stream()
                .map(level -> level.replace(" solution.json", ""))
                .collect(Collectors.toList());
        for (int i = 0; i < levels.length; i++) {
            String levelName = levels[i].substring(0, levels[i].indexOf('.'));
            Button levelButton = new Button(levelName);
            // Large, rounded, bold font, subtle shadow
            String baseStyle = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 12;";
            String normalEffect = "-fx-effect: dropshadow(gaussian, #00000022, 4, 0.2, 0, 1);";
            String hoverEffect = "-fx-effect: dropshadow(gaussian, #00000055, 12, 0.3, 0, 4);";
            levelButton.setStyle(baseStyle + normalEffect);
            levelButton.setMinWidth(140);
            levelButton.setMinHeight(48);
            levelButton.setPadding(new Insets(8, 16, 8, 16));
            levelButton.setOnMouseEntered(e -> {
                String current = levelButton.getStyle();
                // Replace only the -fx-effect property
                String updated = current.replaceAll("-fx-effect:[^;]*;?", "") + hoverEffect;
                levelButton.setStyle(updated);
            });
            levelButton.setOnMouseExited(e -> {
                String current = levelButton.getStyle();
                String updated = current.replaceAll("-fx-effect:[^;]*;?", "") + normalEffect;
                levelButton.setStyle(updated);
            });
            levelButton.setOnAction(event -> {
                Level level = Level.load(levelName);
                mainWindow.setScene(createRenderLevelScene(levelName, level));
            });
            if (levelsSolutions.contains(levelName)) {
                changeButtonColour(levelButton, SOLVED_GREEN);
            } else {
                changeButtonColour(levelButton, UNSOLVED_RED);
            }

            gridPane.add(levelButton, i % 5, i / 5);
            if (i == levels.length - 1) {
                Button backLevelLoaderButton = makeBackButton(mainMenuScene);
                backLevelLoaderButton.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 12;");
                changeButtonColour(backLevelLoaderButton, BUTTON_BLUE);
                backLevelLoaderButton.setMinWidth(140);
                backLevelLoaderButton.setMinHeight(48);
                gridPane.add(backLevelLoaderButton, 0, ((i /5) + 1));
            }
        }
        gridPane.setPadding(new Insets(30,30,30,30));
        gridPane.setVgap(20);
        gridPane.setHgap(20);

        scrollPane.setStyle("-fx-background: " + SCENE_BLUE + ";");
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setContent(gridPane);
        // Also set the viewport background for full coverage
        scrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
            scrollPane.lookup(".viewport").setStyle("-fx-background-color: " + SCENE_BLUE + ";");
        });
        levelLoadScene = new Scene(scrollPane, SCENE_WIDTH, SCENE_HEIGHT);

        return levelLoadScene;
    }
}
