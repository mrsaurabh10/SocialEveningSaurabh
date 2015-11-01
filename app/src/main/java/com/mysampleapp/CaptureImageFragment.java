package com.mysampleapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.amazonaws.mobile.AWSConfiguration;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.content.ContentItem;
import com.amazonaws.mobile.content.ContentProgressListener;
import com.amazonaws.mobile.content.UserFileManager;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobile.util.ImageSelectorUtils;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.mysampleapp.demo.DemoFragmentBase;
import com.mysampleapp.demo.content.ContentListItem;
import com.software.shell.fab.ActionButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by apple on 31/10/15.
 */
public class CaptureImageFragment extends DemoFragmentBase {

    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private ImageView imageView;
    private EditText ediText;
    String mCurrentPhotoPath;
    Uri mImageUri;
    private UserFileManager userFileManager;
    private static final String S3_PREFIX_PUBLIC = "public/";
    private IdentityManager identityManager;
    private String userName;
    private String userId;
    private String mImageFileName;


    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        identityManager = AWSMobileClient.defaultMobileClient()
                .getIdentityManager();
        fetchUserIdentity();
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.capture_image_layout, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imageView = (ImageView) view.findViewById(R.id.imageView2);
        ediText = (EditText) view.findViewById(R.id.teamNameTextView);


        ActionButton uploadBtn = (ActionButton) view.findViewById(R.id.uploadImageBtn);
        uploadBtn.setImageResource(R.mipmap.upload_icon);

        uploadBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                uploadImage();
            }
        });

        String bucket = AWSConfiguration.AMAZON_S3_USER_FILES_BUCKET;
        String prefix = S3_PREFIX_PUBLIC;
        AWSMobileClient.defaultMobileClient()
                .createUserFileManager(bucket,
                        prefix,
                        new UserFileManager.BuilderResultHandler() {

                            @Override
                            public void onComplete(final UserFileManager userFileManager) {
                                CaptureImageFragment.this.userFileManager = userFileManager;

                            }
                        });

         //Create the File where the photo should go
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            // Error occurred while creating the File
        }


        if(photoFile!=null){
            // create Intent to take a picture and return control to the calling application
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            mImageUri = Uri.fromFile(photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
            // start the image capture Intent
            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                File file = new File(getContext().getExternalCacheDir(), mImageFileName);

                imageView.setImageURI(mImageUri);

            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = this.getContext().getExternalFilesDir(null);

        File outputDir = getContext().getCacheDir(); // context being the Activity pointer
        File outputFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        mImageFileName =  imageFileName + ".jpg";
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + outputFile.getAbsolutePath();
        return outputFile;
    }

    private void uploadImage(){
        final Uri uri = mImageUri;

        final String path = mCurrentPhotoPath;
        Log.v(CaptureImageFragment.class.getSimpleName(),"The uri is" + uri.toString());
        final ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setTitle(R.string.content_progress_dialog_title_wait);
        dialog.setMessage(
                getString(R.string.user_files_browser_progress_dialog_message_upload_image,
                        path));
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMax((int) new File(path).length());
        dialog.setCancelable(false);
        dialog.show();

        final File file = new File(mImageUri.getPath());


        String currentPath = "";
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.addUserMetadata("team-name",ediText.getEditableText().toString());
        metadata.addUserMetadata("team-owner",userId);
        userFileManager.uploadContent(file, file.getName(), new ContentProgressListener() {
            @Override
            public void onSuccess(final ContentItem contentItem) {
                //contentListItems.add(new ContentListItem(contentItem));
                //contentListItems.sort(ContentListItem.contentAlphebeticalComparator);
                ///contentListItems.notifyDataSetChanged();
                teamMainFragment();
                dialog.dismiss();
            }

            @Override
            public void onProgressUpdate(final String fileName, final boolean isWaiting,
                                         final long bytesCurrent, final long bytesTotal) {
                dialog.setProgress((int) bytesCurrent);
            }

            @Override
            public void onError(final String fileName, final Exception ex) {
                dialog.dismiss();
                //showError(R.string.user_files_browser_error_message_upload_file,
                  //      ex.getMessage());
            }
        },metadata);
    }

    private void teamMainFragment(){
        TeamMainFragment fragment = new TeamMainFragment();
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .commit();
    }

    private void fetchUserIdentity(){

        AWSMobileClient.defaultMobileClient()
                .getIdentityManager()
                .getUserID(new IdentityManager.IdentityHandler() {

                    @Override
                    public void handleIdentityID(String identityId) {

                        //clearUserInfo();

                        // We have successfully retrieved the user's identity. You can use the
                        // user identity value to uniquely identify the user. For demonstration
                        // purposes here, we will display the value in a text view.
                        //userIdTextView.setText(identityId);
                        userId = identityId;
                        if (identityManager.isUserSignedIn()) {

                            userName = identityManager.getUserName();

                        }
                    }

                    @Override
                    public void handleError(Exception exception) {

                        //clearUserInfo();

                        // We failed to retrieve the user's identity. Set unknown user identitier
                        // in text view.
                        //userIdTextView.setText(unknownUserIdentityText);

                        final Context context = getActivity();

                        if (context != null && isAdded()) {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(R.string.identity_demo_error_dialog_title)
                                    .setMessage(getString(R.string.identity_demo_error_message_failed_get_identity)
                                            + exception.getMessage())
                                    .setNegativeButton(R.string.identity_demo_dialog_dismiss_text, null)
                                    .create()
                                    .show();
                        }
                    }
                });
    }
}