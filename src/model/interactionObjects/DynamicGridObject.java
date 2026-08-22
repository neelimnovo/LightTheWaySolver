package model.interactionObjects;

import javafx.util.Pair;
import model.GridCell;
import model.interactionObjects.filters.BlueFilter;
import model.interactionObjects.filters.RedFilter;
import model.interactionObjects.filters.YellowFilter;
import searchLogic.Light;
import java.util.ArrayList;
import searchLogic.ShortQueue;

import static model.interactionObjects.Colour.*;
import static model.interactionObjects.FaceOrientation.*;

public abstract class DynamicGridObject {

    public abstract String getCorrectImageString();

    public abstract ArrayList<Pair<Integer, Integer>> filter(GridCell[][] grid, ArrayList<Pair<Integer, Integer>> emptySpots);

    public abstract ArrayList<Pair<Integer, Integer>> staticFilter(GridCell[][] grid, ArrayList<Pair<Integer, Integer>> emptySpots);

    public abstract void interactWithLight(short light, GridCell[][] grid, ShortQueue lightProcessingQueue);

    public abstract String toString();

    // EFFECTS: Returns a stable identifier for this kind of object, used when persisting
    // a drafted placement to disk. Derived from the image name so it stays in sync with it
    public final String getTypeId() {
        String image = getCorrectImageString();
        return image.substring(0, image.lastIndexOf('.'));
    }

    // EFFECTS: Recreates a dynamic grid object from the identifier produced by getTypeId
    // Returns null if the identifier is not recognised
    public static DynamicGridObject fromTypeId(String typeId) {
        return switch (typeId) {
            case "upLight" -> new LightSource(UP);
            case "downLight" -> new LightSource(DOWN);
            case "leftLight" -> new LightSource(LEFT);
            case "rightLight" -> new LightSource(RIGHT);

            case "upPrism" -> new Prism(UP);
            case "downPrism" -> new Prism(DOWN);
            case "leftPrism" -> new Prism(LEFT);
            case "rightPrism" -> new Prism(RIGHT);

            case "upTJunction" -> new TJunction(UP);
            case "downTJunction" -> new TJunction(DOWN);
            case "leftTJunction" -> new TJunction(LEFT);
            case "rightTJunction" -> new TJunction(RIGHT);

            case "frontMirror" -> new ForwardMirror();
            case "backMirror" -> new BackwardMirror();

            case "redFilter" -> new RedFilter();
            case "blueFilter" -> new BlueFilter();
            case "yellowFilter" -> new YellowFilter();

            case "upRedShift" -> new ColourShifter(UP, RED);
            case "upBlueShift" -> new ColourShifter(UP, BLUE);
            case "upYellowShift" -> new ColourShifter(UP, YELLOW);
            case "downRedShift" -> new ColourShifter(DOWN, RED);
            case "downBlueShift" -> new ColourShifter(DOWN, BLUE);
            case "downYellowShift" -> new ColourShifter(DOWN, YELLOW);
            case "leftRedShift" -> new ColourShifter(LEFT, RED);
            case "leftBlueShift" -> new ColourShifter(LEFT, BLUE);
            case "leftYellowShift" -> new ColourShifter(LEFT, YELLOW);
            case "rightRedShift" -> new ColourShifter(RIGHT, RED);
            case "rightBlueShift" -> new ColourShifter(RIGHT, BLUE);
            case "rightYellowShift" -> new ColourShifter(RIGHT, YELLOW);

            default -> null;
        };
    }
}
