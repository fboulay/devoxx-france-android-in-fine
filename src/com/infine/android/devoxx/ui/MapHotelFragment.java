/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.infine.android.devoxx.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TabHost;
import android.widget.TextView;

import com.infine.android.devoxx.R;
import com.infine.android.devoxx.util.AnalyticsUtils;

/**
 * Shows a {@link WebView} with a map of the conference venue.
 */
public class MapHotelFragment extends Fragment {

    /**
     * When specified, will automatically point the map to the requested room.
     */
    public static final String EXTRA_ROOM = "com.infine.android.devoxx.extra.ROOM";

    private static final String TAG_LEVEL_0 = "Level 0";
    private static final String TAG_LEVEL_1 = "Level -1";
    
    private ViewGroup mRootView;
    
    private int startTab;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        AnalyticsUtils.getInstance(getActivity()).trackPageView("/Map");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_map_detail, null);
        
        final Intent intent = BaseActivity.fragmentArgumentsToIntent(getArguments());
        Bundle extras = intent.getExtras();
		if (extras != null) {
			startTab = extras.getInt(EXTRA_ROOM);
		}
        TabHost tabHost = (TabHost) mRootView.findViewById(android.R.id.tabhost);
		tabHost.setup();
		tabHost.addTab(tabHost.newTabSpec(TAG_LEVEL_0).setIndicator(buildIndicator(R.string.tab_map_0))
				.setContent(R.id.tab_map_level0));
		tabHost.addTab(tabHost.newTabSpec(TAG_LEVEL_1).setIndicator(buildIndicator(R.string.tab_map_1))
				.setContent(R.id.tab_map_level1));
		tabHost.setCurrentTab(startTab);

        return mRootView;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.refresh_menu_items, menu);
    }

    
	/**
	 * Build a {@link View} to be used as a tab indicator, setting the requested
	 * string resource as its label.
	 * 
	 * @param textRes
	 * @return View
	 */
	private View buildIndicator(int textRes) {
		final TextView indicator = (TextView) getActivity().getLayoutInflater().inflate(R.layout.tab_indicator,
				(ViewGroup) mRootView.findViewById(android.R.id.tabs), false);
		indicator.setText(textRes);
		return indicator;
	}
	
	/**
	 * Given a room, return the floor of the room
	 * @param room
	 * @return 0 for floor 0, or 1 for floor -1
	 */
	public static int roomToFloor(String room) {
		if (room.toUpperCase().contains("SEINE")){
			return 0;
		}
		return 1;
	}

}
