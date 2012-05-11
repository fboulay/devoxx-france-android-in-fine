package com.infine.android.devoxx.ui.adapter;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.codehaus.jackson.JsonProcessingException;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.infine.android.devoxx.R;
import com.infine.android.devoxx.data.TweetData;
import com.infine.android.devoxx.data.TweetsData;
import com.infine.android.devoxx.io.json.TwitterRemoteExecutor;
import com.infine.android.devoxx.util.UIUtils;


public class TwitterListAdapter extends BaseAdapter {
	
	private LayoutInflater mInflater;
	
	
	private final TwitterRemoteExecutor mTitterExecutor;
	
	private TweetsData mTweets;

	public TwitterListAdapter(Context context, HttpClient httpClient) {
		mInflater = LayoutInflater.from(context);
		mTitterExecutor = new TwitterRemoteExecutor(httpClient);
		triggerRefresh();
	}

	@Override
	public int getCount() {
		if (mTweets == null)
			return 0;
		return mTweets.getResults().size();
	}

	@Override
	public Object getItem(int position) {
		return mTweets.getResults().get(position);
	}

	@Override
	public long getItemId(int position) {
		return Long.valueOf(((TweetData) getItem(position)).getIdStr()) ;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;
		if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_tweet, null);
            holder = new ViewHolder();
            holder.icon = (ImageView) convertView.findViewById(R.id.list_item_tweet_icon);
            holder.text = (TextView) convertView.findViewById(R.id.list_item_tweet_text);
            convertView.setTag(holder);
		} else {
            holder = (ViewHolder) convertView.getTag();
        }
		
		if (getCount() >= position) {
			TweetData current = mTweets.getResults().get(position);
			holder.text.setText(current.getText());
			String imageUrl = current.getProfileImageUrl();
			UIUtils.displayImageLazily(mInflater.getContext(), imageUrl, holder.icon);
//			 BitmapUtils.fetchImage(mInflater.getContext(), imageUrl,
//                     new BitmapUtils.OnFetchCompleteListener() {
//                         public void onFetchComplete(Object cookie, Bitmap result) {
//                             if (result != null) {
//                                 holder.icon.setImageBitmap(result);
//                             }
//                         }
//                     });
		}
		
		return (convertView);
	}
	
	public void triggerRefresh() {
		
		try {
			mTweets = mTitterExecutor.executeGetAndDeserialize("http://search.twitter.com/search.json?q=devoxxfr", TweetsData.class);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
	
	
	static class ViewHolder {
		ImageView icon;
		TextView text;
    }
}
