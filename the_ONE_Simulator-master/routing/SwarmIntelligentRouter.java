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
import java.util.HashMap;
import java.util.Map;
import static routing.SwarmIntelligentRouter5.MSG_COUNT_PROPERTY;

/**
 *
 * @author Reena
 */
public class SwarmIntelligentRouter implements RoutingDecisionEngine {

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
    
    private Map<DTNHost, AttributeNode> history;
    private Map<DTNHost, Integer> waktuTerakhir;

    public SwarmIntelligentRouter(Settings s) {
        initialNrofCopies = s.getInt(NROF_COPIES);
        isBinary = s.getBoolean(BINARY_MODE);
    }

    public SwarmIntelligentRouter(SwarmIntelligentRouter proto) {
        this.history = new HashMap<DTNHost, AttributeNode>();
        this.waktuTerakhir = new HashMap<DTNHost, Integer>();
        this.initialNrofCopies = proto.initialNrofCopies;
        this.isBinary = proto.isBinary;
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        int time = 0;
        int delayValue = 0;
        int calcDelay;
        int calcHop;

        int stime = SimClock.getIntTime();

        if (waktuTerakhir.get(peer) == null) {
            time = 0;
        } else {
            time = waktuTerakhir.get(peer);
        }

        if (stime - time > 0) {
            delayValue = stime - time;
        }

        AttributeNode attribute = new AttributeNode();

        SwarmIntelligentRouter sw = getOtherDecisionEngine(peer);

        if (history.isEmpty()) {
            attribute.setDelay(0); 
            attribute.setNrOfHops(1); 
            history.put(peer, attribute);
            sw.history.put(thisHost, attribute);
        } else {
            if (!history.containsKey(peer)) {
                    attribute.setDelay(0); 
                    attribute.setNrOfHops(1); 
                    history.put(peer, attribute);
                    sw.history.put(thisHost, attribute);
            }
            for (Map.Entry<DTNHost, AttributeNode> entry : sw.history.entrySet()) {
                DTNHost key = entry.getKey();
                AttributeNode value = entry.getValue();
                if (key == thisHost) {
                    attribute.setDelay(0);
                    attribute.setNrOfHops(1);
                    sw.history.replace(key, attribute);
                } else if (history.containsKey(key)) { 
                    calcDelay = history.get(key).getDelay() + delayValue;
                    attribute.setDelay(calcDelay);
                    calcHop = history.get(key).getNrOfHops();
                    attribute.setNrOfHops(calcHop);
                    this.history.replace(key, attribute);
                }
            }
        }
        waktuTerakhir.remove(peer);
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        this.waktuTerakhir.put(peer, SimClock.getIntTime());

        AttributeNode attribute = new AttributeNode();

        SwarmIntelligentRouter sw = getOtherDecisionEngine(peer);

        for (Map.Entry<DTNHost, AttributeNode> entry : sw.history.entrySet()) {
            DTNHost key = entry.getKey();
            AttributeNode value = entry.getValue();

            if (history.containsKey(key)) {
                int d = Math.min(value.getDelay(), history.get(key).getDelay());

                if (d == history.get(key).getDelay()) { 
                    attribute.setDelay(history.get(key).delay);
                    attribute.setNrOfHops(history.get(key).nrOfHops + 1);
                    getOtherDecisionEngine(peer).history.replace(thisHost, attribute);
                } else {
                    attribute.setDelay(getOtherDecisionEngine(peer).history.get(key).delay);
                    attribute.setNrOfHops(getOtherDecisionEngine(peer).history.get(key).nrOfHops + 1);
                    this.history.replace(peer, attribute);
                }
            } 
        }
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
    public boolean isFinalDest(Message m, DTNHost aHost
    ) {
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
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost
    ) {
        return m.getTo() != thisHost;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        AttributeNode attribute = new AttributeNode();
        DTNHost dst = m.getTo();

        if (dst == otherHost) {
            return true;
        } else {
            Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
            if (nrofCopies > 1) {
                if (history.containsKey(dst) && getOtherDecisionEngine(otherHost).history.containsKey(dst)) { 
                    int tD = history.get(dst).getDelay();
                    int rD = getOtherDecisionEngine(otherHost).history.get(dst).getDelay();

                    if ((Math.abs(tD - rD) <= 3600) && (Math.abs(tD - rD) >= 0)) {
                        int tH = history.get(dst).getNrOfHops();
                        int rH = getOtherDecisionEngine(otherHost).history.get(dst).getNrOfHops();
                        int h = Math.min(tH, rH);

                        if (h == rH) {
                            return true;
                        }

                    } else {
                        if (rD < tD) { 
                            return true;
                        }
                    }
                }
            } 
        }
        return false;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        int nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);

        if (isBinary) {
            nrofCopies /= 2;
        } else {
            nrofCopies--;
        }

        m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);

        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld
    ) {
        return false;
    }

    private SwarmIntelligentRouter getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";
        return (SwarmIntelligentRouter) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    @Override
    public void update(DTNHost thisHost) {
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new SwarmIntelligentRouter(this);
    }
}
