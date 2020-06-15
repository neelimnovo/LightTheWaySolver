package ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Border;
import javafx.scene.layout.GridPane;
import model.GridCell;
import model.GridLayout;
import model.Level;
import model.interactionObjects.StaticGridObject;
import model.interactionObjects.*;
import model.interactionObjects.filters.BlueFilter;
import model.interactionObjects.filters.RedFilter;
import model.interactionObjects.filters.YellowFilter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;

import static ui.MainMenu.*;


/** This class is heavily coupled with the GridLayout class and most likely
 *  violates the Law of Demeter, but I want to keep all the UI design and logic
 *  separately in the UI package
**/

public class LevelGrid {

    static Button[][] gridButtonRefArray;
    static TextField levelName;

    // These comboboxes are static so that they can be parsed in a different method without being passed
    // as a parameter
    static ComboBox<Integer> redFiltersList;
    static ComboBox<Integer> blueFiltersList;
    static ComboBox<Integer> yellowFiltersList;
    static ComboBox<Integer> upwardLightSourceList;
    static ComboBox<Integer> downwardLightSourceList;
    static ComboBox<Integer> leftwardLightSourceList;
    static ComboBox<Integer> rightwardLightSourceList;
    static ComboBox<Integer> forwardMirrorsList;
    static ComboBox<Integer> backwardMirrorsList;
    static ComboBox<Integer> upwardPrismList;
    static ComboBox<Integer> downwardPrismList;
    static ComboBox<Integer> leftwardPrismList;
    static ComboBox<Integer> rightwardPrismList;
    static ComboBox<Integer> upwardTJunctionsList;
    static ComboBox<Integer> downwardTJunctionsList;
    static ComboBox<Integer> leftwardTJunctionsList;
    static ComboBox<Integer> rightwardTJunctionsList;

    static ComboBox<Integer> upRedShifterList;
    static ComboBox<Integer> downRedShifterList;
    static ComboBox<Integer> leftRedShifterList;
    static ComboBox<Integer> rightRedShifterList;

    static ComboBox<Integer> upBlueShifterList;
    static ComboBox<Integer> downBlueShifterList;
    static ComboBox<Integer> leftBlueShifterList;
    static ComboBox<Integer> rightBlueShifterList;

    static ComboBox<Integer> upYellowShifterList;
    static ComboBox<Integer> downYellowShifterList;
    static ComboBox<Integer> leftYellowShifterList;
    static ComboBox<Integer> rightYellowShifterList;

    static GridPane levelGrid;
    public static HashMap<String, ImageView> imageMap = new HashMap<>();

    public static void processLevel(String xDim, String yDim) {
        GridPane uiGridPane = initialiseLevelGridInput(Integer.parseInt(xDim), Integer.parseInt(yDim));
        uiGridPane.setPadding(new Insets(30, 30, 30, 50));
        levelGridScene = new Scene(uiGridPane, SCENE_WIDTH, SCENE_HEIGHT);
        mainWindow.setScene(levelGridScene);
    }

