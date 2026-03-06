package model.interactionObjects.filters;

import static model.interactionObjects.Colour.YELLOW;

public final class YellowFilter extends Filter {

    public YellowFilter() {
        super(YELLOW);
    }

    @Override
    public String getCorrectImageString() {
        return "yellowFilter.png";
    }

}
