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

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import com.infine.android.devoxx.R;
import com.infine.android.devoxx.provider.ScheduleContract.Sessions;
import com.infine.android.devoxx.provider.ScheduleContract.Speakers;
import com.infine.android.devoxx.ui.phone.SessionDetailActivity;

/**
 * An activity that shows session and sandbox search results. This activity can be either single
 * or multi-pane, depending on the device configuration. We want the multi-pane support that
 * {@link BaseMultiPaneActivity} offers, so we inherit from it instead of
 * {@link BaseSinglePaneActivity}.
 */
public class SearchActivity extends BaseMultiPaneActivity {

    public static final String TAG_SESSIONS = "sessions";
    public static final String TAG_SPEAKERS = "speakers";

    private String mQuery;

    private TabHost mTabHost;
    private TabWidget mTabWidget;

    private SessionsFragment mSessionsFragment;
    private SpeakersFragment mSpeakersFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();        
        mQuery = intent.getStringExtra(SearchManager.QUERY);

        setContentView(R.layout.activity_search);

        final CharSequence title = getString(R.string.title_search_query, mQuery);
        getActivityHelper().setupActionBar(getTitle(), 0);
        getActivityHelper().setActionBarTitle(title);

        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabWidget = (TabWidget) findViewById(android.R.id.tabs);
        mTabHost.setup();

        setupSessionsTab();
        setupSpeakersTab();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getActivityHelper().setupSubActivity();

        ViewGroup detailContainer = (ViewGroup) findViewById(R.id.fragment_container_search_detail);
        if (detailContainer != null && detailContainer.getChildCount() > 1) {
            findViewById(android.R.id.empty).setVisibility(View.GONE);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        mQuery = intent.getStringExtra(SearchManager.QUERY);

        final CharSequence title = getString(R.string.title_search_query, mQuery);
        getActivityHelper().setActionBarTitle(title);

        mTabHost.setCurrentTab(0);

        mSessionsFragment.reloadFromArguments(getSessionsFragmentArguments());
        mSpeakersFragment.reloadFromArguments(getSpeakersFragmentArguments());
    }

    /**
     * Build and add "sessions" tab.
     */
    private void setupSessionsTab() {
        // TODO: this is very inefficient and messy, clean it up
        FrameLayout fragmentContainer = new FrameLayout(this);
        fragmentContainer.setId(R.id.fragment_sessions);
        fragmentContainer.setLayoutParams(
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                        ViewGroup.LayoutParams.FILL_PARENT));
        ((ViewGroup) findViewById(android.R.id.tabcontent)).addView(fragmentContainer);

        final FragmentManager fm = getSupportFragmentManager();
        mSessionsFragment = (SessionsFragment) fm.findFragmentByTag("sessions");
        if (mSessionsFragment == null) {
            mSessionsFragment = new SessionsFragment();
            mSessionsFragment.setArguments(getSessionsFragmentArguments());
            fm.beginTransaction()
                    .add(R.id.fragment_sessions, mSessionsFragment, "sessions")
                    .commit();
        } else {
            mSessionsFragment.reloadFromArguments(getSessionsFragmentArguments());
        }

        // Sessions content comes from reused activity
        mTabHost.addTab(mTabHost.newTabSpec(TAG_SESSIONS)
                .setIndicator(buildIndicator(R.string.starred_sessions))
                .setContent(R.id.fragment_sessions));
    }

    /**
     * Build and add "speaker" tab.
     * TODO : update it to search in speaker names ?
     */
    private void setupSpeakersTab() {
        // TODO: this is very inefficient and messy, clean it up
        FrameLayout fragmentContainer = new FrameLayout(this);
        fragmentContainer.setId(R.id.fragment_speakers);
        fragmentContainer.setLayoutParams(
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                        ViewGroup.LayoutParams.FILL_PARENT));
        ((ViewGroup) findViewById(android.R.id.tabcontent)).addView(fragmentContainer);

        final FragmentManager fm = getSupportFragmentManager();
        mSpeakersFragment = (SpeakersFragment) fm.findFragmentByTag("speakers");
        if (mSpeakersFragment == null) {
            mSpeakersFragment = new SpeakersFragment();
            mSpeakersFragment.setArguments(getSpeakersFragmentArguments());
            fm.beginTransaction()
                    .add(R.id.fragment_speakers, mSpeakersFragment, "speakers")
                    .commit();
        } else {
            mSpeakersFragment.reloadFromArguments(getSpeakersFragmentArguments());
        }

        // Vendors content comes from reused activity
        mTabHost.addTab(mTabHost.newTabSpec(TAG_SPEAKERS)
                .setIndicator(buildIndicator(R.string.starred_speakers))
                .setContent(R.id.fragment_speakers));
    }

    private Bundle getSessionsFragmentArguments() {
        return intentToFragmentArguments(
                new Intent(Intent.ACTION_VIEW, Sessions.buildSearchUri(mQuery)));
    }

    private Bundle getSpeakersFragmentArguments() {
        return intentToFragmentArguments(
                new Intent(Intent.ACTION_VIEW, Speakers.buildSearchUri(mQuery)));
    }

    /**
     * Build a {@link View} to be used as a tab indicator, setting the requested string resource as
     * its label.
     */
    private View buildIndicator(int textRes) {
        final TextView indicator = (TextView) getLayoutInflater().inflate(R.layout.tab_indicator,
                mTabWidget, false);
        indicator.setText(textRes);
        return indicator;
    }

    @Override
    public BaseMultiPaneActivity.FragmentReplaceInfo onSubstituteFragmentForActivityLaunch(
            String activityClassName) {
        if (findViewById(R.id.fragment_container_search_detail) != null) {
            // The layout we currently have has a detail container, we can add fragments there.
            findViewById(android.R.id.empty).setVisibility(View.GONE);
            if (SessionDetailActivity.class.getName().equals(activityClassName)) {
                clearSelectedItems();
                return new BaseMultiPaneActivity.FragmentReplaceInfo(
                        SessionDetailFragment.class,
                        "session_detail",
                        R.id.fragment_container_search_detail);
            } 
        }
        return null;
    }

    private void clearSelectedItems() {
        if (mSessionsFragment != null) {
            mSessionsFragment.clearCheckedPosition();
        }
        if (mSpeakersFragment != null) {
            mSpeakersFragment.clearCheckedPosition();
        }
    }
}
