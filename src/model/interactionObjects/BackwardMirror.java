package model.interactionObjects;

import javafx.util.Pair;
import model.GridCell;
import model.GridLayout;
import searchLogic.Light;

import java.util.ArrayList;
import java.util.LinkedList;

import static model.interactionObjects.FaceOrientation.*;
import static model.interactionObjects.StaticGridObject.EMPTY;
import static model.interactionObjects.StaticGridObject.WALL;

public class BackwardMirror extends DynamicGridObject {

    @Override
    public String getCorrectImageString() {
        return "backMirror.png";
    }

    @Override
    // EFFECTS: Filters a spot if both reflection pathways are blocked
    public ArrayList<Pair<Integer, Integer>> filter(GridCell[][] grid, ArrayList<Pair<Integer, Integer>> emptySpots) {
        ArrayList<Pair<Integer, Integer>> resultSpots = new ArrayList<>();
        for (Pair<Integer, Integer> spot : emptySpots) {
            int spotX = spot.getKey();
            int spotY = spot.getValue();
            boolean topValid = isUnblockedMirrorSpot(grid, spotX, spotY - 1, UP);
            boolean downValid = isUnblockedMirrorSpot(grid, spotX, spotY + 1, DOWN);
            boolean leftValid = isUnblockedMirrorSpot(grid, spotX - 1, spotY, LEFT);
            boolean rightValid = isUnblockedMirrorSpot(grid, spotX + 1, spotY, RIGHT);

            if ((topValid || leftValid) && (downValid || rightValid)
                    && (topValid || downValid) && (leftValid || rightValid)) {
                resultSpots.add(new Pair<>(spotX, spotY));
            }
        }
        return resultSpots;
    }

    static boolean isUnblockedMirrorSpot(GridCell[][] grid, int spotX, int spotY, FaceOrientation orientation) {
        if (GridLayout.isWithinBounds(grid, spotX, spotY)) {
            if (grid[spotX][spotY].cellStaticItem == WALL) {
                return false;
            } else if (grid[spotX][spotY].cellStaticItem == EMPTY) {
                DynamicGridObject dgo = grid[spotX][spotY].cellDynamicItem;
                if (dgo == null) {
                    return true;
                } else {
                    switch (orientation) {
                        case UP:
                            return (dgo.getClass() != TJunction.class || ((TJunction) dgo).orientation != DOWN)
                                    && (dgo.getClass() != ColourShifter.class || ((ColourShifter) dgo).orientation == DOWN)
                                    && (dgo.getClass() != LightSource.class || ((LightSource) dgo).orientation != DOWN);
                        case DOWN:
                            return (dgo.getClass() != TJunction.class || ((TJunction) dgo).orientation != UP)
                                    && (dgo.getClass() != ColourShifter.class || ((ColourShifter) dgo).orientation == UP)
                                    && (dgo.getClass() != LightSource.class || ((LightSource) dgo).orientation != UP);
                        case LEFT:
                            return (dgo.getClass() != TJunction.class || ((TJunction) dgo).orientation != RIGHT)
                                    && (dgo.getClass() != ColourShifter.class || ((ColourShifter) dgo).orientation == RIGHT)
                                    && (dgo.getClass() != LightSource.class || ((LightSource) dgo).orientation != RIGHT);
                        case RIGHT:
                            return (dgo.getClass() != TJunction.class || ((TJunction) dgo).orientation != LEFT)
                                    && (dgo.getClass() != ColourShifter.class || ((ColourShifter) dgo).orientation == LEFT)
                                    && (dgo.getClass() != LightSource.class || ((LightSource) dgo).orientation != LEFT);
                        default:
                            throw new IllegalStateException("Unexpected value: " + orientation);
                    }
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }


    @Override
    public void interactWithLight(Light light, GridCell[][] grid, LinkedList<Light> lightProcessingQueue) {
        Light interactedLight = null;
        switch (light.orientation) {
            case UP:
                if (GridLayout.isWithinBounds(grid, light.xPos - 1, light.yPos)) {
                    interactedLight = new Light(light.colour, LEFT, light.xPos - 1, light.yPos);
                    grid[light.xPos - 1][light.yPos].light = interactedLight;
                }
                break;
            case DOWN:
                if (GridLayout.isWithinBounds(grid, light.xPos + 1, light.yPos)) {
                    interactedLight = new Light(light.colour, RIGHT, light.xPos + 1, light.yPos);
                    grid[light.xPos + 1][light.yPos].light = interactedLight;
                }
                break;
            case LEFT:
                if (GridLayout.isWithinBounds(grid, light.xPos, light.yPos - 1)) {
                    interactedLight = new Light(light.colour, UP, light.xPos, light.yPos - 1);
                    grid[light.xPos][light.yPos - 1].light = interactedLight;
                }
                break;
            case RIGHT:
                if (GridLayout.isWithinBounds(grid, light.xPos, light.yPos + 1)) {
                    interactedLight = new Light(light.colour, DOWN, light.xPos, light.yPos + 1);
                    grid[light.xPos][light.yPos + 1].light = interactedLight;
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + light.orientation);
        }
        if (interactedLight != null) {
            lightProcessingQueue.add(interactedLight);
        }
    }

    @Override
    public String toString() {
        return "Backward Mirror";
    }
}
