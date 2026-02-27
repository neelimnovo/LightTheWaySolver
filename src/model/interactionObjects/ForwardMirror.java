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

public class ForwardMirror extends DynamicGridObject {

    @Override
    public String getCorrectImageString() {
        return "frontMirror.png";
    }

    @Override
    // EFFECTS: Filters a spot if both reflection pathways are blocked
    public ArrayList<Pair<Integer, Integer>> filter(GridCell[][] grid, ArrayList<Pair<Integer, Integer>> emptySpots) {
        ArrayList<Pair<Integer, Integer>> resultSpots = new ArrayList<>(emptySpots.size());
        for (Pair<Integer, Integer> spot : emptySpots) {
            int spotX = spot.getKey();
            int spotY = spot.getValue();

            boolean topValid = isUnblockedMirrorSpot(grid, spotX, spotY - 1, UP);
            boolean downValid = isUnblockedMirrorSpot(grid, spotX, spotY + 1, DOWN);
            boolean leftValid = isUnblockedMirrorSpot(grid, spotX - 1, spotY, LEFT);
            boolean rightValid = isUnblockedMirrorSpot(grid, spotX + 1, spotY, RIGHT);

            /**
             * For forward mirror
             *    # # # 
             *    # / #
             *    # # #
             *  If the left side and top side are clear, one reflective side is functional. This is valid
             *       |
             *    -- / ##
             *      ###
             * 
             *  If the bottom side and right side are clear, one reflective side is functional. This is valid
             *    ### 
             *  ## / --
             *     |
             * 
             * Invalid examples
             *  Top and bottom are invalid, neither side can reflect
             *        ###
             *      -- / --
             *        ###
             * 
             * Left and right are invalid, neither side can reflect
             *         |
             *      ## / ##
             *         | 
             */

            if ((leftValid && topValid)
            || (rightValid && downValid)) {
                resultSpots.add(new Pair<>(spotX, spotY));
            }
        }
        return resultSpots;
    }

    @Override
    public void interactWithLight(Light light, GridCell[][] grid, LinkedList<Light> lightProcessingQueue) {
        int x = light.xPos, y = light.yPos;
        int nx = x, ny = y;
        FaceOrientation newOrientation = null;
        switch (light.orientation) {
            case UP:
                nx = x + 1;
                newOrientation = RIGHT;
                break;
            case DOWN:
                nx = x - 1;
                newOrientation = LEFT;
                break;
            case LEFT:
                ny = y + 1;
                newOrientation = DOWN;
                break;
            case RIGHT:
                ny = y - 1;
                newOrientation = UP;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + light.orientation);
        }
        if (GridLayout.isWithinBounds(grid, nx, ny)) {
            Light interactedLight = new Light(light.colour, newOrientation, nx, ny);
            grid[nx][ny].light = interactedLight;
            lightProcessingQueue.add(interactedLight);
        }
    }

    @Override
    public String toString() {
        return "Forward Mirror";
    }
}
