package vimeo;

public interface OnExtrationListener {

    void onSuccess(VideoDownloader video);
    void onFailure(Throwable throwable);
}
