package model.interactionObjects;

import javafx.util.Pair;
import model.GridCell;
import model.GridLayout;
import searchLogic.Light;
import java.util.ArrayList;
import searchLogic.ShortQueue;

import static model.interactionObjects.BackwardMirror.isDynamicUnblockedMirrorSpot;
import static model.interactionObjects.BackwardMirror.isStaticUnblockedMirrorSpot;
import static model.interactionObjects.FaceOrientation.*;

public class ForwardMirror extends DynamicGridObject {

    @Override
    public String getCorrectImageString() {
        return "frontMirror.png";
    }

    @Override
    // EFFECTS: Filters a spot if both reflection pathways are blocked
    public ArrayList<Pair<Integer, Integer>> filter(GridCell[][] grid, ArrayList<Pair<Integer, Integer>> emptySpots) {
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
        ArrayList<Pair<Integer, Integer>> resultSpots = new ArrayList<>(emptySpots.size());
        for (Pair<Integer, Integer> spot : emptySpots) {
            int spotX = spot.getKey();
            int spotY = spot.getValue();

            boolean topValid = isDynamicUnblockedMirrorSpot(grid, spotX, spotY - 1, UP);
            boolean downValid = isDynamicUnblockedMirrorSpot(grid, spotX, spotY + 1, DOWN);
            boolean leftValid = isDynamicUnblockedMirrorSpot(grid, spotX - 1, spotY, LEFT);
            boolean rightValid = isDynamicUnblockedMirrorSpot(grid, spotX + 1, spotY, RIGHT);

            if ((leftValid && topValid)
            || (rightValid && downValid)) {
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

            boolean topValid = isStaticUnblockedMirrorSpot(grid, spotX, spotY - 1);
            boolean downValid = isStaticUnblockedMirrorSpot(grid, spotX, spotY + 1);
            boolean leftValid = isStaticUnblockedMirrorSpot(grid, spotX - 1, spotY);
            boolean rightValid = isStaticUnblockedMirrorSpot(grid, spotX + 1, spotY);

            if ((leftValid && topValid) || (rightValid && downValid)) {
                resultSpots.add(new Pair<>(spotX, spotY));
            }
        }
        return resultSpots;
    }

    @Override
    public void interactWithLight(short light, GridCell[][] grid, ShortQueue lightProcessingQueue) {
        int x = Light.getX(light), y = Light.getY(light);
        int nx = x, ny = y;
        FaceOrientation newOrientation = null;
        switch (Light.getOrientation(light)) {
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
                throw new IllegalStateException("Unexpected value: " + Light.getOrientation(light));
        }
        short interactedLight = Light.create(nx, ny, Light.getColour(light), newOrientation);
        grid[nx][ny].light = interactedLight;
        lightProcessingQueue.add(interactedLight);
    }

    @Override
    public String toString() {
        return "Forward Mirror";
    }
}
