import vomm.VMM;
import java.util.Arrays;
import java.io.UnsupportedEncodingException;

public class TestingVMM {

    public static void main(String[] args) throws UnsupportedEncodingException {
        String[] alphabet = {"a", "b", "c", "d", "r"};
        String seqs = "abbbbbcdddddr";

        VMM vmm = new VMM(alphabet, 4);
        vmm.learn(seqs);
		/*
		System.out.println(vmm.getCounts().get(0).toString(""));
        System.out.println(vmm.getVMM().get(0).toString(""));
		vmm.update("", "a",false,false);
        System.out.println(vmm.getCounts().get(0).toString(""));
        System.out.println(vmm.getVMM().get(3).toString("abb"));

 */
        System.out.println(vmm.sample("abb", 0.6, 4, false, false,true));
        //System.out.println(vmm.counts.get(0).toString(""));
        //System.out.println(vmm.sample("b"));
        //System.out.println(vmm.generate_sequence(11, 4));
    }
}
