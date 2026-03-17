package model.interactionObjects;

import javafx.util.Pair;
import model.GridCell;
import model.GridLayout;
import searchLogic.Light;
import java.util.ArrayList;
import searchLogic.ShortQueue;

import static model.interactionObjects.FaceOrientation.*;
import static model.interactionObjects.StaticGridObject.*;

public class TJunction extends DynamicGridObject {
    public FaceOrientation orientation;

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
                    if (!isDynamicValidJunctionExits(1, grid, spotX, spotY)
                            || !isDynamicValidJunctionEntrance(grid, spotX, spotY)
                            || !isDynamicValidJunctionBack(grid, spotX, spotY)){
                        isValidSpot = false;
                    }
                    break;
                case LEFT:
                case RIGHT:
                    if (!isDynamicValidJunctionExits(2, grid, spotX, spotY)
                            || !isDynamicValidJunctionEntrance(grid, spotX, spotY)
                            || !isDynamicValidJunctionBack(grid, spotX, spotY)){
                        isValidSpot = false;
                    }
                    break;
            }
            if (isValidSpot) {
                resultSpots.add(new Pair<>(spotX, spotY));
            }
        }
        return resultSpots;
    }

    @Override
    public ArrayList<Pair<Integer, Integer>> staticFilter(GridCell[][] grid, ArrayList<Pair<Integer, Integer>> emptySpots) {
        ArrayList<Pair<Integer, Integer>> resultSpots = new ArrayList<>();
        for (Pair<Integer, Integer> spot : emptySpots) {
            int spotX = spot.getKey();
            int spotY = spot.getValue();
            boolean isValidSpot = true;
            switch (this.orientation) {
                case UP:
                case DOWN:
                    if (!isStaticValidJunctionExits(1, grid, spotX, spotY)
                            || !isStaticValidJunctionEntrance(grid, spotX, spotY)
                            || !isStaticValidJunctionBack(grid, spotX, spotY)){
                        isValidSpot = false;
                    }
                    break;
                case LEFT:
                case RIGHT:
                    if (!isStaticValidJunctionExits(2, grid, spotX, spotY)
                            || !isStaticValidJunctionEntrance(grid, spotX, spotY)
                            || !isStaticValidJunctionBack(grid, spotX, spotY)){
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


    private boolean isStaticValidJunctionBack(GridCell[][] grid, int spotX, int spotY) {
        int newX = spotX, newY = spotY;
        switch (this.orientation) {
            case UP:    newY = spotY - 1; break;
            case DOWN:  newY = spotY + 1; break;
            case LEFT:  newX = spotX - 1; break;
            case RIGHT: newX = spotX + 1; break;
        }
        return grid[newX][newY].receiver == null;
    }

    private boolean isDynamicValidJunctionBack(GridCell[][] grid, int spotX, int spotY) {
        int newX = spotX, newY = spotY;
        FaceOrientation backDgoOrientation = null;
        switch (this.orientation) {
            case UP:
                newY = spotY - 1;
                backDgoOrientation = DOWN;
                break;
            case DOWN:
                newY = spotY + 1;
                backDgoOrientation = UP;
                break;
            case LEFT:
                newX = spotX - 1;
                backDgoOrientation = RIGHT;
                break;
            case RIGHT:
                newX = spotX + 1;
                backDgoOrientation = LEFT;
                break;
        }
        
        DynamicGridObject dgo = grid[newX][newY].cellDynamicItem;
        if (dgo == null) return true;
        // Light source cannot face directly into the backside of the junction
        if (dgo instanceof LightSource && ((LightSource) dgo).orientation == backDgoOrientation) return false;
        // On the backside of one junction, another junction can only be placed if it has inverse orientation
        if (dgo instanceof TJunction && ((TJunction) dgo).orientation != backDgoOrientation) return false;
        // A prism can never be behind a junction, light gets blocked
        if (dgo instanceof Prism) return false;
        // Colour shifter cannot face directly into the backside of the junction
        if (dgo instanceof ColourShifter && ((ColourShifter) dgo).orientation == backDgoOrientation) return false;

        return true;
    }

    private boolean isStaticValidJunctionEntrance(GridCell[][] grid, int spotX, int spotY) {
        int entX = spotX, entY = spotY;
        switch (this.orientation) {
            case UP:    entY = spotY + 1; break;
            case DOWN:  entY = spotY - 1; break;
            case LEFT:  entX = spotX + 1; break;
            case RIGHT: entX = spotX - 1; break;
        }
        return grid[entX][entY].cellStaticItem == EMPTY;
    }

    private boolean isDynamicValidJunctionEntrance(GridCell[][] grid, int spotX, int spotY) {
        int entX = spotX, entY = spotY;
        switch (this.orientation) {
            case UP:    entY = spotY + 1; break;
            case DOWN:  entY = spotY - 1; break;
            case LEFT:  entX = spotX + 1; break;
            case RIGHT: entX = spotX - 1; break;
        }
        
        DynamicGridObject dgo = grid[entX][entY].cellDynamicItem;
        if (dgo == null) return true;
        
        switch (this.orientation) {
            /**
             * For each orientation case, check for the entrance that:
             * 1) The entrance is within bounds
             * 2) The entrance is not a wall or a receiver
             * 3) If light source, the light must enter the entrance
             * 4) If Tjunction, the Tjunctions exits must face the entrance
             * 5) If colour shifter, the light must enter the entrance
             * 6) If prism, the prism's entrance cannot face the junction entrance
             */
            case UP:
                if(dgo instanceof LightSource && ((LightSource) dgo).orientation != UP) return false;
                if(dgo instanceof TJunction
                        && (((TJunction) dgo).orientation != LEFT 
                            || ((TJunction) dgo).orientation != RIGHT)) return false;
                if(dgo instanceof ColourShifter && ((ColourShifter) dgo).orientation != UP) return false;
                if(dgo instanceof Prism && ((Prism) dgo).orientation != DOWN) return false;
                break;
            case DOWN:
                if(dgo instanceof LightSource && ((LightSource) dgo).orientation != DOWN) return false;
                    if(dgo instanceof TJunction
                            && (((TJunction) dgo).orientation != LEFT 
                                || ((TJunction) dgo).orientation != RIGHT)) return false;
                if(dgo instanceof ColourShifter && ((ColourShifter) dgo).orientation != DOWN) return false;
                if(dgo instanceof Prism && ((Prism) dgo).orientation != UP) return false;
                break;
            case LEFT:
                if(dgo instanceof LightSource && ((LightSource) dgo).orientation != LEFT) return false;
                    if(dgo instanceof TJunction
                            && (((TJunction) dgo).orientation != UP 
                                || ((TJunction) dgo).orientation != DOWN)) return false;
                if(dgo instanceof ColourShifter && ((ColourShifter) dgo).orientation != LEFT) return false;
                if(dgo instanceof Prism && ((Prism) dgo).orientation != RIGHT) return false;
                break;
            case RIGHT:
                if(dgo instanceof LightSource && ((LightSource) dgo).orientation != RIGHT) return false;
                    if(dgo instanceof TJunction
                            && (((TJunction) dgo).orientation != UP 
                                || ((TJunction) dgo).orientation != DOWN)) return false;
                if(dgo instanceof ColourShifter && ((ColourShifter) dgo).orientation != RIGHT) return false;
                if(dgo instanceof Prism && ((Prism) dgo).orientation != LEFT) return false;
                break;
        }
        return true;
    }


    private boolean isStaticValidJunctionExits(int sides, GridCell[][] grid, int spotX, int spotY) {
        switch (sides) {
            case 1: // Check Left and Right sides
                return grid[spotX - 1][spotY].cellStaticItem != WALL && grid[spotX + 1][spotY].cellStaticItem != WALL;
            case 2: // Check Top and Bottom sides
                return grid[spotX][spotY - 1].cellStaticItem != WALL && grid[spotX][spotY + 1].cellStaticItem != WALL;
        }
        return true;
    }

    private boolean isDynamicValidJunctionExits(int sides, GridCell[][] grid, int spotX, int spotY) {
        /*
        * Sides refers to whether the exits are left and right facing (for up and down junctions)
        * or
        * up and down facing (for left and right junctions)
        */
        switch (sides) {
            case 1: // Check Left and Right sides
                if (isInvalidDynamicSideForExit(grid, LEFT, spotX, spotY)) return false;
                if (isInvalidDynamicSideForExit(grid, RIGHT, spotX, spotY)) return false;
                break;
            case 2: // Check Top and Bottom sides
                if (isInvalidDynamicSideForExit(grid, UP, spotX, spotY)) return false;
                if (isInvalidDynamicSideForExit(grid, DOWN, spotX, spotY)) return false;
                break;
        }
        return true;
    }


    private boolean isInvalidDynamicSideForExit(GridCell[][] grid, FaceOrientation exitOrientation, int spotX, int spotY) {
        DynamicGridObject dgo = null;
        switch (exitOrientation) {
            /*
            * A dynamic object placed next to an exit is invalid IF
            * 1) If its a light source. Then it blocks the exit light, regardless of orientation
            * 2) If its a colour shifter that is NOT in the same direction as the exit
            * 3) If its a prism whose white side does not face the exit 
            */
            case UP:
                dgo = grid[spotX][spotY - 1].cellDynamicItem;
                if (dgo == null) return false;
                return ((dgo instanceof LightSource)
                        || (dgo instanceof TJunction && ((TJunction) dgo).orientation != UP)
                        || (dgo instanceof Prism && ((Prism) dgo).orientation != UP)
                        || (dgo instanceof ColourShifter && ((ColourShifter) dgo).orientation != UP));
    
            case DOWN:
                dgo = grid[spotX][spotY + 1].cellDynamicItem;
                if (dgo == null) return false;
                return ((dgo instanceof LightSource)
                        || (dgo instanceof TJunction && ((TJunction) dgo).orientation != DOWN)
                        || (dgo instanceof Prism && ((Prism) dgo).orientation != DOWN)
                        || (dgo instanceof ColourShifter && ((ColourShifter) dgo).orientation != DOWN));
            case LEFT:
                dgo = grid[spotX - 1][spotY].cellDynamicItem;
                if (dgo == null) return false;
                return ((dgo instanceof LightSource)
                        || (dgo instanceof TJunction && ((TJunction) dgo).orientation != LEFT)
                        || (dgo instanceof Prism && ((Prism) dgo).orientation != LEFT)
                        || (dgo instanceof ColourShifter && ((ColourShifter) dgo).orientation != LEFT));
            case RIGHT:
                dgo = grid[spotX + 1][spotY].cellDynamicItem;
                if (dgo == null) return false;
                return ((dgo instanceof LightSource)
                        || (dgo instanceof TJunction && ((TJunction) dgo).orientation != RIGHT)
                        || (dgo instanceof Prism && ((Prism) dgo).orientation != RIGHT)
                        || (dgo instanceof ColourShifter && ((ColourShifter) dgo).orientation != RIGHT));
        }
        return false;
    }


    @Override
    public void interactWithLight(short light, GridCell[][] grid, ShortQueue lightProcessingQueue) {
        if (this.orientation == Light.getOrientation(light)) {
            int x1 = Light.getX(light);
            int y1 = Light.getY(light);
            int x2 = x1;
            int y2 = y1;
            FaceOrientation l1 = null;
            FaceOrientation l2 = null;
            switch (this.orientation) {
                case UP:
                case DOWN:
                    // For both these cases, one light to the left and one to the right
                    x1 -= 1;
                    x2 += 1;
                    l1 = LEFT;
                    l2 = RIGHT;
                    break;
                case LEFT:
                case RIGHT:
                    // For both these cases, one light to the top and one to the bottom
                    y1 -= 1;
                    y2 += 1;
                    l1 = UP;
                    l2 = DOWN;
                    break;
            }

            short light1 = Light.create(x1, y1, Light.getColour(light), l1);
            grid[x1][y1].light = light1;
            lightProcessingQueue.add(light1);

            short light2 = Light.create(x2, y2, Light.getColour(light), l2);
            grid[x2][y2].light = light2;
            lightProcessingQueue.add(light2);

        }
    }

    @Override
    public String toString() {
        return this.orientation + " T-Junction";
    }
}
