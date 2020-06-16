package ui;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import model.Stats;
import java.io.File;
import java.util.ArrayList;

import static ui.MainMenu.*;

public class LevelStats {

    static ArrayList<Stats> levelStatsArray;

    static Scene createLevelStatsScene() {
        levelStatsArray = new ArrayList<>();
        // Load all the statistics
        File levelsFolder = new File("src\\solutionFiles\\");
        String[] levels = levelsFolder.list();
        for (String level : levels) {
            Stats tempStat = new Stats();
            tempStat.load(level);
            levelStatsArray.add(tempStat);
        }

        // Setup tableView
        TableView tableView = new TableView();
        tableView.setPlaceholder(new Label("No save files!"));

        TableColumn<String, Stats> titleColumn = new TableColumn<>("Level Number");
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("levelName"));

        TableColumn<Long, Stats> timeColumn = new TableColumn<>("Solving Time (seconds)");
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("totalTime"));

        TableColumn<Long, Stats> attemptPermutationsColumn = new TableColumn<>("Attempted Permutations");
        attemptPermutationsColumn.setCellValueFactory(new PropertyValueFactory<>("attemptPermutations"));

        TableColumn<Long, Stats> totalPermutationsColumn = new TableColumn<>("Total Possible Permutations");
        totalPermutationsColumn.setCellValueFactory(new PropertyValueFactory<>("totalPermutations"));

        TableColumn<Double, Stats> permutationsRatioColumn = new TableColumn<>("Percentage of Attempted Permutations");
        permutationsRatioColumn.setCellValueFactory(new PropertyValueFactory<>("permutationRatio"));
        permutationsRatioColumn.setMinWidth(300);

        tableView.getColumns().addAll(titleColumn, timeColumn,
                attemptPermutationsColumn, totalPermutationsColumn, permutationsRatioColumn);

        for (Stats stats: levelStatsArray) {
            tableView.getItems().add(stats);
        }

        GridPane gridPane = new GridPane();
        GridPane.setConstraints(tableView, 0 , 0);
        GridPane.setHgrow(tableView, Priority.ALWAYS);
        GridPane.setVgrow(tableView, Priority.ALWAYS);

        Button backLevelStats = makeBackButton(mainMenuScene);
        changeButtonColour(backLevelStats, BUTTON_BLUE);
        GridPane.setConstraints(backLevelStats, 0, 1);

        gridPane.getChildren().addAll(tableView, backLevelStats);
        gridPane.setStyle("-fx-background-color:" + SCENE_BLUE);
        levelStatsScene = new Scene(gridPane, SCENE_WIDTH, SCENE_HEIGHT);
        return levelStatsScene;
    }
}
