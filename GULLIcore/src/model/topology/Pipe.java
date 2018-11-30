package model.topology;

import control.GlobalParameter;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import model.particle.Particle;
import model.particle.ParticleQueue;
import model.timeline.array.ArrayTimeLineMeasurement;
import model.timeline.array.TimeLinePipe;
import model.topology.profile.CircularProfile;
import model.topology.profile.Medium;
import model.topology.profile.Profile;

/**
 *
 * @author saemann
 */
public class Pipe extends Capacity {

    /**
     * upper bound suction connection, Einfluss am oberen Ende
     */
    protected final Connection_Manhole_Pipe startConnection;

    /**
     * lower Bound outlet Connection_Manhole_Pipe, Auslass am unteren Rohrende
     */
    protected final Connection_Manhole_Pipe endConnection;

    protected float length;

    /**
     * Decline from upper to lower bound relative to pipelength (negative
     * value). Gefälle der Leitung von oberer zur unteren Öffnung relativ zur
     * Rohrlänge (normalerweise ein negativer Wert)
     */
    protected float decline;

    /**
     * A definition String
     */
    protected String name;

    /**
     * Roughness k [m]
     *
     * absoluter Rauheitsbeiwert k [m] (wird in Tabellenwerk in mm angegeben)
     */
    protected float roughness_k = 0.001f;

    /**
     * Roughness ks [m^(1/3) / s]
     *
     * absoluter Rauheitsbeiwert kst (wird in Tabellenwerk in [m^(1/3) / s]
     * angegeben);
     */
    protected float roughness_kst = 90;

    /**
     * Orientation & Order of Particles in this pipe in the last simulation step
     */
//    protected ParticleQueue particleQueuePast;
    /**
     * Orientation & Order of Particles in this pipe at the end of this sim.step
     */
    protected ParticleQueue particleQueueFuture;
    private boolean negativeflowDirection;

    /**
     * true if |decline| is less than 0.00001
     */
    private boolean isHorizontal = false;

//    private double velocity = 0;
//    private double q_flow = 0;
//    private float reynolds = 0;
//    private float fullVolume = 0;
//    /**
//     * [m] to pipe sole
//     */
//    protected double water_level=0;
    private TimeLinePipe timelineStatus;
    private ArrayTimeLineMeasurement timelineMeasurement;

//    private SimplePipeStamp actualValues;
//    private double fillRate;
//    private double fillRate;
    public Pipe(Connection_Manhole_Pipe inletConnection, Connection_Manhole_Pipe outletConnection, Profile profile) {
        super(profile);
        this.startConnection = inletConnection;
        if (inletConnection != null) {
            inletConnection.setPipe(this);
            inletConnection.setIsStartOfPipe(true);
        }
        this.endConnection = outletConnection;
        if (outletConnection != null) {
            outletConnection.setPipe(this);
            outletConnection.setIsStartOfPipe(false);
        }
//        length = (float) startConnection.positionGK.distance(endConnection.positionGK);
//        decline = (endConnection.height - startConnection.height) / length;
        if (inletConnection != null && outletConnection != null) {
            if (Math.abs(inletConnection.getHeight() - outletConnection.getHeight()) < 0.0001) {
                isHorizontal = true;
            }
        }
    }

    /**
     * Returns the Connection at the pipes local x=0 coordinate. This will not
     * change over time.
     *
     * @return
     */
    public Connection_Manhole_Pipe getStartConnection() {
        return startConnection;
    }

    /**
     * Returns the static Connection at the pipes end. This will not change over
     * time.
     *
     * @return
     */
    public Connection_Manhole_Pipe getEndConnection() {
        return endConnection;
    }

    /**
     * Returns the actual velocity/flow depending Inlet connection. e.g. the
     * pipeend-connection if the velocity is negative
     *
     * @return
     */
    public Connection_Manhole_Pipe getFlowInletConnection() {
        if (negativeflowDirection) {
            return endConnection;
        }
        return startConnection;
    }

    /**
     * Returns the actual velocity/flow depending Outlet connection. e.g. the
     * start-connection if the velocity is negative
     *
     * @return
     */
    public Connection_Manhole_Pipe getFlowOutletConnection() {
        if (negativeflowDirection) {
            return startConnection;
        }
        return endConnection;
    }

    @Override
    public Connection_Manhole_Pipe[] getConnections() {
        return new Connection_Manhole_Pipe[]{startConnection, endConnection};
    }

    public float getDecline() {
        return decline;
    }

