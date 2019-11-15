/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.particle;

import com.vividsolutions.jts.geom.Coordinate;
import model.topology.Capacity;
import model.topology.Position3D;

/**
 *
 * @author saemann
 */
public class Particle {

    /**
     * invisible Counter that is used to give every particle a unique ID
     */
    private static int counterID = 0;

    /**
     * An Id for every particle to track this one easily.
     */
    protected final int id = counterID++;

    /**
     * Material of this particle with more information about physical attributes
     */
    protected Material material;

//    public float mass;
    /**
     * The Volumen, that contains this particle.
     */
    protected Capacity surrounding_actual;

    /**
     * The Volumen, that contains this particle.
     */
//    protected Capacity surrounding_past;
    /**
     * Each particle represents a volume.
     */
//    public static double massPerParticle = 1;
    /**
     * Mass of this one particle [kg];
     */
    public float particleMass = 0;//(float) massPerParticle;
    /**
     * The position in 3D (surface,soil) space
     */
    protected Coordinate position3d = new Coordinate(0, 0, 0);

//    /**
//     * The position above sea level.
//     */
//    protected float height;
    /**
     * Position in a capacity, especially a pipe, along the axis
     */
    protected float position1d_actual;
//    /**
//     * Position in 2d space
//     */
//    protected Position3D position2d_actual;
    /**
     * Position in a capacity, especially a pipe, along the axis
     */
//    protected double position1d_past;
    /**
     * Velocity [m/s] along the pipe-axis.
     */
    protected float velocity1d;

    /**
     * Length of movement along the travelpath. negative movement summed up as
     * negative.
     */
    protected float moveLengthCummulative = 0;

    /**
     * Length of movement along the path. Sum of absolute stepsize. Negative
     * movement added as positive.
     */
    protected float moveLengthAbsolute = 0;

    /**
     * Status to determine the domain of the particle. -10:leftSimulation,
     * -1:waiting, 0:inactive; 10: pipenetwork 20:surface; 30:underground.
     */
    public byte status = -1;   //-10 left simulation, -1 waiting,0=inactive, 10=pipenetwork , 20=surface, 30=Underground, 

    public boolean deposited = false;

    /**
     * time when the particle is set to active.
     */
    protected long injectionTime = 0;

    /**
     * Pipe/Manhole/SurfaceTriangle this particle is injected at spilltime.
     */
    public final Capacity injectionSurrounding;

    public final float injectionPosition1D;

    public Capacity toSurface, toPipenetwork, toSoil;

    /**
     * Position along travellength, when the particle was spilled out.
     */
    public float posToSurface = 0;

    /**
     * When the particle was spilled to the surface.
     */
    public long toSurfaceTimestamp;

    /**
     * ID of the surface triangle/element the particle is contained in.
     */
    public int surfaceCellID = -1;

    protected int injectionCellID = -1;

    /**
     * If particle stays on this cell, do not count it again.
     */
    public int lastSurfaceCellID = -1;

//    public final ArrayList<Shortcut> usedShortcuts=new ArrayList<>(0);
//    public float ds=0;
    public Particle(Capacity injectionSurrounding, double injectionPosition1D) {
        this.injectionSurrounding = injectionSurrounding;
        this.injectionPosition1D = (float) injectionPosition1D;
    }

    public Particle(Capacity injectionSurrounding, double injectionPosition1D, long injectionTime) {
        this(injectionSurrounding, injectionPosition1D);
        this.injectionTime = injectionTime;
    }

    public Particle(Capacity injectionSurrounding, double injectionPosition1D, long injectionTime, float mass_kg) {
        this(injectionSurrounding, injectionPosition1D, injectionTime);
        this.particleMass = mass_kg;
    }

    public boolean isInactive() {
        return status < 1;
    }

    public boolean hasLeftSimulation(){
        return status==-10;
    }

    /**
     * Is waiting for release into the simulation
     *
     * @return
     */
    public boolean isWaiting() {
        return status == -1;
    }

    public boolean isActive() {
        return status > 0;
    }

    public boolean isInPipeNetwork() {
        return status == 10;
    }

    public boolean isOnSurface() {
        return status == 20;
    }

    public boolean isInSoil() {
        return status == 30;
    }

    public void setInactive() {
        this.status = 0;
    }

    public void setInPipenetwork() {
        this.status = 10;
    }

    public void setOnSurface() {
        this.status = 20;
    }

    public void setInSoil() {
        this.status = 30;
    }

    public void setWaiting() {
        this.status = -1;
    }

    public void setLeftSimulation(){
        this.status=-10;
    }

    public float getPosition1d_actual() {
        return position1d_actual; //position3d.x;//
    }

