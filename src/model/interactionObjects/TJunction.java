package model.interactionObjects;

import javafx.util.Pair;
import model.GridCell;
import model.GridLayout;
import searchLogic.Light;
import java.util.ArrayList;
import java.util.LinkedList;

import static model.interactionObjects.FaceOrientation.*;
import static model.interactionObjects.StaticGridObject.*;

public class TJunction extends DynamicGridObject {
    FaceOrientation orientation;

    public TJunction (FaceOrientation orientation) {
        this.orientation = orientation;
    }

    @Override
    public String getCorrectImageString() {
        switch (orientation) {
            case UP:
                return "upTJunction.png";
            case DOWN:
                return "downTJunction.png";
            case LEFT:
                return "leftTJunction.png";
            case RIGHT:
                return "rightTJunction.png";
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
            boolean isValidSpot = true;
            switch (this.orientation) {
                case UP:
                case DOWN:
                    if (!isValidJunctionExits(1, grid, spotX, spotY)
                            || !isValidJunctionEntrance(grid, spotX, spotY)
                            || !isValidJunctionBack(grid, spotX, spotY)){
                        isValidSpot = false;
                    }
                    break;
                case LEFT:
                case RIGHT:
                    if (!isValidJunctionExits(2, grid, spotX, spotY)
                            || !isValidJunctionEntrance(grid, spotX, spotY)
                            || !isValidJunctionBack(grid, spotX, spotY)){
                        isValidSpot = false;
                    }
                    break;
            }
            // Add the spot if all neighbouring spots are valid
            if (isValidSpot) {
                resultSpots.add(new Pair<>(spotX, spotY));
            }
        }
        return resultSpots;
    }


    private boolean isValidJunctionBack(GridCell[][] grid, int spotX, int spotY) {
        DynamicGridObject dgo = null;
        switch (this.orientation) {
            case UP:
                if (GridLayout.isWithinBounds(grid, spotX, spotY - 1)) {
                    if (grid[spotX][spotY - 1].receiver != null) {
                        return false;
                    }
                    dgo = grid[spotX][spotY - 1].cellDynamicItem;
                    if (dgo != null) {
                        if (dgo.getClass() == LightSource.class && ((LightSource) dgo).orientation == DOWN) {
                            return false;
                        }
                        if (dgo.getClass() == TJunction.class && ((TJunction) dgo).orientation != DOWN) {
                            return false;
                        }
                        if (dgo.getClass() == ColourShifter.class && ((ColourShifter) dgo).orientation == DOWN) {
                            return false;
                        }
                        if (dgo.getClass() == Prism.class) {
                            return false;
                        }
                    }
                }
                break;
            case DOWN:
                if (GridLayout.isWithinBounds(grid, spotX, spotY + 1)) {
                    if (grid[spotX][spotY + 1].receiver != null) {
                        return false;
                    }
                    dgo = grid[spotX][spotY + 1].cellDynamicItem;
                    if (dgo != null) {
                        if (dgo.getClass() == LightSource.class && ((LightSource) dgo).orientation == UP) {
                            return false;
                        }
                        if (dgo.getClass() == TJunction.class && ((TJunction) dgo).orientation != UP) {
                            return false;
                        }
                        if (dgo.getClass() == ColourShifter.class && ((ColourShifter) dgo).orientation == UP) {
                            return false;
                        }
                        if (dgo.getClass() == Prism.class) {
                            return false;
                        }
                    }
                }
                break;
            case LEFT:
                if (GridLayout.isWithinBounds(grid, spotX - 1, spotY)) {
                    if (grid[spotX - 1][spotY].receiver != null) {
                        return false;
                    }
                    dgo = grid[spotX - 1][spotY].cellDynamicItem;
                    if (dgo != null) {
                        if (dgo.getClass() == LightSource.class && ((LightSource) dgo).orientation == LEFT) {
                            return false;
                        }
                        if (dgo.getClass() == TJunction.class && ((TJunction) dgo).orientation != LEFT) {
                            return false;
                        }
                        if (dgo.getClass() == ColourShifter.class && ((ColourShifter) dgo).orientation == LEFT) {
                            return false;
                        }
                        if (dgo.getClass() == Prism.class) {
                            return false;
                        }
                    }
                }
                break;
            case RIGHT:
                if (GridLayout.isWithinBounds(grid, spotX + 1, spotY)) {
                    if (grid[spotX + 1][spotY].receiver != null) {
                        return false;
                    }
                    dgo = grid[spotX + 1][spotY].cellDynamicItem;
                    if (dgo != null) {
                        if (dgo.getClass() == LightSource.class && ((LightSource) dgo).orientation == RIGHT) {
                            return false;
                        }
                        if (dgo.getClass() == TJunction.class && ((TJunction) dgo).orientation != RIGHT) {
                            return false;
                        }
                        if (dgo.getClass() == ColourShifter.class && ((ColourShifter) dgo).orientation == RIGHT) {
                            return false;
                        }
                        if (dgo.getClass() == Prism.class) {
                            return false;
                        }
                    }
                }
                break;
        }
        return true;
    }


