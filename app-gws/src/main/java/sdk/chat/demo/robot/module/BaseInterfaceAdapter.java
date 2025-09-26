package sdk.chat.demo.robot.module;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import sdk.chat.core.Tab;
import sdk.chat.core.avatar.AvatarGenerator;
import sdk.chat.core.dao.Message;
import sdk.chat.core.dao.Thread;
import sdk.chat.core.dao.User;
import sdk.chat.core.interfaces.ChatOption;
import sdk.chat.core.interfaces.ChatOptionsDelegate;
import sdk.chat.core.interfaces.ChatOptionsHandler;
import sdk.chat.core.interfaces.InterfaceAdapter;
import sdk.chat.core.interfaces.LocalNotificationHandler;
import sdk.chat.core.notifications.NotificationDisplayHandler;
import sdk.chat.core.types.SearchActivityType;
import sdk.chat.core.ui.ProfileFragmentProvider;
import sdk.chat.core.utils.ProfileOption;

public class BaseInterfaceAdapter implements InterfaceAdapter {

    @Override
    public void initialize(Context context) {

    }

    @Override
    public Fragment privateThreadsFragment() {
        return null;
    }

    @Override
    public Fragment publicThreadsFragment() {
        return null;
    }

    @Override
    public Fragment contactsFragment() {
        return null;
    }

    @Override
    public Fragment profileFragment(User user) {
        return null;
    }

    @Override
    public Class<? extends Activity> getLoginActivity() {
        return null;
    }

    @Override
    public Class<? extends Activity> getMainActivity() {
        return null;
    }

    @Override
    public Class<? extends Activity> getChatActivity() {
        return null;
    }

    @Override
    public Class<? extends Activity> getThreadDetailsActivity() {
        return null;
    }

    @Override
    public Class<? extends Activity> getEditThreadActivity() {
        return null;
    }

    @Override
    public Class<? extends Activity> getAddUsersToThreadActivity() {
        return null;
    }

    @Override
    public Class<? extends Activity> getCreateThreadActivity() {
        return null;
    }

    @Override
    public Class<? extends Activity> getForwardMessageActivity() {
        return null;
    }

    @Override
    public Class<? extends Activity> getSearchActivity() {
        return null;
    }

    @Override
    public Class<? extends Activity> getEditProfileActivity() {
        return null;
    }

    @Override
    public Class<? extends Activity> getProfileActivity() {
        return null;
    }

    @Override
    public Class<? extends Activity> getSplashScreenActivity() {
        return null;
    }

    @Override
    public Class<? extends Activity> getPostRegistrationActivity() {
        return null;
    }

    @Override
    public Class<? extends Activity> getModerationActivity() {
        return null;
    }

    @Override
    public Class<? extends Activity> getSettingsActivity() {
        return null;
    }

    @Override
    public Class<? extends Activity> getImageEditorActivity() {
        return null;
    }

    @Override
    public void setLoginActivity(Class<? extends Activity> loginActivity) {

    }

    @Override
    public void setSplashScreenActivity(Class<? extends Activity> splashScreenActivity) {

    }

    @Override
    public void setMainActivity(Class<? extends Activity> mainActivity) {

    }

    @Override
    public void setChatActivity(Class<? extends Activity> chatActivity) {

    }

    @Override
    public void setThreadDetailsActivity(Class<? extends Activity> threadDetailsActivity) {

    }

    @Override
    public void setEditThreadActivity(Class<? extends Activity> editThreadActivity) {

    }

    @Override
    public void setForwardMessageActivity(Class<? extends Activity> forwardMessageActivity) {

    }

    @Override
    public void setAddUsersToThreadActivity(Class<? extends Activity> addUsersToThreadActivity) {

    }

    @Override
    public void setCreateThreadActivity(Class<? extends Activity> createThreadActivity) {

    }

    @Override
    public void setSearchActivity(Class<? extends Activity> searchActivity) {

    }

    @Override
    public void setEditProfileActivity(Class<? extends Activity> editProfileActivity) {

    }

    @Override
    public void setProfileActivity(Class<? extends Activity> profileActivity) {

    }

    @Override
    public void setModerationActivity(Class<? extends Activity> moderationActivity) {

    }

    @Override
    public void setSettingsActivity(Class<? extends Activity> settingsActivity) {

    }

    @Override
    public void setImageEditorActivity(Class<? extends Activity> imageEditorActivity) {

    }

    @Override
    public void setPostRegistrationActivity(Class<? extends Activity> postRegistrationActivity) {

    }

    @Override
    public void setPrivateThreadsFragment(Fragment privateThreadsFragment) {

    }

