package model.interactionObjects.filters;

import javafx.util.Pair;
import model.GridCell;
import java.util.ArrayList;
import static model.interactionObjects.Colour.BLUE;


public class BlueFilter extends Filter {

    public BlueFilter() {
        super(BLUE);
    }

    @Override
    public String getCorrectImageString() {
        return "blueFilter.png";
    }

}
