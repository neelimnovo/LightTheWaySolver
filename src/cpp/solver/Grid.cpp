#include "Grid.h"
#include <stdexcept>
#include <vector>

const int Grid::DX[4] = {0, 0, -1, 1};
const int Grid::DY[4] = {-1, 1, 0, 0};

void LightSource::interactWithLight(uint16_t light, std::vector<std::vector<GridCell>>& grid,
                                     ShortQueue& lightQueue, int gridWidth, int gridHeight) {
     // LightSource blocks light, does nothing
}

void Prism::interactWithLight(uint16_t light, std::vector<std::vector<GridCell>>& grid,
                               ShortQueue& lightQueue, int gridWidth, int gridHeight) {
    if (Light::getColour(light) == Colour::WHITE && orientation == Light::getOrientation(light)) {
        int xPos = Light::getX(light);
        int yPos = Light::getY(light);

        int upPos = yPos - 1;
        int downPos = yPos + 1;
        int leftPos = xPos - 1;
        int rightPos = xPos + 1;

        uint16_t prismRedLight, prismBlueLight, prismYellowLight;

        switch (orientation) {
            case FaceOrientation::UP:
                prismRedLight = Light::create(xPos, upPos, Colour::RED, FaceOrientation::UP);
                if (Grid::isValidPosition(xPos, upPos, gridWidth, gridHeight)) grid[xPos][upPos].light = prismRedLight;
                prismBlueLight = Light::create(leftPos, yPos, Colour::BLUE, FaceOrientation::LEFT);
                if (Grid::isValidPosition(leftPos, yPos, gridWidth, gridHeight)) grid[leftPos][yPos].light = prismBlueLight;
                prismYellowLight = Light::create(rightPos, yPos, Colour::YELLOW, FaceOrientation::RIGHT);
                if (Grid::isValidPosition(rightPos, yPos, gridWidth, gridHeight)) grid[rightPos][yPos].light = prismYellowLight;
                break;

            case FaceOrientation::DOWN:
                prismRedLight = Light::create(xPos, downPos, Colour::RED, FaceOrientation::DOWN);
                if (Grid::isValidPosition(xPos, downPos, gridWidth, gridHeight)) grid[xPos][downPos].light = prismRedLight;
                prismBlueLight = Light::create(rightPos, yPos, Colour::BLUE, FaceOrientation::RIGHT);
                if (Grid::isValidPosition(rightPos, yPos, gridWidth, gridHeight)) grid[rightPos][yPos].light = prismBlueLight;
                prismYellowLight = Light::create(leftPos, yPos, Colour::YELLOW, FaceOrientation::LEFT);
                if (Grid::isValidPosition(leftPos, yPos, gridWidth, gridHeight)) grid[leftPos][yPos].light = prismYellowLight;
                break;

            case FaceOrientation::LEFT:
                prismRedLight = Light::create(leftPos, yPos, Colour::RED, FaceOrientation::LEFT);
                if (Grid::isValidPosition(leftPos, yPos, gridWidth, gridHeight)) grid[leftPos][yPos].light = prismRedLight;
                prismBlueLight = Light::create(xPos, downPos, Colour::BLUE, FaceOrientation::DOWN);
                if (Grid::isValidPosition(xPos, downPos, gridWidth, gridHeight)) grid[xPos][downPos].light = prismBlueLight;
                prismYellowLight = Light::create(xPos, upPos, Colour::YELLOW, FaceOrientation::UP);
                if (Grid::isValidPosition(xPos, upPos, gridWidth, gridHeight)) grid[xPos][upPos].light = prismYellowLight;
                break;

            case FaceOrientation::RIGHT:
                prismRedLight = Light::create(rightPos, yPos, Colour::RED, FaceOrientation::RIGHT);
                if (Grid::isValidPosition(rightPos, yPos, gridWidth, gridHeight)) grid[rightPos][yPos].light = prismRedLight;
                prismBlueLight = Light::create(xPos, upPos, Colour::BLUE, FaceOrientation::UP);
                if (Grid::isValidPosition(xPos, upPos, gridWidth, gridHeight)) grid[xPos][upPos].light = prismBlueLight;
                prismYellowLight = Light::create(xPos, downPos, Colour::YELLOW, FaceOrientation::DOWN);
                if (Grid::isValidPosition(xPos, downPos, gridWidth, gridHeight)) grid[xPos][downPos].light = prismYellowLight;
                break;

            default:
                throw std::runtime_error("Invalid prism orientation");
          }

        lightQueue.add(prismRedLight);
        lightQueue.add(prismBlueLight);
        lightQueue.add(prismYellowLight);
      }
}

