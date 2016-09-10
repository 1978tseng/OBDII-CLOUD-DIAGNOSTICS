package com.example.android.bluetoothchat;

/**
 * Created by Chun-Wei Tseng on 2016/2/16.
 */
import android.content.Context;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;

/*
 * Handles basic helper functions used throughout the app.
 */
public class Util {
//    private static final String  IdentityPoolID = "us-east-1:a2a71a56-63a0-4953-841a-c89a78763de0";

    private static final String  IdentityPoolID =  "us-east-1:c81bbc26-ee33-44d3-bafd-bc98b83dfd8f"; //Cody

    // We only need one instance of the clients and credentials provider
    private static AmazonS3Client s3Client;
    private static CognitoCachingCredentialsProvider sCredProvider;
    private static TransferUtility sTransferUtility;

    private static CognitoCachingCredentialsProvider getCredProvider(Context context) {
        if (sCredProvider == null) {
            sCredProvider = new CognitoCachingCredentialsProvider(
                    context.getApplicationContext(),
                    IdentityPoolID,
                    Regions.US_EAST_1);
        }
        return sCredProvider;
    }

    public static AmazonS3Client getS3Client(Context context) {
        if (s3Client == null) {
            BasicAWSCredentials awsCreds = new BasicAWSCredentials("AKIAJJNH4PRBO6CM5BVQ", "4IlvEih+7d5id0i+rITFnaHTyQ0XSz3LXgIi+wPV");
             s3Client = new AmazonS3Client(awsCreds);
//            s3Client = new AmazonS3Client(getCredProvider(context.getApplicationContext()));
        }
        return s3Client;
    }

    public static TransferUtility getTransferUtility(Context context) {
        if (sTransferUtility == null) {
            sTransferUtility = new TransferUtility(getS3Client(context.getApplicationContext()), context.getApplicationContext());
        }

        return sTransferUtility;
    }

    public static String getBytesString(long bytes) {
        String[] quantifiers = new String[] {
                "KB", "MB", "GB", "TB"
        };
        double speedNum = bytes;
        for (int i = 0;; i++) {
            if (i >= quantifiers.length) {
                return "";
            }
            speedNum /= 1024;
            if (speedNum < 512) {
                return String.format("%.2f", speedNum) + " " + quantifiers[i];
            }
        }
    }

    public static void DeleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                DeleteRecursive(child);
        fileOrDirectory.delete();
    }
}
