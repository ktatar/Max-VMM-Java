import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import vomm.VMM;

public class test {
    public static void main(String[] args) throws UnsupportedEncodingException {
        String[] alphabet = {"1", "2", "3", "4", "5", "6"};
        ArrayList<String> seq = new ArrayList<String>(Arrays.asList(alphabet));

        VMM vmm = new VMM(seq, 2);
        vmm.learn(seq);
        System.out.print(vmm.getAlphabet());

        String[] new_alphab = {"7","8","9","10"};
        ArrayList<String> new_seq = new ArrayList<String>(Arrays.asList(new_alphab));
        System.out.println("--------------------");
        vmm.learn(new_seq);
        System.out.println(vmm.getAlphabet());
        ArrayList<String> values = new ArrayList<String>(vmm.prob_mats.get(1).index_1.keySet() );
        System.out.println("2-layer" + vmm.prob_mats.get(1).length());
        //System.out.println(Arrays.asList(vmm.prob_mats.get(1).index_1));
        System.out.println(vmm.alphabet);
        System.out.println("--------------------");
        System.out.println(Arrays.toString(vmm.getCounts().get(0).alpha_pos));

        System.out.println(vmm.sampleStart(1));



    }
}
