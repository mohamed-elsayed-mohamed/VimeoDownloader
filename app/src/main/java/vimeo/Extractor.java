package vimeo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class Extractor {

    private static Extractor instance;

    private Extractor(){}

    public static Extractor getInstance() {
        if(instance == null){
            instance = new Extractor();
        }
        return instance;
    }

    public void fetchVideoWithIdentifier(@NotNull String identifier, @Nullable String referrer, @NotNull final OnExtrationListener listener){
        if(identifier.length() == 0){
            listener.onFailure(new IllegalArgumentException("Video identifier cannot be empty"));
            return;
        }

        final APIManager manager = new APIManager();
        try {
            manager.extractWithIdentifier(identifier, referrer).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    listener.onFailure(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if(response.isSuccessful()) {
                        VideoDownloader vimeoVideo = new VideoDownloader(response.body().string());
                        listener.onSuccess(vimeoVideo);
                    }else{
                        listener.onFailure(manager.getError(response));
                    }
                }
            });
        } catch (IOException e) {
            listener.onFailure(e);
            e.printStackTrace();
        }
    }

    public void fetchVideoWithURL(@NotNull String videoURL, @Nullable String referrer, @NotNull final OnExtrationListener listener){
        if(videoURL.length() == 0){
            listener.onFailure(new IllegalArgumentException("Video URL cannot be empty"));
            return;
        }
        VideoDownloader vimeoVideo = new VideoDownloader();
        if(!vimeoVideo.isVimeoURLValid(videoURL)) {
            listener.onFailure(new IllegalArgumentException("Vimeo URL is not valid"));
            return;
        }

        String videoID = "";
        String[] urlParts = videoURL.split("/");
        videoID = urlParts[urlParts.length - 1];

        fetchVideoWithIdentifier(videoID, referrer, listener);
    }
}
