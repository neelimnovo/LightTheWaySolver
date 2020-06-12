package model.interactionObjects;

public enum StaticGridObject {
    WALL("Wall"),
    RED_RECEIVER("Red Receiver"),
    BLUE_RECEIVER("Blue Receiver"),
    YELLOW_RECEIVER("Yellow Receiver"),
    WHITE_RECEIVER("White Receiver"),
    EMPTY("Empty");

    private String stringVal;

    StaticGridObject(String val) {
        this.stringVal = val;
    }

    @Override
    public String toString(){
        return stringVal;
    }

    public static StaticGridObject getCorrectObject(String id) {
        switch (id) {
            case "wall":
                return StaticGridObject.WALL;
            case "redReceiver":
                return StaticGridObject.RED_RECEIVER;
            case "blueReceiver":
                return StaticGridObject.BLUE_RECEIVER;
            case "yellowReceiver":
                return StaticGridObject.YELLOW_RECEIVER;
            case "whiteReceiver":
                return StaticGridObject.WHITE_RECEIVER;
            case "void":
                return StaticGridObject.EMPTY;
        }
        return null;
    }

    public static String getCorrectImageString(StaticGridObject sgo) {
        switch (sgo) {
            case WALL:
                return "wall.png";
            case RED_RECEIVER:
                return "redReceiver.png";
            case BLUE_RECEIVER:
                return "blueReceiver.png";
            case YELLOW_RECEIVER:
                return "yellowReceiver.png";
            case WHITE_RECEIVER:
                return "whiteReceiver.png";
            case EMPTY:
                return "void.png";
            default:
                return null;
        }
    }
}
