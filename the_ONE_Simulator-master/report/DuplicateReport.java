/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import core.SimClock;
import core.UpdateListener;
import java.util.*;

/**
 *
 * @author Reena
 */
public class DuplicateReport extends Report implements MessageListener, UpdateListener {
    private double lastRecord = Double.MIN_VALUE;
    private int nrOfCopy;
    private int nrOfMsgCreated;
    private int cumulative;
    private int interval = 300;
	

    public DuplicateReport() {
      super();
    }
    
    @Override
    public void newMessage(Message m) {
        nrOfMsgCreated++;
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
      
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
       
    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
        
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        nrOfCopy++;
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        String print = "";
        if (SimClock.getTime() - lastRecord >= interval){
            lastRecord = SimClock.getTime();
            
            double time = SimClock.getIntTime();
            
            cumulative = (cumulative + nrOfCopy) / nrOfMsgCreated;
            print = time + "\t" +cumulative;
            write(print);
        }
    }   
}