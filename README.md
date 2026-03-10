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

# Prerequisites to run

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
    * tJunctions 
    * front Mirrors (maybe bugged)
    * back Mirrors (maybe bugged)
    * shifters

* After beating a record, saving level solution saves solving time as zero seconds
* optimize isUnblockedMirrorSpot
* Fix bug with leftover light in a level
* Profile sub 10 second level with visualVM


# L23
Number of DynamicObjects: 6
Total empty spots: 49
Number of attempts: 38889667
Time spent filtering for DGO: 828
Time spent projecting light: 7204
    Time spent emitting light: 2670
    Time spent spreading light: 2637
        Time spent incrementing light: 1644
Time spent checking receivers powered: 275

# L21
Number of DynamicObjects: 5
Total empty spots: 57
Total permutations: 502452720
Number of attempts: 39212857
Found solution!
Time spent filtering for DGO: 614
Time spent projecting light: 8152
    Time spent emitting light: 2820
    Time spent spreading light: 3225
        Time spent incrementing light: 2068
Time spent checking receivers powered: 344

# L20
Time spent projecting light: 71769
Time spent emitting light: 26482
Time spent spreading light: 33213
Time spent incrementing light: 23505
Time spent checking receivers powered: 2382

Time spent projecting light: 29659
Time spent emitting light: 8441
Time spent spreading light: 15757
Time spent incrementing light: 10706
Time spent checking receivers powered: 1889

# L24
Time spent projecting light: 4808
Time spent emitting light: 1978
Time spent spreading light: 2156
Time spent incrementing light: 1404
Time spent checking receivers powered: 435

Time spent projecting light: 5489
Time spent emitting light: 1841
Time spent spreading light: 2195
Time spent incrementing light: 1492
Time spent checking receivers powered: 509

# L13
Number of DynamicObjects: 5
Total empty spots: 54
Total permutations: 379501200
Number of attempts: 14159909
Time spent filtering for DGO: 227
Time spent projecting light: 2851
Time spent emitting light: 826
Time spent spreading light: 948
Time spent incrementing light: 367
Time spent checking receivers powered: 188


# L18
Number of DynamicObjects: 4
Total empty spots: 60
Total permutations: 11703240
Number of attempts: 1239933
Found solution!
Time spent filtering for DGO: 30
Time spent projecting light: 140
Time spent emitting light: 68
Time spent spreading light: 14
Time spent incrementing light: 6
Time spent checking receivers powered: 3


# L16
"totalTime": 32338356,
"totalPermutations": 36045979200,
"attemptPermutations": 8970751500,
"permutationRatio": 24.89