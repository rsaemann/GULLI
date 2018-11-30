package model.topology.measurement;

import java.util.Collection;
import model.particle.Particle;
import model.topology.Pipe;

/**
 *
 * @author saemann
 */
public class ParticleMeasurementSegment implements ParticleMeasurement {

    Pipe pipe;
    double positionAbsolut;
    double sectionLength = 2.;

    double startpositionAbsolute, endpositionAbsolute;

    protected int counter = 0;

    public ParticleMeasurementSegment(Pipe pipe, double positionAbsolut) {
        this.pipe = pipe;
        this.positionAbsolut = positionAbsolut;
        if (sectionLength > pipe.getLength()) {
            startpositionAbsolute = 0;
            endpositionAbsolute = pipe.getLength();
            sectionLength = pipe.getLength();
        } else if (positionAbsolut - sectionLength < 0) {
            startpositionAbsolute = 0;
            endpositionAbsolute = sectionLength;
        } else if (positionAbsolut + sectionLength > pipe.getLength()) {
            startpositionAbsolute = pipe.getLength() - sectionLength;
            endpositionAbsolute = pipe.getLength();
        } else {
            this.startpositionAbsolute = positionAbsolut - 0.5 * sectionLength;
            this.endpositionAbsolute = positionAbsolut + 0.5 * sectionLength;
        }
    }

    @Override
    public void measureParticles(Collection<Particle> c) {
        int count = 0;
        for (Particle p : c) {
            if (p.getSurrounding_actual() == pipe) {
                if (p.getPosition1d_actual() >= startpositionAbsolute && p.getPosition1d_actual() <= endpositionAbsolute) {
                    count++;
                }
            }
        }
        this.counter += count;
//        return count;
    }

    @Override
    public void resetCounter() {
        this.counter = 0;
    }

    @Override
    public void writeCounter(long timestamp) {
        throw new UnsupportedOperationException();
//        ConcentrationParticleSegment value = new ConcentrationParticleSegment(counter / sectionLength);
//        SimplePipeStamp st = pipe.getStatusTimeLine().getEarlierUntilNow(timestamp);
//        st.setValue(value,st.getIndex(value));
//        resetCounter();
    }
}
