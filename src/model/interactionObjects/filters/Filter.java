package model.interactionObjects.filters;

import javafx.util.Pair;
import model.GridCell;
import model.GridLayout;
import model.interactionObjects.*;
import searchLogic.Light;

import java.util.ArrayList;
import java.util.LinkedList;

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
            Receiver receiver;
            boolean isValidSpot = true;
            boolean upOccluded = false;
            boolean downOccluded = false;
            boolean leftOccluded = false;
            boolean rightOccluded = false;

            // Check UP
            if (GridLayout.isWithinBounds(grid, spotX, spotY - 1)) {
                if (isOccludedSpot(grid, spotX, spotY - 1)) {
                    upOccluded = true;
                }
                receiver = grid[spotX][spotY - 1].receiver;
                if (!isValidReceiver(receiver) || isOnWrongSideOfPrism(grid, spotX, spotY - 1, UP)) {
                    isValidSpot = false;
                }
            }
            // Check DOWN
            if (isValidSpot && GridLayout.isWithinBounds(grid, spotX, spotY + 1)) {
                if (isOccludedSpot(grid, spotX, spotY + 1)) {
                    downOccluded = true;
                }
                receiver = grid[spotX][spotY + 1].receiver;
                if (!isValidReceiver(receiver) || isOnWrongSideOfPrism(grid, spotX, spotY + 1, DOWN)) {
                    isValidSpot = false;
                }
            }
            // Check LEFT
            if (isValidSpot && GridLayout.isWithinBounds(grid, spotX - 1, spotY)) {
                if (isOccludedSpot(grid, spotX - 1, spotY) && (upOccluded || downOccluded)) {
                    isValidSpot = false;
                }
                receiver = grid[spotX - 1][spotY].receiver;
                if (!isValidReceiver(receiver) || isOnWrongSideOfPrism(grid, spotX - 1, spotY, LEFT)) {
                    isValidSpot = false;
                }
            }
            // Check RIGHT
            if (isValidSpot && GridLayout.isWithinBounds(grid, spotX + 1, spotY)) {
                if (isOccludedSpot(grid, spotX + 1, spotY) && (upOccluded || downOccluded)) {
                    isValidSpot = false;
                }
                receiver = grid[spotX + 1][spotY].receiver;
                if (!isValidReceiver(receiver) || isOnWrongSideOfPrism(grid, spotX + 1, spotY, RIGHT)) {
                    isValidSpot = false;
                }
            }
            if (isValidSpot) {
                resultSpots.add(new Pair<>(spotX, spotY));
            }
        }
        return resultSpots;
    }

    private boolean isOccludedSpot(GridCell[][] grid, int spotX, int spotY) {
        // Walls block everything
        if (grid[spotX][spotY].cellStaticItem == WALL) return true;
        DynamicGridObject dgo = grid[spotX][spotY].cellDynamicItem;
        // Occlussion by another filter happens if it of a different colour
        if (dgo != null && dgo.getClass() == Filter.class) {
            return ((Filter) dgo).colour != this.colour;
        } else {
            return false;
        }
    }

    private boolean isOnWrongSideOfPrism(GridCell[][] grid, int spotX, int spotY, FaceOrientation filterSide) {
        DynamicGridObject dgo = grid[spotX][spotY].cellDynamicItem;
        if (dgo != null && dgo.getClass() == Prism.class) {
            switch (filterSide) {
                // Above the filter
                case UP:
                    // upPrisms are blocked if placed above a filter
                    if (((Prism) dgo).orientation == UP) return true;
                    switch (colour) {
                        case RED:
                            return ((Prism) dgo).orientation != DOWN;
                        case BLUE:
                            return ((Prism) dgo).orientation != LEFT;
                        case YELLOW:
                            return ((Prism) dgo).orientation != UP;
                    }
                    break;
                // Below the filter
                case DOWN:
                    // downPrisms are blocked if placed below a filter
                    if (((Prism) dgo).orientation == DOWN) return true;
                    switch (colour) {
                        case RED:
                            return ((Prism) dgo).orientation != UP;
                        case BLUE:
                            return ((Prism) dgo).orientation != RIGHT;
                        case YELLOW:
                            return ((Prism) dgo).orientation != LEFT;
                    }
                // Left of the filter
                case LEFT:
                    // leftPrisms are blocked if placed to the left of a filter
                    if (((Prism) dgo).orientation == LEFT) return true;
                    switch (colour) {
                        case RED:
                            return ((Prism) dgo).orientation != RIGHT;
                        case BLUE:
                            return ((Prism) dgo).orientation != DOWN;
                        case YELLOW:
                            return ((Prism) dgo).orientation != UP;
                    }
                // Right of the filter
                case RIGHT:
                    // rightPrisms are blocked if placed to the right of a filter
                    if (((Prism) dgo).orientation == RIGHT) return true;
                    switch (colour) {
                        case RED:
                            return ((Prism) dgo).orientation != LEFT;
                        case BLUE:
                            return ((Prism) dgo).orientation != UP;
                        case YELLOW:
                            return ((Prism) dgo).orientation != DOWN;
                    }
            }
        }
        return false;
    }

    private boolean isValidReceiver(Receiver receiver) {
        return receiver == null || receiver.colour == this.colour;
    }

    public void interactWithLight(Light light, GridCell[][] grid, LinkedList<Light> lightProcessingQueue) {
        // Only interact with the light if the light is of the same colour or white
        if (light.colour == WHITE || light.colour == this.colour) {
            int x = light.xPos, y = light.yPos;
            FaceOrientation orientation = light.orientation;
            int newX = x, newY = y;
            switch (orientation) {
                case UP:    newY = y - 1; break;
                case DOWN:  newY = y + 1; break;
                case LEFT:  newX = x - 1; break;
                case RIGHT: newX = x + 1; break;
            }
            if (GridLayout.isWithinBounds(grid, newX, newY)) {
                Light interactedLight = new Light(this.colour, orientation, newX, newY);
                grid[newX][newY].light = interactedLight;
                lightProcessingQueue.add(interactedLight);
            }
        }
    }

    @Override
    public String toString() {
        return this.colour + " Filter";
    }
}
