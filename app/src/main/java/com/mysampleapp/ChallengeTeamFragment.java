package com.mysampleapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.amazonaws.mobile.AWSMobileClient;
import com.mysampleapp.demo.DemoFragmentBase;

import java.util.ArrayList;

/**
 * Created by apple on 01/11/15.
 */
public class ChallengeTeamFragment extends DemoFragmentBase {

    private ArrayList<String> usersList;

    private ListView listView;

    public void setUsersList(ArrayList<String> usersList){
        this.usersList = usersList;
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        return inflater.inflate(R.layout.challenge_layout, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = (ListView) view.findViewById(R.id.userslistView);
        //listView.setAdapter(new ArrayAdapter<S>());

        String[] values = usersList.toArray(new String[usersList.size()]);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, values);

        TextView textView = new TextView(getActivity());
        textView.setText("Team Members");
        textView.setTextSize(20);
        listView.addHeaderView(textView);
        listView.setAdapter(adapter);

        Button sendChallengeBtn = (Button) view.findViewById(R.id.sendChallengeBtn);
        sendChallengeBtn.setOnClickListener( new View.OnClickListener() {
                                                 @Override
                                                 public void onClick(View view) {
                                                     //send notification to the usersList
                                                 }
                                             }

        );




    }
}
