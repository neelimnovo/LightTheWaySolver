package model.interactionObjects.filters;

import static model.interactionObjects.Colour.BLUE;

public final class BlueFilter extends Filter {

    public BlueFilter() {
        super(BLUE);
    }

    @Override
    public String getCorrectImageString() {
        return "blueFilter.png";
    }

}
