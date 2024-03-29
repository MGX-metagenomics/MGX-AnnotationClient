/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.annotationclient;

import de.cebitec.mgx.seqcompression.SequenceException;
import de.cebitec.mgx.seqstorage.DNASequence;
import de.cebitec.mgx.sequence.DNASequenceI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author sj
 */
public class GCTest {
    
    /**
     * Test of gc method, of class GC.
     */
    @Test
    public void testGc() throws SequenceException {
        System.out.println("gc");
        DNASequenceI seq = new DNASequence("AAAAAAATTTTTTTTGCGCGCTTTTT".getBytes());
        float result = GC.gc(seq);
        assertEquals(23.076923, result, 0.000001);
    }
    
}
