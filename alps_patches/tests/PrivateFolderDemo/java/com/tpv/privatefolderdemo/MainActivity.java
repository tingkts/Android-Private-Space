package com.tpv.privatefolderdemo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
//import com.tpv.privatefolderdemo.R;

public class MainActivity extends Activity {
    TextView textRootPath;

    class UiItemGroup {
        Button btnCreate;
        Button btnDelete;
        TextView textMessage;
    }

    UiItemGroup uiFileInRoot;
    UiItemGroup uiFileInSubFolder;

    File PRIVATE_ROOT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PRIVATE_ROOT = getPrivateFilesDir();
        setContentView(R.layout.activity_main);
        textRootPath = (TextView)findViewById(R.id.id_text_private_folder_root);
        textRootPath.setText(PRIVATE_ROOT.toString());

        uiFileInRoot = new UiItemGroup();
        uiFileInRoot.btnCreate = (Button)findViewById(R.id.id_btn_root_create);
        uiFileInRoot.btnDelete = (Button)findViewById(R.id.id_btn_root_delete);
        uiFileInRoot.textMessage = (TextView)findViewById(R.id.id_text_root_message);
        uiFileInRoot.btnCreate.setOnClickListener(
            new View.OnClickListener() {
                public void onClick(View v) {
                    fileOperation(FilePath.ROOT, FileAction.CREATE);
                }
            }
        );
        uiFileInRoot.btnDelete.setOnClickListener(
                new View.OnClickListener() {
                public void onClick(View v) {
                    fileOperation(FilePath.ROOT, FileAction.DELETE);
                }
            }
        );
        uiFileInRoot.btnDelete.setEnabled(false);

        uiFileInSubFolder = new UiItemGroup();
        uiFileInSubFolder.btnCreate = (Button)findViewById(R.id.id_btn_subfolder_create);
        uiFileInSubFolder.btnDelete = (Button)findViewById(R.id.id_btn_subfolder_delete);
        uiFileInSubFolder.textMessage = (TextView)findViewById(R.id.id_text_subfolder_message);
        uiFileInSubFolder.btnCreate.setOnClickListener(
            new View.OnClickListener() {
                public void onClick(View v) {
                    fileOperation(FilePath.SUB_FOLDER, FileAction.CREATE);
                }
            }
        );
        uiFileInSubFolder.btnDelete.setOnClickListener(
            new View.OnClickListener() {
                public void onClick(View v) {
                    fileOperation(FilePath.SUB_FOLDER, FileAction.DELETE);
                }
            }
        );
        uiFileInSubFolder.btnDelete.setEnabled(false);
    }

    private enum FilePath {
        ROOT,
        SUB_FOLDER
    }

    private enum FileAction {
        CREATE,
        DELETE
    }

    void fileOperation(FilePath filePath, FileAction fileAction) {
        try {
            UiItemGroup uiItemGroup = null;
            File file = null;
            if (filePath == FilePath.ROOT) {
                uiItemGroup = uiFileInRoot;
                file = new File(PRIVATE_ROOT, "DemoFile.jpg");
            }
            else if (filePath == FilePath.SUB_FOLDER) {
                uiItemGroup = uiFileInSubFolder;
                new File(PRIVATE_ROOT.getPath()+"/subFolder").mkdir();
                file = new File(PRIVATE_ROOT.getPath()+"/subFolder", "DemoFile.jpg");
            }
            if (fileAction == FileAction.CREATE) {
                InputStream is = getResources().openRawResource(R.drawable.balloons);
                OutputStream os = new FileOutputStream(file);
                byte[] data = new byte[is.available()];
                is.read(data);
                os.write(data);
                is.close();
                os.close();
                if (file.exists()) {
                    uiItemGroup.textMessage.setText(file + " created.");
                    uiItemGroup.btnCreate.setEnabled(false);
                    uiItemGroup.btnDelete.setEnabled(true);
                }
            } else if (fileAction == FileAction.DELETE) {
                file.delete();
                if (!file.exists()) {
                    uiItemGroup.textMessage.setText(file + " deleted.");
                    uiItemGroup.btnCreate.setEnabled(true);
                    uiItemGroup.btnDelete.setEnabled(false);
                }
            }
        } catch (SecurityException e) {
            Log.e(getClass().getName(), "", e);
        } catch (IOException e) {
            Log.e(getClass().getName(), "", e);
        } catch (Exception e) {
            Log.e(getClass().getName(), "", e);
        }
    }
}