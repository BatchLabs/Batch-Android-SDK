package com.batch.android.debug.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.batch.android.Batch;
import com.batch.android.BatchAttributesFetchListener;
import com.batch.android.BatchTagCollectionsFetchListener;
import com.batch.android.BatchUserAttribute;
import com.batch.android.R;
import com.batch.android.debug.adapter.CollectionAdapter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class UserDataDebugFragment extends Fragment {

    private TextView customUserId;
    private ListView attributeList;
    private ListView collectionList;

    private ArrayAdapter<String> attributeAdapter;
    private CollectionAdapter collectionAdapter;

    public static UserDataDebugFragment newInstance() {
        return new UserDataDebugFragment();
    }

    private String formatAttribute(BatchUserAttribute attribute) {
        if (attribute.type == BatchUserAttribute.Type.DATE) {
            DateFormat df = new SimpleDateFormat(getString(R.string.com_batchsdk_debug_date_format), Locale.US);
            return df.format(attribute.getDateValue());
        }
        return attribute.value.toString();
    }

    private void loadAttributes() {
        if (attributeAdapter == null) {
            attributeAdapter =
                new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, android.R.id.text1);
        } else {
            attributeAdapter.clear();
            attributeAdapter.notifyDataSetChanged();
        }
        attributeList.setAdapter(attributeAdapter);

        Batch.User.fetchAttributes(
            getContext(),
            new BatchAttributesFetchListener() {
                @Override
                public void onSuccess(@NonNull Map<String, BatchUserAttribute> attributes) {
                    attributeAdapter.setNotifyOnChange(false);
                    for (Map.Entry<String, BatchUserAttribute> entry : attributes.entrySet()) {
                        attributeAdapter.add(entry.getKey() + ": " + formatAttribute(entry.getValue()));
                    }
                    attributeAdapter.notifyDataSetChanged();
                }

                @Override
                public void onError() {
                    attributeAdapter.clear();
                    attributeAdapter.notifyDataSetChanged();
                }
            }
        );
    }

    private void loadCollections() {
        if (collectionAdapter == null) {
            collectionAdapter = new CollectionAdapter(getContext());
        } else {
            collectionAdapter.clear();
            collectionAdapter.notifyDataSetChanged();
        }
        collectionList.setAdapter(collectionAdapter);

        Batch.User.fetchTagCollections(
            getContext(),
            new BatchTagCollectionsFetchListener() {
                @Override
                public void onSuccess(@NonNull Map<String, Set<String>> tagCollections) {
                    for (Map.Entry<String, Set<String>> entry : tagCollections.entrySet()) {
                        collectionAdapter.add("t." + entry.getKey(), entry.getValue());
                    }
                    collectionAdapter.notifyDataSetChanged();
                }

                @Override
                public void onError() {
                    collectionAdapter.clear();
                    collectionAdapter.notifyDataSetChanged();
                }
            }
        );
    }

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.com_batchsdk_user_data_debug_fragment, container, false);
        customUserId = view.findViewById(R.id.com_batchsdk_user_data_debug_fragment_user_id);
        attributeList = view.findViewById(R.id.com_batchsdk_user_data_debug_fragment_attribute_list);
        collectionList = view.findViewById(R.id.com_batchsdk_user_data_debug_fragment_collection_list);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        String userId = Batch.User.getIdentifier(getContext());
        if (userId == null) {
            customUserId.setText(R.string.com_batchsdk_debug_view_empty);
        } else {
            customUserId.setText(userId);
        }

        loadAttributes();
        loadCollections();
    }
}