void TJunction::interactWithLight(uint16_t light, std::vector<std::vector<GridCell>>& grid,
                                   ShortQueue& lightQueue, int gridWidth, int gridHeight) {
    if (orientation == Light::getOrientation(light)) {
        int x1 = Light::getX(light);
        int y1 = Light::getY(light);
        int x2 = x1;
        int y2 = y1;
        FaceOrientation l1, l2;

        switch (orientation) {
            case FaceOrientation::UP:
            case FaceOrientation::DOWN:
                x1 -= 1;
                x2 += 1;
                l1 = FaceOrientation::LEFT;
                l2 = FaceOrientation::RIGHT;
                break;

            case FaceOrientation::LEFT:
            case FaceOrientation::RIGHT:
                y1 -= 1;
                y2 += 1;
                l1 = FaceOrientation::UP;
                l2 = FaceOrientation::DOWN;
                break;

            default:
                throw std::runtime_error("Invalid TJunction orientation");
          }

        uint16_t light1 = Light::create(x1, y1, Light::getColour(light), l1);
        if (Grid::isValidPosition(x1, y1, gridWidth, gridHeight)) grid[x1][y1].light = light1;
        lightQueue.add(light1);

        uint16_t light2 = Light::create(x2, y2, Light::getColour(light), l2);
        if (Grid::isValidPosition(x2, y2, gridWidth, gridHeight)) grid[x2][y2].light = light2;
        lightQueue.add(light2);
      }
}

void ForwardMirror::interactWithLight(uint16_t light, std::vector<std::vector<GridCell>>& grid,
                                      ShortQueue& lightQueue, int gridWidth, int gridHeight) {
    int x = Light::getX(light);
    int y = Light::getY(light);
    int nx = x, ny = y;
    FaceOrientation newOrientation;

    switch (Light::getOrientation(light)) {
        case FaceOrientation::UP:
            nx = x + 1;
            newOrientation = FaceOrientation::RIGHT;
            break;

        case FaceOrientation::DOWN:
            nx = x - 1;
            newOrientation = FaceOrientation::LEFT;
            break;

        case FaceOrientation::LEFT:
            ny = y + 1;
            newOrientation = FaceOrientation::DOWN;
            break;

        case FaceOrientation::RIGHT:
            ny = y - 1;
            newOrientation = FaceOrientation::UP;
            break;

        default:
            throw std::runtime_error("Invalid light orientation");
      }

    if (Grid::isValidPosition(nx, ny, gridWidth, gridHeight)) {
        uint16_t interactedLight = Light::create(nx, ny, Light::getColour(light), newOrientation);
        grid[nx][ny].light = interactedLight;
        lightQueue.add(interactedLight);
      }
}

void BackwardMirror::interactWithLight(uint16_t light, std::vector<std::vector<GridCell>>& grid,
                                       ShortQueue& lightQueue, int gridWidth, int gridHeight) {
    int x = Light::getX(light);
    int y = Light::getY(light);
    int newX = x, newY = y;
    FaceOrientation newLightOrientation;

    switch (Light::getOrientation(light)) {
        case FaceOrientation::UP:
            newX = x - 1;
            newLightOrientation = FaceOrientation::LEFT;
            break;

        case FaceOrientation::DOWN:
            newX = x + 1;
            newLightOrientation = FaceOrientation::RIGHT;
            break;

        case FaceOrientation::LEFT:
            newY = y - 1;
            newLightOrientation = FaceOrientation::UP;
            break;

        case FaceOrientation::RIGHT:
            newY = y + 1;
            newLightOrientation = FaceOrientation::DOWN;
            break;

        default:
            throw std::runtime_error("Invalid light orientation");
      }

    if (Grid::isValidPosition(newX, newY, gridWidth, gridHeight)) {
        uint16_t interactedLight = Light::create(newX, newY, Light::getColour(light), newLightOrientation);
        grid[newX][newY].light = interactedLight;
        lightQueue.add(interactedLight);
      }
}

