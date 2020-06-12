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
                return "[||]";
            case WHITE_RECEIVER:
                return "[Wr]";
            case RED_RECEIVER:
                return "[Rr]";
            case BLUE_RECEIVER:
                return "[Br]";
            case YELLOW_RECEIVER:
                return "[Yr]";
            case EMPTY:
                if (this.cellDynamicItem != null) {
                    if (this.cellDynamicItem.getClass() == BackwardMirror.class
                            || this.cellDynamicItem.getClass() == ForwardMirror.class) {
                        return "[mR]";
                    }
                    if (this.cellDynamicItem.getClass() == ColourShifter.class) {
                        return "[cS]";
                    }
                    if (this.cellDynamicItem.getClass() == LightSource.class) {
                        return "[lS]";
                    }
                    if (this.cellDynamicItem.getClass() == Prism.class) {
                        return "[pR]";
                    }
                    if (this.cellDynamicItem.getClass() == RedFilter.class) {
                        return "[rF]";
                    }
                    if (this.cellDynamicItem.getClass() == BlueFilter.class) {
                        return "[bF]";
                    }
                    if (this.cellDynamicItem.getClass() == YellowFilter.class) {
                        return "[yF]";
                    }
                    if (this.cellDynamicItem.getClass() == TJunction.class) {
                        return "[tJ]";
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
