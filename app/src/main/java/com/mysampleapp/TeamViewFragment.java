package com.mysampleapp;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.amazonaws.mobile.AWSConfiguration;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.content.ContentDownloadPolicy;
import com.amazonaws.mobile.content.ContentItem;
import com.amazonaws.mobile.content.ContentListHandler;
import com.amazonaws.mobile.content.ContentManager;
import com.amazonaws.mobile.content.ContentState;
import com.amazonaws.mobile.content.UserFileManager;
import com.amazonaws.mobile.user.IdentityManager;
import com.mysampleapp.demo.DemoFragmentBase;
import com.mysampleapp.demo.UserFilesBrowserFragment;
import com.mysampleapp.demo.content.ContentListItem;
import com.mysampleapp.demo.content.ContentListViewAdapter;
import com.software.shell.fab.ActionButton;

import java.util.List;

import bolts.Capture;


/**
 * Created by apple on 30/10/15.
 */
public class TeamViewFragment extends DemoFragmentBase implements View.OnClickListener,  AdapterView.OnItemClickListener {

    private static final String S3_PREFIX_PUBLIC = "public/";
    private UserFileManager userFileManager;

    private ListView listView;
    private ContentListViewAdapter contentListItems;
    private boolean listingContentInProgress;
    private  String userId;
    private  String userName;
    private IdentityManager identityManager;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        identityManager = AWSMobileClient.defaultMobileClient()
                .getIdentityManager();
        fetchUserIdentity();
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.team_layout, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ActionButton addImage = (ActionButton) view.findViewById(R.id.createTeam_action_button);
        addImage.setButtonColor(R.color.com_facebook_blue);
        addImage.setImageResource(R.drawable.fab_plus_icon);
        listView = (ListView) view.findViewById(R.id.teamlistView);
        listView.setOnItemClickListener(this);

        addImage.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                captureImageFragment();
            }
        });


        final Bundle args = getArguments();
        String bucket = AWSConfiguration.AMAZON_S3_USER_FILES_BUCKET;
        String prefix = S3_PREFIX_PUBLIC;

        final ProgressDialog dialog = getProgressDialog(
                R.string.content_progress_dialog_message_load_local_content);

        // Create the User File Manager
        AWSMobileClient.defaultMobileClient()
                .createUserFileManager(bucket,
                        prefix,
                        new UserFileManager.BuilderResultHandler() {

                            @Override
                            public void onComplete(final UserFileManager userFileManager) {
                                TeamViewFragment.this.userFileManager = userFileManager;
                                createContentList(getView(), userFileManager);
                                userFileManager.setContentRemovedListener(contentListItems);
                                dialog.dismiss();
                                refreshContent("");
                            }
                        });


    }

    private ProgressDialog getProgressDialog(final int resId, Object... args) {
        return ProgressDialog.show(getActivity(),
                getString(R.string.content_progress_dialog_title_wait),
                getString(resId, (Object[]) args));
    }

    private void createContentList(final View fragmentView, final ContentManager contentManager) {

        contentListItems = new ContentListViewAdapter(getActivity(), contentManager,
                new ContentListViewAdapter.ContentListPathProvider() {
                    @Override
                    public String getCurrentPath() {
                        return "";
                    }
                },
                new ContentListViewAdapter.ContentListCacheObserver() {
                    @Override
                    public void onCacheChanged() {
                        //refreshCacheSummary();
                    }
                },
                R.layout.fragment_demo_user_files_browser);
        contentListItems.setUserIdentity(userId);
        contentListItems.setUserFileManager(userFileManager);
        listView.setAdapter(contentListItems);
        //listView.setOnItemClickListener(this);
        listView.setOnCreateContextMenuListener(this);
    }


    private void refreshContent(final String newCurrentPath) {
        if (!listingContentInProgress)
        {
            listingContentInProgress = true;

            //refreshCacheSummary();

            // Remove all progress listeners.
            userFileManager.clearProgressListeners();

            // Clear old content.
            contentListItems.clear();
            contentListItems.notifyDataSetChanged();

            //currentPath = newCurrentPath;
            //updatePath();

            final ProgressDialog dialog = getProgressDialog(
                    R.string.content_progress_dialog_message_load_content);

            userFileManager.listAvailableContent(newCurrentPath, new ContentListHandler() {
                @Override
                public boolean onContentReceived(final int startIndex,
                                                 final List<ContentItem> partialResults,
                                                 final boolean hasMoreResults) {
                    // if the activity is no longer alive, we can stop immediately.
                    if (getActivity() == null) {
                        listingContentInProgress = false;
                        return false;
                    }
                    if (startIndex == 0) {
                        dialog.dismiss();
                    }

                    for (final ContentItem contentItem : partialResults) {
                        // Add the item to the list.
                        contentListItems.add(new ContentListItem(contentItem));

                       userFileManager.getContent(contentItem.getFilePath(), contentItem.getSize(),
                               ContentDownloadPolicy.DOWNLOAD_ALWAYS, false, contentListItems);

                        userFileManager.getContent(contentItem.getFilePath(), contentItem.getSize(),
                                ContentDownloadPolicy.DOWNLOAD_METADATA, false, contentListItems);

                        // If the content is transferring, ensure the progress listener is set.
                        final ContentState contentState = contentItem.getContentState();
                        if (ContentState.isTransferringOrWaitingToTransfer(contentState)) {
                            userFileManager.setProgressListener(contentItem.getFilePath(),
                                    contentListItems);
                        }
                    }
                    //contentListItems.sort(ContentListItem.contentAlphebeticalComparator);

                    if (!hasMoreResults) {
                        listingContentInProgress = false;
                    }
                    // Return true to continue listing.
                    return true;
                }

                @Override
                public void onError(final Exception ex) {
                    dialog.dismiss();
                    listingContentInProgress = false;
                    final Activity activity = getActivity();
                    if (activity != null) {
                        final AlertDialog.Builder errorDialogBuilder = new AlertDialog.Builder(activity);
                        errorDialogBuilder.setTitle(activity.getString(R.string.content_list_failure_text));
                        errorDialogBuilder.setMessage(ex.getMessage());
                        errorDialogBuilder.setNegativeButton(
                                activity.getString(R.string.content_dialog_ok), null);
                        errorDialogBuilder.show();
                    }
                }
            });
        }
    }


    @Override
    public void onClick(View view) {

    }

    private void captureImageFragment(){
        CaptureImageFragment fragment = new CaptureImageFragment();
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .commit();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

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
