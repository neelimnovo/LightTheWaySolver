package model.interactionObjects;

import javafx.util.Pair;
import model.GridCell;
import model.GridLayout;
import searchLogic.Light;

import java.util.ArrayList;
import java.util.LinkedList;

import static model.interactionObjects.FaceOrientation.*;
import static model.interactionObjects.FaceOrientation.getOppositeOrientation;
import static model.interactionObjects.StaticGridObject.*;

public class LightSource extends DynamicGridObject {
    public FaceOrientation orientation;

    public LightSource(FaceOrientation orientation) {
        this.orientation = orientation;
    }

    @Override
    public String getCorrectImageString() {
        switch (orientation) {
            case UP:
                return "upLight.png";
            case DOWN:
                return "downLight.png";
            case LEFT:
                return "leftLight.png";
            case RIGHT:
                return "rightLight.png";
            default:
                return null;
        }
    }


    @Override
    public ArrayList<Pair<Integer, Integer>> filter(GridCell[][] grid, ArrayList<Pair<Integer, Integer>> emptySpots) {
        ArrayList<Pair<Integer, Integer>> resultSpots = new ArrayList<>();
        for (Pair<Integer, Integer> spot : emptySpots) {
            int spotX = spot.getKey();
            int spotY = spot.getValue();
            switch (this.orientation) {
                case UP:
                    if (GridLayout.isWithinBounds(grid, spotX, spotY - 1)) {
                        if (isValidExit(grid, spotX, spotY - 1)
                        && isNotReceiver(grid, spotX, spotY, DOWN)
                        && isNotReceiver(grid, spotX, spotY, LEFT)
                        && isNotReceiver(grid, spotX, spotY, RIGHT)) {
                            resultSpots.add(new Pair<>(spotX, spotY));
                        }
                    }
                    break;
                case DOWN:
                    if (GridLayout.isWithinBounds(grid, spotX, spotY + 1)) {
                        if (isValidExit(grid, spotX, spotY + 1)
                                && isNotReceiver(grid, spotX, spotY, UP)
                                && isNotReceiver(grid, spotX, spotY, LEFT)
                                && isNotReceiver(grid, spotX, spotY, RIGHT)) {
                            resultSpots.add(new Pair<>(spotX, spotY));
                        }
                    }
                    break;
                case LEFT:
                    if (GridLayout.isWithinBounds(grid, spotX - 1, spotY)) {
                        if (isValidExit(grid, spotX - 1, spotY)
                                && isNotReceiver(grid, spotX, spotY, UP)
                                && isNotReceiver(grid, spotX, spotY, DOWN)
                                && isNotReceiver(grid, spotX, spotY, RIGHT)) {
                            resultSpots.add(new Pair<>(spotX, spotY));
                        }
                    }
                    break;
                case RIGHT:
                    if (GridLayout.isWithinBounds(grid, spotX + 1, spotY)) {
                        if (isValidExit(grid, spotX + 1, spotY)
                                && isNotReceiver(grid, spotX, spotY, UP)
                                && isNotReceiver(grid, spotX, spotY, DOWN)
                                && isNotReceiver(grid, spotX, spotY, LEFT)) {
                            resultSpots.add(new Pair<>(spotX, spotY));
                        }
                    }
                    break;
            }
        }
        return resultSpots;
    }

    private boolean isNotReceiver(GridCell[][] grid, int spotX, int spotY, FaceOrientation direction) {
        switch (direction) {
            case UP:
                if (!GridLayout.isWithinBounds(grid, spotX, spotY - 1)) {
                    return true;
                } else {
                    return grid[spotX][spotY - 1].receiver == null;
                }
            case DOWN:
                if (!GridLayout.isWithinBounds(grid, spotX, spotY + 1)) {
                    return true;
                } else {
                    return grid[spotX][spotY + 1].receiver == null;
                }
            case LEFT:
                if (!GridLayout.isWithinBounds(grid, spotX - 1, spotY)) {
                    return true;
                } else {
                    return grid[spotX - 1][spotY].receiver == null;
                }
            case RIGHT:
                if (!GridLayout.isWithinBounds(grid, spotX + 1, spotY)) {
                    return true;
                } else {
                    return grid[spotX + 1][spotY].receiver == null;
                }
            default:
                throw new IllegalStateException("Unexpected value: " + direction);
        }
    }

    @Override
    public void interactWithLight(Light light, GridCell[][] grid, LinkedList<Light> lightProcessingQueue) {
        // System.out.println("Blocks light");
    }

    @Override
    public String toString() {
        return "Light Source: " + orientation;
    }

    private boolean isValidExit(GridCell[][] grid, int spotX, int spotY) {
        StaticGridObject sgo = grid[spotX][spotY].cellStaticItem;
        if (sgo == WHITE_RECEIVER) {
            return true;
        } else if (sgo == EMPTY) {
            DynamicGridObject dgo = grid[spotX][spotY].cellDynamicItem;
            if (dgo == null) {
                return true;
            } else if (dgo.getClass() == LightSource.class) {
                return false;
            } else if (dgo.getClass() == ColourShifter.class
                    && ((ColourShifter) dgo).orientation == getOppositeOrientation(this.orientation)) {
                return false;
            } else if (dgo.getClass() == Prism.class && ((Prism) dgo).orientation != this.orientation) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }
}
