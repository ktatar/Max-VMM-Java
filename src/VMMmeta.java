import com.cycling74.max.*;
import vomm.VMM;

import java.util.Arrays;

public class VMMmeta extends MaxObject {

    //member variables
    public boolean learningVar;
    public String[] alphabet;
    int alphabetSize;
    int max_order;
    boolean stream_learning;
    VMM VMMinst;

    public VMMmeta()
    {
        bail("(mxj foo) must provide 3 parameters max. order and the alphabet size [mxj VMMmeta 3 10]");
    }
    public VMMmeta(int orderIn)
    {
        this(orderIn, 100);
    }

    public VMMmeta(int orderIn, int alphabetSizeIn)
    {
        max_order = orderIn;

        //create the alphabet
        alphabetSize = alphabetSizeIn;
        alphabet = new String[alphabetSize];
        for (int i=0; i<alphabetSize;i++){
            alphabet[i]= Integer.toString(i);
        }
        stream_learning = false;
        VMMinst = new VMM(alphabet, max_order);
        post("Created a VMMmeta with max. order "+max_order+", and the alphabet size "+alphabetSize);

    }

    public void stream_learning (boolean stream_learningIn) {
        stream_learning = stream_learningIn;
    }

/*    public void bang() {
        //testCode - TO BE SUPPRESSED
        String[] alphabet = {"a","b","c","d","r"};
        String[] seqs = {"abbbbbcdddddr"};

        VMM vmm = new VMM(alphabet);
        vmm.learn(seqs, 10);
        //System.out.println(vmm.sample("b"));
        System.out.println(vmm.generate_sequence(11,4));
        outlet(0, "Congratulations!");
    }*/
/**
     *
     * Generates VOMM from Dataset of sequences
     */
    public void learn(String[] sequenceIn){

        //String learnSequence = Atom.toOneString(sequenceIn);
        VMMinst.learn(sequenceIn);
        post("learning the sequence " + Arrays.toString(sequenceIn));
    }

    public void load(){

        VMMinst.loadVMM();
    }

    public void load(String filePath){

        VMMinst.loadVMM(filePath);
    }

    public void write(){

        VMMinst.writeVMM();
    }

    public void write(String filePath){

        VMMinst.writeVMM(filePath);
    }




    /**//**
     * Samples Symbol from probabilities of Symbols appearing after empty sequence
     * @param modularity value between 1-0 to decide the modularity of the probabilities
     * @param keep_history boolean if sampled String should be added to generated history
     * @return Symbol sampled from the probability distribution following an empty seed
     *//*
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

    *//**
     * Returns one character sampling from probabilities of Symbols appearing after context
     *
     * @param seed Seed used to sample next symbol
     * @param modularity value between 1-0 to decide the modularity of the probabilities
     * @param max_order int Maximal order of VOMM that should be used
     * @param use_seed boolean if true internal seed is used
     * @param use_feedback boolean if true will use whole history not just seed to sample
     * @param keep_history boolean if true adds sample to this.generated_history
     * @return Sampled Symbol
     *//*
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

    *//**
     * Sampling Symbol from the mean probability distribution described by similar contexts to input(Similarity-Measure = Hamming-Distance)
     * @param seed String context to get the similar contexts of     *
     * @param distance Sets the amount of Hamming-distance
     * @param modularity value between 1-0 to decide the modularity of the probabilities
     * @param use_seed boolean if true internal seed is used
     * @param use_feedback boolean if true will use whole history not just seed to sample
     * @param keep_history boolean if true adds sample to this.generated_history
     * @return Sampled symbol
     *//*
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
*/

}
