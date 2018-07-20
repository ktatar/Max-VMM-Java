package vomm;

import org.apache.commons.math3.distribution.*;
import vomm.lib.*;
import java.io.*;
import java.util.*;


public class VMM implements java.io.Serializable{

    private ArrayList<ArrayList<Integer>> permutations = new ArrayList<ArrayList<Integer>>();
    private ArrayList<Pandas> vmm;
    private ArrayList<Pandas> counts;
    private String[] alphabet;
    private int[] alpha_pos;
    private int max_depth;
    private Set<ArrayList<Integer>> masks = new HashSet<ArrayList<Integer>>();
    //Seed usable by sample (Use set method)
    private String seed = "";
    //history of samples
    private String generated_history = "";

    public VMM(String[] alphabet, int max_depth){
        this.alphabet = alphabet;
        //this.vmm = new ArrayList<Pandas>();
        this.counts = new ArrayList<Pandas>();
        this.alpha_pos = new int[alphabet.length];
        this.max_depth = max_depth;
        for (int i = 0; i < alphabet.length; i++){
            this.alpha_pos[i] = i;
        }


    }

    /**
     *
     * Generates VOMM from Dataset of sequences
     * @param sequence Dataset of sequences to learn from
     */
    public void learn(String sequence){
        if (sequence.length() <= max_depth) {
            throw new RuntimeException("Going too deep!");
        }
        for(int level = 0; level <= this.max_depth; level++) {
            Pandas df = new Pandas(this.alphabet, level);
            this.counts.add(df);
            this.fillPandas(sequence, level);
        }
        this.vmm = this.copy(this.counts);
        this.compute_prob_mat();
        this.writeVMM();
    }

    /**
     * updates VOMM according to a history and the symbol appearing afterwards
     *
     * @param history String of chars appearing before symbol
     * @param symbol String that appears after history
     * @param use_seed boolean if Class seed should be used
     * @param use_feedback boolean if class generated history should be used
     */
    public void update(String history, String symbol, boolean use_seed, boolean use_feedback){
        if(use_seed){history = this.seed;}
        if(use_feedback){history = this.seed+this.generated_history;}
        int depth = history.length() < this.max_depth? history.length(): this.max_depth;
        if(depth == 0){
            this.counts.get(0).incrementValue(history, symbol);

            double[] values = Arrays.copyOf(this.counts.get(0).getValue(history), this.alphabet.length);
            int dividend = (int)Helper.sum_array(values);
            this.vmm.get(0).setValue(history, Helper.divide_array(values, dividend));
        }
        for (int order = 0; order < depth; order++){
            String context = history.substring(history.length()-order);
            System.out.println("in");
            this.counts.get(order).incrementValue(context, symbol);
            int divident = (int)Helper.sum_array(this.counts.get(order).getValue(context));
            this.vmm.get(order).setValue(context, Helper.divide_array(this.counts.get(order).getValue(context), divident));
        }
    }

