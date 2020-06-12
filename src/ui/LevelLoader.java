package ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import model.Level;

import java.io.File;

import static ui.LevelRender.createRenderLevelScene;
import static ui.MainMenu.*;

public class LevelLoader {

    static Scene createLevelLoaderScene() {
        ScrollPane scrollPane = new ScrollPane();
        GridPane gridPane = new GridPane();
        File levelsFolder = new File("src\\saveFiles\\");
        String[] levels = levelsFolder.list();
        for (int i = 0; i < levels.length; i++) {
            String levelName = levels[i].substring(0, levels[i].indexOf('.'));
            Button levelButton = new Button(levelName);
            levelButton.setOnAction(event -> {
                Level level = Level.load(levelName);
                mainWindow.setScene(createRenderLevelScene(levelName, level));
            });
            changeButtonColour(levelButton, BUTTON_BLUE);
            gridPane.add(levelButton, i % 5, i / 5);
            if (i == levels.length - 1) {
                Button backLevelLoaderButton = makeBackButton(mainMenuScene);
                changeButtonColour(backLevelLoaderButton, BUTTON_BLUE);
                gridPane.add(backLevelLoaderButton, 0, ((i /5) + 1));
            }
        }
        gridPane.setPadding(new Insets(30,30,30,30));
        gridPane.setVgap(15);
        gridPane.setHgap(15);

        gridPane.setStyle("-fx-background-color:" + SCENE_BLUE);
        scrollPane.setContent(gridPane);
        levelLoadScene = new Scene(scrollPane, SCENE_WIDTH, SCENE_HEIGHT);

        return levelLoadScene;
    }

}
