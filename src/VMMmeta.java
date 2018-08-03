import vomm.VMM;

import com.cycling74.max.*;

public class VMMmeta extends MaxObject {

    //member variables
    public boolean learningVar;
    public String[] alphabet;
    public boolean generation_started;
    int alphabetSize;
    int max_order;
    int gen_max_order;
    VMM VMMinst;
    int info_idx;


    public VMMmeta()
    {
        bail("(mxj foo) must provide 2 parameters max. order and the alphabet size [mxj VMMmeta 3 10]");
    }
    public VMMmeta(int orderIn)
    {
        max_order = orderIn;
        gen_max_order = max_order;
        generation_started = false;

        //create the alphabet
        //TODO alphabet = new String[];
        VMMinst = new VMM(alphabet, max_order);
        post("Created a VMMmeta with max. order "+max_order+", and the alphabet size "+alphabetSize);

        //Declare outlets
        this.declareOutlets(new int[]{DataTypes.ALL});
        info_idx = getInfoIdx();
    }

    public VMMmeta(int orderIn, int alphabetSizeIn)
    {
        max_order = orderIn;
        gen_max_order = max_order;
        generation_started = false;

        //create the alphabet
        alphabetSize = alphabetSizeIn;
        alphabet = new String[alphabetSize];
        for (int i=0; i<alphabetSize;i++){
            alphabet[i]= Integer.toString(i);
        }
        VMMinst = new VMM(alphabet, max_order);
        post("Created a VMMmeta with max. order "+max_order+", and the alphabet size "+alphabetSize);

        //Declare outlets
        this.declareOutlets(new int[]{DataTypes.ALL});
        info_idx = getInfoIdx();

    }

    //Training methods start here
    //Learn a sequence
    public void learn(Atom[] sequenceIn){

        String learnSequence = Atom.toOneString(sequenceIn);
        VMMinst.learn(learnSequence);
        post("learning the sequence " + learnSequence);
    }

    //Learn a symbol combined with the input history.
    public void stream_learning(Atom[] stream_learningIn) {
        if (stream_learningIn.length>1) {
            bail("stream_learning expects one symbol at a time. Use learn to train VMM on sequences.");
        }
        else {
            String stream_symbol = Atom.toOneString(stream_learningIn);
            VMMinst.update_input_history(stream_symbol);
            VMMinst.learn(VMMinst.getInput_history());
        }
    }

    public void stream(Atom[] stream_genIn){
        this.stream_learning(stream_genIn);
        String generated = VMMinst.sample(VMMinst.getGenerated_history(), VMMinst.typicality, this.gen_max_order);
        VMMinst.update_generated_history(generated);
        //TODO outlet generated
    }

    //Generation Methods
    public void genstart(){
        VMMinst.clearWholeHistory();
        post("History Cleared");
        this.generation_started = true;
        String generated = VMMinst.sample(VMMinst.typicality);
        VMMinst.update_generated_history(generated);
        post("Generation Started");
        //outlet(info_idx,new Atom[]{Atom.newAtom("genstarted")});
        outlet(0, new Atom[]{Atom.newAtom(generated)});
    }

    public void bang(){
        if(!(this.generation_started)) this.genstart();
        else {
            String generated = VMMinst.sample(VMMinst.getGenerated_history(), VMMinst.typicality, this.gen_max_order);
            VMMinst.update_generated_history(generated);
            //TODO outlet generated
        }
    }

    public void context(Atom[] contextIn){
        String contextGen = Atom.toOneString(contextIn);
        VMMinst.update_input_history(contextGen);
        String generated = VMMinst.sample(contextGen, VMMinst.typicality, this.gen_max_order);
        //TODO outlet generated
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
    //
    public void setGen_max_order(int max_orderIn){
        if ( max_orderIn <= this.max_order){
            this.gen_max_order = max_orderIn;
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
    }

}
