package model.topology.profile;

/**
 *
 * @author saemann
 */
public abstract class Profile {

    /**
     * Von Karman constant = 0.41
     */
    public static final double kappa = 0.41;

    /**
     * wake strength in y-direction
     */
    public static final double PI_c = -0.1013211836;

    public abstract double getTotalArea();

    /**
     * Returns the area of the crossection, that is filled with fluid [qm].
     *
     * Durchflussfläche
     *
     * @param water_level_in_pipe The height from Profile-sole [meter]
     * @return flow Area [m²]
     */
    public abstract double getFlowArea(double water_level_in_pipe);

    /**
     * Get the wetted perimeter of the crossection at the given water level [m].
     *
     * Benetzter Umfang
     *
     * @param water_level_in_pipe The height from Profile-sole [meter]
     * @return wetted perimeter [m]
     */
    public abstract double getWettedPerimeter(double water_level_in_pipe);

    /**
     * Get the hydraulic diameter at the given water level. [m]
     *
     * @param water_level_in_pipe [m]
     * @return hydraulic diameter [m]
     */
    public abstract double getHydraulicDiameter(double water_level_in_pipe);

    /**
     * Get the hydraulic radius at the given water level. [m]
     *
     * @param water_level_in_pipe [m]
     * @return hydraulic radius [m]
     */
    public abstract double getHydraulicRadius(double water_level_in_pipe);

    /**
     * returns the width of the water surface [m].
     *
     * Breite des Freispiegels des Fluids.
     *
     * @param water_level_in_pipe [m]
     * @return Top channel width [m]
     */
    public abstract double getTopChannelWidth(double water_level_in_pipe);

    /**
     * Q with fr=3.71
     *
     * @param water_level_in_pipe from sole [m]
     * @param k rougness
     * @param hydraulicGradient I
     * @return Q [m³/s]
     */
    public double getFlow(double water_level_in_pipe, double k, double hydraulicGradient) {
        return getFlow(water_level_in_pipe, k, hydraulicGradient, 3.71);
    }

    /**
     *
     * @param water_level_in_pipe from sole
     * @param k roughness
     * @param hydraulicGradient I
     * @param fr
     * @return Q
     */
    public abstract double getFlow(double water_level_in_pipe, double k, double hydraulicGradient, double fr);

    /**
     * Returns the y-coordinate [width] of the boundary, at the given z [height]
     * position from z=0 at profile sole.
     *
     * @param z height from sole
     * @return width at given height.
     */
    public abstract double symmetricBoundaryCoordinate(double z);

    /**
     * According to Guo (2015) PI, the wake strength coefficient in z-direction
     *
     * @param water_level_in_pipe
     * @return
     */
    public double wake_strength_PI(double water_level_in_pipe) {
        return kappa * 0.5 * (1 + Math.exp(-0.5 * Math.pow(getTopChannelWidth(water_level_in_pipe) / (Math.PI * water_level_in_pipe), 3. / 2.)));
    }

    /**
     * According to Guo (2015) Computes the height from sole, where the fastest
     * streamline is located. Velocity-dip position, delta
     *
     * @param water_level_in_pipe
     * @return dip position height[m]
     */
    public double getVelocity_dip_position(double water_level_in_pipe) {
        return water_level_in_pipe / (1. + Math.exp(-(Math.pow(getTopChannelWidth(water_level_in_pipe) / (Math.PI * water_level_in_pipe), 3. / 2.))));
    }

    public double getVelocityPrimary(double waterl_level_in_pipe, double y, double z, double wall_shear_velocity_uw, double centerline_velocity_u0z) {
        return -(centerline_velocity_u0z + wall_shear_velocity_uw * (-1. / kappa * (Math.log(1 - 2 * Math.abs(y) / getTopChannelWidth(waterl_level_in_pipe)) + 1. / 3. * Math.pow(1 - (1 - 2 * Math.abs(y) / getTopChannelWidth(waterl_level_in_pipe)), 3)) + 2 * PI_c / kappa * Math.pow(Math.sin(Math.PI * Math.abs(y) / getTopChannelWidth(waterl_level_in_pipe)), 2)));
    }

    public abstract boolean isFreeflow(double water_level_in_pipe);

    public abstract double getStreamlineVelocity(double y, double z, double water_level_in_pipe, double roughnessK, double average_shear_velocity);

    public abstract double getLaminarAverageBoundaryShearStress(double water_level_in_pipe, double decline, double weight);

    public abstract double getLaminarVelocity(double ywidth, double zheight, double water_level_in_pipe, double gradH, double weight, double dynamicViscosity);

    /**
     * The uniformed Velocity has to be multiplied by weight*S_h
     * /(dynamicViscosity) to get the real Velocity in a laminar flow
     *
     * @param ywidth
     * @param zheight
     * @param water_level_in_pipe
     * @return
     */
    public abstract double getLaminarUniformVelocity(double ywidth, double zheight, double water_level_in_pipe);
    
    /**
     *
     * @param flowArea [m²]
     * @return water level from sole [m]
     */
    public abstract double getWaterLevel_byFlowArea(double flowArea);

    public abstract double getFillRate(double waterlevel);
}
