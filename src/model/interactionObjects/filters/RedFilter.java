package model.interactionObjects.filters;

import static model.interactionObjects.Colour.RED;

public final class RedFilter extends Filter {
    public RedFilter() {
        super(RED);
    }

    @Override
    public String getCorrectImageString() {
        return "redFilter.png";
    }

}
