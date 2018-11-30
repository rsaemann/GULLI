package model.topology.profile;

import model.particle.Material;

/**
 *
 * @author saemann
 */
public class Medium extends Material {

    /**
     * dynamic viscosity [kg/(m*s)]=[N*s/m²], eta ~0,001
     */
    public final double dyn_viscosity;

    /**
     * kinematic viscosity [m²/s], nue ~E-6
     */
    public final double kin_viscosity;

    public Medium(String name, double density, double dyn_viscosity, double kin_viscosity) {
        super(name, density,true);
        this.dyn_viscosity = dyn_viscosity;
        this.kin_viscosity = kin_viscosity;
    }

    public Medium(String name, double density, double dyn_viscosity) {
        this(name, density, dyn_viscosity, dyn_viscosity / density);
    }

    public static Medium WATER10 = new Medium("Water 10°C", 999.7, 0.001307, 0.00000130744);

    public static Medium WATER20 = new Medium("Water 20°C", 998.2, 0.00100219, 0.000001307);

//    public static Medium WATER04=new Medium("Water", 998.2, 0.0100219,0.00001004);
    public static Medium SEWER10 = new Medium("Wastewater 10°C", 999.6, 0.001296, 0.000001297);

}
