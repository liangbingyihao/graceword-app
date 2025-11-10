package sdk.chat.demo.robot.api.model;

import com.google.gson.annotations.SerializedName;

public class Song {
    private final String id;
    private final String title;
    @SerializedName("english_title")
    private final String englishTitle;
    @SerializedName("play_url")
    private final String listenUrl;
    @SerializedName("sheet_url")
    private final String sheetMusicUrl;
    @SerializedName("ppt_url")
    private final String pptUrl;
    private final String composer;
    private final String lyricist;
    private final String album;
    private final String lyrics;
    private final String copyright;
    private final String artist;
    private boolean isLyricsExpanded;
    private boolean isSelected;

    public Song(String album, String id, String title, String englishTitle, String listenUrl, String sheetMusicUrl, String pptUrl, String composer, String lyricist, String lyrics, String copyright, String artist) {
        this.album = album;
        this.id = id;
        this.title = title;
        this.englishTitle = englishTitle;
        this.listenUrl = listenUrl;
        this.sheetMusicUrl = sheetMusicUrl;
        this.pptUrl = pptUrl;
        this.composer = composer;
        this.lyricist = lyricist;
        this.lyrics = lyrics;
        this.copyright = copyright;
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getComposer() {
        return composer;
    }

    public String getCopyright() {
        return copyright;
    }

    public String getId() {
        return id;
    }

    public boolean isLyricsExpanded() {
        return isLyricsExpanded;
    }

    public void setLyricsExpanded(boolean lyricsExpanded) {
        isLyricsExpanded = lyricsExpanded;
    }

    public String getListenUrl() {
        return listenUrl;
    }

    public String getLyricist() {
        return lyricist;
    }

    public String getLyrics() {
        return lyrics;
    }

    public String getSheetMusicUrl() {
        return sheetMusicUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getEnglishTitle() {
        return englishTitle;
    }

    public String getPptUrl() {
        return pptUrl;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public String getArtist() {
        return artist;
    }

    @Override
    public String toString() {
        return "Song{" +
                "album='" + album + '\'' +
                ", title='" + title + '\'' +
                ", englishTitle='" + englishTitle + '\'' +
                ", listenUrl='" + listenUrl + '\'' +
                ", sheetMusicUrl='" + sheetMusicUrl + '\'' +
                ", pptUrl='" + pptUrl + '\'' +
                ", composer='" + composer + '\'' +
                ", lyricist='" + lyricist + '\'' +
                ", lyrics='" + lyrics + '\'' +
                ", copyright='" + copyright + '\'' +
                ", artist='" + artist + '\'' +
                ", isLyricsExpanded=" + isLyricsExpanded +
                '}';
    }
}
