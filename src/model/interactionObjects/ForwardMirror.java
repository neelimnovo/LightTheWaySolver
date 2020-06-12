package model.interactionObjects;

import javafx.util.Pair;
import model.GridCell;
import model.GridLayout;
import searchLogic.Light;

import java.util.ArrayList;
import java.util.LinkedList;

import static model.interactionObjects.BackwardMirror.isUnblockedMirrorSpot;
import static model.interactionObjects.FaceOrientation.*;
import static model.interactionObjects.FaceOrientation.DOWN;

public class ForwardMirror extends DynamicGridObject{

    @Override
    public String getCorrectImageString() {
        return "frontMirror.png";
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

            if ((leftValid || downValid) && (topValid || rightValid)
                    && (topValid || downValid) && (leftValid || rightValid)) {
                resultSpots.add(new Pair<>(spotX, spotY));
            }
        }
        return resultSpots;
    }

    @Override
    public void interactWithLight(Light light, GridCell[][] grid, LinkedList<Light> lightProcessingQueue) {
        Light interactedLight = null;
        switch (light.orientation) {
            case UP:
                if (GridLayout.isWithinBounds(grid, light.xPos + 1, light.yPos)) {
                    interactedLight = new Light(light.colour, RIGHT, light.xPos + 1, light.yPos);
                    grid[light.xPos + 1][light.yPos].light = interactedLight;
                }
                break;
            case DOWN:
                if (GridLayout.isWithinBounds(grid, light.xPos - 1, light.yPos)) {
                    interactedLight = new Light(light.colour, LEFT, light.xPos - 1, light.yPos);
                    grid[light.xPos - 1][light.yPos].light = interactedLight;
                }
                break;
            case LEFT:
                if (GridLayout.isWithinBounds(grid, light.xPos, light.yPos + 1)) {
                    interactedLight = new Light(light.colour, DOWN, light.xPos, light.yPos + 1);
                    grid[light.xPos][light.yPos + 1].light = interactedLight;
                }
                break;
            case RIGHT:
                if (GridLayout.isWithinBounds(grid, light.xPos, light.yPos - 1)) {
                    interactedLight = new Light(light.colour, UP, light.xPos, light.yPos - 1);
                    grid[light.xPos][light.yPos - 1].light = interactedLight;
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
        return "Forward Mirror";
    }
}
