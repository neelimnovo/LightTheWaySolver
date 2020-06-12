package model.interactionObjects;

import javafx.util.Pair;
import model.GridCell;
import model.GridLayout;
import model.interactionObjects.filters.BlueFilter;
import model.interactionObjects.filters.RedFilter;
import model.interactionObjects.filters.YellowFilter;
import searchLogic.Light;

import java.util.ArrayList;
import java.util.LinkedList;

import static model.interactionObjects.Colour.*;
import static model.interactionObjects.StaticGridObject.WALL;


public class ColourShifter extends DynamicGridObject {
    FaceOrientation orientation;
    Colour colour;

    public ColourShifter(FaceOrientation orientation, Colour colour) {
        this.orientation = orientation;
        this.colour = colour;
    }

    @Override
    public String getCorrectImageString() {
        switch (orientation) {
            case UP:
                switch (colour) {
                    case RED:
                        return "upRedShift.png";
                    case BLUE:
                        return "upBlueShift.png";
                    case YELLOW:
                        return "upYellowShift.png";
                }
            case DOWN:
                switch (colour) {
                    case RED:
                        return "downRedShift.png";
                    case BLUE:
                        return "downBlueShift.png";
                    case YELLOW:
                        return "downYellowShift.png";
                }
            case LEFT:
                switch (colour) {
                    case RED:
                        return "leftRedShift.png";
                    case BLUE:
                        return "leftBlueShift.png";
                    case YELLOW:
                        return "leftYellowShift.png";
                }
            case RIGHT:
                switch (colour) {
                    case RED:
                        return "rightRedShift.png";
                    case BLUE:
                        return "rightBlueShift.png";
                    case YELLOW:
                        return "rightYellowShift.png";
                }
            default:
                return null;
        }
    }

    @Override
    // TODO Filtering should check neighbours for occlusion (aside from the exit)
    // EFFECTS: Filters based on whether the exit of the colour shifter has valid objects or not
    public ArrayList<Pair<Integer, Integer>> filter(GridCell[][] grid, ArrayList<Pair<Integer, Integer>> emptySpots) {
        ArrayList<Pair<Integer, Integer>> resultSpots = new ArrayList<>();
        for (Pair<Integer, Integer> spot : emptySpots) {
            int spotX = spot.getKey();
            int spotY = spot.getValue();
            if (isValidExit(grid, spotX, spotY)) {
                resultSpots.add(new Pair<>(spotX, spotY));
            }
        }
        return resultSpots;
        // return emptySpots;
    }

    private boolean isValidExit(GridCell[][] grid, int spotX, int spotY) {
        switch (this.orientation) {
            case UP:
                return GridLayout.isWithinBounds(grid, spotX, spotY - 1)
                        && spotIsNotWall(grid, spotX, spotY - 1)
                        && spotIsNotWrongReceiver(grid, spotX, spotY - 1)
                        && spotIsNotWrongFilterOrPrism(grid, spotX, spotY - 1)
                        && spotIsNotWrongColourShifter(grid, spotX, spotY - 1);
            case DOWN:
                return GridLayout.isWithinBounds(grid, spotX, spotY + 1)
                        && spotIsNotWall(grid, spotX, spotY + 1)
                        && spotIsNotWrongReceiver(grid, spotX, spotY + 1)
                        && spotIsNotWrongFilterOrPrism(grid, spotX, spotY + 1)
                        && spotIsNotWrongColourShifter(grid, spotX, spotY + 1);
            case LEFT:
                return GridLayout.isWithinBounds(grid, spotX - 1, spotY)
                        && spotIsNotWall(grid, spotX - 1, spotY)
                        && spotIsNotWrongReceiver(grid, spotX - 1, spotY)
                        && spotIsNotWrongFilterOrPrism(grid, spotX - 1, spotY)
                        && spotIsNotWrongColourShifter(grid, spotX - 1, spotY);
            case RIGHT:
                return GridLayout.isWithinBounds(grid, spotX + 1, spotY)
                        && spotIsNotWall(grid, spotX + 1, spotY)
                        && spotIsNotWrongReceiver(grid, spotX + 1, spotY)
                        && spotIsNotWrongFilterOrPrism(grid, spotX + 1, spotY)
                        && spotIsNotWrongColourShifter(grid, spotX + 1, spotY);
            default:
                throw new IllegalStateException("Unexpected value: " + this.orientation);
        }
    }

    private boolean spotIsNotWrongColourShifter(GridCell[][] grid, int spotX, int spotY) {
        DynamicGridObject dgo = grid[spotX][spotY].cellDynamicItem;
        //            if (((ColourShifter) dgo).colour == this.colour
        //                    || ((ColourShifter) dgo).orientation == FaceOrientation.getOppositeOrientation(this.orientation)){
        //                return false;
        //            } else {
        //                return true;
        //            }
        return dgo == null || dgo.getClass() != ColourShifter.class;
    }

    private boolean spotIsNotWall(GridCell[][] grid, int spotX, int spotY) {
        return grid[spotX][spotY].cellStaticItem != WALL;
    }

    private boolean spotIsNotWrongReceiver(GridCell[][] grid, int spotX, int spotY) {
        Receiver receiver = grid[spotX][spotY].receiver;
        return receiver == null || receiver.colour == this.colour;
    }

    private boolean spotIsNotWrongFilterOrPrism(GridCell[][] grid, int spotX, int spotY) {
        DynamicGridObject dgo = grid[spotX][spotY].cellDynamicItem;
        if (dgo == null) {
            return true;
        } else if (dgo.getClass() == Prism.class) {
            // return ((Prism) dgo).orientation != this.orientation;
            return false;
        } else if (dgo.getClass() == RedFilter.class) {
            return this.colour == RED;
        } else if (dgo.getClass() == BlueFilter.class) {
            return this.colour == BLUE;
        } else if (dgo.getClass() == YellowFilter.class) {
            return this.colour == YELLOW;
        } else {
            return true;
        }
    }

    @Override
    public void interactWithLight(Light light, GridCell[][] grid, LinkedList<Light> lightProcessingQueue) {
        Light interactedLight = null;
        switch (this.orientation) {
            case UP:
                if (GridLayout.isWithinBounds(grid, light.xPos, light.yPos - 1)) {
                    interactedLight = new Light(this.colour, this.orientation, light.xPos, light.yPos - 1);
                    grid[light.xPos][light.yPos - 1].light = interactedLight;
                }
                break;
            case DOWN:
                if (GridLayout.isWithinBounds(grid, light.xPos, light.yPos + 1)) {
                    interactedLight = new Light(this.colour, this.orientation, light.xPos, light.yPos + 1);
                    grid[light.xPos][light.yPos + 1].light = interactedLight;
                }
                break;
            case LEFT:
                if (GridLayout.isWithinBounds(grid, light.xPos - 1, light.yPos)) {
                    interactedLight = new Light(this.colour, this.orientation, light.xPos - 1, light.yPos);
                    grid[light.xPos - 1][light.yPos].light = interactedLight;
                }
                break;
            case RIGHT:
                if (GridLayout.isWithinBounds(grid, light.xPos + 1, light.yPos)) {
                    interactedLight = new Light(this.colour, this.orientation, light.xPos + 1, light.yPos);
                    grid[light.xPos + 1][light.yPos].light = interactedLight;
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + this.orientation);
        }
        if (interactedLight != null) {
            lightProcessingQueue.add(interactedLight);
        }
    }

    @Override
    public String toString() {
        return this.colour + " " + this.orientation.toString() + " ColourShifter";
    }
}
