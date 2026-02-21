package ui;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import static ui.LevelLoader.createLevelLoaderScene;
import static ui.LevelSetup.createLevelSetupScene;
import static ui.LevelStats.createLevelStatsScene;

import java.io.File;
import java.io.FileInputStream;


public class MainMenu extends Application {

    // Main window and other scenes
    static Stage mainWindow;
    static Scene mainMenuScene;
    static Scene levelSetupScene;
    static Scene levelStatsScene;
    static Scene levelGridScene;
    static Scene levelLoadScene;
    static Scene levelRenderScene;
    final static int SCENE_WIDTH = 1280;
    final static int SCENE_HEIGHT = 720;

    // Theme constants (used throughout the UI)
    final static String BUTTON_BLUE = "#81b9bf";
    final static String SCENE_BLUE = "#b2ebf2";

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Search The Way");
        mainWindow = primaryStage;

        // Title / header
        Label title = new Label("Search The Way");
        title.setFont(Font.font("System", FontWeight.BOLD, 36));
        title.setTextFill(Color.web("#083344"));

        Label subtitle = new Label("Solves puzzles from the 2007 NDS game, Light The Way");
        subtitle.setFont(Font.font("System", 14));
        subtitle.setTextFill(Color.web("#2e2e2e"));

        // Buttons
        Button newLevelButton = new Button("Create new level");
        newLevelButton.setOnAction(e -> mainWindow.setScene(createLevelSetupScene()));

        Button solveLevelButton = new Button("Load level");
        solveLevelButton.setOnAction(e -> mainWindow.setScene(createLevelLoaderScene()));

        Button showLevelStatsButton = new Button("Show level statistics");
        showLevelStatsButton.setOnAction(e -> mainWindow.setScene(createLevelStatsScene()));

        // Apply consistent material styling
        changeButtonColour(newLevelButton, BUTTON_BLUE);
        changeButtonColour(solveLevelButton, BUTTON_BLUE);
        changeButtonColour(showLevelStatsButton, BUTTON_BLUE);
        materialiseButton(newLevelButton);
        materialiseButton(solveLevelButton);
        materialiseButton(showLevelStatsButton);

        // Layout
        HBox buttonsRow = new HBox(18, newLevelButton, solveLevelButton, showLevelStatsButton);
        buttonsRow.setAlignment(Pos.CENTER);

        VBox content = new VBox(18, title, subtitle, buttonsRow);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40));

        StackPane root = new StackPane(content);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #e6fbff, " + SCENE_BLUE + ");");

        Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);

        // Keyboard shortcuts
        scene.getAccelerators().put(KeyCombination.keyCombination("Ctrl+N"), () -> newLevelButton.fire());
        scene.getAccelerators().put(KeyCombination.keyCombination("Ctrl+L"), () -> solveLevelButton.fire());
        scene.getAccelerators().put(KeyCombination.keyCombination("Ctrl+S"), () -> showLevelStatsButton.fire());

        // finalise
        mainMenuScene = scene;
        mainWindow.setScene(mainMenuScene);
        mainWindow.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    // EFFECTS: Changes JavaFX button colours (kept public for reuse)
    public static void changeButtonColour(Button button, String colour) {
        // keep a simple, consistent appearance — text stays readable on colored buttons
        button.setStyle("-fx-background-color: " + colour + "; -fx-text-fill: white; -fx-background-radius: 12;");
    }

    // EFFECTS: Creates a button that transitions to prior scene on click
    public static Button makeBackButton(Scene priorScene) {
        Button result = new Button("Go back");
        changeButtonColour(result, BUTTON_BLUE);
        materialiseButton(result);
        result.setOnAction(event -> mainWindow.setScene(priorScene));
        return result;
    }

    // EFFECTS: Gives the JavaFX button a material design-like aesthetic and robust font loading
    public static void materialiseButton(Button button) {
        final double fontSize = 15;

        // try load bundled font (fallback to system font)
        try {
            File fontFile = new File("resources/fonts/SourceSansPro-SemiBold.ttf");
            if (fontFile.exists()) {
                try (FileInputStream fis = new FileInputStream(fontFile)) {
                    Font loaded = Font.loadFont(fis, fontSize);
                    if (loaded != null) button.setFont(loaded);
                    else button.setFont(Font.font("System", FontWeight.SEMI_BOLD, fontSize));
                }
            } else {
                button.setFont(Font.font("System", FontWeight.SEMI_BOLD, fontSize));
            }
        } catch (Exception ex) {
            button.setFont(Font.font("System", FontWeight.SEMI_BOLD, fontSize));
        }

        button.setPadding(new Insets(10, 26, 10, 26));
        button.setMinWidth(190);
        button.setStyle(button.getStyle() + " -fx-cursor: hand; -fx-font-weight: 600;");

        // subtle hover effect
        DropShadow hover = new DropShadow(12, Color.rgb(0, 0, 0, 0.18));
        button.setOnMouseEntered(e -> {
            button.setEffect(hover);
            button.setScaleX(1.02);
            button.setScaleY(1.02);
        });
        button.setOnMouseExited(e -> {
            button.setEffect(null);
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });
    }
}