    private boolean isValidJunctionEntrance(GridCell[][] grid, int spotX, int spotY) {
        DynamicGridObject dgo = null;
        switch (this.orientation) {
            case UP:
                if (GridLayout.isWithinBounds(grid, spotX, spotY + 1)) {
                    if (grid[spotX][spotY + 1].cellStaticItem != EMPTY) {
                        return false;
                    }
                    dgo = grid[spotX][spotY + 1].cellDynamicItem;
                    if (dgo != null) {
                        if(dgo.getClass() == LightSource.class && ((LightSource) dgo).orientation != UP) {
                            return false;
                        }
                        if(dgo.getClass() ==  TJunction.class
                                && (((TJunction) dgo).orientation != LEFT || ((TJunction) dgo).orientation != RIGHT)){
                            return false;
                        }
                        if(dgo.getClass() == ColourShifter.class && ((ColourShifter) dgo).orientation != UP) {
                            return false;
                        }
                        if(dgo.getClass() == Prism.class && ((Prism) dgo).orientation != DOWN) {
                            return false;
                        }
                    }
                }
                break;
            case DOWN:
                if (GridLayout.isWithinBounds(grid, spotX, spotY - 1)) {
                    if (grid[spotX][spotY - 1].cellStaticItem != EMPTY) {
                        return false;
                    }
                    dgo = grid[spotX][spotY - 1].cellDynamicItem;
                    if (dgo != null) {
                        if(dgo.getClass() == LightSource.class && ((LightSource) dgo).orientation != DOWN) {
                            return false;
                        }
                        if(dgo.getClass() ==  TJunction.class
                                && (((TJunction) dgo).orientation != LEFT || ((TJunction) dgo).orientation != RIGHT)){
                            return false;
                        }
                        if(dgo.getClass() == ColourShifter.class && ((ColourShifter) dgo).orientation != DOWN) {
                            return false;
                        }
                        if(dgo.getClass() == Prism.class && ((Prism) dgo).orientation != UP) {
                            return false;
                        }
                    }
                }
                break;
            case LEFT:
                if (GridLayout.isWithinBounds(grid, spotX + 1, spotY)) {
                    if (grid[spotX + 1][spotY].cellStaticItem != EMPTY) {
                        return false;
                    }
                    dgo = grid[spotX + 1][spotY].cellDynamicItem;
                    if (dgo != null) {
                        if(dgo.getClass() == LightSource.class && ((LightSource) dgo).orientation != LEFT) {
                            return false;
                        }
                        if(dgo.getClass() ==  TJunction.class
                                && (((TJunction) dgo).orientation != UP || ((TJunction) dgo).orientation != DOWN)){
                            return false;
                        }
                        if(dgo.getClass() == ColourShifter.class && ((ColourShifter) dgo).orientation != LEFT) {
                            return false;
                        }
                        if(dgo.getClass() == Prism.class && ((Prism) dgo).orientation != RIGHT) {
                            return false;
                        }
                    }
                }
                break;
            case RIGHT:
                if (GridLayout.isWithinBounds(grid, spotX - 1, spotY)) {
                    if (grid[spotX - 1][spotY].cellStaticItem != EMPTY) {
                        return false;
                    }
                    dgo = grid[spotX - 1][spotY].cellDynamicItem;
                    if (dgo != null) {
                        if(dgo.getClass() == LightSource.class && ((LightSource) dgo).orientation != RIGHT) {
                            return false;
                        }
                        if(dgo.getClass() ==  TJunction.class
                                && (((TJunction) dgo).orientation != UP || ((TJunction) dgo).orientation != DOWN)){
                            return false;
                        }
                        if(dgo.getClass() == ColourShifter.class && ((ColourShifter) dgo).orientation != RIGHT) {
                            return false;
                        }
                        if(dgo.getClass() == Prism.class && ((Prism) dgo).orientation != LEFT) {
                            return false;
                        }
                    }
                }
                break;
        }
        return true;
    }



    private boolean isValidJunctionExits(int sides, GridCell[][] grid, int spotX, int spotY) {
        switch (sides) {
            case 1: // Left or Right sides
                if (GridLayout.isWithinBounds(grid, spotX - 1, spotY)) {
                    if (isInvalidDynamicSide(grid, LEFT, spotX, spotY)
                            || grid[spotX - 1][spotY].cellStaticItem == WALL) {
                        return false;
                    }
                }
                if (GridLayout.isWithinBounds(grid, spotX + 1, spotY)) {
                    if (isInvalidDynamicSide(grid, RIGHT, spotX, spotY)
                            || grid[spotX + 1][spotY].cellStaticItem == WALL) {
                        return false;
                    }
                }
                break;
            case 2: // Top or bottom sides
                if (GridLayout.isWithinBounds(grid, spotX, spotY - 1)) {
                    if (isInvalidDynamicSide(grid, UP, spotX, spotY)
                            || grid[spotX][spotY - 1].cellStaticItem == WALL) {
                        return false;
                    }
                }
                if (GridLayout.isWithinBounds(grid, spotX, spotY + 1)) {
                    if (isInvalidDynamicSide(grid, DOWN, spotX, spotY)
                            || grid[spotX][spotY + 1].cellStaticItem == WALL) {
                        return false;
                    }
                }
                break;
        }
        return true;
    }

