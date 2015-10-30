package com.mysampleapp;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.mysampleapp.demo.DemoFragmentBase;
import com.mysampleapp.demo.content.ContentListItem;
import com.mysampleapp.demo.content.ContentListViewAdapter;
import com.software.shell.fab.ActionButton;

import java.util.List;


/**
 * Created by apple on 30/10/15.
 */
public class TeamViewFragment extends DemoFragmentBase {

    private static final String S3_PREFIX_PUBLIC = "public/";
    private UserFileManager userFileManager;

    private ListView listView;
    private ContentListViewAdapter contentListItems;
    private boolean listingContentInProgress;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.team_layout, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);



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
                                //userFileManager.setContentRemovedListener(contentListItems);
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
        listView = (ListView) fragmentView.findViewById(R.id.teamlistView);
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

                        // If the content is transferring, ensure the progress listener is set.
                        final ContentState contentState = contentItem.getContentState();
                        if (ContentState.isTransferringOrWaitingToTransfer(contentState)) {
                            userFileManager.setProgressListener(contentItem.getFilePath(),
                                    contentListItems);
                        }
                    }
                    contentListItems.sort(ContentListItem.contentAlphebeticalComparator);

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



}
