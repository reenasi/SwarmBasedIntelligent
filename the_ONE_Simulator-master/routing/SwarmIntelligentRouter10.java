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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Reena
 */
public class SwarmIntelligentRouter10 implements RoutingDecisionEngine {

    /**
     * identifier for the initial number of copies setting ({@value})
     */
    public static final String NROF_COPIES_S = "nrofCopies";
    /**
     * Message property key for the remaining available copies of a message
     */
    public static final String MSG_COUNT_PROP = "SprayAndFocus.copies";

    protected int initialNrofCopies;

    private Map<DTNHost, AttributeNode> history;
    private Map<DTNHost, Integer> waktuTerakhir;

    public SwarmIntelligentRouter10(Settings s) {
        initialNrofCopies = s.getInt(NROF_COPIES_S);
    }

    public SwarmIntelligentRouter10(SwarmIntelligentRouter10 proto) {
        this.history = new HashMap<DTNHost, AttributeNode>();
        this.waktuTerakhir = new HashMap<DTNHost, Integer>();
        this.initialNrofCopies = proto.initialNrofCopies;

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

        SwarmIntelligentRouter10 sw = getOtherDecisionEngine(peer);

        //jika historynya kosong
        if (history.isEmpty()) {
            System.out.println("history kosong");
            attribute.setDelay(0); //reset delay jd 0
            attribute.setNrOfHops(1); //reset nrhop jd 1
            history.put(peer, attribute);
            sw.history.put(thisHost, attribute);
            System.out.println("history kosong put " + sw.history.put(thisHost, attribute));
        } else {
            //jika historynya ada peer yg ditemui / tdk
            if (!history.containsKey(peer)) {
                    attribute.setDelay(0); //reset delay jd 0
                    attribute.setNrOfHops(1); //reset nrhop jd 1
                    history.put(peer, attribute);
                    sw.history.put(thisHost, attribute);
                    
                    System.out.println("g ktm history " + history.put(peer, attribute));
                    System.out.println("g ktm sw.history " + sw.history.put(thisHost, attribute));
            }
            
            for (Map.Entry<DTNHost, AttributeNode> entry : sw.history.entrySet()) {
                DTNHost key = entry.getKey();
                AttributeNode value = entry.getValue();
                // ktemu lsgs sm peer yg ada di map ku / dst
                if (key == thisHost) {
                    System.out.println("sudah pernah ketemu " + sw.history.get(thisHost) + " sebelum update");
                    attribute.setDelay(0);
                    attribute.setNrOfHops(1);
                    sw.history.replace(key, attribute); //simpan ke history this
                    System.out.println("sudah pernah ketemu " + sw.history.get(thisHost) + " setelah update");
                    System.out.println("reset his " + sw.history.replace(key, attribute));
                
                } else if (history.containsKey(key)) { // sudah pernah ketemu node yg sama
                    System.out.println("sudah pernah ketemu node yg sama " + sw.history.get(thisHost) + " sebelum update");
                    calcDelay = history.get(key).getDelay() + delayValue;
                    attribute.setDelay(calcDelay);
                    calcHop = history.get(key).getNrOfHops();
                    attribute.setNrOfHops(calcHop);
                    this.history.replace(key, attribute);
//                    System.out.println("sudah pernah ketemu node yg sama " + sw.history.get(thisHost) + " setelah update");
                    System.out.println("cetak = "+this.history.replace(key, attribute));
                }
            }
        }
        waktuTerakhir.remove(peer);
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        this.waktuTerakhir.put(peer, SimClock.getIntTime());

//        Collection<Message> m = peer.getMessageCollection();
        AttributeNode attribute = new AttributeNode();

        SwarmIntelligentRouter10 sw = getOtherDecisionEngine(peer);
//        System.out.println("lenght " + sw.history.size());

        int i = 0;

        for (Map.Entry<DTNHost, AttributeNode> entry : sw.history.entrySet()) {
            DTNHost key = entry.getKey();
            AttributeNode value = entry.getValue();
//            System.out.println("loop ke-" + i);

            if (history.containsKey(key)) { //di mapku dan relay ada node yg sama
//                System.out.println("delay peer" + value.getDelay() + " delayku " + history.get(key).getDelay());
                int d = Math.min(value.getDelay(), history.get(key).getDelay());

                if (d == history.get(key).getDelay()) { //thisDelay yg minimum
                    attribute.setDelay(history.get(key).delay);
                    attribute.setNrOfHops(history.get(key).nrOfHops + 1);
                    getOtherDecisionEngine(peer).history.replace(thisHost, attribute); //simpan ke history 
//                    System.out.println("delay setelah update              " + sw.history.get(thisHost));
                } else {
                    attribute.setDelay(getOtherDecisionEngine(peer).history.get(key).delay);
                    attribute.setNrOfHops(getOtherDecisionEngine(peer).history.get(key).nrOfHops + 1);
                    this.history.replace(peer, attribute); //simpan ke history
//                    System.out.println("delay setelah update " + history.get(peer));
                }

            } else { //ktemu relay yang g punya node yang sama
                attribute.setDelay(history.get(peer).delay);
                attribute.setNrOfHops(history.get(peer).nrOfHops + 1);
                getOtherDecisionEngine(peer).history.put(thisHost, attribute); //simpan ke history  
//                System.out.println("delay setelah update " + sw.history.get(thisHost));
            }
            i++;
        }
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer
    ) {
    }

    @Override
    public boolean newMessage(Message m
    ) {
        m.addProperty(MSG_COUNT_PROP, initialNrofCopies);
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost
    ) {
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROP);
        nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
        m.updateProperty(MSG_COUNT_PROP, nrofCopies);

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
            int nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROP);
            if (nrofCopies > 1) {
                return true;
            } else {
                if (history.containsKey(dst) && getOtherDecisionEngine(otherHost).history.containsKey(dst)) { //punya pesan ke tujuan yg sm
                    int tD = history.get(dst).getDelay();
                    int rD = getOtherDecisionEngine(otherHost).history.get(dst).getDelay();

                    if ((Math.abs(tD - rD) <= 3600) && (Math.abs(tD - rD) >= 0)) { // delay similar 
                        int tH = history.get(dst).getNrOfHops();
                        int rH = getOtherDecisionEngine(otherHost).history.get(dst).getNrOfHops();
                        int h = Math.min(tH, rH);
                        System.out.println("masok");

                        if (h == rH) { //h relay min
                            return true;
                        }

                    } else {
                        if (rD < tD) { //relayDelay yg minimum
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost
    ) {
        int nrofCopies;

        nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROP);

        if (nrofCopies > 1) {
            nrofCopies /= 2;
        } else {
            return true;
        }

        m.updateProperty(MSG_COUNT_PROP, nrofCopies);

        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld
    ) {
        return m.getTo() == hostReportingOld;
    }

    private SwarmIntelligentRouter10 getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";
        return (SwarmIntelligentRouter10) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    @Override
    public void update(DTNHost thisHost) {
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new SwarmIntelligentRouter10(this);
    }
}
