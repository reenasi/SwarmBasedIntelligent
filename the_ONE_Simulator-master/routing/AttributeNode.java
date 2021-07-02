/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing;

/**
 *
 * @author Reena
 */
public class AttributeNode {
    public int delay;
    public int nrOfHops;

    public AttributeNode() {
    }

    
    public AttributeNode(int delay, int nrOfHops) {
        this.delay = delay;
        this.nrOfHops = nrOfHops;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getNrOfHops() {
        return nrOfHops;
    }

    public void setNrOfHops(int nrOfHops) {
        this.nrOfHops = nrOfHops;
    }

    @Override
    public String toString() {
        return " delay = " + delay + ", nrOfHops = " + nrOfHops;
    }
}