    static GridPane initialiseLevelGridInput(int xDim, int yDim) {
        levelGrid = new GridPane();
        GridPane innerGrid = new GridPane();
        GridPane interactionObjectsGrid = new GridPane();

        Button constructLevel = new Button("Construct");
        changeButtonColour(constructLevel, BUTTON_BLUE);
        constructLevel.setOnAction(event -> processLevelCreationInputs(xDim, yDim));

        Button backLevelGrid = makeBackButton(levelSetupScene);
        changeButtonColour(backLevelGrid, BUTTON_BLUE);

        levelName = new TextField();
        levelName.setPromptText("Level Name");

        Label interactionObjectsLabel = new Label("Interactable Objects:");
        Insets padding = new Insets(5, 0, 5, 0);
        interactionObjectsLabel.setPadding(padding);

        GridPane.setConstraints(constructLevel, 0, 0);
        GridPane.setConstraints(levelName, 2, 0);
        GridPane.setConstraints(backLevelGrid, 4, 0);
        GridPane.setConstraints(innerGrid, 0, 1);
        GridPane.setConstraints(interactionObjectsLabel, 0, 2);
        GridPane.setConstraints(interactionObjectsGrid, 0, 3);

        levelGrid.setVgap(15);
        levelGrid.setHgap(15);

        setupImageMap();

        setupLevelStaticLayoutGrid(xDim, yDim, innerGrid);


        Label redFiltersLabel = new Label("", imageMap.get("redFilter.png"));
        Label blueFiltersLabel = new Label("", imageMap.get("blueFilter.png"));
        Label yellowFiltersLabel = new Label("", imageMap.get("yellowFilter.png"));

        Label upLightSourcesLabel = new Label("", imageMap.get("upLight.png"));
        Label downLightSourcesLabel = new Label("", imageMap.get("downLight.png"));
        Label leftLightSourcesLabel = new Label("", imageMap.get("leftLight.png"));
        Label rightLightSourcesLabel = new Label("", imageMap.get("rightLight.png"));

        Label forwardMirrorsLabel = new Label("", imageMap.get("frontMirror.png"));
        Label backwardMirrorsLabel = new Label("", imageMap.get("backMirror.png"));

        Label upPrismsLabel = new Label("", imageMap.get("upPrism.png"));
        Label downPrismsLabel = new Label("", imageMap.get("downPrism.png"));
        Label leftPrismsLabel = new Label("", imageMap.get("leftPrism.png"));
        Label rightPrismsLabel = new Label("", imageMap.get("rightPrism.png"));

        Label upTJunctionsLabel = new Label("", imageMap.get("upTJunction.png"));
        Label downTJunctionsLabel = new Label("", imageMap.get("downTJunction.png"));
        Label leftTJunctionsLabel = new Label("", imageMap.get("leftTJunction.png"));
        Label rightTJunctionsLabel = new Label("", imageMap.get("rightTJunction.png"));

        Label upRedShifterLabel = new Label("", imageMap.get("upRedShift.png"));
        Label downRedShifterLabel = new Label("", imageMap.get("downRedShift.png"));
        Label leftRedShifterLabel = new Label("", imageMap.get("leftRedShift.png"));
        Label rightRedShifterLabel = new Label("", imageMap.get("rightRedShift.png"));

        Label upBlueShifterLabel = new Label("", imageMap.get("upBlueShift.png"));
        Label downBlueShifterLabel = new Label("", imageMap.get("downBlueShift.png"));
        Label leftBlueShifterLabel = new Label("", imageMap.get("leftBlueShift.png"));
        Label rightBlueShifterLabel = new Label("", imageMap.get("rightBlueShift.png"));

        Label upYellowShifterLabel = new Label("", imageMap.get("upYellowShift.png"));
        Label downYellowShifterLabel = new Label("", imageMap.get("downYellowShift.png"));
        Label leftYellowShifterLabel = new Label("", imageMap.get("leftYellowShift.png"));
        Label rightYellowShifterLabel = new Label("", imageMap.get("rightYellowShift.png"));

        //redFiltersLabel.setPadding(padding);
        redFiltersList = new ComboBox<>();
        redFiltersList.setValue(0);
        blueFiltersList = new ComboBox<>();
        blueFiltersList.setValue(0);
        yellowFiltersList = new ComboBox<>();
        yellowFiltersList.setValue(0);
        upwardLightSourceList = new ComboBox<>();
        upwardLightSourceList.setValue(0);
        downwardLightSourceList = new ComboBox<>();
        downwardLightSourceList.setValue(0);
        leftwardLightSourceList = new ComboBox<>();
        leftwardLightSourceList.setValue(0);
        rightwardLightSourceList = new ComboBox<>();
        rightwardLightSourceList.setValue(0);
        forwardMirrorsList = new ComboBox<>();
        forwardMirrorsList.setValue(0);
        backwardMirrorsList = new ComboBox<>();
        backwardMirrorsList.setValue(0);
        upwardPrismList = new ComboBox<>();
        upwardPrismList.setValue(0);
        downwardPrismList = new ComboBox<>();
        downwardPrismList.setValue(0);
        leftwardPrismList = new ComboBox<>();
        leftwardPrismList.setValue(0);
        rightwardPrismList = new ComboBox<>();
        rightwardPrismList.setValue(0);
        upwardTJunctionsList = new ComboBox<>();
        upwardTJunctionsList.setValue(0);
        downwardTJunctionsList = new ComboBox<>();
        downwardTJunctionsList.setValue(0);
        leftwardTJunctionsList = new ComboBox<>();
        leftwardTJunctionsList.setValue(0);
        rightwardTJunctionsList = new ComboBox<>();
        rightwardTJunctionsList.setValue(0);

        upRedShifterList = new ComboBox<>();
        upRedShifterList.setValue(0);
        downRedShifterList = new ComboBox<>();
        downRedShifterList.setValue(0);
        leftRedShifterList = new ComboBox<>();
        leftRedShifterList.setValue(0);
        rightRedShifterList = new ComboBox<>();
        rightRedShifterList.setValue(0);

        upBlueShifterList = new ComboBox<>();
        upBlueShifterList.setValue(0);
        downBlueShifterList = new ComboBox<>();
        downBlueShifterList.setValue(0);
        leftBlueShifterList = new ComboBox<>();
        leftBlueShifterList.setValue(0);
        rightBlueShifterList = new ComboBox<>();
        rightBlueShifterList.setValue(0);

        upYellowShifterList = new ComboBox<>();
        upYellowShifterList.setValue(0);
        downYellowShifterList = new ComboBox<>();
        downYellowShifterList.setValue(0);
        leftYellowShifterList = new ComboBox<>();
        leftYellowShifterList.setValue(0);
        rightYellowShifterList = new ComboBox<>();
        rightYellowShifterList.setValue(0);

        for (int i = 0; i <= 8; i++) {
            redFiltersList.getItems().add(i);
            blueFiltersList.getItems().add(i);
            yellowFiltersList.getItems().add(i);

            upwardLightSourceList.getItems().add(i);
            downwardLightSourceList.getItems().add(i);
            leftwardLightSourceList.getItems().add(i);
            rightwardLightSourceList.getItems().add(i);

            forwardMirrorsList.getItems().add(i);
            backwardMirrorsList.getItems().add(i);

            upwardPrismList.getItems().add(i);
            downwardPrismList.getItems().add(i);
            leftwardPrismList.getItems().add(i);
            rightwardPrismList.getItems().add(i);

            upwardTJunctionsList.getItems().add(i);
            downwardTJunctionsList.getItems().add(i);
            leftwardTJunctionsList.getItems().add(i);
            rightwardTJunctionsList.getItems().add(i);

            upRedShifterList.getItems().add(i);
            downRedShifterList.getItems().add(i);
            leftRedShifterList.getItems().add(i);
            rightRedShifterList.getItems().add(i);

            upBlueShifterList.getItems().add(i);
            downBlueShifterList.getItems().add(i);
            leftBlueShifterList.getItems().add(i);
            rightBlueShifterList.getItems().add(i);

            upYellowShifterList.getItems().add(i);
            downYellowShifterList.getItems().add(i);
            leftYellowShifterList.getItems().add(i);
            rightYellowShifterList.getItems().add(i);
        }

        int tempRowIndex = 0;
        int listContentIndex = 1;


        GridPane.setConstraints(redFiltersLabel, listContentIndex - 1, tempRowIndex);
        GridPane.setConstraints(redFiltersList, listContentIndex, tempRowIndex);

        GridPane.setConstraints(upLightSourcesLabel, listContentIndex + 1, tempRowIndex);
        GridPane.setConstraints(upwardLightSourceList, listContentIndex + 2, tempRowIndex);

        GridPane.setConstraints(upPrismsLabel, listContentIndex + 3, tempRowIndex);
        GridPane.setConstraints(upwardPrismList, listContentIndex + 4, tempRowIndex);

        GridPane.setConstraints(upTJunctionsLabel, listContentIndex + 5, tempRowIndex);
        GridPane.setConstraints(upwardTJunctionsList, listContentIndex + 6, tempRowIndex++);


        GridPane.setConstraints(blueFiltersLabel, listContentIndex - 1, tempRowIndex);
        GridPane.setConstraints(blueFiltersList, listContentIndex, tempRowIndex);

        GridPane.setConstraints(downLightSourcesLabel, listContentIndex + 1, tempRowIndex);
        GridPane.setConstraints(downwardLightSourceList, listContentIndex + 2, tempRowIndex);

        GridPane.setConstraints(downPrismsLabel, listContentIndex + 3, tempRowIndex);
        GridPane.setConstraints(downwardPrismList, listContentIndex + 4, tempRowIndex);

        GridPane.setConstraints(downTJunctionsLabel, listContentIndex + 5, tempRowIndex);
        GridPane.setConstraints(downwardTJunctionsList, listContentIndex + 6, tempRowIndex++);


        GridPane.setConstraints(yellowFiltersLabel, listContentIndex - 1, tempRowIndex);
        GridPane.setConstraints(yellowFiltersList, listContentIndex, tempRowIndex);

        GridPane.setConstraints(leftLightSourcesLabel, listContentIndex + 1, tempRowIndex);
        GridPane.setConstraints(leftwardLightSourceList, listContentIndex + 2, tempRowIndex);

        GridPane.setConstraints(leftPrismsLabel, listContentIndex + 3, tempRowIndex);
        GridPane.setConstraints(leftwardPrismList, listContentIndex + 4, tempRowIndex);

        GridPane.setConstraints(leftTJunctionsLabel, listContentIndex + 5, tempRowIndex);
        GridPane.setConstraints(leftwardTJunctionsList, listContentIndex + 6, tempRowIndex++);


        GridPane.setConstraints(forwardMirrorsLabel, listContentIndex - 1, tempRowIndex);
        GridPane.setConstraints(forwardMirrorsList, listContentIndex, tempRowIndex);

        GridPane.setConstraints(rightLightSourcesLabel, listContentIndex + 1, tempRowIndex);
        GridPane.setConstraints(rightwardLightSourceList, listContentIndex + 2, tempRowIndex);

        GridPane.setConstraints(rightPrismsLabel, listContentIndex + 3, tempRowIndex);
        GridPane.setConstraints(rightwardPrismList, listContentIndex + 4, tempRowIndex);

        GridPane.setConstraints(rightTJunctionsLabel, listContentIndex + 5, tempRowIndex);
        GridPane.setConstraints(rightwardTJunctionsList, listContentIndex + 6, tempRowIndex++);


        GridPane.setConstraints(backwardMirrorsLabel, listContentIndex - 1, tempRowIndex);
        GridPane.setConstraints(backwardMirrorsList, listContentIndex, tempRowIndex++);


        GridPane.setConstraints(upRedShifterLabel, listContentIndex - 1, tempRowIndex);
        GridPane.setConstraints(upRedShifterList, listContentIndex, tempRowIndex);

        GridPane.setConstraints(downRedShifterLabel, listContentIndex + 1, tempRowIndex);
        GridPane.setConstraints(downRedShifterList, listContentIndex + 2, tempRowIndex);

        GridPane.setConstraints(leftRedShifterLabel, listContentIndex + 3, tempRowIndex);
        GridPane.setConstraints(leftRedShifterList, listContentIndex + 4, tempRowIndex);

        GridPane.setConstraints(rightRedShifterLabel, listContentIndex + 5, tempRowIndex);
        GridPane.setConstraints(rightRedShifterList, listContentIndex + 6, tempRowIndex++);


        GridPane.setConstraints(upBlueShifterLabel, listContentIndex - 1, tempRowIndex);
        GridPane.setConstraints(upBlueShifterList, listContentIndex, tempRowIndex);

        GridPane.setConstraints(downBlueShifterLabel, listContentIndex + 1, tempRowIndex);
        GridPane.setConstraints(downBlueShifterList, listContentIndex + 2, tempRowIndex);

        GridPane.setConstraints(leftBlueShifterLabel, listContentIndex + 3, tempRowIndex);
        GridPane.setConstraints(leftBlueShifterList, listContentIndex + 4, tempRowIndex);

        GridPane.setConstraints(rightBlueShifterLabel, listContentIndex + 5, tempRowIndex);
        GridPane.setConstraints(rightBlueShifterList, listContentIndex + 6, tempRowIndex++);


        GridPane.setConstraints(upYellowShifterLabel, listContentIndex - 1, tempRowIndex);
        GridPane.setConstraints(upYellowShifterList, listContentIndex, tempRowIndex);

        GridPane.setConstraints(downYellowShifterLabel, listContentIndex + 1, tempRowIndex);
        GridPane.setConstraints(downYellowShifterList, listContentIndex + 2, tempRowIndex);

        GridPane.setConstraints(leftYellowShifterLabel, listContentIndex + 3, tempRowIndex);
        GridPane.setConstraints(leftYellowShifterList, listContentIndex + 4, tempRowIndex);

        GridPane.setConstraints(rightYellowShifterLabel, listContentIndex + 5, tempRowIndex);
        //noinspection UnusedAssignment
        GridPane.setConstraints(rightYellowShifterList, listContentIndex + 6, tempRowIndex++);

        interactionObjectsGrid.getChildren()
                .addAll(
                        redFiltersLabel, redFiltersList, upLightSourcesLabel, upwardLightSourceList,
                        upPrismsLabel, upwardPrismList,
                        upTJunctionsLabel, upwardTJunctionsList,

                        blueFiltersLabel, blueFiltersList, downLightSourcesLabel, downwardLightSourceList,
                        downPrismsLabel, downwardPrismList,
                        downTJunctionsLabel, downwardTJunctionsList,
                        yellowFiltersLabel, yellowFiltersList, leftLightSourcesLabel, leftwardLightSourceList,
                        leftPrismsLabel, leftwardPrismList,
                        leftTJunctionsLabel, leftwardTJunctionsList,

                        forwardMirrorsLabel, rightLightSourcesLabel, rightwardLightSourceList,
                        rightPrismsLabel, rightwardPrismList,
                        rightTJunctionsLabel, rightwardTJunctionsList,

                        forwardMirrorsList,
                        backwardMirrorsLabel,
                        backwardMirrorsList,

                        upRedShifterLabel, downRedShifterLabel, leftRedShifterLabel, rightRedShifterLabel,
                        upBlueShifterLabel, downBlueShifterLabel, leftBlueShifterLabel, rightBlueShifterLabel,
                        upYellowShifterLabel, downYellowShifterLabel, leftYellowShifterLabel, rightYellowShifterLabel,
                        upRedShifterList, downRedShifterList, leftRedShifterList, rightRedShifterList,
                        upBlueShifterList, downBlueShifterList, leftBlueShifterList, rightBlueShifterList,
                        upYellowShifterList, downYellowShifterList, leftYellowShifterList, rightYellowShifterList);

        levelGrid.setStyle("-fx-background-color:" + SCENE_BLUE);
        levelGrid.getChildren()
                .addAll(constructLevel, levelName, backLevelGrid, interactionObjectsLabel, innerGrid, interactionObjectsGrid);

        return levelGrid;
    }

