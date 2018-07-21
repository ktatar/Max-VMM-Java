import vomm.VMM;
import vomm.lib.Helper;

import java.util.Arrays;
import java.io.UnsupportedEncodingException;

public class TestingVMM {

	public static void main(String[] args) throws UnsupportedEncodingException {
		String[] alphabet = {"a", "b", "c", "d", "r"};
		String seq = "abracadabra";

		VMM vmm = new VMM(alphabet, 4);
		vmm.learn(seq);
        System.out.println(vmm.sample("a", 1, 4, false, false,true));
		/*
		System.out.println(vmm.getCounts().get(0).toString(""));
        System.out.println(vmm.getVMM().get(0).toString(""));
		vmm.update("", "a",false,false);
        System.out.println(vmm.getCounts().get(0).toString(""));
        System.out.println(vmm.getVMM().get(3).toString("abb"));

 */
        Helper.writeVMM(vmm);
        VMM vmm_loaded = Helper.loadVMM();
        System.out.println(vmm_loaded.sample("a", 1, 4, false, false,true));
        //System.out.println(vmm.counts.get(0).toString(""));
		//System.out.println(vmm.sample("b"));
		//System.out.println(vmm.generate_sequence(11, 4));
	}
}
