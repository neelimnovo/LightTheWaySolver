package model.interactionObjects;

import javafx.util.Pair;
import model.GridCell;
import model.GridLayout;
import searchLogic.Light;
import java.util.ArrayList;
import searchLogic.ShortQueue;

import static model.interactionObjects.Colour.*;
import static model.interactionObjects.FaceOrientation.*;
import static model.interactionObjects.StaticGridObject.*;

public class Prism extends DynamicGridObject {
    public final FaceOrientation orientation;

    public Prism(FaceOrientation orientation) {
        this.orientation = orientation;
    }

    @Override
    public String getCorrectImageString() {
        switch (orientation) {
            case UP:
                return "upPrism.png";
            case DOWN:
                return "downPrism.png";
            case LEFT:
                return "leftPrism.png";
            case RIGHT:
                return "rightPrism.png";
            default:
                return null;
        }
    }

    @Override
    // EFFECTS: Filters based on neighbouring wall or incorrect receiver
    public ArrayList<Pair<Integer, Integer>> filter(GridCell[][] grid, ArrayList<Pair<Integer, Integer>> emptySpots) {
        ArrayList<Pair<Integer, Integer>> resultSpots = new ArrayList<>();
        for (Pair<Integer, Integer> spot : emptySpots) {
            int spotX = spot.getKey();
            int spotY = spot.getValue();
            if (isValidNeighbour(grid, spotX, spotY - 1, UP)
                && isValidNeighbour(grid, spotX, spotY + 1, DOWN)
                && isValidNeighbour(grid, spotX - 1, spotY, LEFT)
                && isValidNeighbour(grid, spotX + 1, spotY, RIGHT)) {
                resultSpots.add(new Pair<>(spotX, spotY));
            }
        }
        return resultSpots;
    }

    private boolean isValidNeighbour(GridCell[][] grid, int spotX, int spotY, FaceOrientation neighbourSpot) {
        if (!GridLayout.isWithinBounds(grid, spotX, spotY)) return false;
        if (grid[spotX][spotY].cellStaticItem == WALL) return false;
        if (grid[spotX][spotY].cellStaticItem == EMPTY) {
            DynamicGridObject dgo = grid[spotX][spotY].cellDynamicItem;
            if (dgo == null) return true;
            // Neighbour cannot be a prism. This is the first DGO to be placed, so doesn't have to filter for other DGO types
            return !(dgo instanceof Prism);
        } else {
            // If the static item is not a WALL or EMPTY, it must be a receiver
            switch (this.orientation) {
                case UP: // For a prism facing up, top is red, left is blue, right is yellow, bottom is white
                    switch (neighbourSpot) {
                        case UP:
                            return grid[spotX][spotY].cellStaticItem != RED_RECEIVER;
                        case DOWN:
                            // Cannot block white side entrance
                            return false;
                        case LEFT:
                            return grid[spotX][spotY].cellStaticItem != BLUE_RECEIVER;
                        case RIGHT:
                            return grid[spotX][spotY].cellStaticItem != YELLOW_RECEIVER;
                    }
                case DOWN: // For a prism facing down, bottom is red, left is yellow, right is blue, top is white
                    switch (neighbourSpot) {
                        case UP:
                            // Cannot block white side entrance
                            return false;
                        case DOWN:
                            return grid[spotX][spotY].cellStaticItem != RED_RECEIVER;
                        case LEFT:
                            return grid[spotX][spotY].cellStaticItem != YELLOW_RECEIVER;
                        case RIGHT:
                            return grid[spotX][spotY].cellStaticItem != BLUE_RECEIVER;
                    }
                case LEFT: // For a prism facing left, left is red, top is yellow, bottom is blue, right is white
                    switch (neighbourSpot) {
                        case UP:
                            return grid[spotX][spotY].cellStaticItem != YELLOW_RECEIVER;
                        case DOWN:
                            return grid[spotX][spotY].cellStaticItem != BLUE_RECEIVER;
                        case LEFT:
                            return grid[spotX][spotY].cellStaticItem != RED_RECEIVER;
                        case RIGHT:
                            // Cannot block white side entrance
                            return false;
                    }
                case RIGHT: // For a prism facing right, right is red, top is blue, bottom is yellow, left is white
                    switch (neighbourSpot) {
                        case UP:
                            return grid[spotX][spotY].cellStaticItem != BLUE_RECEIVER;
                        case DOWN:
                            return grid[spotX][spotY].cellStaticItem != YELLOW_RECEIVER;
                        case LEFT:
                            // Cannot block white side entrance
                            return false;
                        case RIGHT:
                            return grid[spotX][spotY].cellStaticItem != RED_RECEIVER;
                    }
            }
            return true;
        }
    }


    @Override
    // EFFECTS: Takes in white light and emits red, blue and yellow light along sides
    public void interactWithLight(short light, GridCell[][] grid, ShortQueue lightProcessingQueue) {
        if (Light.getColour(light) == WHITE && this.orientation == Light.getOrientation(light)) {
            short prismRedLight = -1;
            short prismBlueLight = -1;
            short prismYellowLight = -1;
            int xPos = Light.getX(light);
            int yPos = Light.getY(light);
            int upPos = yPos - 1;
            int downPos = yPos + 1;
            int leftPos = xPos - 1;
            int rightPos = xPos + 1;
            switch (this.orientation) {
                case UP:
                    prismRedLight = Light.create(xPos, upPos, RED, UP);
                    grid[xPos][upPos].light = prismRedLight;

                    prismBlueLight = Light.create(leftPos, yPos, BLUE, LEFT);
                    grid[leftPos][yPos].light = prismBlueLight;

                    prismYellowLight = Light.create(rightPos, yPos, YELLOW, RIGHT);
                    grid[rightPos][yPos].light = prismYellowLight;
                    break;
                case DOWN:
                    prismRedLight = Light.create(xPos, downPos, RED, DOWN);
                    grid[xPos][downPos].light = prismRedLight;

                    prismBlueLight = Light.create(rightPos, yPos, BLUE, RIGHT);
                    grid[rightPos][yPos].light = prismBlueLight;

                    prismYellowLight = Light.create(leftPos, yPos, YELLOW, LEFT);
                    grid[leftPos][yPos].light = prismYellowLight;
                    break;
                case LEFT:
                    prismRedLight = Light.create(leftPos, yPos, RED, LEFT);
                    grid[leftPos][yPos].light = prismRedLight;

                    prismBlueLight = Light.create(xPos, downPos, BLUE, DOWN);
                    grid[xPos][downPos].light = prismBlueLight;

                    prismYellowLight = Light.create(xPos, upPos, YELLOW, UP);
                    grid[xPos][upPos].light = prismYellowLight;
                    break;
                case RIGHT:
                    prismRedLight = Light.create(rightPos, yPos, RED, RIGHT);
                    grid[rightPos][yPos].light = prismRedLight;

                    prismBlueLight = Light.create(xPos, upPos, BLUE, UP);
                    grid[xPos][upPos].light = prismBlueLight;

                    prismYellowLight = Light.create(xPos, downPos, YELLOW, DOWN);
                    grid[xPos][downPos].light = prismYellowLight;
                    break;
            }            

            lightProcessingQueue.add(prismRedLight);
            lightProcessingQueue.add(prismBlueLight);
            lightProcessingQueue.add(prismYellowLight);
        }        
    }
    

    @Override
    public String toString() {
        return this.orientation + " Prism";
    }
}
