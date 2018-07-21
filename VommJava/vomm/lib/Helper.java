package vomm.lib;

import vomm.VMM;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Helper {

    public static double[] divide_array(double[] input, double value) {
        for (int i = 0; i < input.length; i++) {
            input[i] /= value;
        }
        return input;
    }

    public static double[] modulate(double[] input, double value) {
        double mean = sum_array(input)/input.length;
        for (int i = 0; i < input.length; i++) {
            input[i] = (value*input[i]) + (1-value)*(1-input[i]-mean);
        }
        divide_array(input, sum_array(input));
        System.out.println(Arrays.toString(input));
        return input;
    }

    public static double sum_array(double[] array) {
        double sum = 0.0;
        for (double value : array) {
            sum += value;
        }
        return sum;
    }

    public static byte[] binarize(String str) {
        return str.getBytes();
    }


    /**
     * Loading VOMM from directory name
     *
     * @param name Directory to load VOMM from
     */
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
    public static void writeVMM(VMM store) {
        try {
            FileOutputStream fileOut = new FileOutputStream("vmm.ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(store);
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
    public static void writeVMM(VMM store, String file) {
        try {
            FileOutputStream fileOut = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(store);
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in " + file);
        } catch (IOException i) {
            i.printStackTrace();
        }
    }
}
