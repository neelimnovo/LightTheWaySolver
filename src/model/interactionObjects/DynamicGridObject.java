package model.interactionObjects;

import javafx.util.Pair;
import model.GridCell;
import searchLogic.Light;
import java.util.ArrayList;
import searchLogic.ShortQueue;

public abstract class DynamicGridObject {

    public abstract String getCorrectImageString();

    public abstract ArrayList<Pair<Integer, Integer>> filter(GridCell[][] grid, ArrayList<Pair<Integer, Integer>> emptySpots);

    public abstract void interactWithLight(short light, GridCell[][] grid, ShortQueue lightProcessingQueue);

    public abstract String toString();
}
