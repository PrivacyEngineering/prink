package pringtest.datatypes;

import org.apache.flink.api.java.tuple.*;
import pringtest.generalizations.AggregationGeneralizer;
import pringtest.generalizations.NonNumericalGeneralizer;
import pringtest.generalizations.ReductionGeneralizer;

import java.util.*;
import java.util.stream.Collectors;

public class Cluster {

    private final CastleRule[] config;

    private final ArrayList<Tuple> entries = new ArrayList<>();

    private final AggregationGeneralizer aggreGeneralizer;
    private final ReductionGeneralizer reductGeneralizer;
    private final NonNumericalGeneralizer nonNumGeneralizer;

    // DEBUG params
    boolean showRemoveEntry = false;
    boolean showAddedEntry = false;
    boolean showInfoLoss = false;
    boolean showEnlargement = false;

    public Cluster(CastleRule[] rules) {
        this.config = rules;
        aggreGeneralizer = new AggregationGeneralizer(config);
        reductGeneralizer = new ReductionGeneralizer(this);
        nonNumGeneralizer = new NonNumericalGeneralizer(config);
    }

    public Float enlargementValue(Cluster input) {
        if(entries.size() <= 0) System.out.println("ERROR: enlargementValue(Cluster) called on cluster with size: 0 cluster:" + this);
        if(showEnlargement) System.out.println("Enlargement Value Cluster:" + (informationLossWith(input) - infoLoss()));
        return informationLossWith(input) - infoLoss();
    }

    public Float enlargementValue(Tuple input) {
        if(entries.size() <= 0) System.out.println("ERROR: enlargementValue(tuple) called on cluster with size: 0 cluster:" + this);
        if(showEnlargement) System.out.println("Enlargement Value Tuple:" + (informationLossWith(input) - infoLoss()));
        return informationLossWith(input) - infoLoss();
    }

    public float informationLossWith(Cluster input) {
        return informationLossWith(input.getAllEntries());
    }

    public float informationLossWith(Tuple input) {
        return informationLossWith(Collections.singletonList(input));
    }

    private float informationLossWith(List<Tuple> input) {
        if(entries.size() <= 0) System.out.println("ERROR: informationLossWith() called on cluster with size: 0 cluster:" + this);
        double[] infoLossWith = new double[config.length];

        for (int i = 0; i < config.length; i++) {
            switch (config[i].getGeneralizationType()) {
                case NONE:
                    infoLossWith[i] = 0;
                    break;
                case REDUCTION:
                case REDUCTION_WITHOUT_GENERALIZATION:
                    infoLossWith[i] = reductGeneralizer.generalize(input, i).f1;
                    break;
                case AGGREGATION:
                case AGGREGATION_WITHOUT_GENERALIZATION:
                    infoLossWith[i] = aggreGeneralizer.generalize(input, i).f1;
                    break;
                case NONNUMERICAL:
                case NONNUMERICAL_WITHOUT_GENERALIZATION:
                    infoLossWith[i] = nonNumGeneralizer.generalize(input, i).f1;
                    break;
                default:
                    System.out.println("ERROR: inside Cluster: undefined transformation type:" + config[i]);
            }
        }
        double sumWith = Arrays.stream(infoLossWith).sum();
        if(showInfoLoss) System.out.println("InfoLossTuple with: " + Arrays.toString(infoLossWith) + " Result:" + ((float) sumWith) / config.length);
        return ((float) sumWith) / config.length;
    }

    public float infoLoss() {
        if(entries.size() <= 0) System.out.println("ERROR: infoLoss() called on cluster with size: 0 cluster:" + this);
        double[] infoLoss = new double[config.length];

        for (int i = 0; i < config.length; i++) {
            switch (config[i].getGeneralizationType()) {
                case NONE:
                    infoLoss[i] = 0;
                    break;
                case REDUCTION:
                case REDUCTION_WITHOUT_GENERALIZATION:
                    infoLoss[i] = reductGeneralizer.generalize(i).f1;
                    break;
                case AGGREGATION:
                case AGGREGATION_WITHOUT_GENERALIZATION:
                    infoLoss[i] = aggreGeneralizer.generalize(i).f1;
                    break;
                case NONNUMERICAL:
                case NONNUMERICAL_WITHOUT_GENERALIZATION:
                    infoLoss[i] = nonNumGeneralizer.generalize(i).f1;
                    break;
                default:
                    System.out.println("ERROR: inside Cluster: undefined transformation type:" + config[i]);
            }
        }
        double sumWith = Arrays.stream(infoLoss).sum();
        if(showInfoLoss) System.out.println("InfoLoss with: " + Arrays.toString(infoLoss) + " Result:" + ((float) sumWith) / config.length);
        return ((float) sumWith) / config.length;
    }