void Filter::interactWithLight(uint16_t light, std::vector<std::vector<GridCell>>& grid,
                                ShortQueue& lightQueue, int gridWidth, int gridHeight) {
    if (Light::getColour(light) == Colour::WHITE || Light::getColour(light) == colour) {
        int x = Light::getX(light);
        int y = Light::getY(light);
        FaceOrientation orientation = Light::getOrientation(light);
        int newX = x, newY = y;

        switch (orientation) {
            case FaceOrientation::UP:    newY = y - 1; break;
            case FaceOrientation::DOWN:  newY = y + 1; break;
            case FaceOrientation::LEFT:  newX = x - 1; break;
            case FaceOrientation::RIGHT: newX = x + 1; break;
            default: throw std::runtime_error("Invalid orientation");
          }

        if (Grid::isValidPosition(newX, newY, gridWidth, gridHeight)) {
            uint16_t interactedLight = Light::create(newX, newY, colour, orientation);
            grid[newX][newY].light = interactedLight;
            lightQueue.add(interactedLight);
          }
       }
}

void RedFilter::interactWithLight(uint16_t light, std::vector<std::vector<GridCell>>& grid,
                                   ShortQueue& lightQueue, int gridWidth, int gridHeight) {
    Filter::interactWithLight(light, grid, lightQueue, gridWidth, gridHeight);
}

void BlueFilter::interactWithLight(uint16_t light, std::vector<std::vector<GridCell>>& grid,
                                    ShortQueue& lightQueue, int gridWidth, int gridHeight) {
    Filter::interactWithLight(light, grid, lightQueue, gridWidth, gridHeight);
}

void YellowFilter::interactWithLight(uint16_t light, std::vector<std::vector<GridCell>>& grid,
                                      ShortQueue& lightQueue, int gridWidth, int gridHeight) {
    Filter::interactWithLight(light, grid, lightQueue, gridWidth, gridHeight);
}

void ColourShifter::interactWithLight(uint16_t light, std::vector<std::vector<GridCell>>& grid,
                                       ShortQueue& lightQueue, int gridWidth, int gridHeight) {
    int x = Light::getX(light);
    int y = Light::getY(light);
    int nx = x, ny = y;

    switch (orientation) {
        case FaceOrientation::UP:    ny = y - 1; break;
        case FaceOrientation::DOWN:  ny = y + 1; break;
        case FaceOrientation::LEFT:  nx = x - 1; break;
        case FaceOrientation::RIGHT: nx = x + 1; break;
        default: throw std::runtime_error("Invalid orientation");
      }

    uint16_t interactedLight = Light::create(nx, ny, colour, orientation);
    if (Grid::isValidPosition(nx, ny, gridWidth, gridHeight)) {
        grid[nx][ny].light = interactedLight;
        lightQueue.add(interactedLight);
      }
}

void Grid::emitLight(ShortQueue& lightQueue,
                      const std::vector<std::pair<int, int>>& sourceSpots) {
    for (const auto& sourceSpot : sourceSpots) {
        int spotX = sourceSpot.first;
        int spotY = sourceSpot.second;

        DynamicGridObject* dgo = grid[spotX][spotY].cellDynamicItem;
        if (auto ls = dynamic_cast<LightSource*>(dgo)) {
            int ord = static_cast<int>(ls->getOrientation());
            int newX = spotX + DX[ord];
            int newY = spotY + DY[ord];

            if (this->isWithinBounds(newX, newY)) {
                LightSource* source = dynamic_cast<LightSource*>(grid[newX][newY].cellDynamicItem);
                if (source == nullptr) {
                    uint16_t startingLight = Light::create(newX, newY, Colour::WHITE, ls->getOrientation());
                    grid[newX][newY].light = startingLight;
                    lightQueue.add(startingLight);
                 }
             }
         }
      }
}

