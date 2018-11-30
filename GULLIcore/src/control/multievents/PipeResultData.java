/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package control.multievents;

import control.Controller;
import io.extran.HE_Database;
import java.io.File;
import model.timeline.array.ArrayTimeLineManholeContainer;
import model.timeline.array.ArrayTimeLinePipeContainer;
import model.topology.graph.Pair;

/**
 *
 * @author saemann
 */
public class PipeResultData {

    private File file;
    private String name;

    private ArrayTimeLinePipeContainer pipeTimeline;
    private ArrayTimeLineManholeContainer manholeTimeline;
    
    private boolean loading=false;

    public PipeResultData(File file, String name, ArrayTimeLinePipeContainer pipeTimeline, ArrayTimeLineManholeContainer manholeTimeline) {
        this.file = file;
        this.name = name;
        this.pipeTimeline = pipeTimeline;
        this.manholeTimeline = manholeTimeline;
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayTimeLinePipeContainer getPipeTimeline() {
        return pipeTimeline;
    }

    public ArrayTimeLineManholeContainer getManholeTimeline() {
        return manholeTimeline;
    }

    public boolean loadData(Controller control) throws Exception {
        if (control == null || control.getNetwork() == null) {
            return false;
        }
        this.loading=true;
        Pair<ArrayTimeLinePipeContainer, ArrayTimeLineManholeContainer> p = HE_Database.readTimelines(file, control.getNetwork());
        this.manholeTimeline = p.second;
        this.pipeTimeline = p.first;
        this.loading=false;
        return true;
    }

    public boolean isLoading() {
        return loading;
    }

}
