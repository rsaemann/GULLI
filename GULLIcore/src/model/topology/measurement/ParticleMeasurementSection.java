package model.topology.measurement;

import java.util.Collection;
import model.particle.Particle;
import model.topology.Pipe;

/**
 * Misst die Konzentration der Partikel an genau einem Punkt innerhalb eines
 * Rohr über den Abstand der Partikel zur Messstelle.
 *
 * @author saemann
 */
public class ParticleMeasurementSection implements ParticleMeasurement {

    Pipe pipe;
    double positionAbsolut;
    double nearestBefore, nearestAfter;

    double startpositionAbsolute, endpositionAbsolute;
    private int particlesInPipe = 0;
    protected int counter = 0;

    public ParticleMeasurementSection(Pipe pipe, double positionAbsolut) {
        this.pipe = pipe;
        this.positionAbsolut = positionAbsolut;
    }

    @Override
    public void measureParticles(Collection<Particle> c) {

        for (Particle p : c) {
            if (p.getSurrounding_actual() == pipe) {
                particlesInPipe++;
                if (p.getPosition1d_actual() > positionAbsolut) {
                    if (p.getPosition1d_actual() < nearestAfter) {
                        nearestAfter = p.getPosition1d_actual();
                    }
                } else {
                    if (p.getPosition1d_actual() > nearestBefore) {
                        nearestBefore = p.getPosition1d_actual();
                    }
                }
            }
        }
    }

    @Override
    public void resetCounter() {
        this.counter = 0;
        nearestBefore = 0;
        nearestAfter = pipe.getLength();
        particlesInPipe = 0;
    }

    @Override
    public void writeCounter(long timestamp) {
        throw new UnsupportedOperationException();
//        ConcentrationParticleSection value;
//        if (particlesInPipe == 0) {
//            value = new ConcentrationParticleSection(0);
//        } else {
//            double h = pipe.getStatusTimeLine().getEarlierUntilNow(timestamp).getWaterLevelPipe();
//            double A = pipe.getProfile().getFlowArea(h);
//            double dx = nearestAfter - nearestBefore;
//            double V = A * dx;
//            if (V < 0.000001) {
//                value = new ConcentrationParticleSection(0);
//            } else {
//                value = new ConcentrationParticleSection(1. / (A * dx));
//            }
//        }
//        SimplePipeStamp st = pipe.getStatusTimeLine().getEarlierUntilNow(timestamp);
//        st.setValue(value, st.getIndex(value));
//        resetCounter();
    }
}
