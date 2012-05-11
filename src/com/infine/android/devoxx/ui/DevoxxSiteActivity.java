package com.infine.android.devoxx.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class DevoxxSiteActivity extends BaseSinglePaneActivity {
    
    @Override
    protected Fragment onCreatePane() {
        return new WebClientFragment();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getActivityHelper().setupSubActivity();
    }    


}
