#include "Solver.h"
#include <vector>
#include <algorithm>
#include <chrono>
#include <iostream>
#include <stdexcept>

DynamicGridObject* createDGO(const std::string& type, int orientation, int colour) {
    FaceOrientation orient = static_cast<FaceOrientation>(orientation);

    if (type == "LightSource") {
        return new LightSource(orient);
    } else if (type == "Prism") {
        return new Prism(orient);
    } else if (type == "TJunction") {
        return new TJunction(orient);
    } else if (type == "ForwardMirror") {
        return new ForwardMirror(orient);
    } else if (type == "BackwardMirror") {
        return new BackwardMirror(orient);
    } else if (type == "RedFilter") {
        return new RedFilter();
    } else if (type == "BlueFilter") {
        return new BlueFilter();
    } else if (type == "YellowFilter") {
        return new YellowFilter();
    } else if (type == "ColourShifter") {
        Colour col = static_cast<Colour>(colour);
        return new ColourShifter(orient, col);
    }

    return nullptr;
}

Solver::Solver(int width, int height)
     : grid(new Grid(width, height)),
      litSpotX(50), litSpotY(50), litCount(0),
      attemptPermutations(0), totalPermutations(0), startTime(0) {
}

Solver::~Solver() {
    delete grid;
    for (auto dgo : dgoQueue) {
        delete dgo;
    }
}

void Solver::initialize(const std::vector<std::pair<int, int>>& emptySpots,
                         const std::vector<std::pair<int, int>>& receiverSpots,
                         const std::vector<DynamicGridObject*>& dgoData) {
    this->emptySpots = emptySpots;
    this->receiverSpots = receiverSpots;

    for (const auto& dgo : dgoData) {
        dgoQueue.push_back(dgo);
     }

    for (const auto& spot : emptySpots) {
        grid->setDynamicItem(spot.first, spot.second, nullptr);
     }

    litSpotX.resize(50);
    litSpotY.resize(50);
    litCount = 0;
}

bool Solver::areIdenticalDGOs(DynamicGridObject* a, DynamicGridObject* b) {
    if (dynamic_cast<LightSource*>(a) && dynamic_cast<LightSource*>(b)) {
        return static_cast<LightSource*>(a)->getOrientation() ==
               static_cast<LightSource*>(b)->getOrientation();
    }
    if (dynamic_cast<Prism*>(a) && dynamic_cast<Prism*>(b)) {
        return static_cast<Prism*>(a)->getOrientation() ==
               static_cast<Prism*>(b)->getOrientation();
    }
    if (dynamic_cast<TJunction*>(a) && dynamic_cast<TJunction*>(b)) {
        return static_cast<TJunction*>(a)->getOrientation() ==
               static_cast<TJunction*>(b)->getOrientation();
    }
    if (dynamic_cast<ForwardMirror*>(a) && dynamic_cast<ForwardMirror*>(b)) {
        return true;
    }
    if (dynamic_cast<BackwardMirror*>(a) && dynamic_cast<BackwardMirror*>(b)) {
        return true;
    }
    if (dynamic_cast<RedFilter*>(a) && dynamic_cast<RedFilter*>(b)) {
        return true;
    }
    if (dynamic_cast<BlueFilter*>(a) && dynamic_cast<BlueFilter*>(b)) {
        return true;
    }
    if (dynamic_cast<YellowFilter*>(a) && dynamic_cast<YellowFilter*>(b)) {
        return true;
    }
    if (dynamic_cast<ColourShifter*>(a) && dynamic_cast<ColourShifter*>(b)) {
        return static_cast<ColourShifter*>(a)->getOrientation() ==
               static_cast<ColourShifter*>(b)->getOrientation() &&
               static_cast<ColourShifter*>(a)->getColour() ==
               static_cast<ColourShifter*>(b)->getColour();
    }

    return false;
}

int Solver::emptySpotIndex(const std::pair<int, int>& spot) {
    for (size_t i = 0; i < emptySpots.size(); ++i) {
        if (emptySpots[i].first == spot.first && emptySpots[i].second == spot.second) {
            return static_cast<int>(i);
        }
     }
    return -1;
}

