package ui;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import static ui.LevelLoader.createLevelLoaderScene;
import static ui.LevelSetup.createLevelSetupScene;
import static ui.LevelStats.createLevelStatsScene;

public class MainMenu extends Application {

    //Main window and all other interaction scenes
    static Stage mainWindow;
    static Scene mainMenuScene;
    static Scene levelSetupScene;
    static Scene levelStatsScene;
    static Scene levelGridScene;
    static Scene levelLoadScene;
    static Scene levelRenderScene;
    final static int SCENE_WIDTH = 1280;
    final static int SCENE_HEIGHT = 720;

    // constants for themes
    final static String BUTTON_BLUE = "#81b9bf";
    final static String SCENE_BLUE = "#b2ebf2";


    @Override
    public void start(Stage primaryStage) {
        //Setup main stage
        primaryStage.setTitle("Search The Way");
        mainWindow = primaryStage;

        Button newLevelButton = new Button("Create new level");
        newLevelButton.setStyle("-fx-background-radius: 30");
        changeButtonColour(newLevelButton, BUTTON_BLUE);
        newLevelButton.setOnAction(event -> mainWindow.setScene(createLevelSetupScene()));

        Button solveLevelButton = new Button("Load level");
        changeButtonColour(solveLevelButton, BUTTON_BLUE);
        solveLevelButton.setOnAction(event -> mainWindow.setScene(createLevelLoaderScene()));

        Button showLevelStatsButton = new Button("Show level statistics");
        changeButtonColour(showLevelStatsButton, BUTTON_BLUE);
        showLevelStatsButton.setOnAction(event -> mainWindow.setScene(createLevelStatsScene()));

        materialiseButton(newLevelButton);

        //Setup layout for main menu
        GridPane mainMenuGridPane = new GridPane();
        mainMenuGridPane.setPadding(new Insets(30,30,30,50));
        mainMenuGridPane.setVgap(20);
        GridPane.setConstraints(newLevelButton, 0, 0);
        GridPane.setConstraints(solveLevelButton, 0, 1);
        GridPane.setConstraints(showLevelStatsButton,0, 2);
        mainMenuGridPane.getChildren().addAll(newLevelButton, solveLevelButton, showLevelStatsButton);
        mainMenuGridPane.setStyle("-fx-background-color:" + SCENE_BLUE);
        mainMenuScene = new Scene(mainMenuGridPane, SCENE_WIDTH, SCENE_HEIGHT);
        mainWindow.setScene(mainMenuScene);
        mainWindow.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    // EFFECTS: Changes JavaFX button colours
    public static void changeButtonColour(Button button, String colour) {
        button.setStyle("-fx-background-color: " + colour + "; ");
    }

    //EFFECTS: Creates a button that transitions to prior scene on click
    public static Button makeBackButton(Scene priorScene) {
        Button result = new Button("Go back");
        changeButtonColour(result, BUTTON_BLUE);
        result.setOnAction(event -> mainWindow.setScene(priorScene));
        return result;
    }

    // TODO make this work
    // EFFECTS: Gives the JavaFX button a material design-like aesthetic
    public static void materialiseButton(Button button) {
        button.setFont(Font.font ("file:resources/fonts/SourceSansPro-SemiBold.ttf", 18));
    }
}
