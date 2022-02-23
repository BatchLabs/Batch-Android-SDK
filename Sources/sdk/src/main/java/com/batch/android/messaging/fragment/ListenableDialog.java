package com.batch.android.messaging.fragment;

/**
 * Interface for fragments that support adding an additional dismiss listener
 */
public interface ListenableDialog {
    void setDialogEventListener(DialogEventListener eventListener);
}
