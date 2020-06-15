package model.interactionObjects;

import javafx.util.Pair;
import model.GridCell;
import model.GridLayout;
import searchLogic.Light;

import java.util.ArrayList;
import java.util.LinkedList;

import static model.interactionObjects.Colour.*;
import static model.interactionObjects.FaceOrientation.*;
import static model.interactionObjects.StaticGridObject.*;

public class Prism extends DynamicGridObject {
    FaceOrientation orientation;

    public Prism(FaceOrientation orientation) {
        this.orientation = orientation;
    }

    @Override
    public String getCorrectImageString() {
        switch (orientation) {
            case UP:
                return "upPrism.png";
            case DOWN:
                return "downPrism.png";
            case LEFT:
                return "leftPrism.png";
            case RIGHT:
                return "rightPrism.png";
            default:
                return null;
        }
    }

    @Override
    // EFFECTS: Filters based on neighbouring wall or incorrect receiver
    public ArrayList<Pair<Integer, Integer>> filter(GridCell[][] grid, ArrayList<Pair<Integer, Integer>> emptySpots) {
        ArrayList<Pair<Integer, Integer>> resultSpots = new ArrayList<>();
        for (Pair<Integer, Integer> spot : emptySpots) {
            int spotX = spot.getKey();
            int spotY = spot.getValue();
            if (GridLayout.isWithinBounds(grid, spotX, spotY - 1)
                    && isValidNeighbour(grid, spotX, spotY - 1, UP)
                    && GridLayout.isWithinBounds(grid, spotX, spotY + 1)
                    && isValidNeighbour(grid, spotX, spotY + 1, DOWN)
                    && GridLayout.isWithinBounds(grid, spotX - 1, spotY)
                    && isValidNeighbour(grid, spotX - 1, spotY, LEFT)
                    && GridLayout.isWithinBounds(grid, spotX + 1, spotY)
                    && isValidNeighbour(grid, spotX + 1, spotY, RIGHT)) {
                resultSpots.add(new Pair<>(spotX, spotY));
            }
        }
        return resultSpots;
        //return emptySpots;
    }

    private boolean isValidNeighbour(GridCell[][] grid, int spotX, int spotY, FaceOrientation orientation) {
        if (grid[spotX][spotY].cellStaticItem == WALL) {
            return false;
        } else if (grid[spotX][spotY].cellStaticItem == EMPTY) {
            DynamicGridObject dgo = grid[spotX][spotY].cellDynamicItem;
            if (dgo == null) {
                return true;
            } else {
                return dgo.getClass() != Prism.class;
            }
        } else {
            switch (orientation) {
                case UP:
                    switch (this.orientation) {
                        case UP:
                            return grid[spotX][spotY].cellStaticItem != RED_RECEIVER;
                        case DOWN:
                            return false;
                        case LEFT:
                            return grid[spotX][spotY].cellStaticItem != YELLOW_RECEIVER;
                        case RIGHT:
                            return grid[spotX][spotY].cellStaticItem != BLUE_RECEIVER;
                    }
                case DOWN:
                    switch (this.orientation) {
                        case UP:
                            return false;
                        case DOWN:
                            return grid[spotX][spotY].cellStaticItem != RED_RECEIVER;
                        case LEFT:
                            return grid[spotX][spotY].cellStaticItem != BLUE_RECEIVER;
                        case RIGHT:
                            return grid[spotX][spotY].cellStaticItem != YELLOW_RECEIVER;
                    }
                case LEFT:
                    switch (this.orientation) {
                        case UP:
                            return grid[spotX][spotY].cellStaticItem != BLUE_RECEIVER;
                        case DOWN:
                            return grid[spotX][spotY].cellStaticItem != YELLOW_RECEIVER;
                        case LEFT:
                            return grid[spotX][spotY].cellStaticItem != RED_RECEIVER;
                        case RIGHT:
                            return false;
                    }
                case RIGHT:
                    switch (this.orientation) {
                        case UP:
                            return grid[spotX][spotY].cellStaticItem != YELLOW_RECEIVER;
                        case DOWN:
                            return grid[spotX][spotY].cellStaticItem != BLUE_RECEIVER;
                        case LEFT:
                            return false;
                        case RIGHT:
                            return grid[spotX][spotY].cellStaticItem != RED_RECEIVER;
                    }
            }
        }
        return true;
    }


