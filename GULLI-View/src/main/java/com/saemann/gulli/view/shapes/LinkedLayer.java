/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.saemann.gulli.view.shapes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import com.saemann.rgis.view.ColorHolder;
import com.saemann.rgis.view.shapes.Layer;
import com.saemann.rgis.view.shapes.PaintInfo;

/**
 *
 * @author saemann
 */
public class LinkedLayer extends Layer{
    
    LinkedList<PaintInfo> list=new LinkedList<>();

    public LinkedLayer(String name, ColorHolder colorHolder) {
        super(name, colorHolder);
        list.clear();
    }

    @Override
    public void add(PaintInfo element) {
        list.addLast(element);
    }

    @Override
    public PaintInfo remove(long element_id) {
        Iterator<PaintInfo> it = list.iterator();
        while(it.hasNext()){
            PaintInfo p=it.next();
            if(p.getId()==element_id){
                it.remove();
                return p;
            }
        }
        return null;
    }

    @Override
    public void clear() {
        list.clear(); 
    }

    @Override
    public ArrayList<PaintInfo> getElementsList() {
        return new ArrayList<>(list); 
    }
    
    
    
    
    
}
