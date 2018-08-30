package vomm;

//import com.cycling74.max.Atom;
import vomm.lib.*;
import org.apache.commons.math3.distribution.*;
import java.io.*;
import java.util.*;

/**
 * The VMM class implements Variable Order Markov Models
 */
public class VMM implements java.io.Serializable{


    /**
     * The {@link ArrayList} of {@link Pandas} that contain the probabilities of the symbols in the alphabet appearing after all contexts, each index represents the order
     */
    public ArrayList<Pandas> prob_mats;
    /**
     * The {@link ArrayList} of {@link Pandas} that contain the times of the symbols in the alphabet appearing after all contexts, each index represents the order
     */
    private ArrayList<Pandas> counts;
    /**
     * The {@link ArrayList} of {@link String} is the alphabet that is used in the VMM
     */
    public ArrayList<String> alphabet;

    /**
     *The int is the maximal order that is represented by the VMM
     */
    private int max_depth;

    /**
     * The {@link ArrayList} of {@link String} is the history of sampled Strings
     */
    private ArrayList<String> generated_history = new ArrayList<String>();
    /**
     * The {@link ArrayList} of {@link String} is the stored seed for the sampling methods
     */
    private ArrayList<String> input_history = new ArrayList<String>();
    /**
     * Typicality used for modulating the probability (double between 1 and 0)
     */
    public double typicality = 1.0;

    /**
     * Constructor Initializes parameters needed for VOMM
     * @param max_depth int maximal order of layers in VOMM
     */
    public VMM(int max_depth){
        alphabet = new ArrayList<String>();
        //this.vmm = new ArrayList<Pandas>();
        this.counts = new ArrayList<Pandas>();
        this.max_depth = max_depth;

    }

    /**
     * Constructor Initializes parameters needed for VOMM
     * @param alphabet {@link ArrayList<String>} alphabet used in the dataset
     * @param max_depth int maximal order of layers in VOMM
     */

    public VMM(ArrayList<String> alphabet, int max_depth){
        this.alphabet = alphabet;
        this.counts = new ArrayList<Pandas>();
        this.max_depth = max_depth;

    }

    /**
     *
     * Generates VOMM from Dataset of sequences
     * @param sequence {@link ArrayList<String>} sequence to learn the probabilities o
     */
    public void learn(ArrayList<String> sequence){
        int level = 0;
        sequence.removeAll(Arrays.asList("",null));
        while((level <= this.max_depth)) {
            Pandas df = new Pandas(this.alphabet, level);
            this.counts.add(df);
            if (sequence.size()-level >= 0 ){
                this.fillPandas(sequence, level);
            }
            level++;
        }
        this.prob_mats = this.copy(this.counts);
        this.compute_prob_mat();
    }


    public void update_generated_history(ArrayList<String> generated){

        generated_history.addAll(generated);
        if(generated_history.size()> this.max_depth){
            this.generated_history = new ArrayList<String>(this.generated_history.subList(generated_history.size()-max_depth, this.generated_history.size()));
        }

    }

    public void update_input_history(ArrayList<String> input){

        input_history.addAll(input);
        if(input_history.size()> this.max_depth){
            this.input_history = new ArrayList<String>(this.input_history.subList(input_history.size()-max_depth, this.input_history.size()));
        }
    }

    
    /**
     * Clears stored history of last samples (generate_history)
     */
    public void clearGeneratedHistory(){
        this.generated_history = new ArrayList<String>();
    }
    public void clearInputHistory(){
        new ArrayList<String>();
    }
    /**
     * Clears input_history and generated_history
     */
    public void clearWholeHistory(){
        this.input_history = new ArrayList<String>();
        this.generated_history = new ArrayList<String>();
    }

    /**
     * Returns probability of Symbol appearing after context
     * @param context String that appeared before symbol
     * @param symbol String that you want to no the probability of appearing from
     * @return probability P(symbol|context)
     */
    public double predict(ArrayList<String> context, String symbol){
        if (this.prob_mats.get(context.size()).getValue(context, symbol) == 0){return this.predict(new ArrayList<String>(context.subList(1, context.size())),symbol);}
        return this.prob_mats.get(context.size()).getValue(context, symbol);
    }

    /**
     * Samples Symbol from probabilities of Symbols appearing after empty sequence
     * @param typicality value between 1-0 to decide the typicality of the probabilities
     * @return Symbol sampled from the probability distribution following an empty seed
     */
    public Atom[] sampleStart(double typicality){
        ArrayList<Double> probabilities = this.prob_mats.get(0).getValue(new ArrayList<String>());
        probabilities = Helper.modulate(probabilities, typicality);
        Double[] Double_array = new Double[probabilities.size()];
        Double_array = probabilities.toArray(Double_array);
        int i = 0;
        double[] array_probs = new double[Double_array.length];
        for(Double d : Double_array) {
            array_probs[i] = d;
            i++;
        }
        EnumeratedIntegerDistribution dist = new EnumeratedIntegerDistribution(this.prob_mats.get(0).alpha_pos, array_probs);
        int idx = dist.sample();
        String sample = this.alphabet.get(idx);
        Atom[] dumpAtom = new Atom[]{Atom.newAtom(sample),Atom.newAtom(array_probs[idx])};
        return dumpAtom;
    }

    /**
     * Returns one character sampling from probabilities of Symbols appearing after context
     *
     * @param seed Seed used to sample next symbol
     * @param typicality value between 1-0 to decide the typicality of the probabilities
     * @param max_order int Maximal order of VOMM that should be used
     * @return Sampled Symbol
     */

