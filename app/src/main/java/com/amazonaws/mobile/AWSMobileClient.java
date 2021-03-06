//
// Copyright 2015 Amazon.com, Inc. or its affiliates (Amazon). All Rights Reserved.
//
// Code generated by AWS Mobile Hub. Amazon gives unlimited permission to 
// copy, distribute and modify it.
//

package com.amazonaws.mobile;

import android.content.Context;
import android.util.Log;

import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobile.push.PushManager;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.regions.Regions;
import com.amazonaws.mobile.content.UserFileManager;
import com.amazonaws.mobile.content.ContentManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The AWS Mobile Client bootstraps the application to make calls to AWS 
 * services. It creates clients which can be used to call services backing the
 * features you selected in your project.
 */
public class AWSMobileClient {

    private final static String LOG_TAG = AWSMobileClient.class.getSimpleName();

    private static AWSMobileClient instance;

    private final Context context;

    private IdentityManager identityManager;
    private PushManager pushManager;
    private CognitoSyncManager syncManager;

    /**
     * Build class used to create the AWS mobile client.
     */
    public static class Builder {

        private Context applicationContext;
        private String  cognitoIdentityPoolID;
        private Regions cognitoRegion;
        private IdentityManager identityManager;

	/**
	 * Constructor.
	 * @param context
	 */
        public Builder(final Context context) {
            this.applicationContext = context.getApplicationContext();
        };

	/**
	 * Provides the Amazon Cognito Identity Pool ID.
	 * @param cognitoIdentityPoolID identity pool ID
	 * @return builder
	 */
        public Builder withCognitoIdentityPoolID(final String cognitoIdentityPoolID) {
            this.cognitoIdentityPoolID = cognitoIdentityPoolID;
            return this;
        };
        
	/**
	 * Provides the Amazon Cognito service region.
	 * @param cognitoRegion service region
	 * @return builder
	 */
        public Builder withCognitoRegion(final Regions cognitoRegion) {
            this.cognitoRegion = cognitoRegion;
            return this;
        }

        /**
         * Provides the identity manager.
	 * @param identityManager identity manager
	 * @return builder
	 */
        public Builder withIdentityManager(final IdentityManager identityManager) {
            this.identityManager = identityManager;
            return this;
        }

	/**
	 * Creates the AWS mobile client instance and initializes it.
	 * @return AWS mobile client
	 */
        public AWSMobileClient build() {
            return
                new AWSMobileClient(applicationContext,
                        cognitoIdentityPoolID,
                        cognitoRegion,
                        identityManager) {};
        }
    }

    private AWSMobileClient(final Context context,
                            final String  cognitoIdentityPoolID,
                            final Regions cognitoRegion,
                            final IdentityManager identityManager) {

        this.context = context;
        this.identityManager = identityManager;


        this.pushManager = new PushManager(context, identityManager.getCredentialsProvider(),
                AWSConfiguration.GOOGLE_CLOUD_MESSAGING_SENDER_ID,
                AWSConfiguration.AMAZON_SNS_PLATFORM_APPLICATION_ARN);
        final List<String> topics = new ArrayList<String>();
        topics.add(AWSConfiguration.AMAZON_SNS_DEFAULT_TOPIC_ARN);
        topics.addAll(Arrays.asList(AWSConfiguration.AMAZON_SNS_TOPIC_ARNS));
        final String[] topicsArray = topics.toArray(new String[topics.size()]);
        pushManager.setTopics(topicsArray);
        this.syncManager = new CognitoSyncManager(context, AWSConfiguration.AMAZON_COGNITO_REGION,
                identityManager.getCredentialsProvider());
    }

    /**
     * Sets the singleton instance of the AWS mobile client.
     * @param client client instance
     */
    public static void setDefaultMobileClient(AWSMobileClient client) {
        instance = client;
    }

    /**
     * Gets the default singleton instance of the AWS mobile client.
     * @return client
     */
    public static AWSMobileClient defaultMobileClient() {
        return instance;
    }

    /**
     * Gets the identity manager.
     * @return identity manager
     */
    public IdentityManager getIdentityManager() {
        return this.identityManager;
    }

    /**
     * Gets the push notifications manager.
     * @return push manager
     */
    public PushManager getPushManager() {
        return this.pushManager;
    }

    /**
     * Gets the Amazon Cognito Sync Manager, which is responsible for saving and
     * loading user profile data, such as game state or user settings.
     * @return sync manager
     */
    public CognitoSyncManager getSyncManager() {
        return syncManager;
    }

    /**
     * Creates and initialize the default AWSMobileClient if it doesn't already
     * exist using configuration constants from {@link AWSConfiguration}.
     *
     * @param context an application context.
     */
    public static void initializeMobileClientIfNecessary(final Context context) {
        if (AWSMobileClient.defaultMobileClient() == null) {
            Log.d(LOG_TAG, "Initializing AWS Mobile Client...");
            final AWSMobileClient awsClient =
                new AWSMobileClient.Builder(context)
                    .withCognitoRegion(AWSConfiguration.AMAZON_COGNITO_REGION)
                    .withCognitoIdentityPoolID(AWSConfiguration.AMAZON_COGNITO_IDENTITY_POOL_ID)
                    .withIdentityManager(new IdentityManager(context))
                    .build();

            AWSMobileClient.setDefaultMobileClient(awsClient);
        }
        Log.d(LOG_TAG, "AWS Mobile Client is OK");
    }

    /**
     * Creates a User File Manager instance, which facilitates file transfers
     * between the device and the specified Amazon S3 (Simple Storage Service) bucket.
     *
     * @param s3Bucket Amazon S3 bucket
     * @param s3FolderPrefix Folder pre-fix for files affected by this user file
     *                       manager instance
     * @param resultHandler handles the resulting UserFileManager instance
     */
    public void createUserFileManager(final String s3Bucket,
                                      final String s3FolderPrefix,
                                      final UserFileManager.BuilderResultHandler resultHandler) {

        new UserFileManager.Builder().withContext(context)
                .withIdentityManager(getIdentityManager())
                .withS3Bucket(s3Bucket)
                .withS3ObjectDirPrefix(s3FolderPrefix)
                .withLocalBasePath(context.getFilesDir().getAbsolutePath())
                .build(resultHandler);
    }

    /**
     * Creates the default Content Manager, which allows files to be downloaded from
     * the Amazon S3 (Simple Storage Service) bucket associated with the App Content
     * Delivery feature (optionally through Amazon CloudFront if Multi-Region CDN option
     * was selected).
     * @param resultHandler handles the resulting ContentManager instance
     */
    public void createDefaultContentManager(final ContentManager.BuilderResultHandler resultHandler) {
        new ContentManager.Builder()
                .withContext(context)
                .withIdentityManager(identityManager)
                .withS3Bucket(AWSConfiguration.AMAZON_CONTENT_DELIVERY_S3_BUCKET)
                .withLocalBasePath(context.getFilesDir().getAbsolutePath())
                .build(resultHandler);
    }
}
