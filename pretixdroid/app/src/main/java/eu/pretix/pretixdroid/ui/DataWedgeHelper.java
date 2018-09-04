package eu.pretix.pretixdroid.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import eu.pretix.pretixdroid.R;

public class DataWedgeHelper {
    private Context ctx;

    public DataWedgeHelper(Context ctx) {
        this.ctx = ctx;
    }

    public boolean isInstalled() {
        try {
            PackageManager pm = ctx.getPackageManager();
            pm.getPackageInfo("com.symbol.datawedge", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void copyAllStagedFiles() throws IOException {
        File stagingDirectory = getStagingDirectory();
        File[] filesToStage = stagingDirectory.listFiles();
        File outputDirectory = new File ("/enterprise/device/settings/datawedge/autoimport");
        if (!outputDirectory.exists())
            outputDirectory.mkdirs();
        if (filesToStage.length == 0)
            return;
        for (int i = 0; i < filesToStage.length; i++)
        {
            //  Write the file as .tmp to the autoimport directory
            try {
                InputStream in = new FileInputStream(filesToStage[i]);
                File outputFile = new File(outputDirectory, filesToStage[i].getName() + ".tmp");
                OutputStream out = new FileOutputStream(outputFile);

                copyFile(in, out);

                //  Rename the temp file
                String outputFileName = outputFile.getAbsolutePath();
                outputFileName = outputFileName.substring(0, outputFileName.length() - 4);
                File fileToImport = new File(outputFileName);
                outputFile.renameTo(fileToImport);
                //set permission to the file to read, write and exec.
                fileToImport.setExecutable(true, false);
                fileToImport.setReadable(true, false);
                fileToImport.setWritable(true, false);
                Log.i("DataWddge", "DataWedge profile written successfully.");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
        out.flush();
        in.close();
        out.close();
    }

    private File getStagingDirectory()
    {
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        File stagingDirectory = new File(externalStorageDirectory.getPath(), "/datawedge_import");
        if (!stagingDirectory.exists()) {
            stagingDirectory.mkdirs();
        }
        return stagingDirectory;
    }


    public void install() throws IOException {
        File stgfile = new File(getStagingDirectory(), "dwprofile_pretix.db");
        if (stgfile.exists()) {
            return;
        }
        FileOutputStream stgout = new FileOutputStream(stgfile);

        InputStream rawin = ctx.getResources().openRawResource(R.raw.dwprofile);
        copyFile(rawin, stgout);
        copyAllStagedFiles();
    }
}
