package model.interactionObjects.filters;

import javafx.util.Pair;
import model.GridCell;
import model.interactionObjects.Colour;
import searchLogic.Light;

import java.util.ArrayList;
import java.util.LinkedList;

import static model.interactionObjects.Colour.YELLOW;

public class YellowFilter extends Filter {

    public YellowFilter() {
        super(YELLOW);
    }

    @Override
    public String getCorrectImageString() {
        return "yellowFilter.png";
    }

}
