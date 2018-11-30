package model.topology;

/**
 *
 * @author saemann
 */
public class Inlet {

    protected Position position;
    protected Capacity pipe;
    protected double pipeposition1d;

    public Inlet(Position pos, Capacity pipe, double pipeposition1d) {
        this.position = pos;
        this.pipe = pipe;
        this.pipeposition1d = pipeposition1d;
    }

    public Capacity getNetworkCapacity() {
        return pipe;
    }

    public double getPipeposition1d() {
        return pipeposition1d;
    }

    public Position getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return "Inlet to (" + pipe.toString() + "," + pipe.getAutoID() + ")";
    }

}
