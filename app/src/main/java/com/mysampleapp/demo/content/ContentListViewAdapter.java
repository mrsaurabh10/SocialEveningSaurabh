package com.mysampleapp.demo.content;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.mobile.content.ContentDownloadPolicy;
import com.amazonaws.mobile.content.ContentItem;
import com.amazonaws.mobile.content.ContentManager;
import com.amazonaws.mobile.content.ContentProgressListener;
import com.amazonaws.mobile.content.ContentRemovedListener;
import com.amazonaws.mobile.content.ContentState;
import com.amazonaws.mobile.content.FileContent;
import com.amazonaws.mobile.content.S3ContentMeta;
import com.amazonaws.mobile.content.UserFileManager;
import com.amazonaws.mobile.util.StringFormatUtils;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.mysampleapp.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ContentListViewAdapter extends ArrayAdapter<ContentListItem>
    implements ContentProgressListener, ContentRemovedListener {
    private final String LOG_TAG = ContentListViewAdapter.class.getSimpleName();

    private final String folderText;

    private final ContentManager contentManager;
    private final ContentListPathProvider pathProvider;
    private final ContentListCacheObserver cacheObserver;
    private UserFileManager userFileManager;

    public void setUserFileManager(UserFileManager fileManager){
        userFileManager = fileManager;
    }

    /** Map from file name to content list item. */
    HashMap<String, ContentListItem> contentListItemMap = new HashMap<>();

    private String userIdentity;

    public void setUserIdentity(String userId){
        userIdentity = userId;
    }


    public interface ContentListPathProvider {
        String getCurrentPath();
    }

    public interface ContentListCacheObserver {
        void onCacheChanged();
    }


    public ContentListViewAdapter(final Context context,
                           final ContentManager contentManager,
                           final ContentListPathProvider pathProvider,
                           final ContentListCacheObserver cacheObserver,
                           final int resource) {
        super(context, resource);
        folderText = getContext().getString(R.string.content_folder_text);
        this.contentManager = contentManager;
        this.pathProvider = pathProvider;
        this.cacheObserver = cacheObserver;
    }

    @Override
    public void add(ContentListItem item) {
        if (item.getContentItem() != null) {
            contentListItemMap.put(item.getContentItem().getFilePath(), item);
        }
        super.add(item);
    }

    @Override
    public void remove(ContentListItem item) {
        contentListItemMap.remove(item.getContentItem().getFilePath());
        super.remove(item);
    }

    @Override
    public void clear() {
        contentListItemMap.clear();
        super.clear();
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        return generateContentItem(layoutInflater, getItem(position), convertView, parent);
    }

    /**
     * This exists only to optimize performance for finding the views in a list view item.
     */
    private class ViewHolder {
        TextView fileNameText;
        TextView fileSizeText;
        ImageView downloadImage;
        TextView downloadPercentText;
        ImageView favoriteImage;
        Button joinNowBtn;
    }

    private View generateContentItem(final LayoutInflater inflater, final ContentListItem listItem,
                                     final View convertView, ViewGroup parent) {
        final ContentItem contentItem = listItem.getContentItem();
        final View itemView;
        final TextView fileNameText;
        final TextView fileSizeText;
        final ImageView downloadImage;
        final TextView downloadPercentText;
        final ViewHolder holder;
        final ImageView favoriteImage;
        final Button joinNowBtn;
        final String contentName = contentItem.getFilePath();
        final ContentListItem item = contentListItemMap.get(contentItem.getFilePath());
        final Map<String,String> userMetaData = item.getMetaData();
//        if (convertView != null) {
//            itemView = convertView;
//            holder = (ViewHolder) itemView.getTag();
//            fileNameText = holder.fileNameText;
//            fileSizeText = holder.fileSizeText;
//            downloadImage = holder.downloadImage;
//            downloadPercentText = holder.downloadPercentText;
//            favoriteImage = holder.favoriteImage;
//            joinNowBtn = holder.joinNowBtn;
//        }
//        else
        {
            itemView = inflater.inflate(
                R.layout.demo_content_list_item, parent,false);
            holder = new ViewHolder();
            holder.fileNameText = fileNameText = (TextView) itemView.findViewById(
                    R.id.content_delivery_file_name);
            holder.fileSizeText = fileSizeText = (TextView) itemView.findViewById(
                    R.id.content_delivery_file_size_text);
            holder.downloadImage = downloadImage = (ImageView) itemView
                .findViewById(R.id.content_delivery_file_download_image);
            holder.downloadPercentText = downloadPercentText = (TextView) itemView.findViewById(
                    R.id.content_delivery_download_percentage);
            holder.favoriteImage = favoriteImage = (ImageView) itemView.findViewById(
                R.id.content_delivery_favorite_image);
            holder.joinNowBtn = joinNowBtn = (Button) itemView.findViewById(R.id.joinNowBtn);
            itemView.setMinimumHeight(100);
            itemView.setTag(holder);

            joinNowBtn.setTag(contentItem.getFilePath());
            fileNameText.setTag(contentItem.getFilePath());

            joinNowBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Log.v(LOG_TAG, "The local path Content Path is  " + userFileManager.getLocalContentPath());
                    Log.v(LOG_TAG, "The file name is " + item.getContentItem().getFilePath());

                    ContentListItem item = contentListItemMap.get(joinNowBtn.getTag());

                    Map<String, String> userSMetadata = item.getMetaData();
                    if (userSMetadata != null) {
                        String users = userSMetadata.get("team-users");
                        joinNowBtn.setText("Joining....");
                        if (users != null) {
                            users += ";" + userIdentity;
                            userSMetadata.put("team-users", users);
                        } else {
                            userSMetadata.put("team-users", userIdentity);
                        }

                        userFileManager.getLocalContentPath();
                        ObjectMetadata objectMetadata = new ObjectMetadata();
                        objectMetadata.setUserMetadata(userSMetadata);

                        final File file = new File(userFileManager.getLocalContentPath(), item.getContentItem().getFilePath());


                        userFileManager.uploadContent(file, file.getName(), new ContentProgressListener() {
                            @Override
                            public void onSuccess(final ContentItem contentItem) {
                                //contentListItems.add(new ContentListItem(contentItem));
                                //contentListItems.sort(ContentListItem.contentAlphebeticalComparator);
                                ///contentListItems.notifyDataSetChanged();
                                joinNowBtn.setText("Joined");
                                joinNowBtn.setEnabled(false);
                            }

                            @Override
                            public void onProgressUpdate(final String fileName, final boolean isWaiting,
                                                         final long bytesCurrent, final long bytesTotal) {
                                //                     dialog.setProgress((int) bytesCurrent);
                            }

                            @Override
                            public void onError(final String fileName, final Exception ex) {
                                //                   dialog.dismiss();
                                //showError(R.string.user_files_browser_error_message_upload_file,
                                //      ex.getMessage());
                            }
                        }, objectMetadata);


                    }
                }
            });

        }

        final String displayName = contentItem.getFilePath()
            .substring(pathProvider.getCurrentPath()
                .length());
        //fileNameText.setText(displayName.isEmpty() ? ".." : displayName);
        fileNameText.setTextColor(
            ContentState.REMOTE_DIRECTORY.equals(contentItem.getContentState()) ? Color.BLUE : Color.BLACK);

        ContentState contentState = contentItem.getContentState();
        if (ContentState.REMOTE_DIRECTORY.equals(contentState)) {
            fileSizeText.setText(folderText);
            downloadImage.setVisibility(View.INVISIBLE);
            downloadPercentText.setWidth(0);
            favoriteImage.setVisibility(View.INVISIBLE);
            return itemView;
        } else {
           // fileSizeText.setText(StringFormatUtils.getBytesString(contentItem.getSize(), false));
            fileSizeText.setText(displayName);
        }

        if (ContentState.isTransferring(contentState) && listItem.getBytesTransferred() == 0) {
            // Override the transferState to waiting if we haven't received a progress update yet
            // for this item.  At the next progress update it will reflect the appropriate
            // percentage.
            contentState = ContentState.TRANSFER_WAITING;
        }

        switch (contentState) {
            case REMOTE:
                downloadImage.setVisibility(View.INVISIBLE);
                break;
            case TRANSFER_WAITING:
            case CACHED_NEW_VERSION_TRANSFER_WAITING:
                downloadImage.setImageResource(R.mipmap.icon_delay);
                break;
            case TRANSFERRING:
            case CACHED_TRANSFERRING_NEW_VERSION:
                downloadPercentText.setWidth(favoriteImage.getLayoutParams().width);
                downloadPercentText.setText(
                    String.format("%.0f%%",
                        100.0 * listItem.getBytesTransferred() / contentItem.getSize()));
                downloadImage.setVisibility(View.INVISIBLE);
                downloadImage.getLayoutParams().width = 0;
                downloadImage.requestLayout();
                break;
            case CACHED:
                // Show the item as available by displaying the check icon.
                //downloadImage.setImageResource(R.mipmap.icon_check);
               File file =  ((FileContent)contentItem).getFile();
                //show thumbnail
//                Bitmap imgthumBitmap=null;
//                final int THUMBNAIL_SIZE = 64;
//                try {
//                    FileInputStream fis = new FileInputStream(file);
//                    imgthumBitmap = BitmapFactory.decodeStream(fis);
//
//                    imgthumBitmap = Bitmap.createScaledBitmap(imgthumBitmap,
//                            THUMBNAIL_SIZE, THUMBNAIL_SIZE, false);
//
//                    ByteArrayOutputStream bytearroutstream = new ByteArrayOutputStream();
//                    imgthumBitmap.compress(Bitmap.CompressFormat.JPEG, 100,bytearroutstream);
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }
                downloadImage.setImageBitmap(decodeFile(file));
                //downloadImage.setImageURI(Uri.fromFile(file));
                //downloadImage.setImageBitmap(imgthumBitmap);

                break;
            case CACHED_WITH_NEWER_VERSION_AVAILABLE:
                // Show the check mark with the download icon on top.
                downloadImage.setImageResource(R.mipmap.icon_check_dated);
                break;
        }

        if (!ContentState.isTransferring(contentItem.getContentState())) {
            downloadPercentText.setText("");
            downloadPercentText.setWidth(0);
            if (contentState != ContentState.REMOTE) {
                downloadImage.setVisibility(View.VISIBLE);
                downloadImage.getLayoutParams().width = favoriteImage.getLayoutParams().width;
                downloadImage.requestLayout();
            }
        }



        //ContentListItem item1 = contentListItemMap.get(fileNameText.getTag());
        //Map<String,String> userSMetaData = item1.getMetaData();

        if (userMetaData!=null && userMetaData.containsKey("team-name")){
            final String displayTeamName = userMetaData.get("team-name");

            fileNameText.setText(displayTeamName);
        }else
            fileNameText.setText("");

        //check for the ownership and users list
        if(userMetaData!= null){
            String ownerId = userMetaData.get("team-owner");
            boolean isOwnerOrUser = false;
            if ( ownerId != null && ownerId.equalsIgnoreCase(userIdentity)){
                //joinNowBtn.setVisibility(View.GONE);
                isOwnerOrUser = true;
                joinNowBtn.setText("Owner");
                joinNowBtn.setEnabled(true);
            }

            String usersList = userMetaData.get("team-users");
            if(usersList!=null ){
                String[] usersArray = usersList.split(";");
                ArrayList<String> usersListArray = new ArrayList<String>(Arrays.asList(usersArray));
                if(usersListArray.contains(userIdentity)){
                    isOwnerOrUser = true;
                    joinNowBtn.setText("Member");
                    joinNowBtn.setEnabled(true);
                }
            }
//            if(isOwnerOrUser){
//                joinNowBtn.setVisibility(View.INVISIBLE);
//            }else{
//                joinNowBtn.setVisibility(View.VISIBLE);
//            }

        }

        if (contentManager.isContentPinned(contentName)) {
            favoriteImage.setImageResource(R.mipmap.icon_star);
            favoriteImage.setVisibility(View.VISIBLE);;
        } else {
            favoriteImage.setVisibility(View.INVISIBLE);;
        }

        return itemView;
    }

    @Override
    public void onProgressUpdate(final String filePath, final boolean isWaiting,
                                 final long bytesCurrent, final long bytesTotal) {
        // This is always called on the main thread.
        final ContentListItem item = contentListItemMap.get(filePath);

        if (item == null) {
            Log.w(LOG_TAG, String.format(
                "Warning progress update for item '%s' is not in the content list.", filePath));
            return;
        }

        if (isWaiting) {
            item.getContentItem().setContentState(ContentState.TRANSFER_WAITING);
        } else {
            if (!ContentState.isTransferring(item.getContentItem().getContentState())) {
                item.getContentItem().setContentState(ContentState.TRANSFERRING);
            }
            item.setBytesTransferred(bytesCurrent);
        }

        notifyDataSetChanged();
    }

    @Override
    public void onSuccess(final ContentItem contentItem) {
        final ContentListItem item = contentListItemMap.get(contentItem.getFilePath());
        if (item == null) {
            Log.w(LOG_TAG, String.format("Warning: item '%s' completed," +
                    " but is not in the content list.", contentItem.getFilePath()));
            return;
        }

        item.setContentItem(contentItem);

        if (contentItem instanceof S3ContentMeta){
            item.setMetaData(contentItem.getUserMetaData());
        }

        // sort calls notifyDataSetChanged()
        sort(ContentListItem.contentAlphebeticalComparator);
        cacheObserver.onCacheChanged();
    }


    @Override
    public void onError(final String filePath, final Exception ex) {
        final Context context = getContext();
        final AlertDialog.Builder errorDialogBuilder = new AlertDialog.Builder(context);
        errorDialogBuilder.setTitle(context.getString(R.string.content_transfer_failure_text));
        errorDialogBuilder.setMessage(ex.getMessage());
        errorDialogBuilder.setNegativeButton(
            context.getString(R.string.content_dialog_ok), null);
        errorDialogBuilder.show();

        if (filePath != null) {
            final ContentListItem item = contentListItemMap.get(filePath);
            if (item == null) {
                Log.w(LOG_TAG, String.format(
                    "Warning file removed for item '%s' is not in the content list.", filePath));
                return;
            }
            item.getContentItem().setContentState(ContentState.REMOTE);

            notifyDataSetChanged();
        }
    }

    private String getRelativeFilePath(final String absolutePath) {
        final String localPath = contentManager.getLocalContentPath();

        if (absolutePath.startsWith(localPath)) {
            return absolutePath.substring(localPath.length() + 1);
        }
        return null;
    }

    @Override
    public void onFileRemoved(final File file) {
        final String filePath = getRelativeFilePath(file.getAbsolutePath());

        cacheObserver.onCacheChanged();

        final ContentListItem item = contentListItemMap.get(filePath);
        if (item == null) {
            Log.w(LOG_TAG, String.format(
                "Warning file removed for item '%s' is not in the content list.", filePath));
            return;
        }
        // Content state needs to be reverted to remote.
        item.getContentItem().setContentState(ContentState.REMOTE);

        notifyDataSetChanged();

        // get the item state from the server.
        contentManager.getContent(filePath, 0, ContentDownloadPolicy.DOWNLOAD_METADATA_IF_NOT_CACHED, false,
            new ContentProgressListener() {
                @Override
                public void onSuccess(final ContentItem contentItem) {
                    item.setContentItem(contentItem);
                    ContentListViewAdapter.this.sort(ContentListItem.contentAlphebeticalComparator);
                }

                @Override
                public void onProgressUpdate(String fileName, boolean isWaiting,
                                             long bytesCurrent, long bytesTotal) {
                    // Nothing to do here.
                }

                @Override
                public void onError(String fileName, Exception ex) {
                    // Remove the item since we can't determine if it exists anymore.
                    ContentListViewAdapter.this.remove(item);
                }
            });
    }

    @Override
    public void onRemoveError(File file) {
        final AlertDialog.Builder errorDialogBuilder = new AlertDialog.Builder(getContext());
        errorDialogBuilder.setTitle(getContext().getString(R.string.content_removal_error_text));
        errorDialogBuilder.setMessage(String.format("Can't remove file '%s'.", file.getName()));
        errorDialogBuilder.setNegativeButton(
            getContext().getString(R.string.content_dialog_ok), null);
        errorDialogBuilder.show();
    }


    // Decodes image and scales it to reduce memory consumption
    private Bitmap decodeFile(File f) {
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);

            // The new size we want to scale to
            final int REQUIRED_SIZE=70;

            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while(o.outWidth / scale / 2 >= REQUIRED_SIZE &&
                    o.outHeight / scale / 2 >= REQUIRED_SIZE) {
                scale *= 2;
            }

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {}
        return null;
    }
}