    private static void setupImageMap() {
        File levelsFolder = new File("resources\\images\\");
        String[] images = levelsFolder.list();
        for (int i = 0; i < images.length; i++) {
            FileInputStream input = null;
            try {
                input = new FileInputStream("resources\\images\\" + images[i]);
                Image image = new Image(input);
                ImageView imageView = new ImageView(image);
                imageMap.put(images[i], imageView);
            } catch (FileNotFoundException e) {
                System.out.println("Couldn't load images");
                e.printStackTrace();
            }
        }
    }

    private static void setupLevelStaticLayoutGrid(int xDim, int yDim, GridPane grid) {
        gridButtonRefArray = new Button[xDim][yDim];
        for (int x = 0; x < xDim; x++) {
            for (int y = 0; y < yDim; y++) {
                Button gridItem = new Button("", readResourceImage("void.png"));
                changeButtonColour(gridItem, "#492149");
                gridItem.setMaxSize(25,25);
                gridItem.setMinSize(25,25);
                gridItem.setId("void");
                gridItem.setOnAction(event -> {
                    switch (gridItem.getId()) {
                        case "void":
                            gridItem.setGraphic(readResourceImage("wall.png"));
                            gridItem.setId("wall");
                            break;
                        case "wall":
                            gridItem.setGraphic(readResourceImage("whiteReceiver.png"));
                            gridItem.setId("whiteReceiver");
                            break;
                        case "whiteReceiver":
                            gridItem.setGraphic(readResourceImage("redReceiver.png"));
                            gridItem.setId("redReceiver");
                            break;
                        case "redReceiver":
                            gridItem.setGraphic(readResourceImage("blueReceiver.png"));
                            gridItem.setId("blueReceiver");
                            break;
                        case "blueReceiver":
                            gridItem.setGraphic(readResourceImage("yellowReceiver.png"));
                            gridItem.setId("yellowReceiver");
                            break;
                        case "yellowReceiver":
                            gridItem.setGraphic(readResourceImage("void.png"));
                            gridItem.setId("void");
                            break;
                    }
                });
                gridButtonRefArray[x][y] = gridItem;
                GridPane.setConstraints(gridItem, x, y);
                grid.getChildren().add(gridItem);
            }
        }
    }