bool Solver::solveRecursive(std::vector<DynamicGridObject*>& dgoQueue, int iterationSpotIndex) {
    if (dgoQueue.empty()) {
        lightQueue.clear();
        grid->emitLight(lightQueue, sourceSpots);

        std::vector<int> currentLitX, currentLitY;
        currentLitX.reserve(grid->getWidth() * grid->getHeight());
        currentLitY.reserve(grid->getWidth() * grid->getHeight());
        int currentLitCount = 0;

        while (!lightQueue.isEmpty()) {
            uint16_t light = lightQueue.remove();

            int x = Light::getX(light);
            int y = Light::getY(light);

            if (currentLitCount >= static_cast<int>(currentLitX.size())) {
                currentLitX.resize(currentLitX.size() == 0 ? 64 : currentLitX.size() * 2);
                currentLitY.resize(currentLitY.size() == 0 ? 64 : currentLitY.size() * 2);
              }

            currentLitX[currentLitCount] = x;
            currentLitY[currentLitCount] = y;
            currentLitCount++;

            grid->spreadLight(light, lightQueue);
          }

        attemptPermutations++;

        bool solution = grid->allReceiversPowered(receiverSpots);

        grid->resetReceivers(receiverSpots);
        grid->resetLitCells(currentLitX, currentLitY, currentLitCount);

        if (solution) {
            return true;
          }

        return false;
      }

    DynamicGridObject* dgo = dgoQueue.back();
    dgoQueue.pop_back();

    std::vector<std::pair<int, int>> filteredSpots;

    for (const auto& spot : emptySpots) {
        DynamicGridObject* existing = grid->getCell(spot.first, spot.second).cellDynamicItem;
        if (existing == nullptr) {
            filteredSpots.push_back(spot);
         }
      }

    int filteredSpotsStartIndex = 0;
    if (iterationSpotIndex > 0) {
        for (size_t i = 0; i < filteredSpots.size(); ++i) {
            if (emptySpotIndex(filteredSpots[i]) >= iterationSpotIndex) {
                filteredSpotsStartIndex = static_cast<int>(i);
                break;
              }
            if (static_cast<int>(i) == static_cast<int>(filteredSpots.size()) - 1) {
                dgoQueue.push_back(dgo);
                return false;
             }
         }
      }

    for (int i = filteredSpotsStartIndex; i < static_cast<int>(filteredSpots.size()); ++i) {
        std::pair<int, int> spot = filteredSpots[i];
        int spotX = spot.first;
        int spotY = spot.second;

        grid->setDynamicItem(spotX, spotY, dgo);

        if (dynamic_cast<LightSource*>(dgo)) {
            sourceSpots.push_back(spot);
          }

        int nextIterationSpotIndex = 0;
        if (!dgoQueue.empty()) {
            DynamicGridObject* nextDGO = dgoQueue.back();
            if (areIdenticalDGOs(dgo, nextDGO)) {
                nextIterationSpotIndex = emptySpotIndex(spot) + 1;
             }
          }

        if (solveRecursive(dgoQueue, nextIterationSpotIndex)) {
            return true;
          }

        grid->setDynamicItem(spotX, spotY, nullptr);

        if (dynamic_cast<LightSource*>(dgo)) {
            sourceSpots.pop_back();
         }
      }

    dgoQueue.push_back(dgo);
    return false;
}

SolverResult Solver::solve() {
    startTime = std::chrono::duration_cast<std::chrono::microseconds>(
        std::chrono::high_resolution_clock::now().time_since_epoch()).count();

    attemptPermutations = 0;

    long long emptyCount = emptySpots.size();
    long long dgoCount = dgoQueue.size();
    totalPermutations = 1;
    for (long long i = 0; i < dgoCount; ++i) {
        totalPermutations *= emptyCount;
        emptyCount--;
     }

    bool solutionFound = solveRecursive(dgoQueue, 0);

    long long endTime = std::chrono::duration_cast<std::chrono::microseconds>(
        std::chrono::high_resolution_clock::now().time_since_epoch()).count();

    SolverResult result;
    result.solutionFound = solutionFound;
    result.attemptPermutations = attemptPermutations;
    result.totalPermutations = totalPermutations;
    result.timeSpent = (endTime - startTime) / 1000;

    if (solutionFound) {
        result.solutionGrid = getSolutionGrid();
      }

    return result;
}

std::vector<std::vector<std::string>> Solver::getSolutionGrid() {
    return grid->getSolutionGrid();
}
