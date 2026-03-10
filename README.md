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

1) Install [JDK](https://learn.microsoft.com/en-us/java/openjdk/install) (JDK-25 should work)

2) Install [Gradle](https://docs.gradle.org/current/userguide/installation.html)


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
    * front Mirrors
    * back Mirrors
    * shifters

* After beating a record, saving level solution saves solving time as zero seconds
* optimize isUnblockedMirrorSpot