    private boolean isInvalidDynamicSide(GridCell[][] grid, FaceOrientation orientation, int spotX, int spotY) {
        DynamicGridObject dgo;
        boolean isInvalidSpot;
        switch (orientation) {
            case UP:
                dgo = grid[spotX][spotY - 1].cellDynamicItem;
                if (dgo != null) {
                    isInvalidSpot = ((dgo.getClass() == LightSource.class)
                            || (dgo.getClass() == TJunction.class && ((TJunction) dgo).orientation != UP)
                            || (dgo.getClass() == ColourShifter.class && ((ColourShifter) dgo).orientation != UP)
                            || (dgo.getClass() == Prism.class && ((Prism) dgo).orientation != UP));
                    if (isInvalidSpot) {
                        return true;
                    }
                }
                break;
            case DOWN:
                dgo = grid[spotX][spotY + 1].cellDynamicItem;
                if (dgo != null) {
                    isInvalidSpot = ((dgo.getClass() == LightSource.class)
                            || (dgo.getClass() == TJunction.class && ((TJunction) dgo).orientation != DOWN)
                            || (dgo.getClass() == ColourShifter.class && ((ColourShifter) dgo).orientation != DOWN)
                            || (dgo.getClass() == Prism.class && ((Prism) dgo).orientation != DOWN));
                    if (isInvalidSpot) {
                        return true;
                    }
                }
                break;
            case LEFT:
                dgo = grid[spotX - 1][spotY].cellDynamicItem;
                if (dgo != null) {
                    isInvalidSpot = ((dgo.getClass() == LightSource.class)
                            || (dgo.getClass() == TJunction.class && ((TJunction) dgo).orientation != LEFT)
                            || (dgo.getClass() == ColourShifter.class && ((ColourShifter) dgo).orientation != LEFT)
                            || (dgo.getClass() == Prism.class && ((Prism) dgo).orientation != LEFT));
                    if (isInvalidSpot) {
                        return true;
                    }
                }
                break;
            case RIGHT:
                dgo = grid[spotX + 1][spotY].cellDynamicItem;
                if (dgo != null) {
                    isInvalidSpot = ((dgo.getClass() == LightSource.class)
                            || (dgo.getClass() == TJunction.class && ((TJunction) dgo).orientation != RIGHT)
                            || (dgo.getClass() == ColourShifter.class && ((ColourShifter) dgo).orientation != RIGHT)
                            || (dgo.getClass() == Prism.class && ((Prism) dgo).orientation != RIGHT));
                    if (isInvalidSpot) {
                        return true;
                    }
                }
                break;
        }
        return false;
    }


    @Override
    public void interactWithLight(Light light, GridCell[][] grid, LinkedList<Light> lightProcessingQueue) {
        if (this.orientation == light.orientation) {
            Light interactedLight1 = null;
            Light interactedLight2 = null;
            switch (this.orientation) {
                case UP:
                case DOWN:
                    if (GridLayout.isWithinBounds(grid, light.xPos - 1, light.yPos)) {
                        interactedLight1 = new Light(light.colour, LEFT, light.xPos - 1, light.yPos);
                        grid[light.xPos - 1][light.yPos].light = interactedLight1;
                    }
                    if (GridLayout.isWithinBounds(grid, light.xPos + 1, light.yPos)) {
                        interactedLight2 = new Light(light.colour, RIGHT, light.xPos + 1, light.yPos);
                        grid[light.xPos + 1][light.yPos].light = interactedLight2;
                    }
                    break;
                case LEFT:
                case RIGHT:
                    if (GridLayout.isWithinBounds(grid, light.xPos, light.yPos - 1)) {
                        interactedLight1 = new Light(light.colour, UP, light.xPos, light.yPos - 1);
                        grid[light.xPos][light.yPos - 1].light = interactedLight1;
                    }
                    if (GridLayout.isWithinBounds(grid, light.xPos, light.yPos + 1)) {
                        interactedLight2 = new Light(light.colour, DOWN, light.xPos, light.yPos + 1);
                        grid[light.xPos][light.yPos + 1].light = interactedLight2;
                    }
                    break;
            }
            if (interactedLight1 != null) {
                lightProcessingQueue.add(interactedLight1);
            }
            if (interactedLight2 != null) {
                lightProcessingQueue.add(interactedLight2);
            }
        }
    }

    @Override
    public String toString() {
        return this.orientation + " T-Junction";
    }
}
