package net.kdt.pojavlaunch.fragments;

import net.kdt.pojavlaunch.extra.ExtraConstants;

import git.artdeell.mojo.BuildConfig;

public class ElyByLoginFragment extends OAuthFragment {
    public static final String TAG = "ELYBY_LOGIN_FRAGMENT";

    public ElyByLoginFragment() {
        super("internalredirect",
                buildAuthorizationUrl(),
                ExtraConstants.ELYBY_LOGIN_TODO);
    }

    private static String buildAuthorizationUrl() {
        String clientId = BuildConfig.ELYBY_CLIENT_ID;
        if(clientId.isEmpty()) {
            clientId = "franyu-unconfigured";
        }
        return "https://account.ely.by/oauth2/v1" +
                "?client_id=" + clientId +
                "&redirect_uri=internalredirect%3A%2F%2Fcomplete" +
                "&response_type=code" +
                "&scope=account_info%20offline_access%20minecraft_server_session";
    }
}
