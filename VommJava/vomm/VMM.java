package vomm;

import vomm.lib.*;
import org.apache.commons.math3.distribution.*;
import java.io.*;
import java.util.*;


public class VMM implements java.io.Serializable{


    // Non-Unique list of masks
    private ArrayList<ArrayList<Integer>> permutations = new ArrayList<ArrayList<Integer>>();
    // Transfer-Matrix of VOMM
    private ArrayList<Pandas> prob_mats;
    // Counts of Symbol given Contexts
    private ArrayList<Pandas> counts;
    //Initial alphabet may not be correct if new symbols are added
    private String[] alphabet;
    //String[]alphabet transformed to their index in VOMM
    private int[] alpha_pos;
    // Maximal order of VOMM
    private int max_depth;
    //Unique Masks used for fuzzy sampling (filter to acquire contexts that are similar to the input)
    private Set<ArrayList<Integer>> masks = new HashSet<ArrayList<Integer>>();
    //history of samples
    private String generated_history = "";
    //history of seeds
    private String input_history = "";
    //Typicality used for modulating the probability
    public double typicality = 1.0;

    /**
     * Constructor Initializes parameters need for VOMM
     * @param alphabet String[] of strings used in the dataset
     * @param max_depth int maximal order of layers in VOMM
     */
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
        int level = 0;
        while((level <= this.max_depth)) {
            Pandas df = new Pandas(this.alphabet, level);
            this.counts.add(df);
            if (sequence.length()-level >= 0 ){
                this.fillPandas(sequence, level);
            }
            level++;
        }
        this.prob_mats = this.copy(this.counts);
        this.compute_prob_mat();
    }


    public void update_generated_history(String generated){

        generated_history += generated;
        if(generated_history.length()> this.max_depth){
            this.generated_history = this.generated_history.substring(generated_history.length()-max_depth);
        }

    }

    public void update_input_history(String input){

        input_history += input;
        if(input_history.length()> this.max_depth){
            this.input_history = this.input_history.substring(input_history.length()-max_depth);
        }
    }

    /**
     * updates VOMM according to a history and the symbol appearing afterwards
     *
     * @param history String of chars appearing before symbol
     * @param symbol String that appears after history
     */
    public void update(String history, String symbol){
        int depth = history.length() < this.max_depth? history.length(): this.max_depth;
        if(depth == 0){
            this.counts.get(0).incrementValue(history, symbol);

            ArrayList<Double> values = (ArrayList<Double>) this.counts.get(0).getValue(history).clone();
            int dividend = (int)Helper.sum_array(values);
            this.prob_mats.get(0).setValue(history, Helper.divide_array(values, dividend));
        }
        for (int order = 0; order < depth; order++){
            String context = history.substring(history.length()-order);
            System.out.println("in");
            this.counts.get(order).incrementValue(context, symbol);
            int dividend= (int)Helper.sum_array(this.counts.get(order).getValue(context));
            this.prob_mats.get(order).setValue(context, Helper.divide_array(this.counts.get(order).getValue(context), dividend));
        }
    }
    /**
     * Clears stored history of last samples (generate_history)
     */
    public void clearGeneratedHistory(){
        this.generated_history = "";
    }
    public void clearInputHistory(){
        this.input_history = "";
    }
    /**
     * Clears input_history and generated_history
     */
    public void clearWholeHistory(){
        this.input_history = "";
        this.generated_history = "";
    }

    /**
     * Returns probability of Symbol appearing after context
     * @param context String that appeared before symbol
     * @param symbol String that you want to no the probability of appearing from
     * @return probability P(symbol|context)
     */
    public double predict(String context, String symbol){
        if (this.prob_mats.get(context.length()).getValue(context, symbol) == 0){return this.predict(context.substring(1),symbol);}
        return this.prob_mats.get(context.length()).getValue(context, symbol);
    }

    /**
     * Samples Symbol from probabilities of Symbols appearing after empty sequence
     * @param typicality value between 1-0 to decide the typicality of the probabilities
     * @return Symbol sampled from the probability distribution following an empty seed
     */
    public String sample(double typicality){
        ArrayList<Double> probabilities = this.prob_mats.get(0).getValue("");
        probabilities = Helper.modulate(probabilities, typicality);
        Double[] Double_array = new Double[probabilities.size()];
        Double_array = probabilities.toArray(Double_array);
        int i = 0;
        double[] array_probs = new double[Double_array.length];
        for(Double d : Double_array) {
            array_probs[i] = (double)d;
            i++;
        }
        EnumeratedIntegerDistribution dist = new EnumeratedIntegerDistribution(this.prob_mats.get(0).alpha_pos, array_probs);
        int idx = dist.sample();
        String sample = this.alphabet[idx];
        update_generated_history(sample);
        return sample;
    }

    /**
     * Returns one character sampling from probabilities of Symbols appearing after context
     *
     * @param seed Seed used to sample next symbol
     * @param typicality value between 1-0 to decide the typicality of the probabilities
     * @param max_order int Maximal order of VOMM that should be used
     * @return Sampled Symbol
     */

    public String sample(String seed, double typicality, int max_order){
        if (max_order > this.prob_mats.size()){throw new RuntimeException("Context too long");}
        if(max_order < 0){throw new RuntimeException("Negative order impossible");}
        if(seed.length() > max_order){seed = seed.substring(seed.length()-max_order);}

        ArrayList<Double> probabilities = this.prob_mats.get(seed.length()).getValue(seed);
        probabilities = Helper.modulate(probabilities, typicality);
        double sum = Helper.sum_array(probabilities);

        //If the context did not appear reduce order by 1
        if (sum != 1){
            System.out.println(sum); return this.sample(seed.substring(1),typicality, max_order);}
        //Sample-method from apache commons
        Double[] Double_array = new Double[probabilities.size()];
        Double_array = probabilities.toArray(Double_array);
        int i = 0;
        double[] array_probs = new double[Double_array.length];
        for(Double d : Double_array) {
            array_probs[i] = (double)d;
            i++;
        }
        EnumeratedIntegerDistribution dist = new EnumeratedIntegerDistribution(this.prob_mats.get(0).alpha_pos, array_probs);
        int idx = dist.sample();
        String sample = this.alphabet[idx];
        update_generated_history(sample);
        return sample;
    }

    /**
     * Sampling Symbol from the mean probability distribution described by similar contexts to input(Similarity-Measure = Hamming-Distance)
     * @param seed String context to get the similar contexts of     *
     * @param distance Sets the amount of Hamming-distance
     * @param typicality value between 1-0 to decide the typicality of the probabilities
     * @return Sampled symbol
     */
    public String sample_fuzzy(String seed, int distance, double typicality, int max_order){
        // Getting rid of unwanted cases
        if (max_order > this.prob_mats.size())throw new RuntimeException("Context too long");
        if(max_order < 0)throw new RuntimeException("Negative order impossible");
        if(distance > seed.length() || distance < 0)throw new RuntimeException("Illegal distance ");
        if(seed.length() > max_order)seed = seed.substring(seed.length()-max_order);

        /*
        If needed masks not yet generated generate masks
        these masks are ints corresponding to boolean arrays that check if only n-values in the contexts are different
        from the original context (n = distance)
        */
        if (this.masks.size() != seed.length()) this.generate_masks(seed.length(), distance);

        Pandas df = this.prob_mats.get(seed.length());
        ArrayList<Integer> idx;
        List<byte[]> contexts = new ArrayList<byte[]>(this.prob_mats.get(seed.length()).binarized.values());
        byte[] is_similar_to = this.prob_mats.get(seed.length()).binarized.get(seed);

        //If context not appeared escape to order -1
        if(is_similar_to == null){return sample(seed.substring(1),typicality, max_order);}

        //Get Contexts similar to input context by filtering with masks (ln.170) and get mean-probability
        idx = get_similar(is_similar_to, contexts );
        ArrayList<Double> sum = new ArrayList<Double>(Collections.nCopies(df.alphabet.size(), 0.0));
        for(int id: idx){
            ArrayList<Double> probabilities = df.getValue(df.inverse_index_0.get(id));
            probabilities = Helper.modulate(probabilities, typicality);
            for(int i = 0; i < probabilities.size(); i++){
                Double value = sum.get(i);
                sum.set(i, value + probabilities.get(i));
            }
        }
        //getting mean probability
        ArrayList<Double> mean = Helper.divide_array(sum, idx.size());

        //escape if probability does not sum up to 1
        if (Helper.sum_array(mean) != 1){return this.sample(seed.substring(1),typicality, max_order);}

        //sampling from distribution
        Double[] Double_array = new Double[mean.size()];
        Double_array = mean.toArray(Double_array);
        int i = 0;
        double[] array_probs = new double[Double_array.length];
        for(Double d : Double_array) {
            array_probs[i] = (double)d;
            i++;
        }
        EnumeratedIntegerDistribution dist = new EnumeratedIntegerDistribution(this.prob_mats.get(0).alpha_pos, array_probs);
        int id = dist.sample();
        String sample = this.alphabet[id];
        update_generated_history(sample);
        return sample;
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
        this.alphabet = this.counts.get(depth).alphabet.toArray(new String[this.counts.get(depth).alphabet.size()]);
    }

    /**
     * Computes Probability Matrix used for sampling
     */
    private void compute_prob_mat(){
        for(Pandas df: this.prob_mats){
            for(int i = 0; i < df.length(); i++){
                String context = df.getContext(i);
                ArrayList<Double> values = df.getValue(context);
                double sum = Helper.sum_array(values);
                ArrayList<Double> probability = Helper.divide_array(values, sum);
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



    private ArrayList<Pandas> copy(ArrayList<Pandas> src){
        ArrayList<Pandas> dest = new ArrayList<Pandas>();
        for( Pandas original : src) {
            Pandas clone = new Pandas(original);
            dest.add(clone);
        }
        return dest;
    }



    //Getter and Setter methods
    public ArrayList<Pandas> getVMM(){
        return this.prob_mats;
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

    public String getGenerated_history(){
        return this.generated_history;
    }

    public String getInput_history() { return this.input_history; }


    public static VMM loadVMM(String name) {
        try {
            FileInputStream fileIn = new FileInputStream(name);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            VMM vomm = (VMM) in.readObject();
            in.close();
            fileIn.close();
            System.out.println("Loaded Serialized VMM saved in " + name);
            return vomm;
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("VMM class not found");
            c.printStackTrace();
        }
        throw new RuntimeException("Failed loading");
    }

    /**
     * Load VOMM with name vmm.ser in directory
     */
    public static VMM loadVMM() {

        try {
            FileInputStream fileIn = new FileInputStream("vmm.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            VMM vomm = (VMM) in.readObject();
            in.close();
            fileIn.close();
            System.out.println("Loaded Serialized VMM saved in vmm.ser");
            return vomm;
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("VMM class not found");
            c.printStackTrace();
        }
        throw new RuntimeException("Loading failed");
    }


    /**
     * Stores VOMM in vmm.ser
     */
    public void writeVMM() {
        try {
            FileOutputStream fileOut = new FileOutputStream("vmm.ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this);
            out.close();
            fileOut.close();
            System.out.println("Serialized VMM is saved in vmm.ser");

        } catch (IOException i) {
            i.printStackTrace();
        }

    }

    /**
     * Stores Vomm at set directory
     *
     * @param file Directory to save to
     */
    public void writeVMM(String file) {
        try {
            FileOutputStream fileOut = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this);
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in " + file);
        } catch (IOException i) {
            i.printStackTrace();
        }
    }
}


