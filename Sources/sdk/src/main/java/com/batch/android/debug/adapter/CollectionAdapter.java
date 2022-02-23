package com.batch.android.debug.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CollectionAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final Context context;

    private List<TagCollection> tagCollections;

    public CollectionAdapter(@NonNull Context context) {
        super();
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.tagCollections = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return tagCollections.size();
    }

    @Override
    public TagCollection getItem(int position) {
        return tagCollections.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final View view;
        final TextView collectionName;
        final ListView tagList;

        if (convertView == null) {
            view = inflater.inflate(R.layout.com_batchsdk_user_data_debug_collection_item, parent, false);
        } else {
            view = convertView;
        }

        collectionName = view.findViewById(R.id.com_batchsdk_user_data_debug_collection_name);
        tagList = view.findViewById(R.id.com_batchsdk_user_data_debug_tag_list);

        final TagCollection item = getItem(position);
        collectionName.setText(item.getName());
        tagList.setAdapter(item.getTagAdapter());
        return view;
    }

    public void add(String name, Set<String> tags) {
        ArrayAdapter<String> tagAdapter = new ArrayAdapter<>(
            context,
            android.R.layout.simple_list_item_1,
            android.R.id.text1
        );

        tagAdapter.setNotifyOnChange(false);
        for (String tag : tags) {
            tagAdapter.add(tag);
        }

        TagCollection newCollection = new TagCollection(name, tagAdapter);
        tagCollections.add(newCollection);
    }

    public void clear() {
        tagCollections.clear();
        notifyDataSetChanged();
    }

    private class TagCollection {

        private final String name;
        private ArrayAdapter<String> tagAdapter;

        TagCollection(String name, ArrayAdapter<String> tagAdapter) {
            super();
            this.name = name;
            this.tagAdapter = tagAdapter;
        }

        public String getName() {
            return name;
        }

        public ArrayAdapter<String> getTagAdapter() {
            return tagAdapter;
        }
    }
}
