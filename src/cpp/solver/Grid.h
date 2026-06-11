#ifndef GRID_H
#define GRID_H

#include <vector>
#include <string>
#include <cstdint>
#include <memory>

enum class FaceOrientation : uint8_t {
    UP = 0,
    DOWN = 1,
    LEFT = 2,
    RIGHT = 3
};

enum class Colour : uint8_t {
    WHITE = 0,
    RED = 1,
    BLUE = 2,
    YELLOW = 3
};

enum class StaticGridObject : uint8_t {
    WALL = 0,
    RED_RECEIVER = 1,
    BLUE_RECEIVER = 2,
    YELLOW_RECEIVER = 3,
    WHITE_RECEIVER = 4,
    EMPTY = 5
};

struct Receiver {
    Colour colour;
    bool isPowered;

    Receiver(Colour c) : colour(c), isPowered(false) {}

    void powerUp(Colour lightColour) {
        if (lightColour == colour) {
            isPowered = true;
        }
    }
};

struct GridCell;
class ShortQueue;

class DynamicGridObject {
public:
    virtual ~DynamicGridObject() = default;
    virtual FaceOrientation getOrientation() const = 0;
    virtual void interactWithLight(uint16_t light, std::vector<std::vector<GridCell>>& grid,
                                    ShortQueue& lightQueue, int gridWidth, int gridHeight) = 0;
    virtual std::string getType() const = 0;
};

class LightSource : public DynamicGridObject {
private:
    FaceOrientation orientation;

public:
    LightSource(FaceOrientation orient) : orientation(orient) {}
    FaceOrientation getOrientation() const override { return orientation; }
    std::string getType() const override { return "LightSource"; }
    void interactWithLight(uint16_t light, std::vector<std::vector<GridCell>>& grid,
                            ShortQueue& lightQueue, int gridWidth, int gridHeight) override;
};

class Prism : public DynamicGridObject {
private:
    FaceOrientation orientation;

public:
    Prism(FaceOrientation orient) : orientation(orient) {}
    FaceOrientation getOrientation() const override { return orientation; }
    std::string getType() const override { return "Prism"; }
    void interactWithLight(uint16_t light, std::vector<std::vector<GridCell>>& grid,
                            ShortQueue& lightQueue, int gridWidth, int gridHeight) override;
};

class TJunction : public DynamicGridObject {
private:
    FaceOrientation orientation;

public:
    TJunction(FaceOrientation orient) : orientation(orient) {}
    FaceOrientation getOrientation() const override { return orientation; }
    std::string getType() const override { return "TJunction"; }
    void interactWithLight(uint16_t light, std::vector<std::vector<GridCell>>& grid,
                            ShortQueue& lightQueue, int gridWidth, int gridHeight) override;
};

class ForwardMirror : public DynamicGridObject {
private:
    FaceOrientation orientation;

public:
    ForwardMirror(FaceOrientation orient) : orientation(orient) {}
    FaceOrientation getOrientation() const override { return orientation; }
    std::string getType() const override { return "ForwardMirror"; }
    void interactWithLight(uint16_t light, std::vector<std::vector<GridCell>>& grid,
                            ShortQueue& lightQueue, int gridWidth, int gridHeight) override;
};

class BackwardMirror : public DynamicGridObject {
private:
    FaceOrientation orientation;

public:
    BackwardMirror(FaceOrientation orient) : orientation(orient) {}
    FaceOrientation getOrientation() const override { return orientation; }
    std::string getType() const override { return "BackwardMirror"; }
    void interactWithLight(uint16_t light, std::vector<std::vector<GridCell>>& grid,
                            ShortQueue& lightQueue, int gridWidth, int gridHeight) override;
};

class Filter : public DynamicGridObject {
protected:
    Colour colour;
    FaceOrientation orientation;

public:
    Filter(Colour c, FaceOrientation orient = FaceOrientation::UP) : colour(c), orientation(orient) {}
    Colour getColour() const { return colour; }
    FaceOrientation getOrientation() const override { return orientation; }
    std::string getType() const override { return "Filter"; }
    virtual void interactWithLight(uint16_t light, std::vector<std::vector<GridCell>>& grid,
                                    ShortQueue& lightQueue, int gridWidth, int gridHeight) = 0;
};

class RedFilter : public Filter {
public:
    RedFilter() : Filter(Colour::RED) {}
    void interactWithLight(uint16_t light, std::vector<std::vector<GridCell>>& grid,
                            ShortQueue& lightQueue, int gridWidth, int gridHeight) override;
};

class BlueFilter : public Filter {
public:
    BlueFilter() : Filter(Colour::BLUE) {}
    void interactWithLight(uint16_t light, std::vector<std::vector<GridCell>>& grid,
                            ShortQueue& lightQueue, int gridWidth, int gridHeight) override;
};

class YellowFilter : public Filter {
public:
    YellowFilter() : Filter(Colour::YELLOW) {}
    void interactWithLight(uint16_t light, std::vector<std::vector<GridCell>>& grid,
                            ShortQueue& lightQueue, int gridWidth, int gridHeight) override;
};

