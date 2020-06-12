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

    public Colour colour;

    public Filter(Colour colour) {
        this.colour = colour;
    }

    @Override
    // EFFECTS: Filters based on neighbours being non-matching receivers or a prism
    public ArrayList<Pair<Integer, Integer>> filter(GridCell[][] grid, ArrayList<Pair<Integer, Integer>> emptySpots) {
        ArrayList<Pair<Integer, Integer>> resultSpots = new ArrayList<>();
        for (Pair<Integer, Integer> spot : emptySpots) {
            int spotX = spot.getKey();
            int spotY = spot.getValue();
            Receiver receiver;
            boolean isValidSpot = true;
            int occlusionCount = 0;
            // UP
            if (GridLayout.isWithinBounds(grid, spotX, spotY - 1)) {
                if (isOccludedSpot(grid, spotX, spotY - 1)) {
                    occlusionCount++;
                }
                receiver = grid[spotX][spotY - 1].receiver;
                if (!isValidReceiver(receiver) || isNextToPrism(grid, spotX, spotY - 1)) {
                    isValidSpot = false;
                }
            }
            // DOWN
            if (isValidSpot && GridLayout.isWithinBounds(grid, spotX, spotY + 1)) {
                if (grid[spotX][spotY].cellStaticItem == WALL) {
                    occlusionCount++;
                }
                receiver = grid[spotX][spotY + 1].receiver;
                if (!isValidReceiver(receiver) || isNextToPrism(grid, spotX, spotY + 1)) {
                    isValidSpot = false;
                }
            }
            // LEFT
            if (isValidSpot && GridLayout.isWithinBounds(grid, spotX - 1, spotY) && (occlusionCount < 2)) {
                if (grid[spotX][spotY].cellStaticItem == WALL) {
                    occlusionCount++;
                }
                receiver = grid[spotX - 1][spotY].receiver;
                if (!isValidReceiver(receiver) || isNextToPrism(grid, spotX - 1, spotY)) {
                    isValidSpot = false;
                }
            }
            // RIGHT
            if (isValidSpot && GridLayout.isWithinBounds(grid, spotX + 1, spotY) && (occlusionCount < 2)) {
                if (grid[spotX][spotY].cellStaticItem == WALL) {
                    occlusionCount++;
                }
                receiver = grid[spotX + 1][spotY].receiver;
                if (!isValidReceiver(receiver) || isNextToPrism(grid, spotX + 1, spotY)) {
                    isValidSpot = false;
                }
            }
            // Add the spot if all neighbouring spots are valid
            if (isValidSpot && (occlusionCount < 2)) {
                resultSpots.add(new Pair<>(spotX, spotY));
            }
        }
        return resultSpots;
    }

    private boolean isOccludedSpot(GridCell[][] grid, int spotX, int spotY) {
        DynamicGridObject dgo = grid[spotX][spotY].cellDynamicItem;
        if (grid[spotX][spotY].cellStaticItem == WALL) {
            return true;
        } else if (dgo != null) {
            if (dgo.getClass() == Prism.class){
                return true;
            } else if ((dgo.getClass() == RedFilter.class && this.colour != RED)
            || (dgo.getClass() == BlueFilter.class && this.colour != BLUE)
            || (dgo.getClass() == YellowFilter.class && this.colour != YELLOW)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean isNextToPrism(GridCell[][] grid, int spotX, int spotY) {
        DynamicGridObject dgo = grid[spotX][spotY].cellDynamicItem;
        return dgo != null && dgo.getClass() == Prism.class;
    }


    private boolean isValidReceiver(Receiver receiver) {
        return receiver == null || receiver.colour == this.colour;
    }

    public void interactWithLight(Light light, GridCell[][] grid, LinkedList<Light> lightProcessingQueue) {
        if (light.colour == WHITE || light.colour == this.colour) {
            Light interactedLight = null;
            switch (light.orientation) {
                case UP:
                    if (GridLayout.isWithinBounds(grid, light.xPos, light.yPos - 1)) {
                        interactedLight = new Light(this.colour, UP, light.xPos, light.yPos - 1);
                        grid[light.xPos][light.yPos - 1].light = interactedLight;
                    }
                    break;
                case DOWN:
                    if (GridLayout.isWithinBounds(grid, light.xPos, light.yPos + 1)) {
                        interactedLight = new Light(this.colour, DOWN, light.xPos, light.yPos + 1);
                        grid[light.xPos][light.yPos + 1].light = interactedLight;
                    }
                    break;
                case LEFT:
                    if (GridLayout.isWithinBounds(grid, light.xPos - 1, light.yPos)) {
                        interactedLight = new Light(this.colour, LEFT, light.xPos - 1, light.yPos);
                        grid[light.xPos - 1][light.yPos].light = interactedLight;
                    }
                    break;
                case RIGHT:
                    if (GridLayout.isWithinBounds(grid, light.xPos + 1, light.yPos)) {
                        interactedLight = new Light(this.colour, RIGHT, light.xPos + 1, light.yPos);
                        grid[light.xPos + 1][light.yPos].light = interactedLight;
                    }
                    break;
            }
            if (interactedLight != null) {
                lightProcessingQueue.add(interactedLight);
            }
        }
    }

    @Override
    public String toString() {
        return this.colour + " Filter";
    }
}
