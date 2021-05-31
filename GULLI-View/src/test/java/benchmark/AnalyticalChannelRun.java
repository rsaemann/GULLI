/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package benchmark;

import com.saemann.gulli.core.control.Controller;
import com.saemann.gulli.core.control.particlecontrol.ParticlePipeComputing;
import com.saemann.gulli.core.control.threads.ThreadController;
import com.saemann.gulli.core.io.AnalyticalChannel;
import com.saemann.gulli.core.model.material.dispersion.pipe.Dispersion1D_Constant;
import com.saemann.gulli.core.model.particle.Particle;
import com.saemann.gulli.core.model.timeline.MeasurementContainer;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineMeasurement;
import com.saemann.gulli.core.model.timeline.array.ArrayTimeLineMeasurementContainer;
import com.saemann.gulli.core.model.topology.Network;
import com.saemann.gulli.core.model.topology.Position;
import com.saemann.gulli.view.ViewController;
import com.saemann.gulli.view.timeline.SpacelinePanel;
import javax.swing.JFrame;

/**
 * A simple benchmark with a straight channel. This can be used to test
 * different transport and dispersion approaches. The analytical solution is
 * shown in parallel to the particle results
 *
 * @author saemann
 */
public class AnalyticalChannelRun {

    public static void main(String[] args) throws Exception {

        ThreadController.pauseRevokerThread = true;

        ArrayTimeLineMeasurement.useIDsharpParticleCounting = false;
        AnalyticalChannel channel = new AnalyticalChannel(1, 10000, 3600, 300);
        channel.velocity = 1.667f;
        channel.disp = 1f;
        channel.massPerParticle = 0.001f;
        int numberOfParticles = 100000;
        final Network nw = channel.createNetwork(1f);
        channel.resetConcentration();
//        channel.addRectangularProfile(500, 501, 1, 0);
        channel.material.setDispersionCalculatorPipe(new Dispersion1D_Constant(channel.disp));
        channel.addContaminationSuperposition(1000, 0, channel.massPerParticle * numberOfParticles);

        ParticlePipeComputing.measureOnlyFinalCapacity = true;
        MeasurementContainer.timecontinuousMeasures = false;

        final Controller c = new Controller();
        c.getThreadController().setSeed(1);
        ViewController vc = new ViewController(c);
        c.loadNetwork(nw, c);

        c.getThreadController().setDeltaTime(1);
        c.loadScenario(channel.getScenario(), c);
        System.out.println("measurementTimeline gets " + c.getScenario().getStatusTimesPipe().getNumberOfTimes() + " times.");
        c.initMeasurementTimelines(c.getScenario(), false);
        channel.fillPipeTimelinesWithAnalyticalValues();

        vc.getMapViewer().getLayer("MH").setVisibleInMap(false);
        vc.getMapViewer().recalculateShapes();

        c.recalculateInjections();

        //Calculate particles total mass
        double pmass = 0;
        for (Particle particle : c.getThreadController().getParticles()) {
            pmass += particle.particleMass;
        }
        System.out.println("Total particle mass is " + pmass + " kg");

        Position pos = nw.getManholeByName("MH_2000").getPosition();
        vc.getMapViewer().setDisplayPositionByLatLon(pos.getLatitude(), pos.getLongitude(), 16);

//        System.out.println("messungen pro zeitschritt: " + ArrayTimeLineMeasurementContainer.instance.samplesPerTimeinterval);
        c.resetScenario();
        c.start();

        SpacelinePanel spacePlotPanel = new SpacelinePanel(null, (ArrayTimeLineMeasurementContainer) c.getScenario().getMeasurementsPipe(), "Time domain");
        JFrame spaceFrame = new JFrame("Time Domain");
        spaceFrame.add(spacePlotPanel);
        spaceFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        spaceFrame.setBounds(250, 300, 600, 800);
        spacePlotPanel.addOpenMenu(spaceFrame);
        spaceFrame.setVisible(true);
        spacePlotPanel.setDividerlocation(0.6);
        vc.getPaintManager().addCapacitySelectionListener(spacePlotPanel);
    }
}
