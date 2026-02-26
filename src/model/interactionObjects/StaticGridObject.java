package model.interactionObjects;

public enum StaticGridObject {
    WALL("Wall"),
    RED_RECEIVER("Red Receiver"),
    BLUE_RECEIVER("Blue Receiver"),
    YELLOW_RECEIVER("Yellow Receiver"),
    WHITE_RECEIVER("White Receiver"),
    EMPTY("Empty");

    private final String stringVal;

    StaticGridObject(String val) {
        this.stringVal = val;
    }

    @Override
    public String toString(){
        return stringVal;
    }

    private static final java.util.Map<String, StaticGridObject> STRING_TO_ENUM_MAP = new java.util.HashMap<>();
    static {
        STRING_TO_ENUM_MAP.put("wall", WALL);
        STRING_TO_ENUM_MAP.put("redReceiver", RED_RECEIVER);
        STRING_TO_ENUM_MAP.put("blueReceiver", BLUE_RECEIVER);
        STRING_TO_ENUM_MAP.put("yellowReceiver", YELLOW_RECEIVER);
        STRING_TO_ENUM_MAP.put("whiteReceiver", WHITE_RECEIVER);
        STRING_TO_ENUM_MAP.put("void", EMPTY);
    }

    public static StaticGridObject getCorrectObject(String id) {
        return STRING_TO_ENUM_MAP.get(id);
    }

    public static String getCorrectImageString(StaticGridObject sgo) {
        return switch (sgo) {
            case WALL -> "wall.png";
            case RED_RECEIVER -> "redReceiver.png";
            case BLUE_RECEIVER -> "blueReceiver.png";
            case YELLOW_RECEIVER -> "yellowReceiver.png";
            case WHITE_RECEIVER -> "whiteReceiver.png";
            case EMPTY -> "void.png";
            default -> null;
        };
    }
}
