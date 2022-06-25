package com.example.yogapredictionapp.Utils;

import android.widget.TextView;

import com.example.yogapredictionapp.ml.Final;
import com.example.yogapredictionapp.ml.Final1;
import com.example.yogapredictionapp.ml.Final2;

import org.w3c.dom.Text;

public class Tools {
    public static int getIndexOfLargest( float[] array )
    {
        if ( array == null || array.length == 0 ) return -1; // null or empty

        int largest = 0;
        for ( int i = 1; i < array.length; i++ )
        {
            if ( array[i] > array[largest] ) largest = i;
        }
        return largest; // position of the first largest found
    }

    public static String getYogaPose(int i1, int i2, int i3){
        if (i1 == 0|| i2 == 0){
            return "No registered Yoga Pose detected";
        } else {
            i1--;
            i2--;
            if ((i1 == i2) && i2 == i3){
                return Constants.yogaPoses[i3];
            } else {
                return "No registered Yoga Pose detected";
            }
        }
    }

    public static String getYogaPose(int i1, int i2, int i3, TextView d1, TextView d2,
                                     TextView d3){

        if (i1 == 0){
            d1.setText("("+String.valueOf(i1) + ")" +
                    "No Pose");
        } else {
            d1.setText("("+String.valueOf(i1) + ")" + Constants.yogaPoses[(i1-1)]);
        }

        if (i2 == 0){
            d2.setText("("+String.valueOf(i2) + ")" + "No Pose");
        } else {
            d2.setText("("+String.valueOf(i2) + ")" + Constants.yogaPoses[(i2-1)]);
        }

        d3.setText("("+String.valueOf(i3) + ")" + Constants.yogaPoses[(i3)]);


        if (i1 == 0|| i2 == 0){
            return "No registered Yoga Pose detected";
        } else {
            i1--;
            i2--;

            int index = -1;

            if ((i1 == i2) && i2 == i3){
                return Constants.yogaPoses[i3];
            } else {
               if (i1 == i2){
                   index = i1;
               }

                if (i1 == i3){
                    index = i1;
                }

                if (i3 == i2){
                    index = i2;
                }

                if (index != -1){
                    return Constants.yogaPoses[index];
                } else {
                    return "No registered Yoga Pose Detected";
                }
            }
        }
    }
}
