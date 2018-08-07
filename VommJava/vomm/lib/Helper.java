package lib;

import java.util.ArrayList;

public class Helper {

    public static ArrayList<Double> divide_array(ArrayList<Double> input, double value) {
        for (int i = 0; i < input.size(); i++) {
            double to_divide = input.get(i);
            input.set(i, to_divide/value);
        }
        return input;
    }

    public static ArrayList<Double> modulate(ArrayList<Double> input, double value) {
        double mean = sum_array(input)/input.size();
        for (int i = 0; i < input.size(); i++) {
            double prob= input.get(i);
            input.set(i, (value*prob) + (1-value)*(1-prob-mean));
        }
        divide_array(input, sum_array(input));
        return input;
    }

    public static double sum_array(ArrayList<Double> array) {
        double sum = 0.0;
        for (double value : array) {
            sum += value;
        }
        return sum;
    }
}
