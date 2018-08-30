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
        //Cleaning Sequence
        sequence.removeAll(Arrays.asList("",null));
        //Adding and Filling Pandas to VMM (List) until we can describe the wanted orders
        while((level <= this.max_depth)) {
            Pandas df = new Pandas(this.alphabet, level);
            this.counts.add(df);
            if (sequence.size()-level >= 0 ){
                this.fillPandas(sequence, level);
                //update alphabet, since new Strings could have appeared
                this.alphabet = this.counts.get(0).alphabet;
            }
            level++;
        }
        //Creating Transition Matrix
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
        //Escape if probability is 0
        if (this.prob_mats.get(context.size()).getValue(context, symbol) == 0){return this.predict(new ArrayList<String>(context.subList(1, context.size())),symbol);}

        return this.prob_mats.get(context.size()).getValue(context, symbol);
    }



    /**
     * Updates DataFrame representing VOMM at order depth
     * @param seq Data to learn from
     * @param depth VOMM order to update
     */
    private void fillPandas(ArrayList<String> seq, int depth){

        //get all the substrings with length order+1, chop of last char ==> Context|Symbol pair
        int extra_depth = depth +1;
        for (int i = 0; i <= seq.size() - extra_depth; i++) {
            ArrayList<String> context = new ArrayList<String>(seq.subList(i, i + (depth)));
            String symbol = seq.get(i + depth);
            this.counts.get(depth).incrementValue(context, symbol);
        }

    }

    /**
     * Computes Probability Matrix used for sampling
     */
    private void compute_prob_mat(){

        //Iterating over contexts and calculating the distribution
        for(Pandas df: this.prob_mats){
            for(int i = 0; i < df.length(); i++){
                ArrayList<String> context = df.getContext(i);
                ArrayList<Double> values = df.getValue(context);
                double sum = Helper.sumList(values);
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

        //Iterate over List of context and remove all Symbols
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


