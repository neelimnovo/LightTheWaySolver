package model;
import model.interactionObjects.*;
import model.interactionObjects.filters.BlueFilter;
import model.interactionObjects.filters.RedFilter;
import model.interactionObjects.filters.YellowFilter;

import java.awt.*;
import java.util.ArrayList;

public class GridLayout {
    public GridCell[][] gridCellArray;

    // Interactable Objects
    public ArrayList<LightSource> lights;
    public ArrayList<BlueFilter> blueFilters;
    public ArrayList<RedFilter> redFilters;
    public ArrayList<YellowFilter> yellowFilters;
    public ArrayList<BackwardMirror> backMirrors;
    public ArrayList<ForwardMirror> frontMirrors;
    public ArrayList<Prism> prisms;
    public ArrayList<TJunction> tJunctions;
    public ArrayList<ColourShifter> colourShifters;

    // Main constructor used to create a level layout
    public GridLayout (int xSize, int ySize) {
        gridCellArray = new GridCell[xSize][ySize];
        blueFilters = new ArrayList<>();
        redFilters = new ArrayList<>();
        yellowFilters = new ArrayList<>();
        backMirrors = new ArrayList<>();
        frontMirrors = new ArrayList<>();
        lights = new ArrayList<>();
        prisms = new ArrayList<>();
        tJunctions = new ArrayList<>();
        colourShifters = new ArrayList<>();
    }

    // EFFECTS: Copy constructor for the gridCellArray
    public static GridCell[][] copyGridCellArray (GridCell[][] grid) {
        GridCell[][] copyGrid = new GridCell[grid.length][grid[0].length];
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[0].length; y++) {
                copyGrid[x][y] = new GridCell(grid[x][y].cellStaticItem,
                        grid[x][y].cellDynamicItem,
                        grid[x][y].receiver,
                        grid[x][y].light);
            }
        }
        return copyGrid;
    }

    // EFFECTS: Checks if x and y are within the bounds of the gridcell dimensions
    public static boolean isWithinBounds(GridCell[][] grid, int x, int y) {
        return  (0 <= x)
                && (x < grid.length)
                && (0 <= y)
                && (y < grid[0].length);
    }
}
