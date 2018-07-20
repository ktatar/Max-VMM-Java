package vomm.lib;

import java.util.ArrayList;
import java.util.Arrays;

public class Helper{

    public static double[] divide_array(double[] input, double value) {
        for (int i = 0; i < input.length; i++){input[i] /= value;}
        return input;
    }
    public static double[] modulate(double[] input, double value) {
        for (int i = 0; i < input.length; i++){input[i] = Math.abs((1-value) - input[i]);}
        divide_array(input, sum_array(input));
        System.out.println(Arrays.toString(input));
        return input;
    }
    public static double sum_array(double[] array){
        double sum = 0.0;
        for(double value :array){sum+= value;}
        return sum;
    }

    public static byte[] binarize(String str){
        return str.getBytes();
    }

}
