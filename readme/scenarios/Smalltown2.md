# Small town 2 Scenario

Located in directory [GULLI/Scenarios/SWMM/Smalltown2](https://github.com/rsaemann/GULLI/tree/Gulli/master/Scenarios/SWMM/SmallTown2)

### Scenario creation

This Scenario was created using SWMM 5.1 for the hydrailic part.

The drainage pipe network was created using [Anneke Schönfeld's UrbanDrain](https://github.com/DoeringA/urbandrain) model.

The drainage network is fictious and manually adapted to create some spillout of drainage water during a rainfall event for a return period of 50 years.

The topology (surface heights) are taken from raster DEM. The surface is discrtised in rectangles of 6x8 meters.
OpenStreetMap polygons are used to elevate vertices inside buildings to prevent the surface flow from routing through buildings.

The pipe flow field is dynamic with a resolution of 1 minute for a scenario duration of 180 minutes.

The surface flow is **static** here. The velocity follows the slope of the surface. The mean velocity is 0.3 m/s.

-------
# Car accident

### Spill injection
In the Scenario [Car Accident](https://github.com/rsaemann/GULLI/blob/Gulli/master/Scenarios/SWMM/SmallTown2/T50-caraccident.xml), a
point source spills 1 kg of a substance at a traffic junction in the south of the domain.

<img src=smalltown2-ca-setup.png width=300px>


### Pollution trace

The injection lasts for 20 minutes on the surface. A constant spillout is generated and transported to the north.
Particles are collected from manholes and directed in the pipe system to an outlet in the north-west.

<img src=smalltown2-ca-traces.png width=300px>

Many particles remain on the surface and find their way to gardens and backyards.


### Pollution intensity

The intensity of the contamination on the surface can be shown on a logarithmic scale.
Pipes, which transport the pollutant are marked in red. Clean pipes are colored in green.

<img src=smalltown2-ca-heatmap.png width=300px>


### Timeline analysis

About 56% of the spillout mass was catched and directed to the drainage outlet. 44% stops and infiltrates on the surface.
85% of the outlet mass arrives with the first flush within the first 20 minutes of rain. 15% is transported in the following 2 hours (these particles used slower transport on the surface).

<img src=smalltown2-ca-outlet.png width=500px>

-------

