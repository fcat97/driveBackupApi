package media.uqab.libdrivebackup.util;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * @author github/fCat97
 */
class FileUtils {
    public static String a = "upload_part";

    private static String b = "FileUtils";

    private static Uri c = null;

    Context d;

    public FileUtils(Context context) {
        this.d = context;
    }

    @SuppressLint("NewApi")
    public String getPath(final Uri uri) {
        // check here to KITKAT or new version
        final boolean kp = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        String ut = null;
        String[] no = null;
        final Uri o = uri;
        // DocumentProvider

        if (kp) {
            // ExternalStorageProvider

            if (u(o)) {
                final String di = DocumentsContract.getDocumentId(o);
                final String[] po = di.split(":");
                final String tp = po[0];

                String fp = t(po);

                if (fp == null || !u(fp)) {
                    Log.d(b, "Copy files as a fallback");
                    fp = c(o, a);
                }

                if (fp != "") {
                    return fp;
                } else {
                    return null;
                }
            }


            // DownloadsProvider

            if (k(o)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    final String yt;
                    Cursor cursor = null;
                    try {
                        cursor = d.getContentResolver().query(o, new String[] {
                            MediaStore.MediaColumns.DISPLAY_NAME
                        }, null, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            String ni = cursor.getString(0);
                            String ac = Environment.getExternalStorageDirectory().toString() + "/Download/" + ni;
                            if (!TextUtils.isEmpty(ac)) {
                                return ac;
                            }
                        }
                    } finally {
                        if (cursor != null)
                            cursor.close();
                    }
                    yt = DocumentsContract.getDocumentId(o);

                    if (!TextUtils.isEmpty(yt)) {
                        if (yt.startsWith("raw:")) {
                            return yt.replaceFirst("raw:", "");
                        }
                        String[] db = new String[] {
                            "content://downloads/public_downloads",
                            "content://downloads/my_downloads"
                        };

                        for (String contentUriPrefix: db) {
                            try {
                                final Uri b = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.valueOf(yt));

                                return n(d, b, null, null);
                            } catch (NumberFormatException e) {
                                //In Android 8 and Android P the id is not a number
                                return o.getPath().replaceFirst("^/document/raw:", "").replaceFirst("^raw:", "");
                            }
                        }
                    }
                } else {
                    final String id = DocumentsContract.getDocumentId(o);

                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:", "");
                    }
                    try {
                        c = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }

                    if (c != null)
                        return n(d, c, null, null);
                }
            }


            // MediaProvider
            if (m(o)) {
                final String docId = DocumentsContract.getDocumentId(o);
                final String[] split = docId.split(":");
                final String type = split[0];

                Log.d(b, "MEDIA DOCUMENT TYPE: " + type);

                Uri contentUri = null;

                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                } else if ("document".equals(type)) {
                    contentUri = MediaStore.Files.getContentUri(MediaStore.getVolumeName(o));
                }

                ut = "_id=?";
                no = new String[] {
                    split[1]
                };


                return n(d, contentUri, ut, no);
            }

            if (d(o)) {
                return b(o);
            }

            if (w(o)) {
                return l(o);
            }

            if ("content".equalsIgnoreCase(o.getScheme())) {
                if (p(o)) {
                    return o.getLastPathSegment();
                }

                if (d(o)) {
                    return b(o);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // return getFilePathFromURI(context,uri);
                    return c(o, a);
                    // return getRealPathFromURI(context,uri);
                } else {
                    return n(d, o, null, null);
                }

            }

            if ("file".equalsIgnoreCase(o.getScheme())) {
                return o.getPath();
            }
        } else {
            if (w(o)) {
                return l(o);
            }

            if ("content".equalsIgnoreCase(o.getScheme())) {
                String[] projection = {
                    MediaStore.Images.Media.DATA
                };
                Cursor cursor = null;

                try {
                    cursor = d.getContentResolver()
                        .query(o, projection, ut, no, null);
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

                    if (cursor.moveToFirst()) {
                        return cursor.getString(column_index);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return c(o, a);
    }

    public File copyToAppsDir(final Uri uri, @Nullable final String parentDir) {
        File copiedFile = null;

        // Read the contents of the file using a ContentResolver
        ContentResolver resolver = d.getContentResolver();
        try (InputStream inputStream = resolver.openInputStream(uri)) {
            // Convert the URI to a DocumentFile
            DocumentFile documentFile = DocumentFile.fromSingleUri(d, uri);

            // Create a new File object
            File outputFile;
            if (parentDir == null) {
                outputFile = new File(d.getExternalFilesDir(null), documentFile.getName());
            } else {
                File parentFolder = new File(parentDir);
                if (!parentFolder.exists()) parentFolder.mkdirs();

                outputFile = new File(parentDir, documentFile.getName());
            }

            // Write the contents of the InputStream to the new file
            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // The file is now available as a java.io.File object
            copiedFile = outputFile;
        } catch (Exception e) {
            // Handle the exception
            Log.e(b, "copyToAppsDir: ", e);
        }

        return copiedFile;
    }

    private static boolean u(String hg) {
        File file = new File(hg);

        return file.exists();
    }

    private static String t(String[] g) {
        final String i = g[0];
        final String p = File.separator + g[1];
        String f = "";


        Log.d(b, "MEDIA EXTSD TYPE: " + i);
        Log.d(b, "Relative path: " + p);
        // on my Sony devices (4.4.4 & 5.1.1), `type` is a dynamic string
        // something like "71F8-2C0A", some kind of unique id per storage
        // don't know any API that can get the root path of that storage based on its id.
        //
        // so no "primary" type, but let the check here for other devices
        if ("primary".equalsIgnoreCase(i)) {
            f = Environment.getExternalStorageDirectory() + p;
            if (u(f)) {
                return f;
            }
        }

        if ("home".equalsIgnoreCase(i)) {
            f = "/storage/emulated/0/Documents" + p;
            if (u(f)) {
                return f;
            }
        }

        // Environment.isExternalStorageRemovable() is `true` for external and internal storage
        // so we cannot relay on it.
        //
        // instead, for each possible path, check if file exists
        // we'll start with secondary storage as this could be our (physically) removable sd card
        f = System.getenv("SECONDARY_STORAGE") + p;
        if (u(f)) {
            return f;
        }

        f = System.getenv("EXTERNAL_STORAGE") + p;
        if (u(f)) {
            return f;
        }

        return null;
    }

    private String b(Uri bn) {
        Uri v = bn;
        Cursor returnCursor = d.getContentResolver().query(v, null, null, null, null);
        /*
         * Get the column indexes of the data in the Cursor,
         *     * move to the first row in the Cursor, get the data,
         *     * and display it.
         * */
        int c = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int e = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        String l = (returnCursor.getString(c));
        String z = (Long.toString(returnCursor.getLong(e)));
        File a = new File(d.getCacheDir(), l);
        try {
            InputStream q = d.getContentResolver().openInputStream(bn);
            FileOutputStream w = new FileOutputStream(a);
            int ll = 0;
            int pp = 1 * 1024 * 1024;
            int mm = q.available();

            //int bufferSize = 1024;
            int ii = Math.min(mm, pp);

            final byte[] ss = new byte[ii];
            while ((ll = q.read(ss)) != -1) {
                w.write(ss, 0, ll);
            }
            Log.e(b, "Size " + a.length());
            q.close();
            w.close();
            Log.e(b, "Path " + a.getPath());
            Log.e(b, "Size " + a.length());
        } catch (Exception ex) {
            Log.e(b, ex.getMessage());
        }

        return a.getPath();
    }

    /***
     * Used for Android Q+
     * @param po
     * @param cv if you want to create a directory, you can set this variable
     * @return
     */
    private String c(Uri po, String cv) {
        Uri rr = po;

        Cursor uu = d.getContentResolver().query(rr, new String[] {
            OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
        }, null, null, null);


        /*
         * Get the column indexes of the data in the Cursor,
         *     * move to the first row in the Cursor, get the data,
         *     * and display it.
         * */
        int qq = uu.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int ss = uu.getColumnIndex(OpenableColumns.SIZE);
        uu.moveToFirst();
        String hh = (uu.getString(qq));
        String kk = (Long.toString(uu.getLong(ss)));

        File nn;
        if (!cv.equals("")) {
            String oo = UUID.randomUUID().toString();

            File ii = new File(d.getFilesDir() + File.separator + cv + File.separator + oo);
            if (!ii.exists()) {
                ii.mkdirs();
            }
            nn = new File(d.getFilesDir() + File.separator + cv + File.separator + oo + File.separator + hh);
        } else {
            nn = new File(d.getFilesDir() + File.separator + hh);
        }

        try {
            InputStream vv = d.getContentResolver().openInputStream(po);
            FileOutputStream jj = new FileOutputStream(nn);
            int dd = 0;
            int op = 1024;
            final byte[] buffers = new byte[op];

            while ((dd = vv.read(buffers)) != -1) {
                jj.write(buffers, 0, dd);
            }

            vv.close();
            jj.close();
        } catch (Exception e) {
            Log.e(b, e.getMessage());
        }

        return nn.getPath();
    }

    private String l(Uri kj) {
        return c(kj, "whatsapp");
    }

    private String n(Context vv, Uri lo, String jn, String[] tr) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
            column
        };

        try {
            cursor = vv.getContentResolver().query(lo, projection,
                jn, tr, null);

            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return null;
    }

    private static boolean u(Uri yh) {
        return "com.android.externalstorage.documents".equals(yh.getAuthority());
    }

    private static boolean k(Uri cj) {
        return "com.android.providers.downloads.documents".equals(cj.getAuthority());
    }

    private boolean m(Uri io) {
        return "com.android.providers.media.documents".equals(io.getAuthority());
    }

    private boolean p(Uri nr) {
        return "com.google.android.apps.photos.content".equals(nr.getAuthority());
    }

    public boolean w(Uri qw) {
        return "com.whatsapp.provider.media".equals(qw.getAuthority());
    }

    private boolean d(Uri xc) {
        return "com.google.android.apps.docs.storage".equals(xc.getAuthority()) || "com.google.android.apps.docs.storage.legacy".equals(xc.getAuthority());
    }
}