/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.annotationclient;

import de.cebitec.gpms.rest.RESTAccessI;
import de.cebitec.gpms.rest.RESTException;
import de.cebitec.mgx.dto.dto.MGXString;
import de.cebitec.mgx.dto.dto.SeqRunDTO;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.dto.dto.SequenceDTOList;
import de.cebitec.mgx.restgpms.JAXRSRESTAccess;
import de.cebitec.mgx.seqstorage.AsyncWriter;
import de.cebitec.mgx.seqstorage.PairedEndFASTQWriter;
import de.cebitec.mgx.seqstorage.QualityDNASequence;
import de.cebitec.mgx.seqstorage.QualityEncoding;
import de.cebitec.mgx.sequence.DNAQualitySequenceI;
import de.cebitec.mgx.sequence.SeqWriterI;
import gnu.getopt.Getopt;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.Executors;

public class SeqRunFetcher {

    //private final URI host;
    private final String apiKey;
    private final String projectName;
    private final RESTAccessI rest;

    public SeqRunFetcher(URI host, String apiKey, String projectName) {
        this.apiKey = apiKey;
        this.projectName = projectName;
        this.rest = new JAXRSRESTAccess(null, host, false);
        rest.addFilter(new APIKeyFilter(this.apiKey));
    }

    public SeqRunDTO fetchRun(long id) throws RESTException {
        return rest.get(SeqRunDTO.class, projectName, "AnnotationService", "fetchSeqRun", String.valueOf(id));
    }

    public UUID initDownload(long id) throws RESTException {
        String uuid = rest.get(MGXString.class, projectName, "AnnotationService", "initDownload", String.valueOf(id)).getValue();
        return UUID.fromString(uuid);
    }

    public SequenceDTOList fetchSequences(UUID session) throws RESTException {
        return rest.get(SequenceDTOList.class, projectName, "AnnotationService", "fetchSequences", session.toString());
    }

    public void closeDownload(UUID session) throws RESTException {
        rest.get(projectName, "AnnotationService", "closeDownload", session.toString());
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {

        long duration = System.currentTimeMillis();
        /*
         * -a API key
         * -h host
         * -p project name
         * -r run id
         */

        URI host = null;
        String apiKey = null, projectName = null;
        long seqrunId = -1;
        Getopt g = new Getopt("SeqRunFetcher", args, "a:h:p:r:");
        int c;
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'h':
                    host = URI.create(g.getOptarg());
                    break;
                case 'a':
                    apiKey = g.getOptarg();
                    break;
                case 'p':
                    projectName = g.getOptarg();
                    break;
                case 'r':
                    seqrunId = Long.parseLong(g.getOptarg());
                    break;
                default:
                    System.exit(1);
            }
        }

        if (host == null || apiKey == null || projectName == null || seqrunId == -1) {
            System.err.println("SeqRunFetcher: Error.");
            System.exit(1);
        }

        SeqRunFetcher client = new SeqRunFetcher(host, apiKey, projectName);
        SeqRunDTO run = client.fetchRun(seqrunId);
        if (run.getIsPaired()) {
            SeqWriterI<DNAQualitySequenceI> writer = new PairedEndFASTQWriter(String.valueOf(seqrunId) + ".fq", QualityEncoding.Sanger);
            SeqWriterI<DNAQualitySequenceI> aWriter = new AsyncWriter<>(Executors.newFixedThreadPool(2), writer);
            UUID session = client.initDownload(seqrunId);
            SequenceDTOList dtos = client.fetchSequences(session);
            while (dtos.getSeqCount() > 0) {
                dtos = client.fetchSequences(session);
                for (SequenceDTO s : dtos.getSeqList()) {
                    DNAQualitySequenceI qseq = new QualityDNASequence();
                    qseq.setName(s.getName().getBytes());
                    qseq.setSequence(s.getSequence().getBytes());
                    qseq.setQuality(s.getQuality().toByteArray());
                    aWriter.addSequence(qseq);
                }
            }
            client.closeDownload(session);
            aWriter.close();
        }
        

        duration = System.currentTimeMillis() - duration;
        System.err.println("Complete after " + duration + " ms.");

    }

}
