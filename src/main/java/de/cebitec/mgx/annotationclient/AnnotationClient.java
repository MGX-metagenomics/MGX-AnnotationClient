/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.annotationclient;

import de.cebitec.gpms.rest.RESTAccessI;
import de.cebitec.gpms.rest.RESTException;
import de.cebitec.mgx.annotationclient.model.Bin;
import de.cebitec.mgx.dto.dto.AssemblyDTO;
import de.cebitec.mgx.dto.dto.BinDTO;
import de.cebitec.mgx.dto.dto.ContigDTO;
import de.cebitec.mgx.dto.dto.GeneCoverageDTO;
import de.cebitec.mgx.dto.dto.GeneCoverageDTOList;
import de.cebitec.mgx.dto.dto.GeneDTO;
import de.cebitec.mgx.dto.dto.MGXLong;
import de.cebitec.mgx.dto.dto.SequenceDTO;
import de.cebitec.mgx.restgpms.JAXRSRESTAccess;
import de.cebitec.mgx.sequence.DNASequenceI;
import de.cebitec.mgx.sequence.SeqReaderFactory;
import de.cebitec.mgx.sequence.SeqReaderI;
import gnu.getopt.Getopt;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotationClient {

    //private final URI host;
    private final String apiKey;
    private final String projectName;
    private final RESTAccessI rest;

    public AnnotationClient(URI host, long[] runIds, String apiKey, String projectName) {
        this.apiKey = apiKey;
        this.projectName = projectName;
        this.rest = new JAXRSRESTAccess(null, host, false);
        rest.addFilter(new APIKeyFilter(this.apiKey));
    }

    public long createAssembly(String assemblyName, List<File> binFiles, long numReadsAssembled) throws Exception {
        int n50 = N50.n50(binFiles.toArray(new File[]{}));
        AssemblyDTO assembly = AssemblyDTO.newBuilder()
                .setName(assemblyName)
                .setReadsAssembled(numReadsAssembled)
                .setN50(n50)
                .build();
        MGXLong assemblyId = rest.put(assembly, MGXLong.class, projectName, "AnnotationService", "createAssembly");
        return assemblyId.getValue();
    }

    public Map<String, Integer> loadContigCoverage(File f) throws IOException {
        Map<String, Integer> contigCoverage = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                String[] split = line.split("\t");
                contigCoverage.put(split[0], Integer.parseInt(split[1]));
            }
        }

        if (!contigCoverage.containsKey("total")) {
            throw new RuntimeException("no value for total coverage found.");
        }
        return contigCoverage;
    }

    public List<File> getBinFiles(File checkmReport) throws IOException {
        List<File> fNames = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(checkmReport))) {
            br.readLine(); // skip over header
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                String[] split = line.split("\t");
                fNames.add(new File(checkmReport.getParentFile(), split[0] + ".fas"));
            }
        }
        return fNames;
    }

    public List<Bin> createBins(File f, long assemblyId) throws Exception {

        List<Bin> bins = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            br.readLine(); // skip over header
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                String[] split = line.split("\t");

                if (split[0].endsWith("lowDepth")) {
                    continue;
                }

                int n50;
                String taxonomy;
                File binFasta = new File(f.getParentFile(), split[0] + ".fas");
                n50 = N50.n50(binFasta);
                File taxFile = new File(f.getParentFile(), split[0] + ".tax");
                taxonomy = readTaxFile(taxFile);

                String binName;
                if (split[0].contains("unbinned") || split[0].contains("Unbinned")) {
                    binName = "Unbinned";
                } else {
                    int binNumber = Integer.valueOf(split[0].substring(split[0].lastIndexOf(".") + 1));
                    binName = "Bin " + binNumber;
                }
                //String taxonomy = split[1];

                float completeness = Float.valueOf(split[11]);
                float contamination = Float.valueOf(split[12]);
                BinDTO binDTO = BinDTO.newBuilder()
                        .setName(binName)
                        .setCompleteness(completeness)
                        .setContamination(contamination)
                        .setAssemblyId(assemblyId)
                        .setN50(n50)
                        .setTaxonomy(taxonomy)
                        .build();
                MGXLong binId = rest.put(binDTO, MGXLong.class, projectName, "AnnotationService", "createBin");

                Bin bin = new Bin();
                bin.setId(binId.getValue());
                bin.setName(binName);
                bin.setFASTA(binFasta);

                bins.add(bin);

            }
        }
        return bins;
    }

    public Map<String, Integer> readGeneCoverage(File featCount) throws IOException {
        Map<String, Integer> geneCov = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(featCount))) {
            String line;
            while (null != (line = br.readLine())) {
                if (line.startsWith("#") || line.startsWith("Geneid")) {
                    continue;
                }
                String[] elems = line.split("\t");
                geneCov.put(elems[0], Integer.parseInt(elems[6]));
            }
        }
        return geneCov;
    }

    public Map<String, Long> sendGenes(File gff, Map<String, Long> contigIds, Map<String, Integer> totalGeneCoverage) throws Exception {
        Map<String, Long> geneIds = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(gff))) {
            String line;
            while (null != (line = br.readLine())) {
                if (line.startsWith("#")) {
                    continue;
                }
                String[] elems = line.split("\t");
                long contigId = contigIds.get(elems[0]);
                int from = Integer.valueOf(elems[3]) - 1;
                int to = Integer.valueOf(elems[4]) - 1;
                String name = elems[8].split(";")[0].substring(3); // ID=4_1;partial=10;start_type=
                GeneDTO gene = GeneDTO.newBuilder()
                        .setContigId(contigId)
                        .setStart(from)
                        .setStop(to)
                        .setCoverage(totalGeneCoverage.containsKey(name) ? totalGeneCoverage.get(name) : 0)
                        .build();
                MGXLong geneId = rest.put(gene, MGXLong.class, projectName, "AnnotationService", "createGene");
                geneIds.put(elems[0], geneId.getValue());
            }
        }
        return geneIds;
    }

    public Map<String, Long> sendContigs(Bin bin, Map<String, Integer> contigCoverage, Map<String, Long> contigIds) throws Exception {

        SeqReaderI<? extends DNASequenceI> reader = SeqReaderFactory.getReader(bin.getFASTA().getAbsolutePath());
        while (reader.hasMoreElements()) {
            DNASequenceI seq = reader.nextElement();
            String seqName = new String(seq.getName());
            ContigDTO contig = ContigDTO.newBuilder()
                    .setBinId(bin.getId())
                    .setGc(GC.gc(seq))
                    .setLengthBp(seq.getSequence().length)
                    .setCoverage(contigCoverage.containsKey(seqName) ? contigCoverage.get(seqName) : 0)
                    .setName(seqName)
                    .build();

            MGXLong contigId = rest.put(contig, MGXLong.class, projectName, "AnnotationService", "createContig");
            SequenceDTO dto = SequenceDTO.newBuilder()
                    .setName(seqName)
                    .setSequence(new String(seq.getSequence()))
                    .build();
            rest.put(dto, projectName, "AnnotationService", "appendSequence", String.valueOf(bin.getId()));
            contigIds.put(seqName, contigId.getValue());
        }
        return contigIds;
    }

    private static String readTaxFile(File f) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            return br.readLine();
        }
    }

    public void sendGeneCoverage(long runId, Map<String, Long> geneIds, Map<String, Integer> geneCoverage) throws RESTException {
        int num = 0;
        GeneCoverageDTOList.Builder b = GeneCoverageDTOList.newBuilder();

        for (Map.Entry<String, Integer> me : geneCoverage.entrySet()) {
            GeneCoverageDTO covInfo = GeneCoverageDTO.newBuilder()
                    .setGeneId(geneIds.get(me.getKey()))
                    .setRunId(runId)
                    .setCoverage(me.getValue())
                    .build();
            b.addGeneCoverage(covInfo);
            num++;

            if (num == 100) {
                rest.put(b.build(), projectName, "AnnotationService", "createGeneCoverage");
                b = GeneCoverageDTOList.newBuilder();
                num = 0;
            }
        }
        if (num > 0) {
            rest.put(b.build(), projectName, "AnnotationService", "createGeneCoverage");
        }
    }

    public void finishJob() throws RESTException {
        rest.get(projectName, "AnnotationService", "finishJob");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {

        long duration = System.currentTimeMillis();
        /*
         * -h Host
         * -j job id
         * -a API key
         * -p project name
         */

        URI host = null;
        String assemblyName = null, apiKey = null, projectName = null, dirName = null;
        long[] seqrunIds = null;
        Getopt g = new Getopt("AnnotationService", args, "n:h:a:p:s:d:");
        int c;
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'n':
                    assemblyName = g.getOptarg();
                    break;
                case 'h':
                    host = URI.create(g.getOptarg());
                    break;
                case 'a':
                    apiKey = g.getOptarg();
                    break;
                case 'p':
                    projectName = g.getOptarg();
                    break;
                case 's':
                    String elems[] = g.getOptarg().split(",");
                    seqrunIds = new long[elems.length];
                    for (int i = 0; i < elems.length; i++) {
                        seqrunIds[i] = Long.parseLong(elems[i]);
                    }
                    break;
                case 'd':
                    dirName = g.getOptarg();
                    break;
                default:
                    System.exit(1);
            }
        }

        if (assemblyName == null || host == null || apiKey == null || projectName == null || seqrunIds == null) {
            System.err.println("Error.");
            System.exit(1);
        }

        File dir = new File(dirName);
        if (!dir.exists() || !dir.isDirectory() || !dir.canRead()) {
            System.err.println("Cannot access assembly directory " + dir.getAbsolutePath());
            System.exit(1);
        }

        File contigCov = new File(dir, "total_mapped.cov");
        if (!contigCov.exists() || !contigCov.isFile() || !contigCov.canRead()) {
            System.err.println("Cannot access contig coverage file " + contigCov.getAbsolutePath());
            System.exit(1);
        }

        File checkmReport = new File(dir, "checkm.tsv");
        if (!checkmReport.exists() || !checkmReport.isFile() || !checkmReport.canRead()) {
            System.err.println("Cannot access checkm report " + checkmReport.getAbsolutePath());
            System.exit(1);
        }

        for (Long runId : seqrunIds) {
            File geneCoverage = new File(dir, runId.toString() + ".tsv");
            if (!geneCoverage.exists() && geneCoverage.canRead()) {
                System.err.println("Cannot access coverage file " + geneCoverage.getAbsolutePath());
                System.exit(1);
            }
        }

        File gtf = new File(dir, "final.contigs.gff");
        if (!gtf.exists() || !gtf.isFile() || !gtf.canRead()) {
            System.err.println("Cannot access GTF file " + gtf.getAbsolutePath());
            System.exit(1);
        }

        AnnotationClient client = new AnnotationClient(host, seqrunIds, apiKey, projectName);

        Map<String, Integer> contigCoverage = client.loadContigCoverage(contigCov);
        List<File> binFiles = client.getBinFiles(checkmReport);

        long assemblyId = client.createAssembly(assemblyName, binFiles, contigCoverage.get("total"));
        System.err.println("Created assembly id " + assemblyId);

        List<Bin> bins = client.createBins(checkmReport, assemblyId);
        System.err.println("Created " + bins.size() + " bins.");

        Map<String, Long> contigIds = new HashMap<>();
        for (Bin bin : bins) {
            client.sendContigs(bin, contigCoverage, contigIds);
        }
        System.err.println("Sent FASTA sequences.");

        Map<String, Integer> totalGeneCoverage = client.readGeneCoverage(new File(dir, "featureCounts_total.tsv"));

        Map<String, Long> geneIds = client.sendGenes(gtf, contigIds, totalGeneCoverage);
        System.err.println("Created " + geneIds.size() + " genes.");

        for (long runId : seqrunIds) {
            Map<String, Integer> geneCoverage = client.readGeneCoverage(new File(dir, String.valueOf(runId) + ".tsv"));
            client.sendGeneCoverage(runId, geneIds, geneCoverage);
        }
        System.err.println("Created gene coverage data.");

        client.finishJob();
        System.err.println("Job set to FINISHED state");

        duration = System.currentTimeMillis() - duration;
        System.err.println("Complete after " + duration + " ms.");

    }

}
