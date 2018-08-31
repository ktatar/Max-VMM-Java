import lib.Pandas;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import vomm.VMM;

public class test {
    public static void main(String[] args) throws UnsupportedEncodingException {
        String[] alphabet = {"1", "2", "3", "4", "5", "6"};
        ArrayList<String> seq = new ArrayList<String>(Arrays.asList(alphabet));

        VMM vmm = new VMM(seq, 4);
        vmm.learn(seq);

        String[] new_alphab = {"7","8","9","10"};
        ArrayList<String> new_seq = new ArrayList<String>(Arrays.asList(new_alphab));
        vmm.learn(new_seq);
        ArrayList<String> values = new ArrayList<String>(vmm.prob_mats.get(1).index_1.keySet() );

        String[] et = {"1", "2", "3", "4"};
        ArrayList<String> seed = new ArrayList<String>(Arrays.asList(et));
        System.out.println(vmm.sample(seed, 1, 1));



    }
}
