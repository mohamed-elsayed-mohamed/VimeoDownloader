package vimeo;

import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class VideoDownloader {

    private String title;

    private long duration;

    private Map<String, String> streams;

    private Map<String, String> thumbs;

    protected VideoDownloader(){

    }

    protected VideoDownloader(@NotNull String json){
        streams = new HashMap<>();
        thumbs = new HashMap<>();
        parseJson(json);
    }

    private void parseJson(String jsonString) {
        try {

            JSONObject requestJson = new JSONObject(jsonString);

            JSONObject videoInfo = requestJson.getJSONObject("video");
            this.duration = videoInfo.getLong("duration");
            this.title = videoInfo.getString("title");
            JSONObject thumbsInfo = videoInfo.getJSONObject("thumbs");

            Iterator<String> iterator;
            for(iterator = thumbsInfo.keys(); iterator.hasNext();) {
                String key = iterator.next();
                this.thumbs.put(key, thumbsInfo.getString(key));
            }

            JSONArray streamArray = requestJson.getJSONObject("request")
                    .getJSONObject("files")
                    .getJSONArray("progressive");

            for (int streamIndex = 0; streamIndex < streamArray.length(); streamIndex++) {
                JSONObject stream = streamArray.getJSONObject(streamIndex);
                String url = stream.getString("url");
                String quality = stream.getString("quality");
                this.streams.put(quality, url);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean isVimeoURLValid(String url){
        String videoID = "";
        String[] urlParts = url.split("/");
        if(urlParts.length != 0){
            videoID = urlParts[urlParts.length - 1];
        }

        return videoID.length() > 0 && Utils.isDigitsOnly(videoID);
    }

    public String getTitle() {
        return md5(title);
    }

    public long getDuration() {
        return duration;
    }

    public boolean hasStreams(){
        return streams.size() > 0;
    }

    public boolean isHD(){
        return streams.containsKey("1080p") || streams.containsKey("4096p");
    }

    public Map<String, String> getStreams() {
        return streams;
    }

    public boolean hasThumbs() {
        return this.thumbs.size() > 0;
    }

    public Map<String, String> getThumbs() {
        return thumbs;
    }

    private String md5(String fileName) {
        final String MD5 = "MD5";
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance(MD5);
            digest.update(fileName.getBytes());
            byte messageDigest[] = digest.digest();

            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return fileName;
    }
}
