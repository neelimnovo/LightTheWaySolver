package model.interactionObjects;

import javafx.util.Pair;
import model.GridCell;
import model.GridLayout;
import model.interactionObjects.filters.BlueFilter;
import model.interactionObjects.filters.RedFilter;
import model.interactionObjects.filters.YellowFilter;
import searchLogic.Light;
import java.util.ArrayList;
import searchLogic.ShortQueue;

import static model.interactionObjects.Colour.*;
import static model.interactionObjects.StaticGridObject.WALL;


public class ColourShifter extends DynamicGridObject {
    public final FaceOrientation orientation;
    public final Colour colour;

    public ColourShifter(FaceOrientation orientation, Colour colour) {
        this.orientation = orientation;
        this.colour = colour;
    }

    @Override
    public final String getCorrectImageString() {
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
        ArrayList<Pair<Integer, Integer>> resultSpots = new ArrayList<>(emptySpots.size());
        for (Pair<Integer, Integer> spot : emptySpots) {
            int spotX = spot.getKey();
            int spotY = spot.getValue();
            if (isDynamicValidExit(grid, spotX, spotY)) {
                resultSpots.add(new Pair<>(spotX, spotY));
            }
        }
        return resultSpots;
    }

    @Override
    public ArrayList<Pair<Integer, Integer>> staticFilter(GridCell[][] grid, ArrayList<Pair<Integer, Integer>> emptySpots) {
        ArrayList<Pair<Integer, Integer>> resultSpots = new ArrayList<>(emptySpots.size());
        for (Pair<Integer, Integer> spot : emptySpots) {
            int spotX = spot.getKey();
            int spotY = spot.getValue();
            if (isStaticValidExit(grid, spotX, spotY)) {
                resultSpots.add(new Pair<>(spotX, spotY));
            }
        }
        return resultSpots;
    }

    private boolean isStaticValidExit(GridCell[][] grid, int spotX, int spotY) {
        int exX = spotX, exY = spotY;
        switch (this.orientation) {
            case UP:    exY = spotY - 1; break;
            case DOWN:  exY = spotY + 1; break;
            case LEFT:  exX = spotX - 1; break;
            case RIGHT: exX = spotX + 1; break;
        }
        return spotIsNotWall(grid, exX, exY) && spotIsNotWrongReceiver(grid, exX, exY);
    }

    private boolean isDynamicValidExit(GridCell[][] grid, int spotX, int spotY) {
        int exX = spotX, exY = spotY;
        switch (this.orientation) {
            case UP:    exY = spotY - 1; break;
            case DOWN:  exY = spotY + 1; break;
            case LEFT:  exX = spotX - 1; break;
            case RIGHT: exX = spotX + 1; break;
        }
        return spotIsNotWrongFilterOrPrism(grid, exX, exY) && spotIsNotWrongColourShifter(grid, exX, exY);
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
    public void interactWithLight(short light, GridCell[][] grid, ShortQueue lightProcessingQueue) {
        int x = Light.getX(light), y = Light.getY(light);
        int nx = x, ny = y;
        switch (this.orientation) {
            case UP:    ny = y - 1; break;
            case DOWN:  ny = y + 1; break;
            case LEFT:  nx = x - 1; break;
            case RIGHT: nx = x + 1; break;
            default: throw new IllegalStateException("Unexpected value: " + this.orientation);
        }
        short interactedLight = Light.create(nx, ny, this.colour, this.orientation);
        grid[nx][ny].light = interactedLight;
        lightProcessingQueue.add(interactedLight);
    }

    @Override
    public String toString() {
        return this.colour + " " + this.orientation.toString() + " ColourShifter";
    }
}
