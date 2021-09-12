package com.yashsingh.depressiondetection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CalculateHRV extends AppCompatActivity {

    int windowsize = 600;
    public ArrayList<Integer> peakredlist = new ArrayList<Integer>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate_h_r_v);
        Intent intent = getIntent();
        int counter = intent.getIntExtra("Counter",1);
        double[] redarray = intent.getDoubleArrayExtra("Red List");
        double[] greenarray = intent.getDoubleArrayExtra("Green List");
        double[] ts = intent.getDoubleArrayExtra("Time Stamp");
        TextView tv1 = findViewById(R.id.t1);
        //tv1.setText(Integer.toString(counter));

        System.out.println(Arrays.toString(redarray));
        System.out.println(Arrays.toString(greenarray));
        System.out.println(Arrays.toString(ts));

        double[] rsub = new double[windowsize];
        double[] gsub = new double[windowsize];

        double[] midr = new double[windowsize];
        double[] midg = new double[windowsize];
        double[] midt = new double[windowsize];

        System.arraycopy(redarray, 120, midr, 0, windowsize);
        System.arraycopy(greenarray, 120, midg, 0, windowsize);
        System.arraycopy(ts, 120, midt, 0, windowsize);

        double greenmax=0,redmax=0,bluemax=0,greenmin=1000,redmin=1000,bluemin=1000;

        for(int i=0;i<midr.length;i++)
        {
            if(midr[i]>redmax)
                redmax = midr[i];

            if(midg[i]>greenmax)
                greenmax = midg[i];

            if(midr[i]<redmin)
                redmin = midr[i];

            if(midg[i]<greenmin)
                greenmin = midg[i];
        }

        double[] redarraynormalized = new double[redarray.length];
        double[] greenarraynormalized = new double[greenarray.length];

        for(int i=0;i<redarray.length;i++)
        {
            redarraynormalized[i] = redarray[i]/redmax;
            greenarraynormalized[i] = greenarray[i]/greenmax;
        }

        System.arraycopy(redarraynormalized, 120, rsub, 0, windowsize);
        System.arraycopy(greenarraynormalized, 120, gsub, 0, windowsize);

        double value = rsub[0];
        for (int i=1; i<windowsize; ++i){
            double currentValue = rsub[i];
            value += (currentValue - value) / 1.8;
            rsub[i] = value;
        }

        System.out.println(Arrays.toString(rsub));

        int l = rsub.length;
        int leftbase;
        int rightbase;

        double redthreshold = find_threshold(rsub);

        double threshold = redthreshold * 0.2;
        for(int i=1;i<l-10;i++)
        {
            if(rsub[i]>=rsub[i-1] && rsub[i]>=rsub[i+1])
            {
                leftbase = left_prominence(i,rsub[i],rsub);
                rightbase = right_prominence(i,rsub[i],rsub,l);

                if(rsub[i]-rsub[leftbase]>threshold && rsub[i]-rsub[rightbase]>threshold)
                {
                    peakredlist.add(i);
                }
            }
        }

        int[] peakred = new int[peakredlist.size()];

        int it1=0;
        for(int d:peakredlist)
        {
            peakred[it1] = d;
            it1++;
        }

        int[] variance = new int[peakredlist.size()-1];
        for(int i=0; i<(peakredlist.size()-1); i++){
            variance[i] = peakred[i+1] - peakred[i];
        }

        double mode = (double)find_mode(variance, peakredlist.size()-1) * 0.033;
        Map<Integer, Integer> mp = new HashMap<>();
        for(int i = 0; i < (peakredlist.size()-1); i++){
            int key = variance[i];
            if(mp.containsKey(key)){
                int freq = mp.get(key);
                freq++;
                mp.put(key, freq);
            }
            else{
                mp.put(key, 1);
            }
        }
        double modeAmplitude = (double)100.0*((double)Collections.max(mp.values())/(double)(peakredlist.size()-1));//(double)Collections.max(mp.values());
        Arrays.sort(variance);
        Log.e("Variance:", Arrays.toString(variance));
        double MxDMn = (double)((variance[peakredlist.size()-2] - variance[1]) * 0.033);
        double IS = modeAmplitude / (2*mode*MxDMn);
        String result = "Peaks: " + Arrays.toString(peakred) + "\nStress Index = " + Double.toString(IS) + "\nHeart Rate = " + peakred.length * 3;
        Log.e("Mode, AMode:", Double.toString(MxDMn)+" "+Double.toString(mode) + " " + Double.toString(modeAmplitude));
        System.out.println(Arrays.toString(peakred));
        Log.e("Data: ",Arrays.toString(peakred));
        tv1.setText(result);
        System.out.println("HR: " + peakred.length*3);
    }
    public double find_mean(int arr[], int n){
        int sum = 0;
        for (int i = 0; i < n; i++)
            sum += arr[i];

        return (double)sum / (double)n;
    }
    public double find_median(int arr[], int n){

        Arrays.sort(arr);
        if (n % 2 != 0)
            return (double)arr[n / 2];

        return (double)(arr[(n - 1) / 2] + arr[n / 2]) / 2.0;
    }
    public int find_mode(int arr[], int n){
        Map<Integer, Integer> mp = new HashMap<>();
        for(int i = 0; i < n; i++){
            int key = arr[i];
            if(mp.containsKey(key)){
                int freq = mp.get(key);
                freq++;
                mp.put(key, freq);
            }
            else{
                mp.put(key, 1);
            }
        }

        int max_count = 0, res = -1;
        for(Map.Entry<Integer, Integer> val : mp.entrySet()){
            if (max_count < val.getValue()){
                res = val.getKey();
                max_count = val.getValue();
            }
        }
        return res;
    }
    public double find_threshold(double[] arr)
    {
        double maxx=0;
        double result=0;
        double leftmaxx=0;
        double rightmaxx=0;
        for(int i=1;i<arr.length-1;i++)
        {
            if(arr[i-1]<=arr[i] && arr[i]>=arr[i+1])
            {
                leftmaxx=0;
                for(int j=i-1;j>=0 && j>=i-30;j--)
                {
                    leftmaxx=Math.max(leftmaxx,arr[i]-arr[j]);
                }

                rightmaxx=0;
                for(int j=i+1;j<arr.length && j<i+30;j++)
                {
                    rightmaxx=Math.max(rightmaxx,arr[i]-arr[j]);
                }

                maxx=Math.max(leftmaxx,rightmaxx);
                result=Math.max(result,maxx);
            }
        }
        System.out.println("Result");
        System.out.println(result);
        return result;
    }

    public int left_prominence(int index,double height,double[] sub)
    {
        double minn = height;
        int pos=index;

        for(int i=index-1;i>=0 && i>=index-30;i--)
        {
            if(sub[i]<minn)
            {
                minn = sub[i];
                pos = i;
            }
            if(sub[i]>height)
                break;
        }
        return pos;
    }


    public int right_prominence(int index,double height,double[] sub,int l)
    {
        double minn = height;
        int pos=index;

        for(int i=index+1;i<l-10 && i<index+30;i++)
        {
            if(sub[i]<minn)
            {
                minn = sub[i];
                pos = i;
            }
            if(sub[i]>height)
            {
                break;
            }
        }
        return pos;
    }
}