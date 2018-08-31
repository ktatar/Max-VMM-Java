import com.cycling74.max.*;
import java.util.Arrays;
import java.util.ArrayList;


public class VMMmeta extends MaxObject {

    //member variables
    public boolean learningVar;
    public boolean generation_started;
    int max_order;
    int gen_max_order;
    VMM VMMinst;
    int info_idx;
    boolean fuzzy_sampling;
    boolean anything_learned;


    public VMMmeta()
    {
        bail("(mxj foo) must provide 2 parameters max. order [mxj VMMmeta 3]");
    }
    public VMMmeta(int orderIn)
    {
        max_order = orderIn;
        gen_max_order = max_order;
        generation_started = false;

        //create the alphabet
        VMMinst = new VMM(max_order);
        post("Created a VMMmeta with max. order "+max_order);
        anything_learned = false;

        //Declare outlets
        this.declareOutlets(new int[]{DataTypes.ALL,DataTypes.FLOAT});
        info_idx = getInfoIdx();
        fuzzy_sampling = false;
    }

/*    public VMMmeta(int orderIn, int alphabetSizeIn)
    {
        max_order = orderIn;
        gen_max_order = max_order;
        generation_started = false;

        //create the alphabet
        alphabetSize = alphabetSizeIn;
        alphabet = new ArrayList<String> alphabet;
        for (int i=0; i<alphabetSize;i++){
            alphabet Integer.toString(i);
        }
        VMMinst = new VMM(alphabet, max_order);
        post("Created a VMMmeta with max. order "+max_order+", and the alphabet size "+alphabetSize);

        //Declare outlets
        this.declareOutlets(new int[]{DataTypes.ALL});
        info_idx = getInfoIdx();

    }*/

    //Training methods start here
    //Learn a sequence
    public void learn(Atom[] sequenceIn){

        ArrayList<String> learnSequence = new ArrayList<String>(Arrays.asList(Atom.toString(sequenceIn)));
        VMMinst.learn(learnSequence);
        post("learning the sequence " + learnSequence);
        post(String.valueOf(VMMinst.alphabet.size()));
        for(int i=0; i<VMMinst.alphabet.size(); i++){
            post(VMMinst.alphabet.get(i));
        }

        anything_learned = true;
    }

    //Learn a symbol combined with the input history.
    public void stream_learning(Atom[] stream_learningIn) {
        if (stream_learningIn.length>1) {
            bail("stream_learning expects one symbol at a time. Use learn to train VMM on sequences.");
        }
        else {
            ArrayList<String> stream_symbol = new ArrayList<String>(Arrays.asList(Atom.toString(stream_learningIn)));
            VMMinst.update_input_history(stream_symbol);
            VMMinst.learn(VMMinst.getInput_history());
        }
    }

    public void stream(Atom[] stream_genIn){
        this.stream_learning(stream_genIn);
        Atom[] sampleTuple = VMMinst.sample(VMMinst.getGenerated_history(), VMMinst.typicality, this.gen_max_order);
        String generated = sampleTuple[0].getString();
        float prob = sampleTuple[1].getFloat();
        ArrayList<String> generatedHistory = new ArrayList<String>(Arrays.asList(generated));
        VMMinst.update_generated_history(generatedHistory);
        outlet(0, sampleTuple[0]);
        outlet(1, sampleTuple[1]);
    }

    //Generation Methods
    public void genstart(){
        VMMinst.clearWholeHistory();
        if(!anything_learned) bail("VMM is empty.");
        this.generation_started = true;
        Atom[] sampleTuple = VMMinst.sampleStart(VMMinst.typicality);
        String generated = sampleTuple[0].getString();
        ArrayList<String> generatedHistory = new ArrayList<String>(Arrays.asList(generated));
        VMMinst.update_generated_history(generatedHistory);
        post("Generation Started");
        //outlet(info_idx,new Atom[]{Atom.newAtom("genstarted")});
        outlet(0, sampleTuple[0]);
        outlet(1, sampleTuple[1]);
    }

    public void bang(){
        if(!(this.generation_started)) this.genstart();
        else {
            ArrayList<String> prevGenHistory = VMMinst.getGenerated_history();
            //post(String.valueOf(prevGenHistory.toArray()));
            post(VMMinst.getGenerated_history().toString());
            post("dangang");
            Atom[] sampleTuple = VMMinst.sample(VMMinst.getGenerated_history(), VMMinst.typicality, this.gen_max_order);
            String generated = sampleTuple[0].getString();
            ArrayList<String> generatedHistory = new ArrayList<String>(Arrays.asList(generated));
            VMMinst.update_generated_history(generatedHistory);
            outlet(0, sampleTuple[0]);
            outlet(1, sampleTuple[1]);
        }
    }

    public void context(Atom[] contextIn){

        ArrayList<String> contextGen = new ArrayList<String>(Arrays.asList(Atom.toString(contextIn)));
        if(!anything_learned) bail("VMM is empty.");
        VMMinst.update_input_history(contextGen);
        Atom[] sampleTuple = VMMinst.sample(contextGen, VMMinst.typicality, this.gen_max_order);
        String generated = sampleTuple[0].getString();
        ArrayList<String> generatedHistory = new ArrayList<String>(Arrays.asList(generated));
        VMMinst.update_generated_history(generatedHistory);
        outlet(0, sampleTuple[0]);
        outlet(1, sampleTuple[1]);
    }

    //Save and Load the VMM
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
    public void update_generated_history(Atom[] generated_history_update) {
            if (generated_history_update.length>max_order) {
                 post("Update sequence is longer than the max. order "+ max_order +". The sequence is truncated to the max order substring.");
            }
            ArrayList<String> update_symbol = new ArrayList<String>(Arrays.asList(Atom.toString(generated_history_update)));
            VMMinst.update_generated_history(update_symbol);
        }

    public void setGen_max_order(int max_orderIn){
        if ( max_orderIn <= this.max_order){
            this.gen_max_order = max_orderIn;
            post("The max. order of generation is set to "+ gen_max_order);
        }
        else bail("Max. order of generation cannot be greater than the maximum VMM order.");
    }
    //Clear state

    public void clearInputHistory(){
        VMMinst.clearInputHistory();
    }

    public void clearHistory(){
        VMMinst.clearWholeHistory();
        this.generation_started = false;
        post("History cleared");
    }

    //Resets the VMM, deletes whole history
    public void clearAll(){
        VMMinst = new VMM(max_order);
        generation_started = false;
        anything_learned = false;
        post("Created a VMMmeta with max. order "+max_order);

    }

}
