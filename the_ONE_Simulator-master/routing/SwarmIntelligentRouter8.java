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
public class SwarmIntelligentRouter8 implements RoutingDecisionEngine {

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
    private int waktuTerakhir; // global karna nyimpen ke siapaspun

    public SwarmIntelligentRouter8(Settings s) {
        initialNrofCopies = s.getInt(NROF_COPIES);
        isBinary = s.getBoolean(BINARY_MODE);
    }

    public SwarmIntelligentRouter8(SwarmIntelligentRouter8 proto) {
        this.history = new HashMap<DTNHost, AttributeNode>();
        this.initialNrofCopies = proto.initialNrofCopies;
        this.isBinary = proto.isBinary;
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {

    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        this.waktuTerakhir = SimClock.getIntTime();
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        int time = 0;
        int delayValue = 0;
        int calcDelay = 0;
        int hop = 1;

        int stime = SimClock.getIntTime();

        if (this.waktuTerakhir != 0) {
            time = waktuTerakhir;
        }

        if (stime - time > 0) {
            delayValue = stime - time;
        }

        SwarmIntelligentRouter8 relay = getOtherDecisionEngine(peer);

        DTNHost myHost = con.getOtherNode(peer);
        Message m = new Message();

        AttributeNode attribute = new AttributeNode();
        DTNHost dst = m.getTo();
        
        System.out.println("size "+history.size());

        if (history == null) {
            calcDelay = delayValue + history.get(relay).delay;
            history.get(relay).delay = calcDelay;
            history.get(relay).nrOfHops = hop;
            this.history.put(peer, attribute);
            
        } else {

            for (Map.Entry<DTNHost, AttributeNode> entry : relay.history.entrySet()) {
                DTNHost key = entry.getKey();
                AttributeNode value = entry.getValue();

                calcDelay = delayValue + history.get(relay).delay;
                history.get(relay).delay = calcDelay; // update nilai delay

                if (!history.containsKey(key)) {
                    if (dst == peer) { // jika ktemu dst
                        System.out.println("ktemu dst");
                        attribute.setDelay(0); //reset delay jd 0
                        attribute.setNrOfHops(1); //reset nrhop jd 1
                        this.history.put(dst, attribute);
                    } else if (peer.getRouter().hasMessage(m.getId())) { //ktemu relay yang punya pesan ke tujuan yg sm
                        System.out.println("ktemu relay yang punya pesan ke tujuan yg sm");

                        int d = Math.min(history.get(myHost).delay, history.get(relay).delay);

                        if (d == history.get(myHost).delay) { //thisDelay yg minimum
                            System.out.println("thisDelay yg minimum");
                            history.get(relay).delay = history.get(myHost).delay;
                            history.get(relay).nrOfHops = history.get(myHost).nrOfHops + 1;
                            this.history.replace(dst, attribute); //simpan ke history

                        } else {
//                        System.out.println("relayDelay yg minimum");
                            history.get(myHost).delay = history.get(relay).delay;
                            history.get(myHost).nrOfHops = history.get(relay).nrOfHops + 1;
                            history.replace(dst, attribute); //simpan ke history
                        }

                    } else { //ktemu relay yang g punya pesan dgn tujuan yang sama
                        System.out.println("ktemu relay yang g punya pesan dgn tujuan yang sama");
                        history.get(relay).delay = history.get(myHost).delay;
                        history.get(relay).nrOfHops = history.get(myHost).nrOfHops + 1;
                        this.history.replace(dst, attribute); //simpan ke history
                    }
                }
            }
        }

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
        boolean checkShouldSend = false;
        
        AttributeNode attribute = new AttributeNode(); // perhitungan atribut terjadi di do exc
        DTNHost dst = m.getTo();
        
        
        DTNHost myHost = thisHost;
        SwarmIntelligentRouter8 relay = getOtherDecisionEngine(otherHost);

        //tinggal proses kirim aja, gak ada update atau tukar2 delay lagi karna sdh dilakukan di do exc
        if (dst == otherHost) {
            checkShouldSend = true;
        } else if (otherHost.getRouter().hasMessage(m.getId())) { //ktemu relay yang punya pesan ke tujuan yg sm
            System.out.println("ktemu relay yang punya pesan ke tujuan yg sm");
            System.out.println("a1 "+history.get(thisHost).getDelay());
            System.out.println("a2 "+history.get(otherHost).getDelay());

//            int d = Math.min(history.get(thisHost).getDelay(), history.get(otherHost).getDelay());
            int d = Math.min(history.get(thisHost).getDelay(), history.get(otherHost).getDelay());

            if (d == history.get(thisHost).delay) { //thisDelay yg minimum
                checkShouldSend = false;
            } else if (d == history.get(otherHost).getDelay()) {
                checkShouldSend = true;
                Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
                return nrofCopies > 1;
            } else {
                int h = Math.min(history.get(thisHost).getNrOfHops(), history.get(otherHost).getNrOfHops());
                if (h == history.get(thisHost).getNrOfHops()) {
                    checkShouldSend = false;
                } else if (h == history.get(otherHost).getNrOfHops()) {
                    checkShouldSend = true;
                    Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
                    return nrofCopies > 1;
                } else {
                    checkShouldSend = false;
                }
            }

        } else { //ktemu relay yang g punya pesan dgn tujuan yang sama
            checkShouldSend = true;
            Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
            return nrofCopies > 1;
        }

        return checkShouldSend;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost
    ) {
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

    private SwarmIntelligentRouter8 getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";
        return (SwarmIntelligentRouter8) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    @Override
    public void update(DTNHost thisHost) {
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new SwarmIntelligentRouter8(this);
    }
}
