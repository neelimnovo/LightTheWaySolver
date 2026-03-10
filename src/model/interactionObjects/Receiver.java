package model.interactionObjects;

import searchLogic.Light;

public class Receiver {
    public boolean isPowered;
    public final Colour colour;

    public Receiver(Colour colour) {
        this.colour = colour;
        isPowered = false;
    }

    public void powerUp(short light) {
        if (Light.getColour(light) == this.colour) {
            isPowered = true;
        }
    }
}