    @Override
    // EFFECTS: Takes in white light and emits red, blue and yellow light along sides
    public void interactWithLight(Light light, GridCell[][] grid, LinkedList<Light> lightProcessingQueue) {
        Light prismRedLight = null;
        Light prismBlueLight = null;
        Light prismYellowLight = null;
        if (light.colour == WHITE && this.orientation == light.orientation) {
            switch (this.orientation) {
                case UP:
                    if (GridLayout.isWithinBounds(grid, light.xPos, light.yPos - 1)) {
                        prismRedLight = new Light(RED, UP, light.xPos, light.yPos - 1);
                        grid[light.xPos][light.yPos - 1].light = prismRedLight;
                    }
                    if (GridLayout.isWithinBounds(grid, light.xPos - 1, light.yPos)) {
                        prismBlueLight = new Light(BLUE, LEFT, light.xPos - 1, light.yPos);
                        grid[light.xPos - 1][light.yPos].light = prismBlueLight;
                    }
                    if (GridLayout.isWithinBounds(grid, light.xPos + 1, light.yPos)) {
                        prismYellowLight = new Light(YELLOW, RIGHT, light.xPos + 1, light.yPos);
                        grid[light.xPos + 1][light.yPos].light = prismYellowLight;
                    }
                    break;
                case DOWN:
                    if (GridLayout.isWithinBounds(grid, light.xPos, light.yPos + 1)) {
                        prismRedLight = new Light(RED, DOWN, light.xPos, light.yPos + 1);
                        grid[light.xPos][light.yPos + 1].light = prismRedLight;
                    }
                    if (GridLayout.isWithinBounds(grid, light.xPos + 1, light.yPos)) {
                        prismBlueLight = new Light(BLUE, RIGHT, light.xPos + 1, light.yPos);
                        grid[light.xPos + 1][light.yPos].light = prismBlueLight;
                    }
                    if (GridLayout.isWithinBounds(grid, light.xPos - 1, light.yPos)) {
                        prismYellowLight = new Light(YELLOW, LEFT, light.xPos - 1, light.yPos);
                        grid[light.xPos - 1][light.yPos].light = prismYellowLight;
                    }
                    break;
                case LEFT:
                    if (GridLayout.isWithinBounds(grid, light.xPos - 1, light.yPos)) {
                        prismRedLight = new Light(RED, LEFT, light.xPos - 1, light.yPos);
                        grid[light.xPos - 1][light.yPos].light = prismRedLight;
                    }
                    if (GridLayout.isWithinBounds(grid, light.xPos, light.yPos + 1)) {
                        prismBlueLight = new Light(BLUE, DOWN, light.xPos, light.yPos + 1);
                        grid[light.xPos][light.yPos + 1].light = prismBlueLight;
                    }
                    if (GridLayout.isWithinBounds(grid, light.xPos, light.yPos - 1)) {
                        prismYellowLight = new Light(YELLOW, UP, light.xPos, light.yPos - 1);
                        grid[light.xPos][light.yPos - 1].light = prismYellowLight;
                    }
                    break;
                case RIGHT:
                    if (GridLayout.isWithinBounds(grid, light.xPos + 1, light.yPos)) {
                        prismRedLight = new Light(RED, RIGHT, light.xPos + 1, light.yPos);
                        grid[light.xPos + 1][light.yPos].light = prismRedLight;
                    }
                    if (GridLayout.isWithinBounds(grid, light.xPos, light.yPos - 1)) {
                        prismBlueLight = new Light(BLUE, UP, light.xPos, light.yPos - 1);
                        grid[light.xPos][light.yPos - 1].light = prismBlueLight;
                    }
                    if (GridLayout.isWithinBounds(grid, light.xPos, light.yPos + 1)) {
                        prismYellowLight = new Light(YELLOW, DOWN, light.xPos, light.yPos + 1);
                        grid[light.xPos][light.yPos + 1].light = prismYellowLight;
                    }
                    break;
            }
            if (prismRedLight != null) {
                lightProcessingQueue.add(prismRedLight);
            }
            if (prismBlueLight != null) {
                lightProcessingQueue.add(prismBlueLight);
            }
            if (prismYellowLight != null) {
                lightProcessingQueue.add(prismYellowLight);
            }
        }
    }

    @Override
    public String toString() {
        return this.orientation + " Prism";
    }
}
