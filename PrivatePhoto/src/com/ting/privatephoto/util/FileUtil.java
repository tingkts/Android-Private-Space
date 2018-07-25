package com.ting.privatephoto.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import com.ting.privatephoto.media.MediaItem;

public class FileUtil {
    private static FileUtil self;
    private Context context;

    public static FileUtil getInstance(Context ctx) {
        if (self == null) {
            self = new FileUtil(ctx);
        }
        return self;
    }

    public FileUtil(Context context) {
        this.context = context;
    }

    // TODO: <2> add recovery mechanism if fail cases that include file movement fail,
    // MediaStore change fail, PrivateItemProvider change fail.
    public enum NotifyMediaStoreFileChanged{NONE, INPUT_FILE, OUTPUT_FILE};
    public boolean moveFile(String inputDirPath, String[] fileName_, String outputDirPath,
            NotifyMediaStoreFileChanged notifyFileChanged, int mediaType) {
        String fileName = fileName_[0];
        try {
            //create output directory if it doesn't exist
            File dir = new File(outputDirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String renamedFile = outputDirPath+"/"+fileName;
            int renameSuffix = 0;
            do {
                if (renameSuffix > 0)
                    renamedFile = renamedFile.substring(0, renamedFile.lastIndexOf("."))
                            + "(" + renameSuffix + ")"
                            + renamedFile.substring(renamedFile.lastIndexOf("."));
                renameSuffix++;
            } while (new File(renamedFile).exists());
            fileName = renamedFile.substring(renamedFile.lastIndexOf("/")+1);
            fileName_[0] = fileName;

            InputStream in = new FileInputStream(inputDirPath+"/"+fileName);
            OutputStream out = new FileOutputStream(outputDirPath+"/"+fileName);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            // write the output file
            out.flush();
            out.close();
            // delete the original file
            new File(inputDirPath+"/"+fileName).delete();
        } catch (FileNotFoundException e) { e.printStackTrace();
        } catch (Exception e) { e.printStackTrace(); }
        boolean isInputFileExisted = new File(inputDirPath+"/"+fileName).exists();
        boolean isOutputFileExisted = new File(outputDirPath+"/"+fileName).exists();
        boolean isFileMoveSuccess = (!isInputFileExisted && isOutputFileExisted);
        Log.d(FileUtil.class.getSimpleName(), "[moveFile] move " + inputDirPath + "/" + fileName
                + " to " + outputDirPath + " " + (isFileMoveSuccess ? "success" : "failed")
                + ", isInputFileExisted=" + isInputFileExisted
                + ", isOutputFileExisted=" + isOutputFileExisted);
        boolean isMediaStoreChangeSuccess = false;
        if (isFileMoveSuccess) {
            if (notifyFileChanged == NotifyMediaStoreFileChanged.INPUT_FILE)
                isMediaStoreChangeSuccess = notifyMediaScannerFileChanged(inputDirPath+"/"+fileName, false, mediaType);
            else if (notifyFileChanged == NotifyMediaStoreFileChanged.OUTPUT_FILE)
                isMediaStoreChangeSuccess = notifyMediaScannerFileChanged(outputDirPath+"/"+fileName, true, mediaType);
        }
        return (isFileMoveSuccess && isMediaStoreChangeSuccess);
    }

    public boolean deleteFile(String filePath, String fileName) {
        boolean isDeleted = false;
        try {
            isDeleted = new File(filePath+"/"+fileName).delete();
        } catch (NullPointerException e) { e.printStackTrace();
        } catch (SecurityException e) { e.printStackTrace();
        } catch (Exception e) { e.printStackTrace(); }
        Log.d(FileUtil.class.getSimpleName(), "[deleteFile] delete " + filePath + "/" + fileName
                + " " + (isDeleted ? "success" : "fail"));
        return isDeleted;
    }

    private boolean notifyMediaScannerFileChanged(String filePath, boolean insertOrDelete, int mediaType) {
        Uri imageOrVideoUri = mediaType == MediaItem.MEDIA_TYPE_IMAGE ?
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI :
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        boolean isSuccess = true;
        if (insertOrDelete) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA, filePath);
            if (context.getContentResolver().insert(imageOrVideoUri, values) == null) isSuccess = false;
        } else {
            if (context.getContentResolver().delete(imageOrVideoUri,  MediaStore.MediaColumns.DATA + "=?",
                    new String[]{ filePath }) < 0) isSuccess = false;
        }
        return isSuccess;
    }
}