package vomm.lib;

/**
 * The Pandas program implements the functions I need from the python pandas library
 *
 * @author Jonas Kraasch
 * @since 2018-07-17
 */

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;



public class Pandas implements java.io.Serializable {

    //Order of VOMM
    int depth;
    //List of String corresponding to the alphabet used in this order of the VOMM
    public ArrayList<String> alphabet;
    //Converting alphabet to their  (needed for Sample method only takes array not collection)
    public int[] alpha_pos;
    //Row Index (Context -> Index)
    HashMap<String, Integer> index_0;
    //Row Name (Index -> Context)
    public HashMap<Integer, String> inverse_index_0;
    //Column Index (Alphabet -> Index)
    public HashMap<String, Integer> index_1;
    // Binarized Contexts used for bitwise comparison with masks to get contexts with set Hamming-Distance
    public HashMap<String, byte[]> binarized = new HashMap();
    // Inverse of Binarized
    HashMap<byte[], String> inverse_binarized = new HashMap();

    //DataFrame
    public ArrayList<ArrayList<Double>> df = new ArrayList<ArrayList<Double>>();


    /**
     * @param alphabet Array of Strings use in Dataset
     * @param depth    Order of Vomm
     */
    public Pandas(String[] alphabet, int depth) {

        index_0 = new HashMap();
        index_1 = new HashMap();
        inverse_index_0 = new HashMap();
        this.depth = depth;
        this.alphabet = new ArrayList<String>(Arrays.asList(alphabet));
        this.alpha_pos = new int[alphabet.length];
        for (int i = 0; i < alphabet.length; i++) {
            this.index_1.put(alphabet[i], i);
            this.alpha_pos[i] = i;
        }
    }

    public Pandas(Pandas src) {
        index_0 = (HashMap<String, Integer>) src.index_0.clone();
        index_1 = (HashMap<String, Integer>) src.index_1.clone();
        inverse_index_0 = (HashMap<Integer, String>) src.inverse_index_0.clone();
        for(ArrayList<Double>old_df: src.df){
            this.df.add((ArrayList<Double>) old_df.clone());
        }
        this.depth = src.depth;
        this.alphabet = (ArrayList<String>) src.alphabet.clone();
        this.binarized = (HashMap<String, byte[]>) src.binarized.clone();
        this.inverse_binarized = (HashMap<byte[], String>) src.inverse_binarized.clone();
        this.alpha_pos = src.alpha_pos.clone();

    }

    /**
     * @return number of contexts in Dataframe
     */
    public int length() {
        return df.size();
    }

    /**
     * Returns context which was mapped to index
     *
     * @param index Index that you want the context of (Row-Index)
     * @return
     */
    public String getContext(int index) {
        return inverse_index_0.get(index);
    }

    /**
     * Add String to DataFrame, adds context with its reference in the Dataframe to dictionaries
     *
     * @param context String to be added (Row-Name)
     */
    public void addContext(String context) {
        if (context.length() > depth) {
            throw new RuntimeException("Context is to long!");
        }
        this.index_0.put(context, df.size());
        byte[] binarized = Helper.binarize(context);
        this.binarized.put(context, binarized);
        this.inverse_binarized.put(binarized, context);
        this.inverse_index_0.put(df.size(), context);
        ArrayList<Double> context_list = new ArrayList<Double>(Collections.nCopies(this.alphabet.size(), 0.0));
        df.add(context_list);
    }

    /**
     * Sets counts of every symbol appearing after the context to the values in the array values
     *
     * @param context String context you want to change the value of (Row-Name)
     * @param values  Array of values to set
     */
    public void setValue(String context, ArrayList<Double> values) {
        if (this.index_0.containsKey(context)) {
            df.set(this.index_0.get(context), values);
        } else {
            this.addContext(context);
            this.setValue(context, values);
        }
    }

    /**
     * Sets count of symbol appearing after context to value
     *
     * @param context String context you want to change the value of (Row-Name)
     * @param symbol  String of symbol you want to change the value of (Column-Name)
     * @param value   value to set
     */
    public void setValue(String context, String symbol, double value) {

        if (this.index_0.containsKey(context)) {
            df.get(this.index_0.get(context)).set(this.index_1.get(symbol), value);
            if (!this.index_1.containsKey(symbol)) {
                this.index_1.put(symbol, this.index_1.size());
                int[] new_alpha_pos = new int[this.alpha_pos.length+1];
                for(int i = 0; i < new_alpha_pos.length; i++){
                    new_alpha_pos[i] = i;
                }
                this.alpha_pos = new_alpha_pos;
                this.alphabet.add(symbol);
                df.get(this.index_0.get(context)).add(0.0);
            }
        } else {
            this.addContext(context);
            this.setValue(context, symbol, value);
        }
    }

    /**
     * Increments count for symbol appearing after context
     *
     * @param context String context to increment the occurrence of symbol (Row-Name)
     * @param symbol  Sting symbol that appeared and the occurrence needs to be incremented of (Column-Name)
     */
    public void incrementValue(String context, String symbol) {

        if (this.index_0.containsKey(context)) {
            if (!this.index_1.containsKey(symbol)) {
                //System.out.println("Symbol not in alphabet");
                this.index_1.put(symbol, this.index_1.size());
                int[] new_alpha_pos = new int[this.alpha_pos.length+1];
                for(int i = 0; i < new_alpha_pos.length; i++){
                    new_alpha_pos[i] = i;
                }
                this.alpha_pos = new_alpha_pos;
                this.alphabet.add(symbol);
                df.get(this.index_0.get(context)).add(0.0);

            }
            df.get(this.index_0.get(context)).set(this.index_1.get(symbol), df.get(this.index_0.get(context)).get(this.index_1.get(symbol)) + 1);
        } else {
            this.addContext(context);
            this.incrementValue(context, symbol);
        }
    }

    /**
     * Get array of values stored in position [context]
     *
     * @param context String context to get the values from (Row-Name)
     * @return Array of all the values in the row of context
     */
    public ArrayList<Double> getValue(String context) {
        if (context.length() == 0) {
            return df.get(0);
        }
        if (!this.index_0.containsKey(context)) {
            return this.getValue(context.substring(1));
        }
        return df.get(this.index_0.get(context));
    }

    /**
     * Returns value stored in position [context][symbol]
     *
     * @param context String context to get the values from (Row-Name)
     * @param symbol  String symbol to get the value from (Column-Name)
     * @return Value of Symbol given Context
     */
    public double getValue(String context, String symbol) {
        if (!this.index_1.containsKey(symbol)) {
            throw new RuntimeException("Symbol not in alphabet!");
        }
        if (!this.index_0.containsKey(context)) {
            throw new RuntimeException("Context not in training sequences!");
        }
        return df.get(this.index_0.get(context)).get(this.index_1.get(symbol));
    }

    /**
     * Print values given row context
     *
     * @param context String context to print the vales from (Row-Name)
     * @return Printable String of values in row context
     */
    public String toString(String context) {
        if (!this.index_0.containsKey(context)) {
            throw new RuntimeException("Context not in training sequences!");
        }
        String values = "";
        for (double i : this.getValue(context)) {
            values += " | " + i;
        }
        String alphabet = "";
        for (String letter : this.alphabet) {
            alphabet += " | " + letter;
        }

        return alphabet + "\n" + context + "\n" + values;
    }

    public int getDepth() {
        return this.depth;
    }
}