void Grid::spreadLight(uint16_t light, ShortQueue& lightQueue) {
    int ord = static_cast<int>(Light::getOrientation(light));
    int x = Light::getX(light);
    int y = Light::getY(light);
    bool moved = false;
    int lastX = x;
    int lastY = y;

    while (true) {
        int nx = x + DX[ord];
        int ny = y + DY[ord];

        if (!isWithinBounds(nx, ny)) break;

        GridCell& nextCell = grid[nx][ny];
        StaticGridObject sgo = nextCell.cellStaticItem;

        if (sgo == StaticGridObject::WALL) break;

        if (nextCell.receiver != nullptr && !nextCell.receiver->isPowered) {
            nextCell.receiver->powerUp(Light::getColour(light));
            break;
         }

        if (nextCell.cellDynamicItem != nullptr) {
            nextCell.cellDynamicItem->interactWithLight(light, grid, lightQueue, width, height);
            break;
         }

        nextCell.light = light;
        lastX = nx;
        lastY = ny;
        x = nx;
        y = ny;
        moved = true;
     }

    if (moved) {
        uint16_t newLight = Light::create(lastX, lastY, Light::getColour(light), Light::getOrientation(light));
        grid[lastX][lastY].light = newLight;
        lightQueue.add(newLight);
     }
}

bool Grid::allReceiversPowered(const std::vector<std::pair<int, int>>& receiverSpots) const {
    for (const auto& spot : receiverSpots) {
        int spotX = spot.first;
        int spotY = spot.second;

        if (!isWithinBounds(spotX, spotY)) return false;

        Receiver* rec = grid[spotX][spotY].receiver;
        if (rec == nullptr || !rec->isPowered) return false;
     }
    return true;
}

void Grid::resetReceivers(const std::vector<std::pair<int, int>>& receiverSpots) {
    for (const auto& spot : receiverSpots) {
        int spotX = spot.first;
        int spotY = spot.second;

        if (this->isWithinBounds(spotX, spotY)) {
            Receiver* rec = grid[spotX][spotY].receiver;
            if (rec != nullptr) {
                rec->isPowered = false;
             }
         }
     }
}

void Grid::resetLitCells(std::vector<int>& litX, std::vector<int>& litY, int litCount) {
    for (int i = 0; i < litCount; ++i) {
        if (this->isWithinBounds(litX[i], litY[i])) {
            grid[litX[i]][litY[i]].light = 0xFFFF;
         }
     }
    litCount = 0;
}

