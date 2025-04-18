package ai.liv.s2tlibrary;

import android.util.Log;

//import org.jtransforms.fft.DoubleFFT_1D;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by chander on 1/3/16.
 */
public class S2TVAD {
    private static final String TAG = S2TAudioRecorder.class.getName();
    private int noise_count;
    private int nb_of_100_ms;
    private int frame_time_milliseconds;
    private int overlap_time_milliseconds;
    private int frame_length;
    private int hard_stop;//Force stop after (hard_stop / 10) seconds
    private int overlap_length;
    private int score;
    private boolean condition;
    private double trailing_seconds;
    private int trailing_frames;
    private double[] trailing_scores;
    private double[] std_deviations;
    private int trailing_count;
    private ArrayList<Integer> max_scores;
    private ArrayList<double[]> noise;
    private double[][] covar;
    private int sRate;
    public boolean non_speech_detected;

    public S2TVAD(int sRate) {
        this.sRate = sRate;
        reset_vad_params();
    }

    public void reset_vad_params(){
        trailing_count = 0;
        noise_count = 0;
        nb_of_100_ms = 0;
        hard_stop = S2TConstants.PREF_TIMER_INTERVAL_VAL*10;
        score = 0;
        condition = false;
        non_speech_detected = false;
        trailing_seconds = 1;
        overlap_time_milliseconds = 10;
        overlap_length = (sRate * overlap_time_milliseconds) / 1000;
        frame_time_milliseconds = 20;
        frame_length = (sRate * frame_time_milliseconds) / 1000;
        covar = new double[5][5];
        trailing_frames = (int)(trailing_seconds * 1000 / (frame_time_milliseconds - overlap_time_milliseconds));
        trailing_scores = new double[trailing_frames];
        std_deviations = new double[trailing_frames];
        noise = new ArrayList<>();
        max_scores = new ArrayList<>();
    }


    private double distance(@NonNull double[] l1, double[] l2){
        double dist = 0.0;
        for (int i = 0; i < l1.length; i++) {
            dist += Math.pow((l1[i] - l2[i]), 2.0);
        }
        return Math.sqrt(dist);
    }

    double mahalanobis_distance(@NonNull double[] l1, double[][]S, double[] l2){
        double dist;
        double[] intermediate = new double[l1.length];
        for (int i = 0; i < l1.length; i++) {
            dist = 0.0;
            for (int j = 0; j < l1.length; j++){
                dist += (l1[j] - l2[j]) * S[j][i];
            }
            intermediate[i] = dist;
        }
        dist = 0.0;
        for (int i = 0; i < l1.length; i++) {
            dist += (intermediate[i] * (l1[i] - l2[i]));
        }
        return  Math.sqrt(dist);
    }

    @NonNull
    private double[] feature_n(double[] audioDataDoubles, int NFFT){
        double[] magnitude = new double[2*NFFT];
        double[] magnitude1 = new double[NFFT];
        //DoubleFFT_1D fft = new DoubleFFT_1D(NFFT);                                Uncomment this when using VAD
        //fft.complexForward(audioDataDoubles);
        double re, im;
        for(int i = 0; i < (2*NFFT); i+=2){
            re = audioDataDoubles[i];
            im = audioDataDoubles[i+1];
            magnitude[i] = Math.sqrt((re * re) + (im * im));
            magnitude[i+1] = 0.0;
        }
        //fft.complexForward(magnitude);                                            Uncomment this when using VAD
        for(int i = 0; i < NFFT; i++){
            re = magnitude[2*i];
            im = magnitude[(2*i)+1];
            magnitude1[i] = Math.sqrt((re * re) + (im * im));
        }
        double num = 0.0, den = 0.0;
        for (int i = 0; i < NFFT / 2; i++){
            num += magnitude1[i];
            den += (magnitude1[i] / (i+1));
        }
        double f_mean = num / den;
        double L = Math.floor(f_mean);

        double a, b, c, d, x, y, z, a_l0, a_l1, a_h0, a_h1;
        a = b = c = x = y = 0.0;
        for (int i = 0; i < L; i++){
            d = i + 1;
            z = Math.log(d)/ Math.log(2.0);

            a += (1.0 / d);
            b += ( z / d);
            c += (z * z / d);
            x += magnitude1[i] / d;
            y += (magnitude1[i] * z) / d;
        }
        a_l0 = ((c*x) - (b*y)) / ( (a*c) - (b*b));
        a_l1 = ((a*y) - (b*x) ) / ( (a*c) - (b*b));
        a = b = c = x = y = 0.0;
        for (int i = (int)L; i < NFFT / 2; i++){
            d = i + 1;
            a += (1.0 / d);
            z = Math.log(d)/ Math.log(2.0);
            b += ( z / d);
            c += (z * z / d);
            x += magnitude1[i] / d;
            y += (magnitude1[i] * z) / d;
        }
        a_h0 = ((c*x) - (b*y)) / ( (a*c) - (b*b));
        a_h1 = ((a*y) - (b*x) ) / ( (a*c) - (b*b));

        double[] features = new double[5];
        features[0] = f_mean;features[1] = a_l0;features[2] = a_l1;features[3] = a_h0;features[4] = a_h1;
        return features;
    }

