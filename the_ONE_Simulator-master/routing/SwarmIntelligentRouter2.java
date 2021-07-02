/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import routing.community.Duration;

/**
 *
 * @author Reena
 */
public class SwarmIntelligentRouter2 implements RoutingDecisionEngine {
    /** identifier for the initial number of copies setting ({@value})*/ 
    public static final String NROF_COPIES = "nrofCopies";
    /** identifier for the binary-mode setting ({@value})*/ 
    public static final String BINARY_MODE = "binaryMode";
    /** SprayAndWait router's settings name space ({@value})*/ 
    public static final String SPRAYANDWAIT_NS = "SwarmIntelligentRouter";
    /** Message property key */
    public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + "." + "copies";
    protected int initialNrofCopies;
    protected boolean isBinary;
    
    private Map<DTNHost, List<Integer>> history;
    protected int delayValue;
    protected Map<DTNHost, List<Duration>> listDelayHost;
    private double waktuSekarang;
    private Map<DTNHost, Double> waktuTerakhir;
    private ArrayList a = new ArrayList();
    
    public SwarmIntelligentRouter2(Settings s) {
        initialNrofCopies = s.getInt(NROF_COPIES);   
        isBinary = s.getBoolean( BINARY_MODE);
    }

    public SwarmIntelligentRouter2(SwarmIntelligentRouter2 proto) {
        this.history = new HashMap<>();
        waktuTerakhir = new HashMap<DTNHost, Double>();
        listDelayHost = new HashMap<DTNHost, List<Duration>>();
        
        this.initialNrofCopies = proto.initialNrofCopies;
        this.isBinary = proto.isBinary;
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        this.waktuTerakhir.put(peer, SimClock.getTime());
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        double time;

        if (waktuTerakhir.get(peer) == null) {
            time = 0;
        } else {
            time = waktuTerakhir.get(peer);
        }

        double etime = SimClock.getTime();

        // Find or create the contact history list
        List<Duration> contactHistoryList;

        if (!listDelayHost.containsKey(peer)) {
            contactHistoryList = new LinkedList<Duration>();
            listDelayHost.put(peer, contactHistoryList);
//                System.out.println(listDelayHost);
        } else {
            contactHistoryList = listDelayHost.get(peer);
        }
        
        // add this connection to the list
        if (etime - time > 0) {
            contactHistoryList.add(new Duration(time, etime));
        }

//        int del = this.delayCount(peer);
//        List nampung = history.get(peer);
//        System.out.println(nampung);
//        nampung.add(0, del);
//        history.replace(peer, nampung);

        waktuTerakhir.remove(peer);
    }

    @Override
    public boolean newMessage(Message m) {
        m.addProperty(MSG_COUNT_PROPERTY, initialNrofCopies);
        return true;
    }

    public int delayCount(DTNHost host) {
        List<Duration> listDelay = new LinkedList<>();
        int jumlahListDelay = 0;

        if (listDelayHost.containsKey(host)) {
            listDelay = listDelayHost.get(host);
//            System.out.println("listt size " +listDelay.size());
        } else {
            listDelay = new LinkedList<>();
        }

        if (listDelay != null) {
            jumlahListDelay = listDelay.size();
        }

        if (jumlahListDelay == 1) {
            return 0;
        }

        Double end = new Double(0);

        Iterator<Duration> iterator = listDelay.iterator();
        while (iterator.hasNext()) {
            Duration next = iterator.next();
            int e = (int) next.end;
            int s = (int) next.start;
            delayValue = delayValue + (e - s);
        }
        return delayValue;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
//        Integer nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROPERTY);
//            
//            if (isBinary) {
//		/* in binary S'n'W the receiving node gets ceil(n/2) copies */
//		nrofCopies = (int)Math.ceil(nrofCopies/2.0);
//	    }
//	    else {
//		/* in standard S'n'W the receiving node gets only single copy */
//		nrofCopies = 1;
//	    }
//		
//	    m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
        return m.getTo() == aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    public double resetDelay(DTNHost host) {
        this.delayValue = 0;
//        if (listDelayHost.containsKey(host)) {
//            System.out.println("list delay host " +listDelayHost);
//            listDelayHost.clear();
//            System.out.println("list delay host 2 " +listDelayHost);
        return delayValue;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        boolean checkShouldSend = false;

        //yg ditemui merupakan dst
        if (m.getTo() == otherHost) {
//            System.out.println("ktemu tujuan");
            DTNHost dst = m.getTo();
            resetDelay(dst);
            int hop = 1;
            int delay = 0;
            ArrayList a = new ArrayList();
            a.add(delay);
            a.add(hop);
            this.history.put(otherHost, a);

            return true;
            // jika otherHostnya benar yang dituju / merupakan destination dari pesannya
        } else {

            //node yg ditemui / relay 
            SwarmIntelligentRouter2 relay = getOtherDecisionEngine(otherHost);

            List thisH = this.history.get(m.getTo());

            ArrayList ab = new ArrayList();
            int delay = history.get(m.getTo()).get(0);
            int hop = history.get(m.getTo()).get(1);
            int delayPeer = relay.history.get(m.getTo()).get(0);
            int hopPeer = relay.history.get(m.getTo()).get(1);
            int d = Math.min(delay, delayPeer);
            
            checkShouldSend = false;

//            double thisDelay = this.delayCount(dst);
//            double relayDelay = relay.delayCount(dst);
            //ktemu relay yang punya pesan thdp tujuan yg sm
            if (otherHost.getRouter().hasMessage(m.getId())) {
                if (d == delay) {
                    ab.add(delay);
                    ab.add(hop + 1);
                    relay.history.replace(m.getTo(), ab);
                } else {

                    ab.add(delayPeer);
                    ab.add(hopPeer + 1);
                    this.history.replace(m.getTo(), ab);
                }

            } else {
                checkShouldSend = true;
                //ktemu relay yg g punya pesan
                Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
                if (isBinary) {
                    /* in binary S'n'W the receiving node gets ceil(n/2) copies */
                    nrofCopies = (int)Math.ceil(nrofCopies/2.0);
		}
                
                ab.add(delay);
                ab.add(hop + 1);
                relay.history.replace(m.getTo(), ab);
            
                return nrofCopies > 1;
            }
        }
        return checkShouldSend;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        int nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROPERTY);
            
             if (isBinary) {
		nrofCopies /=2;
             } else {
//		nrofCopies--;
             }
            
            m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
		
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return false;
    }

    private SwarmIntelligentRouter2 getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";
        return (SwarmIntelligentRouter2) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    @Override
    public void update(DTNHost thisHost) {
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new SwarmIntelligentRouter2(this);
    }
}