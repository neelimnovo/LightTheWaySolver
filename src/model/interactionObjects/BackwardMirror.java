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
        ArrayList<Pair<Integer, Integer>> resultSpots = new ArrayList<>(emptySpots.size());
        for (Pair<Integer, Integer> spot : emptySpots) {
            int spotX = spot.getKey();
            int spotY = spot.getValue();
            boolean topValid = isUnblockedMirrorSpot(grid, spotX, spotY - 1, UP);
            boolean downValid = isUnblockedMirrorSpot(grid, spotX, spotY + 1, DOWN);
            boolean leftValid = isUnblockedMirrorSpot(grid, spotX - 1, spotY, LEFT);
            boolean rightValid = isUnblockedMirrorSpot(grid, spotX + 1, spotY, RIGHT);

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
            if ((leftValid && downValid) 
            || (topValid && rightValid)) {
                resultSpots.add(new Pair<>(spotX, spotY));
            }
        }
        return resultSpots;
    }

    /**
     * Return true if the neighbouring spot is valid and unblocked
     * Return false if the neighbouring spot is blocked, and hence one side of the mirror would be blocked
     */
    static boolean isUnblockedMirrorSpot(GridCell[][] grid, int spotX, int spotY, FaceOrientation orientation) {
        if (!GridLayout.isWithinBounds(grid, spotX, spotY)) return false;
        if (grid[spotX][spotY].cellStaticItem == WALL) return false;
        DynamicGridObject dgo = grid[spotX][spotY].cellDynamicItem;
        if (dgo == null) return true;
        switch (orientation) {
            case UP:
                // Fastest: check class first, then only cast if needed, order for likely early exit
                if (dgo instanceof TJunction && ((TJunction) dgo).orientation == DOWN) return false;
                if (dgo instanceof LightSource && ((LightSource) dgo).orientation != DOWN) return false;
                // if (dgo.getClass() == ColourShifter.class && ((ColourShifter) dgo).orientation != DOWN) return false;
                return true;
            case DOWN:
                if (dgo instanceof TJunction && ((TJunction) dgo).orientation == UP) return false;
                if (dgo instanceof LightSource && ((LightSource) dgo).orientation != UP) return false;
                // if (dgo.getClass() == ColourShifter.class && ((ColourShifter) dgo).orientation != UP) return false;
                return true;
            case LEFT:
                if (dgo instanceof TJunction && ((TJunction) dgo).orientation == RIGHT) return false;
                if (dgo instanceof LightSource && ((LightSource) dgo).orientation != RIGHT) return false;
                // if (dgo.getClass() == ColourShifter.class && ((ColourShifter) dgo).orientation != RIGHT) return false;
                return true;
            case RIGHT:
                if (dgo instanceof TJunction && ((TJunction) dgo).orientation == LEFT) return false;
                if (dgo instanceof LightSource && ((LightSource) dgo).orientation != LEFT) return false;
                // if (dgo.getClass() == ColourShifter.class && ((ColourShifter) dgo).orientation != LEFT) return false;
                return true;
            default:
                throw new IllegalStateException("Unexpected value: " + orientation);
        }
    }


    @Override
    public void interactWithLight(Light light, GridCell[][] grid, LinkedList<Light> lightProcessingQueue) {
        int x = light.xPos, y = light.yPos;
        int newX = x, newY = y;
        FaceOrientation newLightOrientation;
        switch (light.orientation) {
            case UP:    newX = x - 1; newLightOrientation = LEFT; break;
            case DOWN:  newX = x + 1; newLightOrientation = RIGHT; break;
            case LEFT:  newY = y - 1; newLightOrientation = UP; break;
            case RIGHT: newY = y + 1; newLightOrientation = DOWN; break;
            default:
                throw new IllegalStateException("Unexpected value: " + light.orientation);
        }
        if (GridLayout.isWithinBounds(grid, newX, newY)) {
            Light interactedLight = new Light(light.colour, newLightOrientation, newX, newY);
            grid[newX][newY].light = interactedLight;
            lightProcessingQueue.add(interactedLight);
        }
    }

    @Override
    public String toString() {
        return "Backward Mirror";
    }
}