    //This method decides the time to stop when nothing is being spoken. Logic for VAD is in this method
    public void calcAndStop(@NonNull ByteBuffer bb) {
        int length = bb.limit();
        short[] shorts = new short[length / 2];
        bb.order(ByteOrder.nativeOrder()).asShortBuffer().get(shorts);

        double[] avg_noise_vector = new double[5];
        double[] intermediate = new double[5];
        int NFFT = 1024;
        double[] audioDataDoubles = new double[2*NFFT];

        nb_of_100_ms++;
        //Ignore the first 100 ms
        if (nb_of_100_ms == 1){
            return;
        }
        int j = 0;
        double y;
        while (j <= shorts.length - frame_length) {
            Arrays.fill(audioDataDoubles, 0.0);
            for (int k = 0; k < frame_length; k++) {//Loop to get the data of one frame
                y = shorts[j] / 32768.0;
                audioDataDoubles[2 * k] = y;
                audioDataDoubles[(2 * k) + 1] = 0.0;
                j++;
            }
            j -= overlap_length;//Reduce j so that the next frame contains the overlap data

            //First 20 frames used for the estimation of average_noise_vector
            if (noise_count < 20){
                intermediate = feature_n(audioDataDoubles, NFFT);
                for (int ii = 0; ii < intermediate.length; ii++) {
                    avg_noise_vector[ii] += intermediate[ii];
                }
                noise_count++;
                noise.add(intermediate);
            }

            //Calculate the average noise vector
            else if (noise_count == 20) {

                for (int ii = 0; ii < avg_noise_vector.length; ii++) {
                    avg_noise_vector[ii] /= 10.0;
                }
                noise_count++;
                //Logic for finding the covariance matrix
                double[] vec = new double[5];
                for (int ii = 0; ii < 5; ii++) {
                    for (int jj = 0; jj < 5; jj++) {
                        double covar_sum = 0.0;
                        for (int kk = 0; kk < noise.size(); kk++) {
                            covar_sum += ((noise.get(kk)[ii] - avg_noise_vector[ii]) * (noise.get(kk)[jj] - avg_noise_vector[jj]));
                        }
                        double covar_ii_jj = covar_sum / noise.size();
                        covar[ii][jj] = covar_ii_jj;
                        covar[jj][ii] = covar_ii_jj;
                    }
                }
            }
            //Get into this section only after the average_noise_vector is computed
            else {
                intermediate = feature_n(audioDataDoubles, NFFT);
                score = (int)distance(intermediate, avg_noise_vector);//Parameterization, Wighted distance, Mahanalobis
                //score = (int)mahalanobis_distance(intermediate, covar, avg_noise_vector);//Threshold has to be changed on using this distance
                int ratio = 0;
                int max_sum_scores = 0;
                if (max_scores.size() <= 100){
                    max_scores.add(0-score);
                }
                else{
                    Collections.sort(max_scores);
                    if (0-score < max_scores.get(max_scores.size() - 1)){
                        max_scores.set(max_scores.size() - 1, 0-score);
                    }
                    for (int i = 0; i < max_scores.size(); i++)
                        max_sum_scores += 0 - max_scores.get(i);
                }

                trailing_scores[trailing_count%trailing_frames] = score;
                trailing_count++;
                Log.i("nb_of_100_ms ","nb_of_100_ms "+nb_of_100_ms+" hard_stop "+hard_stop);
                if (nb_of_100_ms >= hard_stop){
                    non_speech_detected = true;
                }
                else {
                    //Attempt to stop after 3.5 seconds
                    if (nb_of_100_ms > 25) {
                        non_speech_detected = true;
                        int sum = 0;
                        int avg = 0;
                        String trailing_scores_string = "";
                        for (int i = 0; i < trailing_frames; i++) {
                            sum += trailing_scores[i];
                            trailing_scores_string += trailing_scores[i] + " ";
                            if (trailing_scores[i] > 100) {
                                non_speech_detected = false;
                                break;
                            }
                            //Log.v("trailing_scores_string ", " trailing_scores_string before"+trailing_scores_string);
                        }
                        Log.i("trailing_scores_string ", " trailing_scores_string"+trailing_scores_string);
                    /*if (non_speech_detected)
                        Log.v(TAG, "Stopping after " + nb_of_100_ms + " X 100 ms");//*/
                        if (trailing_count >= trailing_frames)
                            avg = sum / trailing_frames;
                        else
                            avg = 0;
                        double sd = 0.0;
                        for (int i = 0; i < trailing_frames; i++) {
                            sd += Math.pow(Math.abs(avg - trailing_scores[i]), 2);
                        }
                        std_deviations[trailing_count % trailing_frames] = sd;
                    }
                }
            }
        }

        return;
    }
}