    public Tuple generalize(Tuple input) {

        // Return new tuple with generalized field values
        int inputArity = input.getArity();
        Tuple output = Tuple.newInstance(inputArity);

        for (int i = 0; i < Math.min(inputArity, config.length); i++) {
            switch (config[i].getGeneralizationType()) {
                case REDUCTION:
                    output.setField(reductGeneralizer.generalize(i).f0, i);
                    break;
                case AGGREGATION:
                    output.setField(aggreGeneralizer.generalize(i).f0, i);
                    break;
                case NONNUMERICAL:
                    output.setField(nonNumGeneralizer.generalize(i).f0, i);
                    break;
                case NONE:
                case REDUCTION_WITHOUT_GENERALIZATION:
                case AGGREGATION_WITHOUT_GENERALIZATION:
                case NONNUMERICAL_WITHOUT_GENERALIZATION:
                    output.setField(input.getField(i), i);
                    break;
                default:
                    System.out.println("ERROR: inside Cluster: undefined transformation type:" + config[i]);
            }
        }
        return output;
    }

    public void addEntry(Tuple input) {
        if (showAddedEntry) System.out.println("Added " + input.toString() + " to cluster: " + this.toString() + " size:" + this.entries.size());
        entries.add(input);
        // Update the aggregation boundaries
        aggreGeneralizer.updateAggregationBounds(input);
        nonNumGeneralizer.updateTree(input);
    }

    public void addAllEntries(ArrayList<Tuple> input) {
        if (showAddedEntry) System.out.println("Added multiple (" + input.size() + ") to cluster: " + this.toString() + " size:" + this.entries.size());
        entries.addAll(input);
        // Update the aggregation boundaries for all inputs
        for (Tuple in : input) {
            aggreGeneralizer.updateAggregationBounds(in);
            nonNumGeneralizer.updateTree(in);
        }
    }

    public void removeEntry(Tuple input) {
        // TODO check if boundaries need to be adjusted when removing tuples
        entries.remove(input);
        if(showRemoveEntry) System.out.println("Cluster: removeEntry -> new size:" + entries.size());
    }

    public void removeAllEntries(ArrayList<Tuple> idTuples) {
        // TODO check if boundaries need to be adjusted when removing tuples
        entries.removeAll(idTuples);
        if(showRemoveEntry) System.out.println("Cluster: removeAllEntry -> new size:" + entries.size());
    }

    public ArrayList<Tuple> getAllEntries() {
        return entries;
    }

    public boolean contains(Tuple input) {
        return entries.contains(input);
    }

    public int size() {
        return entries.size();
    }

    /**
     * Returns the diversity of the cluster entries
     * @return cluster diversity
     */
    public int diversity(int[] posSensibleAttributes) {
        if(posSensibleAttributes.length <= 0) return 0;
        if(posSensibleAttributes.length == 1){
            // TODO test if correct
            // Return the amount of different values inside the sensible attribute
            Set<String> output = new HashSet<>();
            for(Tuple tuple: entries) output.add(tuple.getField(posSensibleAttributes[0]));
            return output.size();
        }else{
            // See for concept: https://mdsoar.org/bitstream/handle/11603/22463/A_Privacy_Protection_Model_for_Patient_Data_with_M.pdf?sequence=1
            ArrayList<Tuple> entriesCopy = (ArrayList<Tuple>) entries.clone();
            List<Tuple2<Integer, Map.Entry<Object, Long>>> numOfAppearances = new ArrayList<>();
            int counter = 0;

            while (entriesCopy.size() > 0) {
                counter++;
                for (int pos : posSensibleAttributes) {
                    // TODO check if two strings are added to the same grouping if they have the same value but are different objects
                    Map.Entry<Object, Long> temp = entriesCopy.stream().collect(Collectors.groupingBy(s -> s.getField(pos), Collectors.counting()))
                            .entrySet().stream().max((attEntry1, attEntry2) -> attEntry1.getValue() > attEntry2.getValue() ? 1 : -1).get();
                    numOfAppearances.add(Tuple2.of(pos, temp));
                }
                Tuple2<Integer, Map.Entry<Object, Long>> mapEntryToDelete = numOfAppearances.stream().max((attEntry1, attEntry2) -> attEntry1.f1.getValue() > attEntry2.f1.getValue() ? 1 : -1).get();
//                System.out.println("Least diverse attribute value:" + mapEntryToDelete.toString() + " Counter:" + counter + " CopySize:" + entriesCopy.size() + " OriginalSize:" + entries.size());
                // Remove all entries that have the least diverse attribute
                entriesCopy.removeIf(i -> i.getField(mapEntryToDelete.f0).equals(mapEntryToDelete.f1.getKey()));
                numOfAppearances.clear();
            }
//            System.out.println("Diversity:" + counter);
            return counter;
        }
    }
}