    /**
     * Set Mass of this particle in kg.
     *
     * @param particleMass
     */
    public void setParticleMass(float particleMass) {
        this.particleMass = particleMass;
    }

    /**
     * Return mass of this particle in kg.
     *
     * @return
     */
    public float getParticleMass() {
        return particleMass;
    }

    /**
     * Get the position 1D along the surrounding_actual from the past timestep.
     *
     * @return
     */
//    public double getPosition1d_past() {
//        return position1d_past;
//    }
    public double getVelocity1d() {
        return velocity1d;
    }

    public void setPosition1d_actual(double position1d) {
        this.position1d_actual = (float) position1d;
//        this.position3d.x = position1d;
    }

//    public void setPosition1d_past(double position1d) {
//        this.position1d_past = position1d;
//    }
    public Capacity getSurrounding_actual() {
        return surrounding_actual;
    }

    public void setSurrounding_actual(Capacity surrounding) {
//        System.out.println("Particle: Set surrounding_actual to "+surrounding);
        this.surrounding_actual = surrounding;
    }

//    public Capacity getSurrounding_past() {
//        return surrounding_past;
//    }
//    public void setSurrounding_past(Capacity surrounding) {
//        this.surrounding_past = surrounding;
//    }
    public void setPosition3d(Position3D position2d) {
        if (position2d == null) {
            return;
        }
        this.position3d = position2d.get3DCoordinate();
    }

    public void setPosition3d(Coordinate position2d) {
        this.position3d = position2d;
    }

    public Coordinate getPosition3d() {
        return position3d;
    }

    public void setVelocity1d(double velocity1d) {
        if (velocity1d > 30) {
            try {
                throw new IllegalArgumentException("Velocity of " + velocity1d + " m/s is too much, I think");
            } catch (IllegalArgumentException illegalArgumentException) {
            }
        }
        this.velocity1d = (float) velocity1d;
    }

//    public double getLatitude() {
//        if (position3d == null) {
//            return 0;
//        }
//        return this.position3d.getLatitude();
//    }
//
//    public double getLongitude() {
//        if (position3d == null) {
//            return 0;
//        }
//        return this.position3d.getLongitude();
//    }
    public int getId() {
        return id;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    @Override
    public String toString() {
        return "Particle[" + this.id + "]";
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Particle other = (Particle) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    public double getTravelledPathLength() {
        return moveLengthCummulative;
    }

//    public void setTravelledPathLength(double travelledPathLength) {
//        this.moveLengthCummulative = (float) travelledPathLength;
//    }
//
//    public void addTravelledPathLength(double ds) {
//        this.moveLengthCummulative += ds;
//    }
    public void addMovingLength(double ds) {
        this.moveLengthAbsolute += Math.abs(ds);
        this.moveLengthCummulative += (ds);
    }

    /**
     * Size of particle movement (advective + dispersive) since start.
     *
     * @return
     */
    public float getMoveLength() {
        return moveLengthAbsolute;
    }

//    public long getActivationTime() {
//        return activationTime;
//    }
//
//    public void setActivationTime(long activationTime) {
//        this.activationTime = activationTime;
//    }
//    public boolean isActive() {
//        return status;
//    }
//
//    public void setActive(boolean status) {
//        this.status = status;
//    }
    public void setInsertionTime(long insertionTime) {
        this.injectionTime = insertionTime;
    }

    public long getInsertionTime() {
        return injectionTime;
    }

    public Material getMaterial() {
        return material;
    }

    public boolean isDeposited() {
        return this.deposited;
    }

    public void setDeposited(boolean deposited) {
        this.deposited = deposited;
    }

    public void resetMovementLengths() {
        this.moveLengthAbsolute = 0;
        this.moveLengthCummulative = 0;
    }

    public void setPosition3D(Coordinate c) {
        if (c == null) {
            this.position3d.x = 0;
            this.position3d.y = 0;
            this.position3d.z = 0;
        }
        this.position3d = c;
    }

    public void setPosition3D(Position3D p) {
        if (p == null) {
            this.position3d = null;
        } else {
            this.setPosition3D(p.x, p.y, p.z);
        }
    }

    public void setPosition3D(double x, double y, double z) {
        this.position3d.x = x;
        this.position3d.y = y;
        this.position3d.z = z;
    }

    /**
     * Set x & y component of 3D position. [UTM Coordinates]
     *
     * @param x
     * @param y
     */
    public void setPosition3D(double x, double y) {
        this.position3d.x = x;
        this.position3d.y = y;
    }

    public int getInjectionCellID() {
        return injectionCellID;
    }

    public void setInjectionCellID(int injectionCellID) {
        this.injectionCellID = injectionCellID;
    }

}
