/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.annotationclient;

import de.cebitec.mgx.seqcompression.SequenceException;
import de.cebitec.mgx.sequence.DNASequenceI;
import de.cebitec.mgx.sequence.SeqReaderFactory;
import de.cebitec.mgx.sequence.SeqReaderI;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sj
 */
public class N50 {

    public static int n50(File... files) throws SequenceException {
        List<DNASequenceI> seqs = new ArrayList<>();
        for (File f : files) {
            SeqReaderI<? extends DNASequenceI> reader = SeqReaderFactory.getReader(f.getAbsolutePath());

            while (reader.hasMoreElements()) {
                DNASequenceI seq = reader.nextElement();
                seqs.add(seq);
            }
            reader.close();
        }
        return n50(seqs);
    }

    public static int n50(List<DNASequenceI> seqs) throws SequenceException {
        Collections.sort(seqs, new Comparator<DNASequenceI>() {
            @Override
            public int compare(DNASequenceI t, DNASequenceI t1) {
                try {
                    return Integer.compare(t.getSequence().length, t1.getSequence().length);
                } catch (SequenceException ex) {
                    Logger.getLogger(N50.class.getName()).log(Level.SEVERE, null, ex);
                }
                return 0;
            }
        });
        Collections.reverse(seqs);
        int total = 0;
        int partial = 0;
        int index = 0;
        for (DNASequenceI s : seqs) {
            total += s.getSequence().length;
        }
        while (index < seqs.size() && partial + seqs.get(index).getSequence().length <= (total / 2)) {
            partial += seqs.get(index).getSequence().length;
            index++;
        }

        return seqs.get(index).getSequence().length;
    }
}
