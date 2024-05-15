This repository contains an exact solver for the NP-Hard Dominating set problem and one application of the solver, which is to figure out the smallest set of cities on the earth that can be selected to cover all the other cities with circles of a given radius centered on them.

To compile the program use `javac *.java`\
To run the program after compiling it use `java CityCoverage {filename} {radius}` where the radius is in kilometers and filename points to a file contains a file of semicolon-separated rows with the city's name as the first item and the city's latitude and longitude in degrees as the seventh and eighth items respectively.\
It might finish instantly, or in a few minutes, or not in your lifetime, this is quite the computationally hard problem after all.\
Once it finishes it should print the answer in the console and then make a leaflet map in `./maps/CoverageMap.html` and try to open it in your browser.