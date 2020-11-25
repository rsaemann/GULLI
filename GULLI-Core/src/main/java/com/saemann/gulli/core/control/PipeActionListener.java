package com.saemann.gulli.core.control;

import com.saemann.gulli.core.io.NamedPipeIO.PipeActionEvent;

/**
 *
 * @author saemann
 */
public interface PipeActionListener {

    public void actionPerformed(PipeActionEvent ae);
}
