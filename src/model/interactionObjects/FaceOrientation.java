package model.interactionObjects;

public enum FaceOrientation {
    UP, DOWN, LEFT, RIGHT;

    public static final FaceOrientation[] CACHED_VALUES = values();

    public static FaceOrientation getOppositeOrientation(FaceOrientation orientation) {
        switch (orientation) {
            case UP:
                return DOWN;
            case DOWN:
                return UP;
            case LEFT:
                return RIGHT;
            case RIGHT:
                return LEFT;
            default:
                throw new IllegalStateException("Unexpected value: " + orientation);
        }
    }
}
