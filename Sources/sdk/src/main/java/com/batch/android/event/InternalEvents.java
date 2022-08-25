package com.batch.android.event;

/*
 Groups all internal event names
 */
public final class InternalEvents {

    private InternalEvents() {}

    public static final String START = "_START";

    public static final String STOP = "_STOP";

    public static final String OPEN_FROM_PUSH = "_OPEN_PUSH";

    public static final String MESSAGING = "_MESSAGING";
    public static final String LOCAL_CAMPAIGN_VIEWED = "_LC_VIEW";

    public static final String PROFILE_CHANGED = "_PROFILE_CHANGED";
    public static final String INSTALL_DATA_CHANGED = "_INSTALL_DATA_CHANGED";
    public static final String INSTALL_DATA_CHANGED_TRACK_FAILURE = "_INSTALL_DATA_CHANGED_TRACK_FAIL";

    public static final String LOCATION_CHANGED = "_LOCATION_CHANGED";
    public static final String NOTIFICATION_STATUS_CHANGE = "_NOTIF_STATUS_CHANGE";

    public static final String INBOX_MARK_AS_READ = "_INBOX_MARK_READ";
    public static final String INBOX_MARK_AS_DELETED = "_INBOX_MARK_DELETED";
    public static final String INBOX_MARK_ALL_AS_READ = "_INBOX_MARK_ALL_READ";

    public static final String OPT_IN = "_OPT_IN";
    public static final String OPT_OUT = "_OPT_OUT";
    public static final String OPT_OUT_AND_WIPE_DATA = "_OPTOUT_WIPE_DATA";

    public static final String FIND_MY_INSTALLATION = "_FIND_MY_INSTALLATION";
}
