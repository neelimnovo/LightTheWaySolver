package ui;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import static ui.LevelGrid.processLevel;
import static ui.MainMenu.*;

public class LevelSetup {

    //EFFECTS: Creates the scene where the level size inputs are present
    public static Scene createLevelSetupScene() {
        // Setup Layout
        GridPane gridPane = new GridPane();
        levelSetupScene = new Scene(gridPane, SCENE_WIDTH, SCENE_HEIGHT);
        gridPane.setPadding(new Insets(30,30,30,50));

        Label entryText = new Label("Enter dimensions of level");

        TextField xLength = new TextField();
        xLength.setPromptText("X Dimensions");

        TextField yLength = new TextField();
        yLength.setPromptText("Y Dimensions");

        Button processLevel = new Button("Create Level");
        changeButtonColour(processLevel, BUTTON_BLUE);
        processLevel.setOnAction(event -> {
            processLevel(xLength.getText(), yLength.getText());
        });

        Button backLevelSetup = makeBackButton(mainMenuScene);

        //Setup Layout positions
        gridPane.setVgap(15);
        int nodeColumn = 0;
        int nodeRow = 0;
        GridPane.setConstraints(entryText, nodeColumn, nodeRow++);
        GridPane.setConstraints(xLength, nodeColumn, nodeRow++);
        GridPane.setConstraints(yLength, nodeColumn, nodeRow++);
        GridPane.setConstraints(processLevel, nodeColumn, nodeRow++);
        GridPane.setConstraints(backLevelSetup, nodeColumn ,nodeRow++);
        gridPane.getChildren().addAll(entryText, xLength, yLength, processLevel, backLevelSetup);
        gridPane.setStyle("-fx-background-color:" + SCENE_BLUE);

        return levelSetupScene;
    }
}
