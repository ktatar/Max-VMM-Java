import com.cycling74.max.*;
import vomm.VMM;

public class VMMmeta extends MaxObject {

    //member variables
    public boolean learningVar;
    public String[] alphabet;
    //Constructors
    public VMMmeta()
    {
        bail("(mxj foo) must provide a name for” + ”foo e.g. [mxj VMMmeta 3 1]");
    }
    public VMMmeta(int orderIn)
    {
        this(orderIn, 1, 100);
    }
    public VMMmeta(int orderIn, boolean learningIn, int alphabetSizeIn)
    {
        alphabetSize = alphabetSizeIn;

        post("Created a VMMmeta with max. order "+orderIn);
    }


    public void learning (boolean learningIn) {
        learningVar = learningIn;
    }
    public void bang() {
        String[] alphabet = {"a","b","c","d","r"};
        String[] seqs = {"abbbbbcdddddr"};

        VMM vmm = new VMM(alphabet);
        vmm.learn(seqs, 10);
        //System.out.println(vmm.sample("b"));
        System.out.println(vmm.generate_sequence(11,4));
        outlet(0, "Congratulations!");
    }
}