    private static void processLevelCreationInputs(int xDim, int yDim) {
        GridLayout levelLayout = new GridLayout(xDim, yDim);

        // assign the static properties to the level layout
        for (int i = 0; i < xDim; i++) {
            for (int j = 0; j < yDim; j++) {
                String gridItem = gridButtonRefArray[i][j].getId();
                StaticGridObject sObj = StaticGridObject.getCorrectObject(gridItem);
                levelLayout.gridCellArray[i][j] = new GridCell(sObj);
            }
        }

        // Assign the appropriate number of interactable objects to the level information
        parseInteractableObjects(levelLayout);
        Level resultLevel = new Level(levelName.getText(), levelLayout);

        Alert a1 = new Alert(Alert.AlertType.NONE,
                "Level Created!", ButtonType.OK);
        // show the dialog
        a1.show();
        Level.save(resultLevel);
    }

    private static void parseInteractableObjects(GridLayout gridLayout) {
        for (int i = 0; i < redFiltersList.getValue(); i++) {
            gridLayout.redFilters.add(new RedFilter());
        }
        for (int i = 0; i < blueFiltersList.getValue(); i++) {
            gridLayout.blueFilters.add(new BlueFilter());
        }
        for (int i = 0; i < yellowFiltersList.getValue(); i++) {
            gridLayout.yellowFilters.add(new YellowFilter());
        }
        for (int i = 0; i < upwardLightSourceList.getValue(); i++) {
            gridLayout.lights.add(new LightSource(FaceOrientation.UP));
        }
        for (int i = 0; i < downwardLightSourceList.getValue(); i++) {
            gridLayout.lights.add(new LightSource(FaceOrientation.DOWN));
        }
        for (int i = 0; i < leftwardLightSourceList.getValue(); i++) {
            gridLayout.lights.add(new LightSource(FaceOrientation.LEFT));
        }
        for (int i = 0; i < rightwardLightSourceList.getValue(); i++) {
            gridLayout.lights.add(new LightSource(FaceOrientation.RIGHT));
        }
        for (int i = 0; i < forwardMirrorsList.getValue(); i++) {
            gridLayout.frontMirrors.add(new ForwardMirror());
        }
        for (int i = 0; i < backwardMirrorsList.getValue(); i++) {
            gridLayout.backMirrors.add(new BackwardMirror());
        }
        for (int i = 0; i < upwardPrismList.getValue(); i++) {
            gridLayout.prisms.add(new Prism(FaceOrientation.UP));
        }
        for (int i = 0; i < downwardPrismList.getValue(); i++) {
            gridLayout.prisms.add(new Prism(FaceOrientation.DOWN));
        }
        for (int i = 0; i < leftwardPrismList.getValue(); i++) {
            gridLayout.prisms.add(new Prism(FaceOrientation.LEFT));
        }
        for (int i = 0; i < rightwardPrismList.getValue(); i++) {
            gridLayout.prisms.add(new Prism(FaceOrientation.RIGHT));
        }
        for (int i = 0; i < upwardTJunctionsList.getValue(); i++) {
            gridLayout.tJunctions.add(new TJunction(FaceOrientation.UP));
        }
        for (int i = 0; i < downwardTJunctionsList.getValue(); i++) {
            gridLayout.tJunctions.add(new TJunction(FaceOrientation.DOWN));
        }
        for (int i = 0; i < leftwardTJunctionsList.getValue(); i++) {
            gridLayout.tJunctions.add(new TJunction(FaceOrientation.LEFT));
        }
        for (int i = 0; i < rightwardTJunctionsList.getValue(); i++) {
            gridLayout.tJunctions.add(new TJunction(FaceOrientation.RIGHT));
        }

        for (int i = 0; i < upRedShifterList.getValue(); i++) {
            gridLayout.colourShifters.add(new ColourShifter(FaceOrientation.UP, Colour.RED));
        }
        for (int i = 0; i < upBlueShifterList.getValue(); i++) {
            gridLayout.colourShifters.add(new ColourShifter(FaceOrientation.UP, Colour.BLUE));
        }
        for (int i = 0; i < upYellowShifterList.getValue(); i++) {
            gridLayout.colourShifters.add(new ColourShifter(FaceOrientation.UP, Colour.YELLOW));
        }

        for (int i = 0; i < downRedShifterList.getValue(); i++) {
            gridLayout.colourShifters.add(new ColourShifter(FaceOrientation.DOWN, Colour.RED));
        }
        for (int i = 0; i < downBlueShifterList.getValue(); i++) {
            gridLayout.colourShifters.add(new ColourShifter(FaceOrientation.DOWN, Colour.BLUE));
        }
        for (int i = 0; i < downYellowShifterList.getValue(); i++) {
            gridLayout.colourShifters.add(new ColourShifter(FaceOrientation.DOWN, Colour.YELLOW));
        }

        for (int i = 0; i < leftRedShifterList.getValue(); i++) {
            gridLayout.colourShifters.add(new ColourShifter(FaceOrientation.LEFT, Colour.RED));
        }
        for (int i = 0; i < leftBlueShifterList.getValue(); i++) {
            gridLayout.colourShifters.add(new ColourShifter(FaceOrientation.LEFT, Colour.BLUE));
        }
        for (int i = 0; i < leftYellowShifterList.getValue(); i++) {
            gridLayout.colourShifters.add(new ColourShifter(FaceOrientation.LEFT, Colour.YELLOW));
        }

        for (int i = 0; i < rightRedShifterList.getValue(); i++) {
            gridLayout.colourShifters.add(new ColourShifter(FaceOrientation.RIGHT, Colour.RED));
        }
        for (int i = 0; i < rightBlueShifterList.getValue(); i++) {
            gridLayout.colourShifters.add(new ColourShifter(FaceOrientation.RIGHT, Colour.BLUE));
        }
        for (int i = 0; i < rightYellowShifterList.getValue(); i++) {
            gridLayout.colourShifters.add(new ColourShifter(FaceOrientation.RIGHT, Colour.YELLOW));
        }
    }

    public static ImageView readResourceImage(String image) {
        try {
            return new ImageView(new Image(new FileInputStream("resources\\images\\" + image)));
        } catch (FileNotFoundException e) {
            System.out.println("Couldn't load images");
            e.printStackTrace();
            return null;
        }
    }
}