    public Atom[] sample(ArrayList<String> seed, double typicality, int max_order){
        if (max_order > this.prob_mats.size()){throw new RuntimeException("Order too big");}
        if(max_order < 0){throw new RuntimeException("Negative order impossible");}
        if(seed.size() > max_order){seed = new ArrayList<String>(seed.subList(seed.size()-max_order,seed.size()));}

        if (!(this.prob_mats.get(seed.size()).inverse_index_0.values().contains(seed))){
            return this.sample(new ArrayList<String>(seed.subList(1,seed.size())),typicality, max_order);}
        ArrayList<Double> probabilities = this.prob_mats.get(seed.size()).getValue(seed);
        probabilities = Helper.modulate(probabilities, typicality);

        double sum = Helper.sum_array(probabilities);

        //If the context did not appear reduce order by 1
        if (sum != 1) {
            if (sum <= 0.99) {
                return this.sample(new ArrayList<String>(seed.subList(1, seed.size())), typicality, max_order);
            }
            probabilities = Helper.round(probabilities, sum);
        }
        //Sample-method from apache commons
        Double[] Double_array = new Double[probabilities.size()];
        Double_array = probabilities.toArray(Double_array);
        int i = 0;
        double[] array_probs = new double[Double_array.length];
        for(Double d : Double_array) {
            array_probs[i] = (double)d;
            i++;
        }
        System.out.println(String.valueOf(this.prob_mats.get(0).alpha_pos.length));
        System.out.println(String.valueOf(array_probs.length));
        EnumeratedIntegerDistribution dist = new EnumeratedIntegerDistribution(this.prob_mats.get(0).alpha_pos, array_probs);
        int idx = dist.sample();
        String sample = this.alphabet.get(idx);
        Atom[] dumpAtom = new Atom[]{Atom.newAtom(sample),Atom.newAtom(array_probs[idx])};
        return dumpAtom;
    }

    /**
     * Sampling Symbol from the mean probability distribution described by similar contexts to input(Similarity-Measure = Hamming-Distance)
     * @param seed String context to get the similar contexts of     *
     * @param distance Sets the amount of Hamming-distance
     * @param typicality value between 1-0 to decide the typicality of the probabilities
     * @return Sampled symbol
     */
    public Atom[] sample_fuzzy(ArrayList<String> seed, int distance, double typicality, int max_order){
        // Getting rid of unwanted cases
        if (max_order > this.prob_mats.size())throw new RuntimeException("Context too long");
        if(max_order < 0)throw new RuntimeException("Negative order impossible");
        if(distance > seed.size() || distance < 0)throw new RuntimeException("Illegal distance ");
        if(seed.size() > max_order)seed = new ArrayList<String>(seed.subList(seed.size()-max_order,seed.size()));

        Pandas df = this.prob_mats.get(seed.size());
        ArrayList<Integer> idx;
        ArrayList<ArrayList<String>> contexts = new ArrayList<ArrayList<String>>(this.prob_mats.get(seed.size()).inverse_index_0.values());

        //If context not appeared escape to order -1
        if(!contexts.contains(seed)){return sample(new ArrayList<String>(seed.subList(1,seed.size())),typicality, max_order);}

        //Get Contexts similar to input context by filtering with masks (ln.170) and get mean-probability
        idx = get_similar(seed,contexts,distance );
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

        double sum_single = Helper.sum_array(mean);

        if (sum_single != 1){
            if (sum_single <= 0.99) {
                return this.sample(new ArrayList<String>(seed.subList(1, seed.size())),typicality, max_order);
            }
            mean = Helper.round(mean, sum_single);
        }

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
        String sample = this.alphabet.get(id);
        //DO NOT update history here! update_generated_history(sample);
        Atom[] dumpAtom;
        dumpAtom = new Atom[]{Atom.newAtom(sample),Atom.newAtom(array_probs[id])};
        return dumpAtom;
    }


    /**
     * Updates DataFrame representing VOMM at order depth
     * @param seq Data to learn from
     * @param depth VOMM order to update
     */
    private void fillPandas(ArrayList<String> seq, int depth){
        int extra_depth = depth +1;
        for (int i = 0; i <= seq.size() - extra_depth; i++) {
            ArrayList<String> context = new ArrayList<String>(seq.subList(i, i + (depth)));
            String symbol = seq.get(i + depth);
            this.counts.get(depth).incrementValue(context, symbol);
        }
        System.out.println("filling" + this.alphabet);
        this.alphabet = this.counts.get(depth).alphabet;
        System.out.println("set" + this.alphabet);
    }

    /**
     * Computes Probability Matrix used for sampling
     */
    private void compute_prob_mat(){
        for(Pandas df: this.prob_mats){
            for(int i = 0; i < df.length(); i++){
                ArrayList<String> context = df.getContext(i);
                ArrayList<Double> values = df.getValue(context);
                double sum = Helper.sum_array(values);
                ArrayList<Double> probability = Helper.divide_array(values, sum);
                df.setValue(context, probability);

            }
        }
    }


    /**
     * Returns list of indices of the contexts that are similar to a certain context
     * @param seed context to compare with
     * @param contexts list of all possible contexts
     * @param distance Hamming-Distance to check
     * @return
     */
    private ArrayList<Integer> get_similar(ArrayList<String> seed, ArrayList<ArrayList<String >> contexts, int distance) {
        ArrayList<Integer> idx = new ArrayList<Integer>();
        for(int i = 0; i < contexts.size(); i++){
            contexts.get(i).removeAll(seed);
            if(contexts.get(i).size()== distance){
                idx.add(i);
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

    public ArrayList<String> getAlphabet(){
        return this.alphabet;
    }

    public int getMax_order(){
        return this.max_depth;
    }

    public ArrayList<String> getGenerated_history(){
        return this.generated_history;
    }

    public ArrayList<String> getInput_history() { return this.input_history; }


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


