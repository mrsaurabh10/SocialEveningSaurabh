package com.mysampleapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.mysampleapp.demo.DemoFragmentBase;

/**
 * Created by apple on 31/10/15.
 */
public class TeamMainFragment  extends DemoFragmentBase{

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.team_main, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button aroundYouBtn =  (Button) view.findViewById(R.id.aroundYouBtn);
        aroundYouBtn.setOnClickListener( new View.OnClickListener(){
                                             @Override
                                             public void onClick(View view) {
                                                 teamViewFragment();
                                            }
                                         }
        );
    }


    private void teamViewFragment(){
        TeamViewFragment fragment = new TeamViewFragment();
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .commit();
    }

}
