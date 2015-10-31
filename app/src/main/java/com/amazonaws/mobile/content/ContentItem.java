//
// Copyright 2015 Amazon.com, Inc. or its affiliates (Amazon). All Rights Reserved.
//
// Code generated by AWS Mobile Hub. Amazon gives unlimited permission to 
// copy, distribute and modify it.
//

package com.amazonaws.mobile.content;

import java.io.File;
import java.util.Map;

/** All Content managed by ContentManager implements this interface. */
public interface ContentItem {
    /**
     * @return the time that this content item was last modified.
     */
    long getLastModifiedTime();

    /**
     * @return the relative path and name of the file for this content item.
     */
    String getFilePath();

    /**
     * @return the size of the file this content item represents.
     */
    long getSize();

    /**
     * @return the state of this content, See {@link ContentState}
     */
    ContentState getContentState();

    /**
     * Set the content state for this content item.
     * @param contentState the new content state.
     */
    void setContentState(final ContentState contentState);

    /**
     * @return the UserMeta Data for this content, See {@link ContentState}
     */
    Map<String, String> getUserMetaData ();

    /**
     * Set the content state for this content item.
     * @param contentState the new content state.
     */
    void setUserMetaData(Map<String,String> metaData);

}