    @Override
    public double getCapacityVolume() {
        return this.profile.getTotalArea() * length;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLength(float length) {
        this.length = length;
        this.decline = (endConnection.getHeight() - startConnection.getHeight()) / length;
//        this.fullVolume = (float) (this.profile.getTotalArea() * length);
        if (Math.abs(decline) < 0.00001) {
            isHorizontal = true;
        }
    }

    public float getLength() {
        return length;
    }

    public String getName() {
        return name;
    }

    @Override
    public double getFluidVolume() {
//        if(actualValues==null)return 0;
        try {
            return this.profile.getFlowArea(timelineStatus.getWaterlevel()) * length;
        } catch (Exception e) {
            System.out.println("Profile: " + profile);
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public double getWaterHeight() {
        return 0.5 * getFlowInletConnection().getHeight() + 0.5 * getFlowOutletConnection().getHeight() + this.getWaterlevel();
    }

    /**
     * absolute roughness in [m]
     *
     * @return
     */
    public double getRoughness_k() {
        return roughness_k;
    }

    /**
     * absolute rougness in [m] (not [mm]!)
     *
     * @param roughness_k
     */
    public void setRoughness_k(double roughness_k) {
        this.roughness_k = (float) roughness_k;
        this.roughness_kst = (float) getRoughnessKst(roughness_k);
    }

    /**
     * Set Strickler's coefficient in m^(1/3) / s
     *
     * @param kst
     */
    public void setRoughnessKst(double kst) {
        this.roughness_kst = (float) kst;
    }

    public Position3D getPositionAlongAxisAbsolute(double meter) {
        if (meter < -1) {
            throw new IllegalArgumentException("Position in Pipe is to low :" + meter);
        }
        if (meter < 0) {
            return startConnection.getPosition();
        }

        if (meter > this.length + 1) {
            throw new IllegalArgumentException("Position in pipe [" + this.getName() + "] is too far :" + meter + "m but pipe is only " + this.length + "m long.");
        }
        if (meter > this.length) {
            return endConnection.getPosition();
        }

        return this.startConnection.getPosition().getInterpolatedPosition(meter / this.length, endConnection.getPosition());
    }

    public Position getPositionAlongAxisRelative(double ratio) {
        if (ratio < 0) {
            throw new IllegalArgumentException("relative position in pipe is to low :" + ratio);
        }

        if (ratio > 1) {
            throw new IllegalArgumentException("relative position in pipe [" + this.getName() + "] is too big :" + ratio);
        }

        return this.startConnection.getPosition().getInterpolatedPosition(ratio, endConnection.getPosition());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(ID" + autoID + "/HE" + manual_ID + ")['" + this.name + "']";
    }

//    public ParticleQueue getParticleQueue() {
//        return particleQueuePast;
//    }
//
//    public void initParticleQueue() {
//        if (particleQueuePast != null) {
//            particleQueuePast = new ParticleQueue();
//        }
//    }
//
//    public boolean hasParticles() {
//        if (particleQueuePast == null) {
//            return false;
//        }
//        return !particleQueuePast.isEmpty();
//    }
    /**
     * Add a particle to the queue of the actual/futur simulation step
     *
     * @param particle
     * @return
     */
    public boolean addParticle(Particle particle) {
        if (particleQueueFuture == null) {
            particleQueueFuture = new ParticleQueue();
        }
        particleQueueFuture.insert(particle);
        return true;
    }

//    /**
//     * Removes a particle from the Past-step queue
//     *
//     * @param particle
//     * @return
//     */
//    public boolean removeParticlePast(Particle particle) {
//        if (particleQueuePast == null) {
//            return false;
//        }
//        return particleQueuePast.remove(particle);
//    }
    /**
     * Removes a particle from the actual-step queue
     *
     * @param particle
     * @return
     */
    public boolean removeParticleActual(Particle particle) {
        if (particleQueueFuture == null) {
            return false;
        }
        return particleQueueFuture.remove(particle);
    }

    @Override
    public Position3D getPosition3D(double meter) {
        return getPositionAlongAxisAbsolute(meter);
    }

    /**
     * Is the pipe decline nearly zero?
     *
     * @return
     */
    public boolean isHorizontal() {
        return isHorizontal;
    }

    public void setWaterType(SEWER_TYPE waterType) {
        this.waterType = waterType;
    }

    public void setMedium(Medium medium) {
        this.medium = medium;
//        reynolds = (float) Math.abs(timelineStatus.getVelocity(ArrayTimeLinePipe.getActualTimeIndex()) * profile.getHydraulicDiameter(waterlevel) / medium.kin_viscosity);
    }

    public void setFlowdirectionNegative(boolean negativFlow) {
        if (negativFlow = negativeflowDirection) {
            return;
        }

        this.negativeflowDirection = negativFlow;
    }

    public boolean isNegativeflowDirection() {
        return negativeflowDirection;
    }

    /**
     * Average velocity_m2p [m/s]
     *
     * @param water_level_in_pipe
     * @return
     */
    public double averageVelocity_gravity(double water_level_in_pipe) {
        if (water_level_in_pipe < 0.0001) {
            return 0;
        }
        double rh = profile.getHydraulicRadius(water_level_in_pipe);
        double lambdainv = (4. * Math.pow(Math.log10(roughness_k / (3.71 * rh)), 2));
        double velocity = Math.sqrt(8 * 9.81 * rh * Math.abs(decline) * lambdainv);
        if (Double.isNaN(velocity)) {
            if (Double.isNaN(decline)) {
                System.out.println("Decline is NaN: length=" + length);
            }
            System.out.println("Pipe::AVERAGE_hydraulicRadius velocity in pipe " + this.toString() + " is NaN , decline=" + decline + ", rh(h=" + water_level_in_pipe + ")= " + rh + " sqrt(" + (8 * 9.81 * rh * Math.abs(decline) * lambdainv));
            System.out.println("k=" + roughness_k + "   1/lambda=" + lambdainv + " log10(" + (roughness_k / (3.71 * rh)) + ")=" + (Math.log10(roughness_k / (3.71 * profile.getHydraulicRadius(water_level_in_pipe)))));
            System.out.println("log10(X)^2=" + Math.pow(Math.log10(roughness_k / (3.71 * rh)), 2));
            System.out.println("4xlog10(x)^2=" + 4. * Math.pow(Math.log10(roughness_k / (3.71 * rh)), 2) + " = " + lambdainv);
            try {
                throw new Exception("Kaputt");
            } catch (Exception ex) {
                Logger.getLogger(Pipe.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
            }
        }
        return velocity;
    }

    /**
     * velocity_m2p calculated from delta-waterlevel, without gravity
     *
     * @param dh difference between inlet and outlet water level
     * @return
     */
    public double averageVelocity_horizontal(double dh, double water_level_in_pipe) {
        if (water_level_in_pipe < 0.0001) {
            return 0;
        }
        double rh = profile.getHydraulicRadius(water_level_in_pipe);
        double lambdainv = (4. * Math.pow(Math.log10(roughness_k / (3.71 * rh)), 2));
        double velocity = Math.sqrt(8 * 9.81 * rh * Math.abs(dh) * lambdainv);
        if (Double.isNaN(velocity)) {
            if (Double.isNaN(dh)) {
                System.out.println("grad(h) is NaN: length=" + length);
            }

            System.out.println("Pipe::AVERAGE_horizontal gradH velocity in pipe " + this.toString() + " is NaN ,dh=" + dh + " decline=" + decline + ", rh(h=" + water_level_in_pipe + ")= " + rh + " sqrt(" + (8 * 9.81 * rh * Math.abs(decline) * lambdainv));
            System.out.println("k=" + roughness_k + "   1/lambda=" + lambdainv + " log10(" + (roughness_k / (3.71 * rh)) + ")=" + (Math.log10(roughness_k / (3.71 * profile.getHydraulicRadius(water_level_in_pipe)))));
            System.out.println("log10(X)^2=" + Math.pow(Math.log10(roughness_k / (3.71 * rh)), 2));
            System.out.println("4xlog10(x)^2=" + 4. * Math.pow(Math.log10(roughness_k / (3.71 * rh)), 2) + " = " + lambdainv);

        }
        return velocity;
    }

    public float averageVelocity_actual() {
//        if (isHorizontal) {
//            return startConnection.velocity_m2p;
//        }
        if (timelineStatus != null) {
            return timelineStatus.getVelocity();
        }
        return 0;
//        return velocity;
//        return averageVelocity_gravity(getWaterHeight());
    }

    public double getLambdaInverted(double reynoldsnumber, double water_level_in_pipe) {
        if (reynoldsnumber < 2330) {
            return reynoldsnumber / 64.;
        }
        return 4 * Math.pow(Math.log10(this.getRoughnessKst(roughness_k, water_level_in_pipe) / profile.getHydraulicDiameter(water_level_in_pipe) * 0.269542 + 5.72 * Math.pow(reynoldsnumber, 0.9)), 2);
    }

    public double getAverageShearVelocity(double water_level_in_pipe) {
        double hydraulicRadius = profile.getHydraulicRadius(water_level_in_pipe);
        return Math.sqrt(Math.abs(GlobalParameter.GRAVITY * hydraulicRadius * this.getDecline()));
    }

    public double getFlow(double water_level_in_pipe) {
        if (water_level_in_pipe <= 0) {
            return 0;
        }
        if (profile.isFreeflow(water_level_in_pipe)) {
            //partial-filled-pipe / Gerinneströmung
            double kst = getRoughnessKst(roughness_k);//, profile.getHydraulicRadius(water_level_in_pipe));
//              System.out.println("Kst: " + kst + "   <- k: " + roughness_k * 1000 + "mm    rh=" + profile.getHydraulicRadius(water_level_in_pipe) + "m  hydraulic diameter: " + profile.getHydraulicDiameter(water_level_in_pipe));
            double velocity = kst * Math.pow(profile.getHydraulicRadius(water_level_in_pipe), 2. / 3.) * Math.sqrt(Math.abs(decline));
            //   System.out.println("h: " + water_level_in_pipe + "m  velocity_m2p: " + velocity_m2p + " m/s,    Steigung I: " + decline + " A: " + profile.getFlowArea(water_level_in_pipe) + "m²");
            return velocity * profile.getFlowArea(water_level_in_pipe);
            //FGDL Merkblatt 2003 Freistaat Sachsen
//            double dh = profile.getHydraulicDiameter(water_level_in_pipe);
//            double ls = Math.abs(decline);
//            double v = -2 * Math.log10((2.51 * medium.kin_viscosity) / Math.sqrt(2 * GlobalParameter.GRAVITY * dh/**
//                     * dh*dh
//                     */
//                    * ls) + roughness_k / (3.71 * dh)) * Math.sqrt(2 * GlobalParameter.GRAVITY * dh * ls);
//            double Q = v * profile.getFlowArea(water_level_in_pipe);
//            return Q;
        } else {
            //full-pipe-flow_m2p
            double kst = getRoughnessKst(roughness_k);//, profile.getHydraulicRadius(water_level_in_pipe));

            double velocity = kst * Math.pow(profile.getHydraulicRadius(water_level_in_pipe), 2. / 3.) * Math.sqrt(Math.abs(decline));
            //System.out.println("Kst: " + kst + "   <- k: " + roughness_k * 1000 + "mm    rh=" + (profile.getHydraulicRadius(water_level_in_pipe)));

            // System.out.println("h: " + water_level_in_pipe + "m  velocity_m2p: " + velocity_m2p + " m/s,    Steigung I: " + decline + " A: " + profile.getFlowArea(water_level_in_pipe) + "m²");
            return velocity * profile.getFlowArea(water_level_in_pipe);
        }
    }

    public double getFlow_ManningStrickler(double h) {
        double kst = getRoughnessKst(roughness_k);
        double v = kst * Math.pow(profile.getHydraulicRadius(h), 2. / 3.) * Math.sqrt(Math.abs(decline));
        return profile.getFlowArea(h) * v;
    }

    public double getFlow_Prandtl_Colebrook(double h) {
        double dh = profile.getHydraulicDiameter(h);
        double g2 = 2. * GlobalParameter.GRAVITY;
        double i = Math.abs(decline);
        double q = -2 * Math.log10(2.51 * medium.kin_viscosity / Math.sqrt(g2 * i * dh) + roughness_k / (3.71 * dh)) * Math.sqrt(g2 * i * dh);
        return profile.getFlowArea(h) * q;
    }

    public double getFlow_FGDL2003Freispiegel(double h) {
        double dh = profile.getHydraulicDiameter(h);
        double ls = Math.abs(decline);
        double lambda = 1. / Math.sqrt(-2 * Math.log10(dh));
        double v = -2 * Math.log10((2.51 * medium.kin_viscosity) / Math.sqrt(2 * GlobalParameter.GRAVITY * dh * ls) + roughness_k / (3.71 * dh)) * Math.sqrt(2 * GlobalParameter.GRAVITY * dh * ls);
        double Q = v * profile.getFlowArea(h);
        return Q;
    }

    /**
     * Get the Strickler's coefficient from a rougness k [m]! for smooth walls.
     *
     * @param rougnessK in [m!]
     * @return k_st [m^(1/3) /s]
     */
    public double getRoughnessKst(double rougnessK) {
        return 26. / (Math.pow(rougnessK, 1. / 6.));

    }

    /**
     * Get the Strickler's coefficient from a rougness k [m].
     *
     * @param rougnessK in [m!]
     * @param hydraulicRadius hydraulic radius [m]
     * @return k_st [m^(1/3) /s]
     */
    public double getRoughnessKst(double rougnessK, double hydraulicRadius) {
        if (hydraulicRadius <= 0) {
            return getRoughnessKst(rougnessK);
        }
        return 17.72 / Math.pow(hydraulicRadius, 0.16666667) * Math.log10(14.84 * hydraulicRadius / (rougnessK));

    }

    public double getRoughnessK(double kst) {
        return 1. / Math.pow(kst / 26., 6);

    }

    public double getReynoldsNumber(double water_level_in_pipe) {
        if (water_level_in_pipe <= 0) {
            return 0;
        }
        return averageVelocity_gravity(water_level_in_pipe) * profile.getHydraulicDiameter(water_level_in_pipe) / medium.kin_viscosity;
    }

    public double getShearstress(double water_level_in_pipe) {
        double flow = this.getFlow(water_level_in_pipe);
        return medium.getDensity() * flow * flow / (getLambdaInverted(getReynoldsNumber(water_level_in_pipe), water_level_in_pipe) * profile.getHydraulicDiameter(water_level_in_pipe) * 2 * profile.getFlowArea(water_level_in_pipe));
    }

    public double getLaminarBoundaryShearStress(double water_level_in_pipe) {
        double tau = profile.getLaminarAverageBoundaryShearStress(water_level_in_pipe, decline, medium.getWeight());
        return tau;
    }

    public double getLaminarVelocity(double ywidth, double zheight, double water_level_in_pipe) {
        if (zheight > water_level_in_pipe) {
            return 0;
        }
        double u1 = this.profile.getLaminarVelocity(ywidth, zheight, water_level_in_pipe, decline, medium.getWeight(), medium.dyn_viscosity);
        double u2 = this.profile.getLaminarUniformVelocity(ywidth, zheight, water_level_in_pipe);
        double u2Factor = medium.getWeight() * Math.abs(decline) * 0.25 / medium.dyn_viscosity;
        System.out.println("u1 : " + u1);
        System.out.println("u2 : " + u2 * u2Factor + "  by " + u2 + " x " + u2Factor);
        return u2 * u2Factor;
    }

    /**
     * // * @deprecated Noch nicht fertig Iteration according to Guo 2015
     *
     * @param water_level_in_pipe_guess [m] start value for iteration
     * @param flow [m³/s]
     * @return [water_level,velocity_m2p]
     */
    public double[] iterateLevelAndVelocity(double water_level_in_pipe_guess, double flow) {

//        throw new UnsupportedOperationException("Not yet implemented");
        double height = water_level_in_pipe_guess;
        double frictionSlope = Math.abs(this.decline); //Sf
        double heightnew;
        double area = profile.getFlowArea(height);//A
        if (area / profile.getTotalArea() < 0.001) {
            return new double[]{0, 0};
        }
        double perimeter;//P
        double hydraulicDiameter;//Dh
        double velocity = 0;// V
        double reynolds; //R
        double finvert;//=1/f
        for (int i = 0; i < 5; i++) {
            perimeter = profile.getWettedPerimeter(height);
            //
            hydraulicDiameter = 4 * area / perimeter;
            velocity = flow / area;
            //
            reynolds = velocity * hydraulicDiameter / medium.kin_viscosity;
            //
            finvert = 4 * Math.pow(Math.log10(roughness_k / hydraulicDiameter * 0.2702703 + 5.72 * Math.pow(reynolds, 0.9)), 2);
            //
            velocity = Math.sqrt(19.62 * hydraulicDiameter * frictionSlope * finvert);
            //
            area = flow / velocity;
            //
            heightnew = profile.getWaterLevel_byFlowArea(area);

            double deltaH = Math.abs(heightnew - height);
            System.out.println("iteration " + i + ":  h=" + heightnew + "\tdh=" + deltaH + "\tarea:" + area);
            height = heightnew;
        }
        return new double[]{height, velocity};
    }

//    public double[] getParticleorientedSpaceForParticle(Particle p) {
//        if (p.getSurrounding_actual() == this) {
//            return particleQueuePast.getParticleorientedSpaceForParticle(p);
//        }
//        throw new IllegalArgumentException("Particle is not in Pipe");
//    }
//
//    public double[] getPipeorientedSpaceForParticle(Particle p) {
//        if (p.getSurrounding_actual() == this) {
//            return particleQueuePast.getPipeorientedSpaceForParticle(p);
//        }
//        throw new IllegalArgumentException("Particle is not in Pipe");
//    }
//    public static void main1(String[] args) {
//        JFrame frame = new JFrame("Testrohr ");
//        JFrame frameIteration = new JFrame("Iteriert Testrohr ");
//        JFrame frameRe = new JFrame("Reynoldsnumber");
//        JFrame frameDip = new JFrame("Velocity Dip position");
//        JFrame frameV = new JFrame("Velocity Profile");
//        JFrame frameQ = new JFrame("Flow Profile");
//        JFrame frameP = new JFrame("Velocity Profile Turbulent");
//        JFrame framePLaminar = new JFrame("Velocity Profile Laminar");
//        JFrame framePHsu = new JFrame("Velocity Profile Hsu&Chiu");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setBounds(100, 100, 400, 300);
//        frameRe.setBounds(500, 100, 400, 300);
//        frameDip.setBounds(900, 100, 400, 300);
//        frameIteration.setBounds(1300, 100, 400, 300);
//        frameV.setBounds(100, 400, 400, 300);
//        frameQ.setBounds(500, 400, 400, 300);
//        frameP.setBounds(900, 400, 300, 300);
//        framePLaminar.setBounds(1200, 400, 300, 300);
//        framePHsu.setBounds(1500, 400, 300, 300);
//        XYSeriesCollection dataset = new XYSeriesCollection();
//        XYSeriesCollection datasetRe = new XYSeriesCollection();
//        XYSeriesCollection datasetDip = new XYSeriesCollection();
//        XYSeriesCollection datasetV = new XYSeriesCollection();
//        XYSeriesCollection datasetQ = new XYSeriesCollection();
//        XYSeriesCollection datasetIteration = new XYSeriesCollection();
//
//        XYSeries seriesV = new XYSeries("V/V_f");
//        XYSeries seriesQ = new XYSeries("Q/Q_f");
//        XYSeries seriesA = new XYSeries("A_h/A_f");
//        XYSeries seriesR = new XYSeries("R_h/R_f");
//
//        XYSeries seriesQ_strickler = new XYSeries("Q Strickler");
//        XYSeries seriesQ_prandtl_colebrook = new XYSeries("Q Prandtl-Colebrook");
//        XYSeries seriesQ_FGDL = new XYSeries("Q FGDL/Prandtl");
//
//        XYSeries seriesIterationQ = new XYSeries("Q");
//        XYSeries seriesIterationV = new XYSeries("V");
//        XYSeries seriesIterationh = new XYSeries("h");
//
//        XYSeries seriesRe = new XYSeries("Re");
//
//        XYSeries seriesDip = new XYSeries("Dip");
//        XYSeries seriesVP20 = new XYSeries("V profile 20%");
//        XYSeries seriesVP50 = new XYSeries("V profile 50%");
//        XYSeries seriesVP70 = new XYSeries("V profile 70%");
//        JFreeChart chart = ChartFactory.createXYLineChart("Relative values @DN 1000", "h [m]", "", dataset, PlotOrientation.HORIZONTAL, true, true, false);
//        JFreeChart chartRe = ChartFactory.createXYLineChart("Reynolds @DN 1000", "h [m]", "Re", datasetRe, PlotOrientation.VERTICAL, true, true, false);
//        JFreeChart chartDip = ChartFactory.createXYLineChart("Dip position @DN 1000", "h [m]", "Dip @z [m]", datasetDip, PlotOrientation.VERTICAL, true, true, false);
//        JFreeChart chartV = ChartFactory.createXYLineChart("V profile @DN 1000", "h [m]", "V [m/s]", datasetV, PlotOrientation.HORIZONTAL, true, true, false);
//        JFreeChart chartQ = ChartFactory.createXYLineChart("Q profile @DN 1000", "h [m]", "Q [m³/s]", datasetQ, PlotOrientation.HORIZONTAL, true, true, false);
//        JFreeChart chartIteration = ChartFactory.createXYLineChart("Iterated", "h [m]", "", datasetIteration, PlotOrientation.HORIZONTAL, true, true, false);
//
//        ChartPanel panel = new ChartPanel(chart);
//        frame.add(panel);
//        frame.setVisible(true);
//        frameRe.add(new ChartPanel(chartRe));
//        frameRe.setVisible(true);
//        frameDip.add(new ChartPanel(chartDip));
//        frameDip.setVisible(true);
//        frameV.add(new ChartPanel(chartV));
//        frameV.setVisible(true);
//        frameQ.add(new ChartPanel(chartQ));
//        frameQ.setVisible(true);
//        frameP.setVisible(true);
//        framePLaminar.setVisible(true);
//        framePHsu.setVisible(true);
//        frameIteration.add(new ChartPanel(chartIteration));
//        frameIteration.setVisible(true);
//
//        chart.getPlot().setBackgroundPaint(Color.white);
//        XYPlot plot = (XYPlot) chart.getPlot();
//        plot.setDomainGridlinePaint(Color.lightGray);
//        plot.setRangeGridlinePaint(Color.lightGray);
//
//        chartRe.getPlot().setBackgroundPaint(Color.white);
//        plot = (XYPlot) chartRe.getPlot();
//        plot.setDomainGridlinePaint(Color.lightGray);
//        plot.setRangeGridlinePaint(Color.lightGray);
//
//        plot = (XYPlot) chartDip.getPlot();
//        plot.setBackgroundPaint(Color.white);
//        plot.setDomainGridlinePaint(Color.lightGray);
//        plot.setRangeGridlinePaint(Color.lightGray);
//
//        plot = (XYPlot) chartV.getPlot();
//        plot.setBackgroundPaint(Color.white);
//        plot.setDomainGridlinePaint(Color.lightGray);
//        plot.setRangeGridlinePaint(Color.lightGray);
//
//        plot = (XYPlot) chartQ.getPlot();
//        plot.setBackgroundPaint(Color.white);
//        plot.setDomainGridlinePaint(Color.lightGray);
//        plot.setRangeGridlinePaint(Color.lightGray);
//
//        plot = (XYPlot) chartIteration.getPlot();
//        plot.setBackgroundPaint(Color.white);
//        plot.setDomainGridlinePaint(Color.lightGray);
//        plot.setRangeGridlinePaint(Color.lightGray);
//
//        Connection_Manhole_Pipe in = new Connection_Manhole_Pipe(new Position(0, 0, 0, 0), 50f);
//        Connection_Manhole_Pipe out = new Connection_Manhole_Pipe(new Position(0.1, 0, 1, 0), 47.5f);
//        double r = 1;
//        CircularProfile profile15 = new CircularProfile(r);
//
//        Pipe p = new Pipe(in, out, profile15);
//        p.setLength(100);
//        p.setRoughness_k(0.0005);
//        System.out.println("Pipe roughness: k=" + p.getRoughness_k() + "  , kst=" + p.getRoughnessKst(p.getRoughness_k()));
//        p.setMedium(Medium.WATER10);
//        dataset.addSeries(seriesV);
//        dataset.addSeries(seriesQ);
//        dataset.addSeries(seriesA);
//        dataset.addSeries(seriesR);
//        datasetRe.addSeries(seriesRe);
//        datasetDip.addSeries(seriesDip);
//        datasetV.addSeries(seriesVP20);
//        datasetV.addSeries(seriesVP50);
//        datasetV.addSeries(seriesVP70);
//
//        datasetQ.addSeries(seriesQ_FGDL);
//        datasetQ.addSeries(seriesQ_prandtl_colebrook);
//        datasetQ.addSeries(seriesQ_strickler);
//
//        datasetIteration.addSeries(seriesIterationV);
//        datasetIteration.addSeries(seriesIterationQ);
//        datasetIteration.addSeries(seriesIterationh);
//
//        double Qfull = p.getFlow(r);
//        double Vfull = p.averageVelocity_gravity(r);
//        double Ahfull = p.getProfile().getFlowArea(r);
//        double RhFull = p.getProfile().getHydraulicRadius(r);
//
//        for (int i = 0; i < 1000; i++) {
//            double h = r * (i / 1000.);
//            seriesQ.add(h, p.getFlow(h) /*/ Qfull*/);
//            seriesV.add(h, p.averageVelocity_gravity(h) /*/ Vfull*/);
//            seriesR.add(h, p.getProfile().getHydraulicRadius(h)/* / RhFull*/);
//            seriesA.add(h, p.getProfile().getFlowArea(h) /*/ Ahfull*/);
//            System.out.println("h:" + h + "m -> Re: " + p.getReynoldsNumber(h));
//            seriesRe.add(h, p.getReynoldsNumber(h));
//            seriesDip.add(h, p.getProfile().getVelocity_dip_position(h));
//            seriesVP20.add(h, p.getProfile().getStreamlineVelocity(0, h, 0.2, p.roughness_k, 0.0368));
//            seriesVP50.add(h, p.getProfile().getStreamlineVelocity(0, h, 0.5, p.roughness_k, 0.0368));
//            seriesVP70.add(h, p.getProfile().getStreamlineVelocity(0, h, 0.7, p.roughness_k, 0.0368));
//            seriesQ_FGDL.add(h, p.getFlow_FGDL2003Freispiegel(h));
//            seriesQ_prandtl_colebrook.add(h, p.getFlow_Prandtl_Colebrook(h));
//            seriesQ_strickler.add(h, p.getFlow_ManningStrickler(h));
//            double Q = p.getFlow(h);
//            double[] hv = p.iterateLevelAndVelocity(0.5, Q);
//            seriesIterationh.add(h, hv[0]);
//            seriesIterationV.add(h, hv[1]);
//            seriesIterationQ.add(h, Q);
//        }
//        double waterlevel = 0.7;
//        final double[][] velocities = new double[100][100];
//        final double[][] velocitiesLaminar = new double[velocities.length][velocities[0].length];
//        final double[][] velocitiesHsu = new double[velocities.length][velocities[0].length];
//        double maxVelocity = 0;
//        double maxVelocityLaminar = 0;
//        double maxVelocityHsu = 1;
//        double dippos = p.getProfile().getVelocity_dip_position(waterlevel);
//        double dipdeep = waterlevel - dippos;
//        System.out.println("Dip_height: " + dippos + "m from sole.  -> " + dipdeep + " m below watersurface (" + waterlevel + "m).");
//        double ximax = 1;
//        for (int z = 0; z < velocities[0].length; z++) {
//            double zz = z * (1. / (double) velocities.length);
//            double ybreite = p.getProfile().symmetricBoundaryCoordinate(zz);
//            for (int y = 0; y < velocities.length; y++) {
//                double yy = y * (1. / (double) velocities.length);
//                boolean inPipe = p.getProfile().symmetricBoundaryCoordinate(zz) > yy;
//                if (inPipe) {
//                    velocities[y][z] = p.getProfile().getStreamlineVelocity(yy, zz, waterlevel, p.roughness_k, 0.0368);
//                    velocitiesLaminar[y][z] = p.profile.getLaminarUniformVelocity(yy, zz, waterlevel);//p.getLaminarVelocity(yy, zz, waterlevel);
////                System.out.println("set value at [" + yy + "," + zz + "] to " + velocities[y][z]);
//                    if (!Double.isNaN(velocities[y][z])) {
//                        maxVelocity = Math.max(maxVelocity, velocities[y][z]);
//                    }
//                    if (!Double.isNaN(velocitiesLaminar[y][z])) {
//                        maxVelocityLaminar = Math.max(maxVelocityLaminar, velocitiesLaminar[y][z]);
//                    }
//
//                    //Velocity distribution by Hsu &chiu
//                    double Y = zz / (waterlevel - dipdeep);
//                    double Z = yy / ybreite;
//                    double xi = Y * (1 - Z) * Math.exp(1 - Y + Z);
//                    velocitiesHsu[y][z] = xi;
//
//                } else {
//                    velocities[y][z] = Double.NaN;
//                    velocitiesLaminar[y][z] = Double.NaN;
//                    velocitiesHsu[y][z] = Double.NaN;
//                }
//            }
//        }
//        final float v = (float) maxVelocity;
//        System.out.println("MaxV= " + v);
//        final ProfilePanel panelProfile = new ProfilePanel(velocities, v);
//
//        final DecimalFormat df = new DecimalFormat("0.##");
//        frameP.add(panelProfile);
//        frameP.revalidate();
//
//        //Frame Laminarvelocity
//        final float vLaminar = (float) maxVelocityLaminar;
//        System.out.println("MaxVLaminar= " + vLaminar);
//        final ProfilePanel panelProfileLaminar = new ProfilePanel(velocitiesLaminar, vLaminar);
//        framePLaminar.add(panelProfileLaminar);
//        framePLaminar.revalidate();
//        //Frame Hsu & Chiu velocity_m2p
//        System.out.println("Xi Hsu= " + maxVelocityHsu);
//        final ProfilePanel panelProfileHsu = new ProfilePanel(velocitiesHsu, maxVelocityHsu);
//        panelProfileHsu.setUnit("");
//        framePHsu.add(panelProfileHsu);
//        framePHsu.revalidate();
//
////        double k = 0.0006;
////        double k2 = p.getRoughnessK(p.getRoughnessKst(k));
////        double kst = p.getRoughnessKst(k);
//        System.out.println("K: " + p.getRoughness_k() + "m\t-> kst: " + p.getRoughnessKst(p.getRoughness_k()));
//        System.out.println("I_s=" + p.getDecline());
//
//    }

    /**
     *
     * @return
     */
    public TimeLinePipe getStatusTimeLine() {
        return this.timelineStatus;
    }

    public void setStatusTimeLine(TimeLinePipe tl) {
        this.timelineStatus = tl;
    }

    public double getVelocity() {

        return averageVelocity_actual();
//        return velocity;
    }

//    public void setVelocity(double velocity) {
////        this.velocity = velocity;
////        double d = medium.kin_viscosity;
////        reynolds = Math.abs(velocity * profile.getHydraulicDiameter(waterlevel) / medium.kin_viscosity);
////        this.actualValues.setReynolds(reynolds);
//    }

//    public double getQ_flow() {
//        return getVelocity() * profile.getFlowArea(timelineStatus.getWaterlevel());
////        return q_flow;
//    }

//    public void setQ_flow(double q_flow) {
//        this.q_flow = q_flow;
//    }
    public double getReynoldsNumber_Actual() {
        return Math.abs(getVelocity() * profile.getHydraulicDiameter(getWaterlevel()) / medium.kin_viscosity);
    }

    public double getFillRate() {
//        if (this.actualValues != null) {
        if(this.timelineStatus==null)return -0;
        return this.profile.getFillRate(getWaterlevel());
//        }
//        return fillRate;
    }

    public void setFillRate(double fillRate) {
//        this.fillRate = fillRate;
    }

    @Override
    public void setMeasurementTimeLine(ArrayTimeLineMeasurement tl) {
        this.timelineMeasurement = tl;
    }

    @Override
    public ArrayTimeLineMeasurement getMeasurementTimeLine() {
        return this.timelineMeasurement;
    }

    @Override
    public double getWaterlevel() {
        if(this.timelineStatus==null)return Double.NEGATIVE_INFINITY;
        return this.timelineStatus.getWaterlevel();
    }

    /**
     * Flow for the actual time.
     *
     * @return q [m³/s]
     */
    public double getFlowActual() {
        if(this.timelineStatus==null)return 0;
        return this.timelineStatus.getFlux();
    }

    public ArrayList<Position3D> getGeometry() {
        ArrayList<Position3D> list = new ArrayList<>(2);
        list.add(startConnection.getPosition());
        list.add(endConnection.getPosition());
        return list;
    }
}
