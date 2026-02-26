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
    public final FaceOrientation orientation;

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
        ArrayList<Pair<Integer, Integer>> resultSpots = new ArrayList<>(emptySpots.size());
        for (Pair<Integer, Integer> spot : emptySpots) {
            int spotX = spot.getKey();
            int spotY = spot.getValue();
            switch (this.orientation) {
                case UP:
                    if (GridLayout.isWithinBounds(grid, spotX, spotY - 1)) {
                        if (isValidExit(grid, spotX, spotY - 1)
                        && isValidNeighbour(grid, spotX, spotY, DOWN)
                        && isValidNeighbour(grid, spotX, spotY, LEFT)
                        && isValidNeighbour(grid, spotX, spotY, RIGHT)) {
                            resultSpots.add(new Pair<>(spotX, spotY));
                        }
                    }
                    break;
                case DOWN:
                    if (GridLayout.isWithinBounds(grid, spotX, spotY + 1)) {
                        if (isValidExit(grid, spotX, spotY + 1)
                                && isValidNeighbour(grid, spotX, spotY, UP)
                                && isValidNeighbour(grid, spotX, spotY, LEFT)
                                && isValidNeighbour(grid, spotX, spotY, RIGHT)) {
                            resultSpots.add(new Pair<>(spotX, spotY));
                        }
                    }
                    break;
                case LEFT:
                    if (GridLayout.isWithinBounds(grid, spotX - 1, spotY)) {
                        if (isValidExit(grid, spotX - 1, spotY)
                                && isValidNeighbour(grid, spotX, spotY, UP)
                                && isValidNeighbour(grid, spotX, spotY, DOWN)
                                && isValidNeighbour(grid, spotX, spotY, RIGHT)) {
                            resultSpots.add(new Pair<>(spotX, spotY));
                        }
                    }
                    break;
                case RIGHT:
                    if (GridLayout.isWithinBounds(grid, spotX + 1, spotY)) {
                        if (isValidExit(grid, spotX + 1, spotY)
                                && isValidNeighbour(grid, spotX, spotY, UP)
                                && isValidNeighbour(grid, spotX, spotY, DOWN)
                                && isValidNeighbour(grid, spotX, spotY, LEFT)) {
                            resultSpots.add(new Pair<>(spotX, spotY));
                        }
                    }
                    break;
            }
        }
        return resultSpots;
    }

    /*
    Given the position of a cell that will be adjacent to a light source, 
        and the light source's orientation
    Check that the given cell does not contain an illogical dynamic grid object
    E.g for a light source, it cannot have a neighbouring shifter that points into it
    or a prism
    It cannot be a receiver that is being blocked
    */
    private boolean isValidNeighbour(GridCell[][] grid, int spotX, int spotY, FaceOrientation direction) {
        switch (direction) {
            case UP:
                if (!GridLayout.isWithinBounds(grid, spotX, spotY - 1)) {
                    return true;
                } else {
                    // TODO should not be exiting light source, exiting shifter, prism or occlude a filter
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
        // System.out.println("Blocks light, so does nothing with the lightProcessingQueue");
    }

    @Override
    public String toString() {
        return "Light Source: " + orientation;
    }

    private boolean isValidExit(GridCell[][] grid, int spotX, int spotY) {
        StaticGridObject sgo = grid[spotX][spotY].cellStaticItem;
        // Source can directly face a white receiver
        if (sgo == WHITE_RECEIVER) return true; 
        
        // If the grid is not empty at this point, its a wall or different color receiver
        // which is not valid
        if (sgo != EMPTY) return false; 
        
        DynamicGridObject dgo = grid[spotX][spotY].cellDynamicItem;
        // Fully empty spot is valid
        if (dgo == null) return true;

        // Exit cannot be blocked by another light source
        if (dgo.getClass() == LightSource.class) return false; 

        // Exit cannot be blocked by a shifter that points into the light source.
        if (dgo.getClass() == ColourShifter.class 
                && ((ColourShifter) dgo).orientation == getOppositeOrientation(this.orientation)) return false;
        
        // Exit cannot be blocked by prism of improper orientation
        if (dgo.getClass() == Prism.class && ((Prism) dgo).orientation != this.orientation) return false;

        // Should not reach this case
        return false;
    }
}
