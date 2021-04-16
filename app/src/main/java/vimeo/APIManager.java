package vimeo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class APIManager {
    protected static final String VIMEO_URL = "https://vimeo.com/%s";
    protected static final String VIMEO_CONFIG_URL = "https://player.vimeo.com/video/%s/config";

    protected Call extractWithIdentifier(@NotNull String identifier, @Nullable String referrer) throws IOException {

        String url = String.format(VIMEO_CONFIG_URL, identifier);

        if(Utils.isEmpty(referrer)){
            referrer = String.format(VIMEO_URL, identifier);
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("Referer", referrer)
                .build();

        return client.newCall(request);
    }

    protected Throwable getError(Response response) {

        switch (response.code()){
            case 404:
                return new IOException("Video could not be found");
            case 403:
                return new IOException("Video has restricted playback");
            default:
                return new IOException("An unknown error occurred");
        }
    }
}
