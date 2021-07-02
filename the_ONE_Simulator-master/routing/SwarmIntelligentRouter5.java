/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing;

import com.sun.javafx.scene.control.skin.VirtualFlow;
import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import java.util.ArrayList;
import java.util.Arrays;
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
public class SwarmIntelligentRouter5 implements RoutingDecisionEngine {

    /**
     * identifier for the initial number of copies setting ({@value})
     */
    public static final String NROF_COPIES = "nrofCopies";
    /**
     * identifier for the binary-mode setting ({@value})
     */
    public static final String BINARY_MODE = "binaryMode";
    /**
     * SprayAndWait router's settings name space ({@value})
     */
    public static final String SPRAYANDWAIT_NS = "SwarmIntelligentRouter";
    /**
     * Message property key
     */
    public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + "." + "copies";
    protected int initialNrofCopies;
    protected boolean isBinary;

    private Map<DTNHost, List<AttributeNode>> history;
//    private Map<DTNHost, Double> waktuTerakhir;
    private int waktuTerakhir;
    private double waktuSekarang;
    private int time = 0;
    private int etime;
    private int delayValue;
    AttributeNode attribute = new AttributeNode();

//    private List<DTNHost> path;

    public SwarmIntelligentRouter5(Settings s) {
        initialNrofCopies = s.getInt(NROF_COPIES);
        isBinary = s.getBoolean(BINARY_MODE);
    }

    public SwarmIntelligentRouter5(SwarmIntelligentRouter5 proto) {
        this.history = new HashMap<DTNHost, List<AttributeNode>>();
//        this.waktuTerakhir = new HashMap<DTNHost, Double>();
        this.initialNrofCopies = proto.initialNrofCopies;
        this.isBinary = proto.isBinary;

//        this.path = new ArrayList<DTNHost>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
       
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        this.waktuTerakhir = SimClock.getIntTime();
//        this.waktuTerakhir.put(peer, SimClock.getTime());
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        etime = SimClock.getIntTime();

        if (this.waktuTerakhir != 0) {
            time = waktuTerakhir;
        }
        
        
//
//        if (etime - time > 0) {
//            delayValue = delayValue + (etime - time);
//        }

        this.waktuTerakhir = 0;
    }

    public int delayCount(DTNHost host) {
//        int delay = 0;

        if (etime - time > 0) {
            delayValue = delayValue + (etime - time);
        }

        return delayValue;
    }

    public int getHopCount(DTNHost peer) {
        return attribute.nrOfHops;
    }

    @Override
    public boolean newMessage(Message m) {
        m.addProperty(MSG_COUNT_PROPERTY, initialNrofCopies);
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);

        if (isBinary) {
            /* in binary S'n'W the receiving node gets ceil(n/2) copies */
            nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
        } else {
            /* in standard S'n'W the receiving node gets only single copy */
            nrofCopies = 1;
        }

        m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
        return m.getTo() == aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        boolean checkShouldSend = false;

//        AttributeNode attribute = new AttributeNode();
        List<AttributeNode> list = new ArrayList<AttributeNode>();
        DTNHost dst = m.getTo();
        SwarmIntelligentRouter5 relay = getOtherDecisionEngine(otherHost);
            int thisDelay = this.delayCount(dst);
            int relayDelay = relay.delayCount(dst);
            int thisHop = this.getHopCount(dst);
            int relayHop = relay.getHopCount(dst);
            

        if (m.getTo() == otherHost) {
            System.out.println("ketemu dst");
            attribute.setDelay(0);
            attribute.setNrOfHops(1);
            System.out.println(" att " +attribute);
            list.add(attribute);
            System.out.println(" abc " + list);
            this.history.put(dst, list);
            System.out.println("history 1 " + history);

            //Menampilkan Semua Data di history Menggunakan For-Each
//            for (Map.Entry l : history.entrySet()) {
//                List listt = (List) l.getValue();
//                System.out.println(l.getKey() + ": " + list.get(0).toString());
//            }
//               for (Map.Entry<DTNHost, List<AttributeNode>> entry : history.entrySet()) {
//                DTNHost key = entry.getKey();
//                List value = entry.getValue();
//                System.out.println(key);
//                System.out.println("hahaha smgt "+value.toString());
//                System.out.println(key +" value " +Arrays.deepToString(value.toArray()));
//               }
            return true;
        } else {
            //node yg ditemui / relay 

            int d = Math.min(thisDelay, relayDelay);

            //ktemu relay yang punya pesan thdp tujuan yg sm
            if (otherHost.getRouter().hasMessage(m.getId())) {
                System.out.println("ktemu relay");
                checkShouldSend = false;
                if (d == thisDelay) {
                    attribute.setDelay(thisDelay = relayDelay);
                    attribute.setNrOfHops(thisHop = relayHop);
                    attribute.setNrOfHops(thisHop + 1); //cek hop utk node masih salah hrsnya huhu:'
                    list.add(attribute);
                    history.put(dst, list);
                    System.out.println("history 2 " + history);
                } else {

                    attribute.setDelay(relayDelay = thisDelay);
                    attribute.setNrOfHops(relayHop = thisHop);
                    attribute.setNrOfHops(relayHop+ 1);
                    list.add(attribute);
                    history.put(dst, list);
                    System.out.println("history 3 " + history);
                }
            //ktemu relay yang g punya pesan
            } else {
                checkShouldSend = true;
                System.out.println("ktemu relay 2");

                Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
                if (isBinary) {
                    /* in binary S'n'W the receiving node gets ceil(n/2) copies */
                    nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
                }
                attribute.setDelay(thisDelay = relayDelay);
                attribute.setNrOfHops(thisHop = relayHop);
                attribute.setNrOfHops(thisHop + 1);
                list.add(attribute);
                history.put(dst, list);
                System.out.println("history 4 " + history);

                return nrofCopies > 1;
            }
        }
        return checkShouldSend;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        int nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);

        if (isBinary) {
            nrofCopies /= 2;
        } else {
//		nrofCopies--;
        }

        m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);

        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld
    ) {
        return false;
    }

    private SwarmIntelligentRouter5 getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";
        return (SwarmIntelligentRouter5) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    @Override
    public void update(DTNHost thisHost) {
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new SwarmIntelligentRouter5(this);
    }
}