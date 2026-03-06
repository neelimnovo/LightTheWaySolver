package model;

import model.interactionObjects.*;
import model.interactionObjects.filters.BlueFilter;
import model.interactionObjects.filters.Filter;
import model.interactionObjects.filters.RedFilter;
import model.interactionObjects.filters.YellowFilter;
import searchLogic.Light;

import static model.interactionObjects.StaticGridObject.EMPTY;

public class GridCell {
    public StaticGridObject cellStaticItem;
    public DynamicGridObject cellDynamicItem;
    public Receiver receiver;
    public Light light;

    public GridCell(StaticGridObject cellItem) {
        this.cellStaticItem = cellItem;
    }

    public GridCell(StaticGridObject sgo, DynamicGridObject dgo, Receiver receiver, Light light) {
        this.cellStaticItem = sgo;
        this.cellDynamicItem = dgo;
        this.receiver = receiver;
        this.light = light;
    }

    public String toString() {
        switch (this.cellStaticItem) {
            case WALL:
                // use escape sequence for black background colour
                return "\u001B[100m[||]\u001B[0m";
            case WHITE_RECEIVER:
                return "[Wr]";
            case RED_RECEIVER:
                return "[\u001B[31mRr\u001B[0m]";
            case BLUE_RECEIVER:
                return "[\u001B[34mBr\u001B[0m]";
            case YELLOW_RECEIVER:
                return "[\u001B[33mYr\u001B[0m]";
            case EMPTY:
                if (this.cellDynamicItem != null) {
                    if (this.cellDynamicItem instanceof BackwardMirror
                            || this.cellDynamicItem instanceof ForwardMirror) {
                        // use escape sequence for the colour purple
                        return "\u001B[36m[mR]\u001B[0m";
                    }
                    if (this.cellDynamicItem instanceof ColourShifter) {
                        return "[cS]";
                    }
                    if (this.cellDynamicItem instanceof LightSource) {
                        LightSource ls = (LightSource) this.cellDynamicItem;
                        String lsString = "";
                        switch (ls.orientation) {
                            case UP:
                                lsString = "uL";
                                break;
                            case DOWN:
                                lsString = "dL";
                                break;
                            case LEFT:
                                lsString = "lL";
                                break;
                            case RIGHT:
                                lsString = "rL";
                                break;
                        }
                        // Use green escape sequence
                        return "\u001B[32m[" + lsString + "]\u001B[0m";
                    }
                    if (this.cellDynamicItem instanceof Prism) {
                        return "[pR]";
                    }
                    if (this.cellDynamicItem instanceof RedFilter) {
                        return "\u001B[31m[rF]\u001B[0m";
                    }
                    if (this.cellDynamicItem instanceof BlueFilter) {
                        return "\u001B[34m[bF]\u001B[0m";
                    }
                    if (this.cellDynamicItem instanceof YellowFilter) {
                        return "\u001B[33m[yF]\u001B[0m";
                    }
                    if (this.cellDynamicItem instanceof TJunction) {
                        return "\u001B[35m[tJ]\u001B[0m";
                    }
                } else if (this.light != null) {
                    return "[Li]";
                } else {
                    return "[  ]";
                }
        }
        return null;
    }

    public static void printGridCell(GridCell[][] grid) {
        for (int i = 0; i < grid[0].length; i++) {
            for (int j = 0; j < grid.length; j++) {
                System.out.print(grid[j][i]);
            }
            System.out.println("");
        }
        System.out.println("");
    }
}