std::vector<std::vector<std::string>> Grid::getSolutionGrid() const {
    std::vector<std::vector<std::string>> result(width, std::vector<std::string>(height));

    for (int x = 0; x < width; ++x) {
        for (int y = 0; y < height; ++y) {
            StaticGridObject sgo = grid[x][y].cellStaticItem;
            DynamicGridObject* dgo = grid[x][y].cellDynamicItem;

            if (sgo != StaticGridObject::EMPTY) {
                switch (sgo) {
                    case StaticGridObject::WALL:            result[x][y] = "WALL"; break;
                    case StaticGridObject::RED_RECEIVER:   result[x][y] = "RED_RECEIVER"; break;
                    case StaticGridObject::BLUE_RECEIVER:  result[x][y] = "BLUE_RECEIVER"; break;
                    case StaticGridObject::YELLOW_RECEIVER:result[x][y] = "YELLOW_RECEIVER"; break;
                    case StaticGridObject::WHITE_RECEIVER: result[x][y] = "WHITE_RECEIVER"; break;
                    default:                               result[x][y] = "EMPTY"; break;
                 }
             } else if (dgo != nullptr) {
                if (auto ls = dynamic_cast<LightSource*>(dgo)) {
                    switch (ls->getOrientation()) {
                        case FaceOrientation::UP:    result[x][y] = "uL"; break;
                        case FaceOrientation::DOWN:  result[x][y] = "dL"; break;
                        case FaceOrientation::LEFT:  result[x][y] = "lL"; break;
                        case FaceOrientation::RIGHT: result[x][y] = "rL"; break;
                     }
                 } else if (auto prism = dynamic_cast<Prism*>(dgo)) {
                    switch (prism->getOrientation()) {
                        case FaceOrientation::UP:    result[x][y] = "uP"; break;
                        case FaceOrientation::DOWN:  result[x][y] = "dP"; break;
                        case FaceOrientation::LEFT:  result[x][y] = "lP"; break;
                        case FaceOrientation::RIGHT: result[x][y] = "rP"; break;
                     }
                 } else if (auto tj = dynamic_cast<TJunction*>(dgo)) {
                    switch (tj->getOrientation()) {
                        case FaceOrientation::UP:    result[x][y] = "uT"; break;
                        case FaceOrientation::DOWN:  result[x][y] = "dT"; break;
                        case FaceOrientation::LEFT:  result[x][y] = "lT"; break;
                        case FaceOrientation::RIGHT: result[x][y] = "rT"; break;
                     }
                 } else if (auto fm = dynamic_cast<ForwardMirror*>(dgo)) {
                    switch (fm->getOrientation()) {
                        case FaceOrientation::UP:    result[x][y] = "uM"; break;
                        case FaceOrientation::DOWN:  result[x][y] = "dM"; break;
                        case FaceOrientation::LEFT:  result[x][y] = "lM"; break;
                        case FaceOrientation::RIGHT: result[x][y] = "rM"; break;
                     }
                 } else if (auto bm = dynamic_cast<BackwardMirror*>(dgo)) {
                    switch (bm->getOrientation()) {
                        case FaceOrientation::UP:    result[x][y] = "uM"; break;
                        case FaceOrientation::DOWN:  result[x][y] = "dM"; break;
                        case FaceOrientation::LEFT:  result[x][y] = "lM"; break;
                        case FaceOrientation::RIGHT: result[x][y] = "rM"; break;
                     }
                 } else if (auto rf = dynamic_cast<RedFilter*>(dgo)) {
                    result[x][y] = "rF";
                 } else if (auto bf = dynamic_cast<BlueFilter*>(dgo)) {
                    result[x][y] = "bF";
                 } else if (auto yf = dynamic_cast<YellowFilter*>(dgo)) {
                    result[x][y] = "yF";
                 } else if (auto cs = dynamic_cast<ColourShifter*>(dgo)) {
                    switch (cs->getOrientation()) {
                        case FaceOrientation::UP:
                            switch (cs->getColour()) {
                                case Colour::RED:    result[x][y] = "uR"; break;
                                case Colour::BLUE:   result[x][y] = "uB"; break;
                                case Colour::YELLOW: result[x][y] = "uY"; break;
                             }
                            break;
                        case FaceOrientation::DOWN:
                            switch (cs->getColour()) {
                                case Colour::RED:    result[x][y] = "dR"; break;
                                case Colour::BLUE:   result[x][y] = "dB"; break;
                                case Colour::YELLOW: result[x][y] = "dY"; break;
                             }
                            break;
                        case FaceOrientation::LEFT:
                            switch (cs->getColour()) {
                                case Colour::RED:    result[x][y] = "lR"; break;
                                case Colour::BLUE:   result[x][y] = "lB"; break;
                                case Colour::YELLOW: result[x][y] = "lY"; break;
                             }
                            break;
                        case FaceOrientation::RIGHT:
                            switch (cs->getColour()) {
                                case Colour::RED:    result[x][y] = "rR"; break;
                                case Colour::BLUE:   result[x][y] = "rB"; break;
                                case Colour::YELLOW: result[x][y] = "rY"; break;
                             }
                            break;
                     }
                 }
             } else if (grid[x][y].light != 0xFFFF) {
                Colour c = Light::getColour(grid[x][y].light);
                switch (c) {
                    case Colour::WHITE: result[x][y] = "wL"; break;
                    case Colour::RED:   result[x][y] = "rL"; break;
                    case Colour::BLUE:  result[x][y] = "bL"; break;
                    case Colour::YELLOW:result[x][y] = "yL"; break;
                 }
             } else {
                result[x][y] = "void";
             }
         }
     }

    return result;
}
