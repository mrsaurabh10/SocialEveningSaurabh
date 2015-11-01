package com.amazonaws.mobile.content;
//
// Copyright 2015 Amazon.com, Inc. or its affiliates (Amazon). All Rights Reserved.
//
// Code generated by AWS Mobile Hub. Amazon gives unlimited permission to 
// copy, distribute and modify it.
//


import com.amazonaws.services.s3.model.ObjectMetadata;

import java.io.File;

public interface TransferHelper {
    String DIR_DELIMITER = "/" ;

    void download(String filePath, long fileSize, ContentProgressListener listener);
    void upload(File file, String filePath, ContentProgressListener listener);
    void upload(File file, String filePath,ContentProgressListener listener, ObjectMetadata metadata);
    void setProgressListener(String filePath, ContentProgressListener listener);
    void clearProgressListeners();
    long getSizeTransferring();
    boolean isTransferring(String filePath);
    boolean isTransferWaiting(String filePath);
    void destroy();
}
