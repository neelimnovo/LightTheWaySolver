package searchLogic;

import model.interactionObjects.Colour;
import model.interactionObjects.FaceOrientation;

public class Light {
    
    // Packs light attributes into a 16-bit short
    public static short create(int x, int y, Colour colour, FaceOrientation orientation) {
        int cVal = colour.ordinal();
        int oVal = orientation.ordinal();
        return (short) ((x & 0x0F) | ((y & 0x0F) << 4) | ((cVal & 0x03) << 8) | ((oVal & 0x03) << 10));
    }

    public static int getX(short light) {
        return light & 0x000F;
    }

    public static int getY(short light) {
        return (light & 0x00F0) >> 4;
    }

    public static Colour getColour(short light) {
        return Colour.CACHED_VALUES[(light & 0x0300) >> 8];
    }

    public static FaceOrientation getOrientation(short light) {
        return FaceOrientation.CACHED_VALUES[(light & 0x0C00) >> 10];
    }

    public static String getCorrectLightString(short light) {
        if (light == -1) return null;
        switch (getColour(light)) {
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
