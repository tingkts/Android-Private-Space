package com.ting.privatephoto.media;

public abstract class MediaItem /*implements Comparable<MediaItem>*/ {
    public static final int UNDEFINED_ID_INDEX = -1;

    public static final int MEDIA_TYPE_IMAGE = 0;
    public static final int MEDIA_TYPE_VIDEO = 1;

    public long id;
    public String name;
    public String path;
    public int type;
    public int position = UNDEFINED_ID_INDEX;

//    @Override
//    public int compareTo(MediaItem another) {
//        return this.name.compareTo(another.name);
//    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MediaItem)) return false;
        MediaItem other = (MediaItem) obj;
        return (this.id == other.id);
    }

    @Override
    public String toString() {
        return "MediaItem (" + Integer.toHexString(hashCode()) + "):"
                + " id=" + id
                + ", name=" + name
                + ", path=" + path
                + ", type=" + type
                + ", position=" + position;
    }
}