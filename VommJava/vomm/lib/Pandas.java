package vomm.lib;

/**
 * The Pandas program implements the functions I need from the python pandas library
 *
 * @author Jonas Kraasch
 * @since 2018-07-17
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;



public class Pandas implements java.io.Serializable{

    int depth;
    String[] alphabet;
    HashMap<String, Integer> index_0;
    public HashMap<Integer, String> inverse_index_0;
    HashMap<String, Integer> index_1;
    public HashMap<String, byte[]> binarized = new HashMap();
    HashMap<byte[] ,String> inverse_binarized = new HashMap();
    public ArrayList<double[]> df;
    public double[][] df_array;

    /**
     *
     * @param alphabet Array of Strings use in Dataset
     * @param depth Order of Vomm
     */
    public Pandas(String[] alphabet, int depth){

        index_0 = new HashMap();
        index_1 = new HashMap();
        inverse_index_0 = new HashMap();
        df = new ArrayList();
        this.depth = depth;
        this.alphabet = alphabet;
        for (int i = 0; i < alphabet.length; i++){
            this.index_1.put(alphabet[i], i);
        }
    }
    public Pandas(Pandas src){
        index_0 = src.index_0;
        index_1 = src.index_1;
        inverse_index_0 = src.inverse_index_0;
        this.df = src.df;
        this.depth = src.depth;
        this.alphabet = src.alphabet;

    }

    /**
     *
     * @return number of contexts in Dataframe
     */
    public int length(){return df.size();}

    /**
     * Returns context which was mapped to index
     *
     * @param index Index that you want the context of (Row-Index)
     * @return
     */
    public String getContext(int index){return inverse_index_0.get(index);}

    /**
     * Add String to DataFrame, adds context with its reference in the Dataframe to dictionaries
     *
     * @param context String to be added (Row-Name)
     */
    public void addContext(String context){
        if (context.length() > depth){throw new RuntimeException("Context is to long!");}
        this.index_0.put(context, df.size());
        byte[]binarized = Helper.binarize(context);
        this.binarized.put(context, binarized);
        this.inverse_binarized.put(binarized, context);
        this.inverse_index_0.put(df.size(), context);
        int length = this.alphabet.length;
        double[] context_array = new double[length];
        df.add(context_array);
    }

    /**
     * Sets counts of every symbol appearing after the context to the values in the array values
     *
     * @param context String context you want to change the value of (Row-Name)
     * @param values Array of values to set
     */
    public void setValue(String context, double[] values){
        if (this.index_0.containsKey(context)){
            df.set(this.index_0.get(context), values);
        }else{
            this.addContext(context);
            this.setValue(context, values);
        }
    }

    /**
     * Sets count of symbol appearing after context to value
     *
     * @param context String context you want to change the value of (Row-Name)
     * @param symbol String of symbol you want to change the value of (Column-Name)
     * @param value value to set
     */
    public void setValue(String context, String symbol, double value){
        if (!this.index_1.containsKey(symbol)){throw new RuntimeException("Symbol not in alphabet!");}
        if (this.index_0.containsKey(context)){
            df.get(this.index_0.get(context))[this.index_1.get(symbol)] = value;
        }else{
            this.addContext(context);
            this.setValue(context, symbol, value);
        }
    }

    /**
     * Increments count for symbol appearing after context
     *
     * @param context String context to increment the occurrence of symbol (Row-Name)
     * @param symbol Sting symbol that appeared and the occurrence needs to be incremented of (Column-Name)
     */
    public void incrementValue(String context, String symbol){
        if (!this.index_1.containsKey(symbol)){throw new RuntimeException("Symbol not in alphabet!");}
        if (this.index_0.containsKey(context)){

            df.get(this.index_0.get(context))[this.index_1.get(symbol)] += 1;
        }else{
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
    public double[] getValue(String context){
        if (context.length() == 0){return df.get(0);}
        if (!this.index_0.containsKey(context)){return Arrays.copyOf(this.getValue(context.substring(1)), this.alphabet.length);}
        return  Arrays.copyOf(df.get(this.index_0.get(context)), this.alphabet.length);
    }

    /**
     * Returns value stored in position [context][symbol]
     *
     * @param context String context to get the values from (Row-Name)
     * @param symbol String symbol to get the value from (Column-Name)
     * @return Value of Symbol given Context
     */
    public double getValue(String context, String symbol){
        if (!this.index_1.containsKey(symbol)){throw new RuntimeException("Symbol not in alphabet!");}
        if (!this.index_0.containsKey(context)){throw new RuntimeException("Context not in training sequences!");}
        return  df.get(this.index_0.get(context))[this.index_1.get(symbol)];
    }

    /**
     * Print values given row context
     *
     * @param context String context to print the vales from (Row-Name)
     * @return Printable String of values in row context
     */
    public String toString(String context){
        if (!this.index_0.containsKey(context)){throw new RuntimeException("Context not in training sequences!");}
        String values = "";
        for (double i : this.getValue(context)){values += " | " + i;}
        String alphabet = "";
        for (String letter: this.alphabet){alphabet += " | " + letter;}

        return alphabet + "\n" + context + "\n" + values ;
    }

    public int getDepth() {
        return this.depth;
    }
}
