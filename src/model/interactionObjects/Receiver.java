package model.interactionObjects;

import searchLogic.Light;

public class Receiver {
    public boolean isPowered;
    public Colour colour;

    public Receiver(Colour colour) {
        this.colour = colour;
        isPowered = false;
    }

    public void powerUp(Light light) {
        if (light.colour == this.colour) {
            isPowered = true;
        }
    }
}
