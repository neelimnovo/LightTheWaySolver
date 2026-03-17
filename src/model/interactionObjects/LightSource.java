package model.interactionObjects;

import javafx.util.Pair;
import model.GridCell;
import model.GridLayout;
import searchLogic.Light;
import java.util.ArrayList;
import searchLogic.ShortQueue;

import static model.interactionObjects.FaceOrientation.*;
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
            boolean isDynamicValid = false;
            switch (this.orientation) {
                case UP:    isDynamicValid = isDynamicValidExit(grid, spotX, spotY - 1); break;
                case DOWN:  isDynamicValid = isDynamicValidExit(grid, spotX, spotY + 1); break;
                case LEFT:  isDynamicValid = isDynamicValidExit(grid, spotX - 1, spotY); break;
                case RIGHT: isDynamicValid = isDynamicValidExit(grid, spotX + 1, spotY); break;
            }
            if (isDynamicValid) {
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
            boolean isStaticValid = false;
            switch (this.orientation) {
                case UP:
                    isStaticValid = isStaticValidExit(grid, spotX, spotY - 1)
                        && isStaticValidNeighbour(grid, spotX, spotY, DOWN)
                        && isStaticValidNeighbour(grid, spotX, spotY, LEFT)
                        && isStaticValidNeighbour(grid, spotX, spotY, RIGHT);
                    break;
                case DOWN:
                    isStaticValid = isStaticValidExit(grid, spotX, spotY + 1)
                            && isStaticValidNeighbour(grid, spotX, spotY, UP)
                            && isStaticValidNeighbour(grid, spotX, spotY, LEFT)
                            && isStaticValidNeighbour(grid, spotX, spotY, RIGHT);
                    break;
                case LEFT:
                    isStaticValid = isStaticValidExit(grid, spotX - 1, spotY)
                            && isStaticValidNeighbour(grid, spotX, spotY, UP)
                            && isStaticValidNeighbour(grid, spotX, spotY, DOWN)
                            && isStaticValidNeighbour(grid, spotX, spotY, RIGHT);
                    break;
                case RIGHT:
                    isStaticValid = isStaticValidExit(grid, spotX + 1, spotY)
                            && isStaticValidNeighbour(grid, spotX, spotY, UP)
                            && isStaticValidNeighbour(grid, spotX, spotY, DOWN)
                            && isStaticValidNeighbour(grid, spotX, spotY, LEFT);
                    break;
            }
            if (isStaticValid) {
                resultSpots.add(new Pair<>(spotX, spotY));
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
    private boolean isStaticValidNeighbour(GridCell[][] grid, int spotX, int spotY, FaceOrientation direction) {
        int nbX = spotX, nbY = spotY;
        switch (direction) {
            case UP:    nbY = spotY - 1; break;
            case DOWN:  nbY = spotY + 1; break;
            case LEFT:  nbX = spotX - 1; break;
            case RIGHT: nbX = spotX + 1; break;
        }
        return grid[nbX][nbY].receiver == null;
    }

    @Override
    public void interactWithLight(short light, GridCell[][] grid, ShortQueue lightProcessingQueue) {
        // System.out.println("Blocks light, so does nothing with the lightProcessingQueue");
    }

    @Override
    public String toString() {
        return "Light Source: " + orientation;
    }

    private boolean isStaticValidExit(GridCell[][] grid, int spotX, int spotY) {
        StaticGridObject sgo = grid[spotX][spotY].cellStaticItem;
        return sgo == WHITE_RECEIVER || sgo == EMPTY;
    }

    private boolean isDynamicValidExit(GridCell[][] grid, int spotX, int spotY) {
        DynamicGridObject dgo = grid[spotX][spotY].cellDynamicItem;
        // Fully empty spot is valid
        if (dgo == null) return true;
        
        // Exit cannot be blocked by another light source
        if (dgo instanceof LightSource) return false; 
        
        // Exit cannot be blocked by prism of improper orientation
        if (dgo instanceof ColourShifter && ((ColourShifter) dgo).orientation == getOppositeOrientation(this.orientation)) return false;
        if (dgo instanceof Prism && ((Prism) dgo).orientation != this.orientation) return false;

        // if its other dgo, it's fine. This would be a filter, based on the placement order
        return true;
    }
}
