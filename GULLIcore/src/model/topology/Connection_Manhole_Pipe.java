package model.topology;

/**
 * A Connection_Manhole_Pipe connects two Capacitys.
 *
 * Eine Connection_Manhole_Pipe verbindet zwei Capacitys. Sie definiert die
 * Position der Verbindung sowohl im Raum als auch in der Höhe (2,5 D)
 *
 * @author saemann
 */
public class Connection_Manhole_Pipe implements Connection_Manhole {

    protected Position3D position;

    /**
     * sole-height above sealevel.
     *
     * Sohlhöhe über Meeresspiegel.
     */
//    protected float height;
    /**
     * Capacitys which are connected by this object. If the flow_m2p direction
     * is known the connection should be organized to let the flow_m2p go from
     * the incoming to outgoing volume.
     *
     * Die Capacitys, welche von dieser Connection verbunden werden. Wenn die
     * Fließrichtung bekannt ist, sollten sie so gewält werden, dass das Medium
     * vom incoming durch die Connection zur outgoing strömt.
     */
    protected StorageVolume manhole;

    protected Pipe pipe;

    protected boolean isStartOfPipe = false;

    /**
     * velocity_m2p [m/s] from manhole to outgoing capacity
     */
//    public double velocity_m2p = 0;
    /**
     * FLUIDFLOW [m^3/s] from manhole to outgoing capacity
     */
//    public double flow_m2p = 0;
    public String name = "";

//    /**
//     * [m] from sole.
//     */
//    public double water_level_in_connection = 0;
    public Connection_Manhole_Pipe(Position position, float height) {
        this.position = new Position3D(position, height);
//        this.height = height;
    }

//    public Connection_Manhole_Pipe(Position position, float height) {
//        this.position = position;
//        this.height = height;
//    }
    public Connection_Manhole_Pipe(Manhole manhole, float height) {
        this(manhole.getPosition(), height);
        this.manhole = manhole;
        manhole.addConnection(this);
    }

    public float getHeight() {
//        return height;
        return (float) position.getZ();
    }

    public Position3D getPosition() {
        return position;
    }

    public StorageVolume getManhole() {
        return manhole;
    }

    public void setManhole(StorageVolume manhole) {
        this.manhole = manhole;
    }

    public Pipe getPipe() {
        return pipe;
    }

    public void setPipe(Pipe pipe) {
        if (this.pipe != null && pipe != null) {
            if (!this.pipe.equals(pipe)) {
                System.err.println(this.getClass().getName() + ": replaced the connected Pipe '" + this.pipe + "' by '" + pipe + "'.");
            }
        }
        this.pipe = pipe;
//        System.out.println(this.getClass()+"::setPipe ("+pipe+").startconnection?"+(pipe.getStartConnection()==this)+"");
        if (pipe != null) {
            isStartOfPipe = pipe.getStartConnection() == this;
        } else {
            isStartOfPipe = false;
        }
    }

    public void setIsStartOfPipe(boolean isStartOfPipe) {
        this.isStartOfPipe = isStartOfPipe;
    }

    public boolean isStartOfPipe() {
        return isStartOfPipe;
    }

    public boolean isEndOfPipe() {
        return !isStartOfPipe;
    }

    public boolean isFlowInletToPipe() {
        if (pipe == null) {
            return false;
        }
        if (isStartOfPipe) {
            if (pipe.getVelocity() > 0) {
                return true;
            }
            return false;
        } else if (pipe.getVelocity() < 0) {
            return true;
        }
        return false;
    }

    public boolean isFlowOutletFromPipe() {
        if (pipe == null) {
            return false;
        }
        if (isStartOfPipe) {
            if (pipe.getVelocity() < 0) {
                return true;
            }
            return false;
        } else if (pipe.getVelocity() > 0) {
            return true;
        }
        return false;
    }

//    public Capacity getFlowOutletCapacity() {
//        if (isFlowInletToPipe()) {
//            return pipe;
//        }
//        return manhole;
//    }
//
//    public Capacity getFlowInletCapacity() {
//        if (isFlowInletToPipe()) {
//            return manhole;
//        }
//        return pipe;
//    }
    @Override
    public Capacity getConnectedCapacity() {
        return pipe;
    }

}
