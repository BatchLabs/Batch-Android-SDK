package com.batch.android.debug.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.batch.android.Batch;
import com.batch.android.BatchPushRegistration;
import com.batch.android.BuildConfig;
import com.batch.android.R;

public class IdentifierDebugFragment extends Fragment implements View.OnClickListener {

    private TextView sdkVersion;
    private TextView installId;
    private TextView pushToken;

    public static IdentifierDebugFragment newInstance() {
        return new IdentifierDebugFragment();
    }

    private String getShareString() {
        String shareContent = "Batch SDK:\n";
        shareContent = shareContent.concat("Version: " + BuildConfig.SDK_VERSION + "\n");
        shareContent =
            shareContent.concat(
                String.format(
                    "%s: %s\n",
                    getString(R.string.com_batchsdk_identifier_debug_fragment_install_id),
                    Batch.User.getInstallationID()
                )
            );
        BatchPushRegistration registration = Batch.Push.getRegistration();
        if (registration != null) {
            shareContent =
                shareContent.concat(
                    String.format(
                        "%s: %s\n",
                        getString(R.string.com_batchsdk_identifier_debug_fragment_push_token),
                        registration.getToken()
                    )
                );
        } else {
            shareContent =
                shareContent.concat(
                    String.format(
                        "%s: %s\n",
                        getString(R.string.com_batchsdk_identifier_debug_fragment_push_token),
                        getString(R.string.com_batchsdk_debug_view_empty)
                    )
                );
        }

        return shareContent;
    }

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.com_batchsdk_identifier_debug_fragment, container, false);
        sdkVersion = view.findViewById(R.id.com_batchsdk_identifier_debug_fragment_sdk_version);
        installId = view.findViewById(R.id.com_batchsdk_identifier_debug_fragment_install_id);
        pushToken = view.findViewById(R.id.com_batchsdk_identifier_debug_fragment_push_token);

        View shareButton = view.findViewById(R.id.com_batchsdk_identifier_debug_fragment_share_button);
        shareButton.setOnClickListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        sdkVersion.setText(BuildConfig.SDK_VERSION);
        installId.setText(Batch.User.getInstallationID());

        BatchPushRegistration registration = Batch.Push.getRegistration();
        if (registration != null) {
            pushToken.setText(registration.getToken());
        } else {
            pushToken.setText(R.string.com_batchsdk_debug_view_empty);
        }
    }

    @Override
    public void onClick(View v) {
        String shareContent = getShareString();
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.com_batchsdk_debug_view_share));
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareContent);
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.com_batchsdk_debug_view_share)));
    }
}