class ColourShifter : public DynamicGridObject {
private:
    FaceOrientation orientation;
    Colour colour;

public:
    ColourShifter(FaceOrientation orient, Colour c) : orientation(orient), colour(c) {}
    FaceOrientation getOrientation() const override { return orientation; }
    Colour getColour() const { return colour; }
    std::string getType() const override { return "ColourShifter"; }
    void interactWithLight(uint16_t light, std::vector<std::vector<GridCell>>& grid,
                            ShortQueue& lightQueue, int gridWidth, int gridHeight) override;
};

struct GridCell {
    StaticGridObject cellStaticItem;
    DynamicGridObject* cellDynamicItem;
    Receiver* receiver;
    uint16_t light;

    GridCell() : cellStaticItem(StaticGridObject::EMPTY), cellDynamicItem(nullptr),
                  receiver(nullptr), light(0xFFFF) {}
    GridCell(StaticGridObject sgo) : cellStaticItem(sgo), cellDynamicItem(nullptr),
                                      receiver(nullptr), light(0xFFFF) {}
    GridCell(StaticGridObject sgo, DynamicGridObject* dgo, Receiver* rec, uint16_t l)
           : cellStaticItem(sgo), cellDynamicItem(dgo), receiver(rec), light(l) {}
};

class Light {
public:
    static uint16_t create(int x, int y, Colour colour, FaceOrientation orientation) {
        uint16_t cVal = static_cast<uint16_t>(colour);
        uint16_t oVal = static_cast<uint16_t>(orientation);
        return static_cast<uint16_t>((x & 0x0F) | ((y & 0x0F) << 4) | ((cVal & 0x03) << 8) | ((oVal & 0x03) << 10));
    }

    static int getX(uint16_t light) { return light & 0x000F; }
    static int getY(uint16_t light) { return (light & 0x00F0) >> 4; }
    static Colour getColour(uint16_t light) { return static_cast<Colour>((light & 0x0300) >> 8); }
    static FaceOrientation getOrientation(uint16_t light) { return static_cast<FaceOrientation>((light & 0x0C00) >> 10); }

    static FaceOrientation getOppositeOrientation(FaceOrientation orientation) {
        switch (orientation) {
            case FaceOrientation::UP:    return FaceOrientation::DOWN;
            case FaceOrientation::DOWN:  return FaceOrientation::UP;
            case FaceOrientation::LEFT:  return FaceOrientation::RIGHT;
            case FaceOrientation::RIGHT: return FaceOrientation::LEFT;
            default: throw std::runtime_error("Invalid orientation");
        }
    }
};

class ShortQueue {
private:
    std::vector<uint16_t> elements;
    size_t head;
    size_t tail;
    size_t size;

    void resize() {
        std::vector<uint16_t> newElements(elements.size() * 2);
        for (size_t i = 0; i < size; ++i) {
            newElements[i] = elements[(head + i) % elements.size()];
        }
        elements = std::move(newElements);
        head = 0;
        tail = size;
    }

public:
    ShortQueue(size_t capacity = 1024) : head(0), tail(0), size(0) {
        elements.reserve(capacity);
    }

    void add(uint16_t e) {
        if (size == elements.size()) {
            if (elements.empty()) elements.resize(64);
            else resize();
        }
        elements[tail] = e;
        tail = (tail + 1) % elements.size();
        size++;
    }

    uint16_t remove() {
        if (size == 0) throw std::runtime_error("Queue is empty");
        uint16_t e = elements[head];
        head = (head + 1) % elements.size();
        size--;
        return e;
    }

    bool isEmpty() const { return size == 0; }
    void clear() { head = 0; tail = 0; size = 0; }
    size_t getSize() const { return size; }
};

class Grid {
private:
    int width;
    int height;
    std::vector<std::vector<GridCell>> grid;

    static const int DX[4];
    static const int DY[4];

    bool isWithinBounds(int x, int y) const {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

public:
    static bool isValidPosition(int x, int y, int gridWidth, int gridHeight) {
        return x >= 0 && x < gridWidth && y >= 0 && y < gridHeight;
    }
    Grid(int w, int h) : width(w), height(h) {
        grid.resize(width, std::vector<GridCell>(height));
    }

    void setStaticItem(int x, int y, StaticGridObject sgo) {
        if (isWithinBounds(x, y)) grid[x][y].cellStaticItem = sgo;
    }

    void setDynamicItem(int x, int y, DynamicGridObject* dgo) {
        if (isWithinBounds(x, y)) grid[x][y].cellDynamicItem = dgo;
    }

    void setReceiver(int x, int y, Receiver* rec) {
        if (isWithinBounds(x, y)) grid[x][y].receiver = rec;
    }

    void setLight(int x, int y, uint16_t light) {
        if (isWithinBounds(x, y)) grid[x][y].light = light;
    }

    GridCell& getCell(int x, int y) { return grid[x][y]; }
    const GridCell& getCell(int x, int y) const { return grid[x][y]; }

    int getWidth() const { return width; }
    int getHeight() const { return height; }

    void emitLight(ShortQueue& lightQueue,
                    const std::vector<std::pair<int, int>>& sourceSpots);

    void spreadLight(uint16_t light, ShortQueue& lightQueue);

    bool allReceiversPowered(const std::vector<std::pair<int, int>>& receiverSpots) const;

    void resetReceivers(const std::vector<std::pair<int, int>>& receiverSpots);
    void resetLitCells(std::vector<int>& litX, std::vector<int>& litY, int litCount);

    std::vector<std::vector<std::string>> getSolutionGrid() const;
};

#endif // GRID_H
