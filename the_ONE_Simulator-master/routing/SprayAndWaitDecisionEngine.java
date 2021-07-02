/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.ArrayList;
import java.util.List;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;

/**
 * Implementation of Spray and wait router as depicted in 
 * <I>Spray and Wait: An Efficient Routing Scheme for Intermittently
 * Connected Mobile Networks</I> by Thrasyvoulos Spyropoulus et al.
 *
 */
public class SprayAndWaitDecisionEngine implements RoutingDecisionEngine {
	/** identifier for the initial number of copies setting ({@value})*/ 
	public static final String NROF_COPIES = "nrofCopies";
	/** identifier for the binary-mode setting ({@value})*/ 
	public static final String BINARY_MODE = "binaryMode";
	/** SprayAndWait router's settings name space ({@value})*/ 
	public static final String SPRAYANDWAIT_NS = "SprayAndWaitRouter";
	/** Message property key */
	public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + "." + "copies";
	protected int initialNrofCopies;
	protected boolean isBinary;
        

	public SprayAndWaitDecisionEngine(Settings s) {
		initialNrofCopies = s.getInt(NROF_COPIES);
		isBinary = s.getBoolean( BINARY_MODE);
	}
	
	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected SprayAndWaitDecisionEngine(SprayAndWaitDecisionEngine r) {
		initialNrofCopies = r.initialNrofCopies;
		isBinary = r.isBinary;
	}
	
	public RoutingDecisionEngine replicate()
	{
            return new SprayAndWaitDecisionEngine(this);
	}
        
         @Override
        public void connectionDown(DTNHost thisHost, DTNHost peer) {
        }
        
        
        @Override
        public void connectionUp(DTNHost thisHost, DTNHost peer) {
          
        }
              
        @Override
        public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        }

        @Override
        public boolean newMessage(Message m) {
            m.addProperty(MSG_COUNT_PROPERTY, initialNrofCopies);
            return true;
        }
        
        @Override
        public boolean isFinalDest(Message m, DTNHost aHost) {
            Integer nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROPERTY);
            
                if (isBinary) {
			/* in binary S'n'W the receiving node gets ceil(n/2) copies */
			nrofCopies = (int)Math.ceil(nrofCopies/2.0);
		}
		else {
			/* in standard S'n'W the receiving node gets only single copy */
			nrofCopies = 1;
		}
		
		m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
		return m.getTo() == aHost;
        }

        @Override
        public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
            return m.getTo()!= thisHost;
        }

        @Override
        public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
           if(m.getTo() == otherHost){
              return true;
           }
           int nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROPERTY);
           return nrofCopies > 1;
        }
       
          @Override
        public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
            int nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROPERTY);
            
             if (isBinary) {
			nrofCopies /=2;
		}
		else {
			nrofCopies--;
		}
		
		m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
		return false;
        }

        @Override
        public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
            return false;
        }


    @Override
    public void update(DTNHost thisHost) {
        
    }
}