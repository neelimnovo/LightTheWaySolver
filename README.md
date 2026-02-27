# LightTheWay Solver
A GUI application that allows creation and solving of the levels in the 2007 game "Prism: Light the Way"

The new level menu allows creating new level layout (either from the game, or a custom one)

The load level menu allows for solving levels.

Can solve simple levels:
![](/resources/github/simple.png)

Can solve slightly complex levels:
![](/resources/github/complex.png)

Can solve a bit more complex levels:
![](/resources/github/moreComplex.png)

The app has stored solution for levels that can be solved in a reasonable amount of time. The level loader menu will indicate the solved levels with green buttons.

The solution and solving statistics to each level can also be saved and displayed:
![](/resources/github/stats.PNG)


The wiki explains the approach taken for solving the levels in the game. No copyright infrigement is intended, the app was made simply for a learning experience.

# Instructions to run

To build project, run
```
gradle build
```

To start the JavaFX app, run
```
gradle run
```

TODO
* TODO installation guide for pre gradle dependencies (java, gradle)
* revisit filter logic for
    * shifters
    * lights
    * tJunctions
    * front Mirrors
    * back Mirrors
* Fix total permutations overflow with level 24
* After beating a record, saving level solution saves solving time as zero seconds
* double check t-junction, mirrors filter logic
* What is the longest step of testing one iteration?
* optimize isUnblockedMirrorSpot

# L23
Total empty spots: 49
Total permutations: 10068347520
Number of attempts: 40650824
found solution
Time spent filtering for DGO: 1219
Time spent copying grid: 32972
Time spent projecting light: 8139
    Time spent emitting light: 3776
    Time spent spreading light: 2974
        Time spent incrementing light: 2008
        Time spent interacting with light: 322
Time spent checking receivers powered: 257

# L21
Total empty spots: 57
Total permutations: 502452720
Number of attempts: 39529786
found solution
Time spent filtering for DGO: 967
Time spent copying grid: 37773
Time spent projecting light: 9804
    Time spent emitting light: 3931
    Time spent spreading light: 4116
        Time spent incrementing light: 3003
        Time spent interacting with light: 301
Time spent checking receivers powered: 238

# L13
Total empty spots: 54
Total permutations: 379501200
Number of attempts: 14321383
found solution
Total Time: 12771
Time spent filtering for DGO: 351
Time spent copying grid: 11534
Time spent copying DGO queue: 56
Time spent projecting light: 3167
    Time spent emitting light: 1392
    Time spent spreading light: 1269
        Time spent incrementing light: 842
Time spent checking receivers powered: 59

Time spent filtering for DGO: 282
Time spent copying grid: 23
Time spent projecting light: 2098
    Time spent emitting light: 431
    Time spent spreading light: 1108
        Time spent incrementing light: 553
Time spent checking receivers powered: 150


# L18
Total empty spots: 60
Total permutations: 11703240
Number of attempts: 1234638
found solution
Time spent filtering for DGO: 25
Time spent copying grid: 885
Time spent projecting light: 173
    Time spent emitting light: 108
    Time spent spreading light: 29
        Time spent incrementing light: 13
        Time spent interacting with light: 1
Time spent checking receivers powered: 12

# L10
Total empty spots: 56
Total permutations: 166320
Number of attempts: 54078
found solution
Time spent filtering for DGO: 4
Time spent copying grid: 45
Time spent projecting light: 17
    Time spent emitting light: 4
    Time spent spreading light: 11
        Time spent incrementing light: 2
        Time spent interacting with light: 1
Time spent checking receivers powered: 1