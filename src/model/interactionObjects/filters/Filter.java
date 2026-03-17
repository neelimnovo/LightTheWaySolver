package model.interactionObjects.filters;

import javafx.util.Pair;
import model.GridCell;
import model.GridLayout;
import model.interactionObjects.*;
import searchLogic.Light;

import java.util.ArrayList;
import searchLogic.ShortQueue;

import static model.interactionObjects.Colour.*;
import static model.interactionObjects.FaceOrientation.*;
import static model.interactionObjects.StaticGridObject.WALL;

public abstract class Filter extends DynamicGridObject {

    public final Colour colour;

    public Filter(Colour colour) {
        this.colour = colour;
    }

    @Override
    // EFFECTS: Filters based on neighbours being non-matching receivers or a prism
    public ArrayList<Pair<Integer, Integer>> filter(GridCell[][] grid, ArrayList<Pair<Integer, Integer>> emptySpots) {
        ArrayList<Pair<Integer, Integer>> resultSpots = new ArrayList<>(emptySpots.size());
        for (Pair<Integer, Integer> spot : emptySpots) {
            int spotX = spot.getKey();
            int spotY = spot.getValue();
            boolean isValidSpot = true;

            if (isInvalidDynamicOjbect(grid, spotX, spotY - 1, UP) ||
                isInvalidDynamicOjbect(grid, spotX, spotY + 1, DOWN) ||
                isInvalidDynamicOjbect(grid, spotX - 1, spotY, LEFT) ||
                isInvalidDynamicOjbect(grid, spotX + 1, spotY, RIGHT)) isValidSpot = false;
            
            // If it still valid, check for other invalidity factors
            if (isValidSpot) {
                boolean upDynamicOccluded = isDynamicOccludedSpot(grid, spotX, spotY - 1, UP);
                boolean downDynamicOccluded = isDynamicOccludedSpot(grid, spotX, spotY + 1, DOWN);
                boolean leftDynamicOccluded = isDynamicOccludedSpot(grid, spotX - 1, spotY, LEFT);
                
                // Re-calculating static occlusions for the "&& (up || down)" part
                boolean upStaticOccluded = isStaticOccludedSpot(grid, spotX, spotY - 1);
                boolean downStaticOccluded = isStaticOccludedSpot(grid, spotX, spotY + 1);
                boolean upOccluded = upStaticOccluded || upDynamicOccluded;
                boolean downOccluded = downStaticOccluded || downDynamicOccluded;

                // check left and top or bottom occlusion
                if ((leftDynamicOccluded || isStaticOccludedSpot(grid, spotX - 1, spotY)) && (upOccluded || downOccluded)) isValidSpot = false;
                // check right and top or bottom occlusion
                if (isValidSpot && (isDynamicOccludedSpot(grid, spotX + 1, spotY, RIGHT) || isStaticOccludedSpot(grid, spotX + 1, spotY)) && (upOccluded || downOccluded)) isValidSpot = false;
            }
            
            if (isValidSpot) {
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
            boolean isValidSpot = true;
            
            boolean upOccluded = isStaticOccludedSpot(grid, spotX, spotY - 1);
            boolean downOccluded = isStaticOccludedSpot(grid, spotX, spotY + 1);
            
            if (!isValidReceiver(grid[spotX][spotY - 1].receiver)) isValidSpot = false;
            if (isValidSpot && !isValidReceiver(grid[spotX][spotY + 1].receiver)) isValidSpot = false;
            if (isValidSpot && !isValidReceiver(grid[spotX - 1][spotY].receiver)) isValidSpot = false;
            if (isValidSpot && !isValidReceiver(grid[spotX + 1][spotY].receiver)) isValidSpot = false;
            
            // Becomes invalid if any two adjacent-sides are blocked by walls 
            if (isValidSpot) {
                if (isStaticOccludedSpot(grid, spotX - 1, spotY) && (upOccluded || downOccluded)) isValidSpot = false;
                if (isValidSpot && isStaticOccludedSpot(grid, spotX + 1, spotY) && (upOccluded || downOccluded)) isValidSpot = false;
            }

            if (isValidSpot) {
                resultSpots.add(new Pair<>(spotX, spotY));
            }
        }
        return resultSpots;
    }

    private boolean isStaticOccludedSpot(GridCell[][] grid, int spotX, int spotY) {
        return grid[spotX][spotY].cellStaticItem == WALL;
    }

    private boolean isDynamicOccludedSpot(GridCell[][] grid, int spotX, int spotY, FaceOrientation position) {
        // Dynamic occlusion means one side is covered such that light cannot exit through that side
        DynamicGridObject dgo = grid[spotX][spotY].cellDynamicItem;
        if (dgo == null) return false;
        if (dgo instanceof Filter) {
            // Adjacent to a different coloured filter is an occlusion
            return ((Filter) dgo).colour != this.colour;
        }
        if (dgo instanceof LightSource) {
            // Adjacent to any non-entrance side of a light source is an occlusion
            switch (position) {
                case UP:
                    return ((LightSource) dgo).orientation != DOWN;
                case DOWN:
                    return ((LightSource) dgo).orientation != UP;
                case LEFT:
                    return ((LightSource) dgo).orientation != RIGHT;
                case RIGHT:
                    return ((LightSource) dgo).orientation != LEFT;
            }
        }
        // Backside of tjunction is an occlusion
        if (dgo instanceof TJunction) {
            switch (position) {
                case UP:
                    return ((TJunction) dgo).orientation == DOWN;
                case DOWN:
                    return ((TJunction) dgo).orientation == UP;
                case LEFT:
                    return ((TJunction) dgo).orientation == RIGHT;
                case RIGHT:
                    return ((TJunction) dgo).orientation == LEFT;
            }
        }
        if (dgo instanceof ColourShifter) {
            // Adjacent to a different coloured shifter is an occlusion
            return ((ColourShifter) dgo).colour != this.colour;
        }
        // No occlusion created by either mirrors
        return false;
    }

    private boolean isInvalidDynamicOjbect(GridCell[][] grid, int spotX, int spotY, FaceOrientation position) {
        DynamicGridObject dgo = grid[spotX][spotY].cellDynamicItem;
        if (dgo == null) return false;
        if (dgo instanceof Filter) {
            return ((Filter) dgo).colour != this.colour;
        }
        if (dgo instanceof Prism && isOnWrongSideOfPrism((Prism) dgo, spotX, spotY, position)) {
            return true;
        }
        if (dgo instanceof ColourShifter && ((ColourShifter) dgo).colour != this.colour) {
            // Invalid placement if placed at the mouth of a differently coloured colour shifter
            switch (position) {
                case UP:
                    return ((ColourShifter) dgo).orientation == DOWN;
                case DOWN:
                    return ((ColourShifter) dgo).orientation == UP;
                case LEFT:
                    return ((ColourShifter) dgo).orientation == RIGHT;
                case RIGHT:
                    return ((ColourShifter) dgo).orientation == LEFT;
            }
        }
        return false;
    }

    private boolean isOnWrongSideOfPrism(Prism prism, int spotX, int spotY, FaceOrientation filterSide) {
        switch (filterSide) {
            // Above the filter
            case UP:
                // upPrisms are blocked if placed above a filter
                if (prism.orientation == UP) return true;
                switch (colour) {
                    case RED:
                        return prism.orientation != DOWN;
                    case BLUE:
                        return prism.orientation != LEFT;
                    case YELLOW:
                        return prism.orientation != UP;
                }
                break;
            // Below the filter
            case DOWN:
                // downPrisms are blocked if placed below a filter
                if (prism.orientation == DOWN) return true;
                switch (colour) {
                    case RED:
                        return prism.orientation != UP;
                    case BLUE:
                        return prism.orientation != RIGHT;
                    case YELLOW:
                        return prism.orientation != LEFT;
                }
            // Left of the filter
            case LEFT:
                // leftPrisms are blocked if placed to the left of a filter
                if (prism.orientation == LEFT) return true;
                switch (colour) {
                    case RED:
                        return prism.orientation != RIGHT;
                    case BLUE:
                        return prism.orientation != DOWN;
                    case YELLOW:
                        return prism.orientation != UP;
                }
            // Right of the filter
            case RIGHT:
                // rightPrisms are blocked if placed to the right of a filter
                if (prism.orientation == RIGHT) return true;
                switch (colour) {
                    case RED:
                        return prism.orientation != LEFT;
                    case BLUE:
                        return prism.orientation != UP;
                    case YELLOW:
                        return prism.orientation != DOWN;
                }
        }
        return false;
    }

    private boolean isValidReceiver(Receiver receiver) {
        return receiver == null || receiver.colour == this.colour;
    }

    public void interactWithLight(short light, GridCell[][] grid, ShortQueue lightProcessingQueue) {
        // Only interact with the light if the light is of the same colour or white
        if (Light.getColour(light) == WHITE || Light.getColour(light) == this.colour) {
            int x = Light.getX(light), y = Light.getY(light);
            FaceOrientation orientation = Light.getOrientation(light);
            int newX = x, newY = y;
            switch (orientation) {
                case UP:    newY = y - 1; break;
                case DOWN:  newY = y + 1; break;
                case LEFT:  newX = x - 1; break;
                case RIGHT: newX = x + 1; break;
            }
            short interactedLight = Light.create(newX, newY, this.colour, orientation);
            grid[newX][newY].light = interactedLight;
            lightProcessingQueue.add(interactedLight);
        }
    }

    @Override
    public String toString() {
        return this.colour + " Filter";
    }
}
