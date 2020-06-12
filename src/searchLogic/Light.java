package searchLogic;

import model.interactionObjects.Colour;
import model.interactionObjects.FaceOrientation;

public class Light {
    public Colour colour;
    public FaceOrientation orientation;
    public int xPos;
    public int yPos;

    public Light(Colour colour, FaceOrientation orientation, int xPos, int yPos) {
        this.colour = colour;
        this.orientation = orientation;
        this.xPos = xPos;
        this.yPos = yPos;
    }

    public String getCorrectLightString() {
        switch (this.colour) {
            case WHITE:
                return "whiteLight.png";
            case RED:
                return "redLight.png";
            case BLUE:
                return "blueLight.png";
            case YELLOW:
                return "yellowLight.png";
        }
        return null;
    }
}
