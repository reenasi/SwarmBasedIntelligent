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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import routing.community.Duration;


/**
 *
 * @author Reena
 */
public class SwarmIntelligentRouter1 implements RoutingDecisionEngine {

    private Map<Message, Integer> nrofHops;
    public static final String NROF_COPIES = "nrofCopies";
    protected int initialNrofCopies;
    protected double delayValue;
    protected Map<DTNHost, List<Duration>> listDelayHost;
    private double waktuSekarang;
    private Map<DTNHost, Double> waktuTerakhir;

    public SwarmIntelligentRouter1(Settings s) {
        initialNrofCopies = s.getInt(NROF_COPIES);

    }

    public SwarmIntelligentRouter1(SwarmIntelligentRouter1 proto) {
        this.nrofHops = new HashMap<>();
        waktuTerakhir = new HashMap<DTNHost, Double>();
        listDelayHost = new HashMap<DTNHost, List<Duration>>();
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
//        System.out.println(peer);
//            System.out.println(contactHistoryList);

//        listDelayHost.put(peer, contactHistoryList);
//        System.out.println("list delay host "+listDelayHost);
//            for (Map.Entry<DTNHost, List<Duration>> entry : listDelayHost.entrySet()) {
//                DTNHost key = entry.getKey();
//                List value = entry.getValue();
//                System.out.println(key);
//                System.out.println(value.toString());
//            System.out.println(key +" value " +Arrays.deepToString(value.toArray()));
            
//        }

//        System.out.println("contact his list" + contactHistoryList);

        // add this connection to the list
        if (etime - time > 0) {
            contactHistoryList.add(new Duration(time, etime));
        }

        waktuTerakhir.remove(peer);
    }

    public double delayCount(DTNHost host) {
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

            if(jumlahListDelay == 1){
               return 0;
            }
            
        Double end = new Double(0);
        
            Iterator<Duration> iterator = listDelay.iterator();
            while (iterator.hasNext()) {
                Duration next = iterator.next();
                if (end == 0) {
                    end = next.start;
                }

                delayValue = delayValue + (next.start - end);
                end = next.end;
            
        }
        return delayValue;
    }

//    public List<Duration> getList(DTNHost host) {
//        if (listDelayHost.containsKey(host)) {
//            return listDelayHost.get(host);
//        } else {
//            List<Duration> d = new LinkedList<>();
//            return d;
//        }
//    }

    @Override
    public boolean newMessage(Message m) {
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    public double resetDelay(DTNHost host) {
        this.delayValue = 0.0;
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
//            System.out.println("dst " +dst + " delay sblm " +delayValue);
//            System.out.println("list delay sblm " +listDelayHost);
            resetDelay(dst);
            int hop = 1;
            this.nrofHops.put(m, hop);
            this.nrofHops.get(m);
            
//            this.nrofHops.put(m, m.getHopCount() + 1);
//            System.out.println("delay " +delayValue);
//            System.out.println("list delay " +listDelayHost);
//            System.out.println("this hops " +this.nrofHops.get(m));
            return true;
            
            // jika otherHostnya benar yang dituju / merupakan destination dari pesannya
        } else {
            //node tujuan
            DTNHost dst = m.getTo();

            //node yg ditemui / relay
            SwarmIntelligentRouter1 relay = getOtherDecisionEngine(otherHost);
            double thisDelay = this.delayCount(dst);
            double relayDelay = relay.delayCount(dst);
            
//            System.out.println("ktemu relay");
//            System.out.println("this"+thisHost);
//            System.out.println("this delay"+thisDelay);
//            System.out.println("relay"+relay);
//            System.out.println("relay delay"+relayDelay);

            for (Message msg : otherHost.getMessageCollection()) {
//                System.out.println(m + " " + msg);
                if (String.valueOf(msg) == String.valueOf(m)) {
//                    System.out.println(m + " cek 1 " + msg);
                    checkShouldSend = false;
//                    System.out.println(m + " cek 2 " + msg);
//                    System.out.println("#this delay "+thisDelay);
//                    System.out.println("#relay delay "+relayDelay);
                    
                    if (relayDelay < thisDelay) {
//                        System.out.println("cek lagi");
//                        System.out.println("this delay "+thisDelay);
//                        System.out.println("relay delay "+relayDelay);
            
                        thisDelay = relayDelay;
                        
//                        System.out.println("delay this samain dgn relay");
//                        System.out.println("    this delay "+thisDelay);
//                        System.out.println("    relay delay "+relayDelay);
                        
                        if (nrofHops.containsKey(m)) {
                            int hopCount = nrofHops.get(m);
                            int thisHop = m.getHopCount();
                            int otherHop = msg.getHopCount();
                            
                            thisHop = otherHop;
//                            nrofHops.put(m, hopCount + m.getHopCount());

//                            this.nrofHops = relay.nrofHops;//cek
                            nrofHops.put(m, m.getHopCount()+1);//di cek lg logicnya:'
                        }
                    } else {
                        relayDelay = thisDelay;
                        if (nrofHops.containsKey(m)) {
                            int hopCount = nrofHops.get(m);
                            int thisHop = m.getHopCount();
                            int otherHop = msg.getHopCount();
                            
                            otherHop = thisHop;
//                            nrofHops.put(m, hopCount + m.getHopCount());
                            
                            nrofHops.put(m,  m.getHopCount()+1);
                        }
//                        relay.nrofHops = this.nrofHops;
//                        int rH = this.nrofHops.get(m) + 1;
//    //                    relay.nrofHops.put(m,  m.getHopCount()+1);
//                        relay.nrofHops.put(m,  rH);
                    }
                } else {
                    checkShouldSend = true;
//                    System.out.println("this delay tr "+thisDelay);
//                    System.out.println("relay delay tr "+relayDelay);
                    Integer nrofCopies = (Integer) initialNrofCopies;
                    
                    
//                    System.out.println("---copies bf " +nrofCopies);
                    nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
//                    System.out.println("---copies after " +nrofCopies);
                    relayDelay = thisDelay;
    
//                    System.out.println("this delay tr "+thisDelay + " relay delay tr "+relayDelay);
                    relay.nrofHops.put(m, m.getHopCount() + 1);
                }
            }
        }
        return checkShouldSend;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return false;
    }

    private SwarmIntelligentRouter1 getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";
        return (SwarmIntelligentRouter1) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    @Override
    public void update(DTNHost thisHost) {
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new SwarmIntelligentRouter1(this);
    }
}