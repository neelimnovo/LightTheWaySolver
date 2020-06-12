package model.interactionObjects.filters;

import javafx.util.Pair;
import model.GridCell;
import model.interactionObjects.Colour;
import searchLogic.Light;

import java.util.ArrayList;
import java.util.LinkedList;

import static model.interactionObjects.Colour.RED;

public class RedFilter extends Filter {
    public RedFilter() {
        super(RED);
    }

    @Override
    public String getCorrectImageString() {
        return "redFilter.png";
    }

}