    @Override
    public void setPublicThreadsFragment(Fragment publicThreadsFragment) {

    }

    @Override
    public void setContactsFragment(Fragment contactsFragment) {

    }

    @Override
    public void setProfileFragmentProvider(ProfileFragmentProvider profileFragmentProvider) {

    }

    @Override
    public Intent getLoginIntent(Context context, Map<String, Object> extras) {
        return null;
    }

    @Override
    public void setLoginIntent(Intent intent) {

    }

    @Override
    public List<Tab> defaultTabs() {
        return Collections.emptyList();
    }

    @Override
    public List<Tab> tabs() {
        return Collections.emptyList();
    }

    @Override
    public Tab privateThreadsTab() {
        return null;
    }

    @Override
    public Tab publicThreadsTab() {
        return null;
    }

    @Override
    public Tab contactsTab() {
        return null;
    }

    @Override
    public void setTab(Tab tab, int index) {

    }

    @Override
    public void setTab(String title, Drawable icon, Fragment fragment, int index) {

    }

    @Override
    public void removeTab(int index) {

    }

    @Override
    public void startImageEditorActivity(Activity activity, String path, int resultCode) {

    }

    @Override
    public void startActivity(Context context, Class<? extends Activity> activity) {

    }

    @Override
    public void startActivity(Context context, Intent intent) {

    }

    @Override
    public void startChatActivityForID(Context context, String threadEntityID) {

    }

    @Override
    public void startChatActivityForID(Context context, String threadEntityID, @Nullable Integer flags) {

    }

    @Override
    public void startActivity(Context context, Class<? extends Activity> activity, Map<String, Object> extras, int flags) {

    }

    @Override
    public void startEditThreadActivity(Context context, String threadEntityID) {

    }

    @Override
    public void startEditThreadActivity(Context context, String threadEntityID, ArrayList<String> userEntityIDs) {

    }

    @Override
    public void startThreadDetailsActivity(Context context, String threadEntityID) {

    }

    @Override
    public void startModerationActivity(Context context, String threadEntityID, String userEntityID) {

    }

    @Override
    public void startProfileActivity(Context context, String userEntityID) {

    }

    @Override
    public void startEditProfileActivity(Context context, String userEntityID) {

    }

    @Override
    public void startMainActivity(Context context) {

    }

    @Override
    public void startMainActivity(Context context, Map<String, Object> extras) {

    }

    @Override
    public void startSearchActivity(Context context) {

    }

    @Override
    public void startForwardMessageActivityForResult(Activity activity, Thread thread, List<Message> message, int code) {

    }

    @Override
    public void startPostRegistrationActivity(Context context, Map<String, Object> extras) {

    }

    @Override
    public void startAddUsersToThreadActivity(Context context, String threadEntityID) {

    }

    @Override
    public void startCreateThreadActivity(Context context) {

    }

    @Override
    public void startSplashScreenActivity(Context context) {

    }

    @Override
    public void addSearchActivity(Class<? extends Activity> className, String name, int requestCode) {

    }

    @Override
    public void addSearchActivity(Class<? extends Activity> className, String name) {

    }

    @Override
    public void removeSearchActivity(Class<? extends Activity> className) {

    }

    @Override
    public List<SearchActivityType> getSearchActivities() {
        return Collections.emptyList();
    }

    @Override
    public void addChatOption(ChatOption option) {

    }

    @Override
    public void removeChatOption(ChatOption option) {

    }

    @Override
    public List<ChatOption> getChatOptions() {
        return Collections.emptyList();
    }

    @Override
    public void setChatOptionsHandler(ChatOptionsHandler handler) {

    }

    @Override
    public ChatOptionsHandler getChatOptionsHandler(ChatOptionsDelegate delegate) {
        return null;
    }

    @Override
    public boolean showLocalNotifications(Thread thread) {
        return false;
    }

    @Override
    public void setLocalNotificationHandler(LocalNotificationHandler handler) {

    }

    @Override
    public NotificationDisplayHandler notificationDisplayHandler() {
        return null;
    }

    @Override
    public AvatarGenerator getAvatarGenerator() {
        return null;
    }

    @Override
    public void setAvatarGenerator(AvatarGenerator avatarGenerator) {

    }

    @Override
    public void addProfileOption(ProfileOption option) {

    }

    @Override
    public void removeProfileOption(ProfileOption option) {

    }

    @Override
    public List<ProfileOption> getProfileOptions(User user) {
        return Collections.emptyList();
    }

    @Override
    public void stop() {

    }
}
