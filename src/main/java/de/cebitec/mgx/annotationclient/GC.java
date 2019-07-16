/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.annotationclient;

import de.cebitec.mgx.sequence.DNASequenceI;

/**
 *
 * @author sj
 */
public class GC {

    public static float gc(DNASequenceI seq) {
        byte[] sequence = seq.getSequence();
        int gc = 0;
        for (byte b : sequence) {
            switch (b) {
                case 'g':
                case 'G':
                case 'c':
                case 'C':
                    gc++;
            }
        }
        return gc/sequence.length;
    }
}
