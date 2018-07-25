package com.ting.privatephoto.media;

import java.util.ArrayList;
import android.graphics.Bitmap;
import static com.ting.privatephoto.media.MediaItem.UNDEFINED_ID_INDEX;

public class Album implements Comparable<Album>{
    public String id; // bucket id
    public String name;
    public String path;
    public int position;
    public int count;
    public ThumbnailEntry thumbnail;

    public Album() {
        position = UNDEFINED_ID_INDEX;
        thumbnail = new ThumbnailEntry();
    }

    @Override
    public int compareTo(Album another) {
        if (path == null || path.isEmpty())
            return this.name.compareTo(another.name);
        else
            return this.path.compareTo(another.path);
    }

    @Override
    public String toString() {
        return "album: bucketId=" + id + ", name=" + name + ", path=" + path +", count=" + count
                + ", position=" + position + ", " + thumbnail;
    }

    public static class AlbumList extends ArrayList<Album> {}

    public static class ThumbnailEntry {
        public long id;
        public Bitmap self;

        public ThumbnailEntry() {
            id = UNDEFINED_ID_INDEX;
            self = null;
        }

        public ThumbnailEntry(long id, Bitmap self) {
            this.id = id;
            this.self = self;
        }

        @Override
        public String toString() {
            return "thumbnail: id=" + id + ", " + self;
        }
    }
}