    /**
     * Load VOMM with name vmm.ser in directory
     */
    public void loadVMM(){
        try {
            FileInputStream fileIn = new FileInputStream("vmm.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            this.vmm = (ArrayList) in.readObject();
            in.close();
            fileIn.close();
            System.out.println("Loaded Serialized VMM saved in vmm.ser");
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("VMM class not found");
            c.printStackTrace();
        }
    }

    /**
     * Loading VOMM from directory name
     * @param name Directory to load VOMM from
     */
    public void loadVMM(String name){
        try {
            FileInputStream fileIn = new FileInputStream(name);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            this.vmm = (ArrayList) in.readObject();
            in.close();
            fileIn.close();
            System.out.println("Loaded Serialized VMM saved in "+ name);
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("VMM class not found");
            c.printStackTrace();
        }
    }

    /**
     * Returns probability of Symbol appearing after context
     * @param context String that appeared before symbol
     * @param symbol String that you want to no the probability of appearing from
     * @return probability P(symbol|context)
     */
    public double predict(String context, String symbol){
        if (this.vmm.get(context.length()).getValue(context, symbol) == 0){return this.predict(context.substring(1),symbol);}
        return this.vmm.get(context.length()).getValue(context, symbol);
    }

    /**
     * Samples Symbol from probabilities of Symbols appearing after empty sequence
     * @param modularity value between 1-0 to decide the modularity of the probabilities
     * @param keep_history boolean if sampled String should be added to generated history
     * @return Symbol sampled from the probability distribution following an empty seed
     */
    public String sample(double modularity,
                         boolean keep_history){
        double[] probabilities = this.vmm.get(0).getValue("");
        probabilities = Helper.modulate(probabilities, modularity);
        EnumeratedIntegerDistribution dist = new EnumeratedIntegerDistribution(this.alpha_pos, probabilities);
        int idx = dist.sample();
        String sample = this.alphabet[idx];
        if(keep_history){this.generated_history += sample;}
        return sample;
    }

    /**
     * Returns one character sampling from probabilities of Symbols appearing after context
     *
     * @param seed Seed used to sample next symbol
     * @param modularity value between 1-0 to decide the modularity of the probabilities
     * @param max_order int Maximal order of VOMM that should be used
     * @param use_seed boolean if true internal seed is used
     * @param use_feedback boolean if true will use whole history not just seed to sample
     * @param keep_history boolean if true adds sample to this.generated_history
     * @return Sampled Symbol
     */
    public String sample(String seed, double modularity, int max_order,
                         boolean use_seed, boolean use_feedback, boolean keep_history){
        if (max_order > this.vmm.size()){throw new RuntimeException("Context too long");}
        if(max_order < 0){throw new RuntimeException("Negative order impossible");}
        if(use_seed){seed = this.seed;}
        if(use_feedback){seed = this.seed+this.generated_history;}
        if(seed.length() > max_order){seed = seed.substring(seed.length()-max_order);}

        double[] probabilities = this.vmm.get(seed.length()).getValue(seed);
        probabilities = Helper.modulate(probabilities, modularity);
        double sum = Helper.sum_array(probabilities);

        //If the context did not appear reduce order by 1
        if (sum != 1){
            System.out.println(sum); return this.sample(seed.substring(1),modularity, max_order, false, false, keep_history);}
        //Sample-method from apache commons
        EnumeratedIntegerDistribution dist = new EnumeratedIntegerDistribution(this.alpha_pos, probabilities);
        int idx = dist.sample();
        String sample = this.alphabet[idx];
        if(keep_history){this.generated_history += sample;}
        return sample;
    }

    /**
     * Sampling Symbol from the mean probability distribution described by similar contexts to input(Similarity-Measure = Hamming-Distance)
     * @param seed String context to get the similar contexts of     *
     * @param distance Sets the amount of Hamming-distance
     * @param modularity value between 1-0 to decide the modularity of the probabilities
     * @param use_seed boolean if true internal seed is used
     * @param use_feedback boolean if true will use whole history not just seed to sample
     * @param keep_history boolean if true adds sample to this.generated_history
     * @return Sampled symbol
     */
    public String sample_fuzzy(String seed, int distance, double modularity, int max_order,
                               boolean use_seed, boolean use_feedback, boolean keep_history){
        // Getting rid of unwanted cases
        if (max_order > this.vmm.size())throw new RuntimeException("Context too long");
        if(max_order < 0)throw new RuntimeException("Negative order impossible");
        if(use_seed)seed = this.seed;
        if(use_feedback)seed = this.seed+this.generated_history;
        if (seed.length() == 0)return sample(modularity,keep_history);
        if(distance > seed.length() || distance < 0)throw new RuntimeException("Illegal distance ");
        if(seed.length() > max_order)seed = seed.substring(seed.length()-max_order);

        //If needed masks not yet generated generate masks
        if (this.masks.size() != seed.length()) this.generate_masks(seed.length(), distance);

        Pandas df = this.vmm.get(seed.length());
        ArrayList<Integer> idx;
        List<byte[]> contexts = new ArrayList<byte[]>(this.vmm.get(seed.length()).binarized.values());
        byte[] is_similar_to = this.vmm.get(seed.length()).binarized.get(seed);

        //If context not appeared escape to order -1
        if(is_similar_to == null){return sample(seed.substring(1),modularity, max_order, false, false, keep_history);}

        //Get Contexts similar to input context and get mean-probability
        idx = get_similar(is_similar_to, contexts );
        double[] sum = new double[this.alphabet.length];
        for(int id: idx){
            double[] probabilities = df.getValue(df.inverse_index_0.get(id));
            probabilities = Helper.modulate(probabilities, modularity);
            for(int i = 0; i < sum.length; i++){
                sum[i] += probabilities[i];
            }
        }
        double[]mean = Helper.divide_array(sum, idx.size());
        if (Helper.sum_array(sum) != 1){return this.sample(seed.substring(1),modularity, max_order, false, false, keep_history);}
        EnumeratedIntegerDistribution dist = new EnumeratedIntegerDistribution(this.alpha_pos, mean);
        int id = dist.sample();
        String sample = this.alphabet[id];
        if(keep_history){this.generated_history += sample;}
        return sample;
    }

    public ArrayList<Pandas> getVMM(){
        return this.vmm;
    }

    public ArrayList<Pandas> getCounts(){
        return this.counts;
    }

    public String[] getAlphabet(){
        return this.alphabet;
    }

    public int getMax_order(){
        return this.max_depth;
    }

    public String getSeed(){
        return this.seed;
    }

    public void setSeed(String seed){
        this.seed = seed;
    }

    public String getGenerated_history(){
        return this.generated_history;
    }

    public String getWholeHistory(){
        return this.seed+this.generated_history;
    }

    public void clearSeed(){
        this.seed = "";
    }

    public void clearGenerate_History(){
        this.generated_history = "";
    }

    public void clearWholeHistory(){
        this.seed = "";
        this.generated_history = "";
    }



    /**
     * Updates DataFrame representing VOMM at order depth
     * @param seq Data to learn from
     * @param depth VOMM order to update
     */
    private void fillPandas(String seq, int depth){
        int extra_depth = depth +1;
        for (int i = 0; i <= seq.length() - extra_depth; i++) {
            String context = seq.substring(i, i + (depth));
            String symbol = Character.toString(seq.charAt(i + depth));
            this.counts.get(depth).incrementValue(context, symbol);
        }
    }

    /**
     * Computes Probability Matrix used for sampling
     */
    private void compute_prob_mat(){
        for(Pandas df: this.vmm){
            for(int i = 0; i < df.length(); i++){
                String context = df.getContext(i);
                double[] values = df.getValue(context);
                double sum = Helper.sum_array(values);
                double[] probability = Helper.divide_array(values, sum);
                df.setValue(context, probability);

            }
        }
    }

    /**
     * Generates Masks for finding contexts with certain Hamming-distance
     * @param length length of of context
     * @param distance Hamming-Distance
     */
    private void generate_masks(int length, int distance){
        Integer[]ones = new Integer[length-distance];
        Arrays.fill(ones, Integer.MAX_VALUE);
        Integer []zeros = new Integer[distance];
        Arrays.fill(zeros, 0);
        ArrayList<Integer> alphabet = new ArrayList<Integer>();
        alphabet.addAll(Arrays.asList(ones));
        alphabet.addAll(Arrays.asList(zeros));
        permutingArray(alphabet, 0);
        this.masks = new HashSet<ArrayList<Integer>>(this.permutations);


    }

    /**
     * Used to create all possible masks referring to a set Hamming-Distance
     * @param arrayList
     * @param element
     */
    private void permutingArray(ArrayList<Integer> arrayList, int element) {
        for (int i = element; i < arrayList.size(); i++){
            java.util.Collections.swap(arrayList, i, element);
            permutingArray(arrayList, element + 1);
            java.util.Collections.swap(arrayList, element, i);
        }
        if (element == arrayList.size() - 1) {
            ArrayList<Integer> list3 = new ArrayList<Integer>(arrayList);
            this.permutations.add(list3);
        }
    }

    /**
     * Returns list of indices of the contexts that are similar to a certain context
     * @param is_similar_to context to compare with
     * @param contexts list of all possible contexts
     * @return
     */
    private ArrayList<Integer> get_similar(byte[] is_similar_to, List<byte[]> contexts) {
        ArrayList<Integer> idx = new ArrayList<Integer>();
        boolean is_similar = false;
        for(ArrayList<Integer>mask : this.masks){
            for(int id = 0; id < contexts.size(); id++){
                byte[] context = contexts.get(id);
                is_similar = false;
                for(int i = 0; i < context.length; i++){
                    if(is_similar){
                        idx.add(id);
                        break;
                    }
                    is_similar = ((is_similar_to[i] & mask.get(i)) & context[i]) == (is_similar_to[i] & mask.get(i));
                }
            }
        }
        return idx;
    }

    /**
     * Stores VOMM in vmm.ser
     */
    public void writeVMM(){
        try{
            FileOutputStream fileOut = new FileOutputStream("vmm.ser");
            ObjectOutputStream out= new ObjectOutputStream(fileOut);
            out.writeObject(this.vmm);
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in /tmp/vmm.ser");
        } catch (IOException i) {
            i.printStackTrace();
        }

    }

    /**
     * Stores Vomm at set directory
     * @param file Directory to save to
     */
    public void writeVMM(String file){
        try{
            FileOutputStream fileOut = new FileOutputStream(file);
            ObjectOutputStream out= new ObjectOutputStream(fileOut);
            out.writeObject(this.vmm);
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in " + file);
        } catch (IOException i) {
            i.printStackTrace();
        }

    }

    private ArrayList<Pandas> copy(ArrayList<Pandas> src){
        ArrayList<Pandas> dest = new ArrayList<Pandas>();
        for(int i = 0; i <= this.max_depth; i++){
            Pandas df = new Pandas(this.alphabet, i);
            dest.add(df);
        }
        for( Pandas original : src) {
            for (String context: original.inverse_index_0.values()) {
                double[] values = Arrays.copyOf(original.getValue(context), this.alphabet.length);
                dest.get(context.length()).setValue(context, values);
            }
        }
        return dest;
    }


    /*
        public String sample_typicality(double typicality, boolean keep_history){
            //Cas-Handling
            if (typicality <0 || typicality > 1){throw new RuntimeException("typicality not in range");}

            double[] probs = this.vmm.get(0).getValue("");

            //Find Maximum and Minimum
            int maxAt = 0;
            for (int i = 0; i < probs.length; i++) {
                maxAt = probs[i] > probs[maxAt] ? i : maxAt;
            }
            int minAt = 0;

            for (int i = 0; i < probs.length; i++) {
                minAt = probs[i] < probs[minAt] ? i : minAt;
            }

            //Sample according to typicality
            int[] idx = {minAt, maxAt};
            double[] probabilities = {1-typicality, typicality};
            EnumeratedIntegerDistribution dist = new EnumeratedIntegerDistribution(idx, probabilities);
            int id = dist.sample();
            String sample = this.alphabet[id];
            if(keep_history){this.generated_history += sample;}
            return sample;
        }

        public String sample_typicality(String seed, double typicality, int max_order,
                                        boolean use_seed, boolean use_feedback, boolean keep_history){
            //Case-Handling
            if (typicality <0 || typicality > 1) throw new RuntimeException("typicality not in range");
            if (max_order > this.vmm.size()) throw new RuntimeException("Context too long");
            if(max_order < 0) throw new RuntimeException("Negative order impossible");
            if(use_seed) seed = this.seed;
            if(use_feedback) seed = this.seed + this.generated_history;
            if(seed.length() > max_order) seed = seed.substring(seed.length() - max_order);

            //get probabilities
            double[] probs = this.vmm.get(seed.length()).getValue(seed);
            double sum = Helper.sum_array(probs);
            //If the context did not appear reduce order by 1
            if (sum != 1){return this.sample(seed.substring(1),typicality, max_order, false, false, keep_history);}

            //find Maximum and Minimum in Distribution
            int maxAt = 0;
            for (int i = 0; i < probs.length; i++) maxAt = probs[i] > probs[maxAt] ? i : maxAt;
            int minAt = 0;

            for (int i = 0; i < probs.length; i++) minAt = probs[i] < probs[minAt] ? i : minAt;
            int[] idx = {minAt, maxAt};
            double[] probabilities = {1-typicality, typicality};
            EnumeratedIntegerDistribution dist = new EnumeratedIntegerDistribution(idx, probabilities);
            int id = dist.sample();
            String sample = this.alphabet[id];
            if(keep_history){this.generated_history += sample;}
            return sample;

        }

        public String sample_typicality_fuzzy(String seed, int distance, double typicality, int max_order,
                                              boolean use_seed, boolean use_feedback, boolean keep_history) {
            // Getting rid of unwanted cases
            if (max_order > this.vmm.size()) throw new RuntimeException("Context too long");
            if (max_order < 0) throw new RuntimeException("Negative order impossible");
            if (use_seed) seed = this.seed;
            if (use_feedback) seed = this.seed + this.generated_history;
            if (seed.length() == 0) return sample(typicality, keep_history);
            if (distance > seed.length() || distance < 0) throw new RuntimeException("Illegal distance ");
            if (seed.length() > max_order) seed = seed.substring(seed.length() - max_order);

            //If needed masks not yet generated generate masks
            if (this.masks.size() != seed.length()) generate_masks(seed.length(), distance);

            Pandas df = this.vmm.get(seed.length());
            ArrayList<Integer> idx;
            List<byte[]> contexts = new ArrayList<byte[]>(this.vmm.get(seed.length()).binarized.values());
            byte[] is_similar_to = this.vmm.get(seed.length()).binarized.get(seed);

            //If context not appeared escape to order -1
            if (is_similar_to == null) {
                return sample_typicality_fuzzy(seed.substring(1),distance-1, typicality, max_order, false, false, keep_history);
            }

            //Get Contexts similar to input context and get mean-probability
            idx = get_similar(is_similar_to, contexts);
            double[] sum = new double[this.alphabet.length];
            for (int id : idx) {
                double[] probabilities = df.getValue(df.inverse_index_0.get(id));
                probabilities = Helper.modulate(probabilities, typicality);
                for (int i = 0; i < sum.length; i++) {
                    sum[i] += probabilities[i];
                }
            }
            double[] probs = Helper.divide_array(sum, idx.size());
            if (Helper.sum_array(probs) != 1) {
                return this.sample_typicality_fuzzy(seed.substring(1), distance-1, typicality, max_order, false, false, keep_history);
            }
            int maxAt = 0;
            for (int i = 0; i < probs.length; i++) {
                maxAt = probs[i] > probs[maxAt] ? i : maxAt;
            }
            int minAt = 0;

            for (int i = 0; i < probs.length; i++) {
                minAt = probs[i] < probs[minAt] ? i : minAt;
            }
            int[] ids = {minAt, maxAt};
            double[] probabilities = {1-typicality, typicality};
            EnumeratedIntegerDistribution dist = new EnumeratedIntegerDistribution(ids, probabilities);
            int id = dist.sample();
            String sample = this.alphabet[id];
            if(keep_history){this.generated_history += sample;}
            return sample;
        }
    */
}