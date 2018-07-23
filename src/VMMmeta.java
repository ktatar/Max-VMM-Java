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

    public void stream_learning(boolean stream_learningIn) {
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
    public void learn(Atom[] sequenceIn){

        String learnSequence = Atom.toOneString(sequenceIn);
        VMMinst.learn(learnSequence);
        post("learning the sequence " + learnSequence);
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

    public void genstart(){
        VMMinst.clearWholeHistory();
        VMMinst.sample(VMMinst.typicality);
    }

}
