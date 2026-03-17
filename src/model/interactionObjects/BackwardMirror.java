package model.interactionObjects;

import javafx.util.Pair;
import model.GridCell;
import model.GridLayout;
import searchLogic.Light;
import java.util.ArrayList;
import searchLogic.ShortQueue;

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
        /**
         * For back mirror
         *    # # # 
         *    # \ #
         *    # # #
         *  If the left side and bottom side are clear, one reflective side is functional. This is valid
         *      ###
         *    -- \ ##
         *       |
         * 
         *  If the top side and right side are clear, one reflective side is functional. This is valid
         *     | 
         *  ## \ --
         *    ###
         * 
         * Invalid examples
         *  Top and bottom are invalid, neither side can reflect
         *        ###
         *      -- \ --
         *        ###
         * 
         * Left and right are invalid, neither side can reflect
         *         |
         *      ## \ ##
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

            if ((leftValid && downValid) 
            || (topValid && rightValid)) {
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

            if ((leftValid && downValid) || (topValid && rightValid)) {
                resultSpots.add(new Pair<>(spotX, spotY));
            }
        }
        return resultSpots;
    }

    /**
     * Return true if the neighbouring spot is valid (not a wall)
     */
    static boolean isStaticUnblockedMirrorSpot(GridCell[][] grid, int spotX, int spotY) {
        return grid[spotX][spotY].cellStaticItem != WALL;
    }

    /**
     * Return true if the neighbouring spot is not blocked by a dynamic object
     */
    static boolean isDynamicUnblockedMirrorSpot(GridCell[][] grid, int spotX, int spotY, FaceOrientation orientation) {
        DynamicGridObject dgo = grid[spotX][spotY].cellDynamicItem;
        if (dgo == null) return true;
        switch (orientation) {
            case UP:
                if (dgo instanceof TJunction && ((TJunction) dgo).orientation == DOWN) return false;
                if (dgo instanceof LightSource && ((LightSource) dgo).orientation != DOWN) return false;
                return true;
            case DOWN:
                if (dgo instanceof TJunction && ((TJunction) dgo).orientation == UP) return false;
                if (dgo instanceof LightSource && ((LightSource) dgo).orientation != UP) return false;
                return true;
            case LEFT:
                if (dgo instanceof TJunction && ((TJunction) dgo).orientation == RIGHT) return false;
                if (dgo instanceof LightSource && ((LightSource) dgo).orientation != RIGHT) return false;
                return true;
            case RIGHT:
                if (dgo instanceof TJunction && ((TJunction) dgo).orientation == LEFT) return false;
                if (dgo instanceof LightSource && ((LightSource) dgo).orientation != LEFT) return false;
                return true;
            default:
                throw new IllegalStateException("Unexpected value: " + orientation);
        }
    }


    @Override
    public void interactWithLight(short light, GridCell[][] grid, ShortQueue lightProcessingQueue) {
        int x = Light.getX(light), y = Light.getY(light);
        int newX = x, newY = y;
        FaceOrientation newLightOrientation;
        switch (Light.getOrientation(light)) {
            case UP:    newX = x - 1; newLightOrientation = LEFT; break;
            case DOWN:  newX = x + 1; newLightOrientation = RIGHT; break;
            case LEFT:  newY = y - 1; newLightOrientation = UP; break;
            case RIGHT: newY = y + 1; newLightOrientation = DOWN; break;
            default:
                throw new IllegalStateException("Unexpected value: " + Light.getOrientation(light));
        }
        short interactedLight = Light.create(newX, newY, Light.getColour(light), newLightOrientation);
        grid[newX][newY].light = interactedLight;
        lightProcessingQueue.add(interactedLight);
    }

    @Override
    public String toString() {
        return "Backward Mirror";
    }
}
