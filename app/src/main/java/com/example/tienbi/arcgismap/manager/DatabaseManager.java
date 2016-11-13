package com.example.tienbi.arcgismap.manager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import com.example.tienbi.arcgismap.App;
import com.example.tienbi.arcgismap.mode.Location;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Created by TienBi on 09/10/2016.
 */
public class DatabaseManager {
    private Context context;
    String DATABASE_NAME = "gis.db";
    private static final String DB_PATH_SUFFIX = "/databases/";
    private SQLiteDatabase sqLiteDatabase=null;
    public static DatabaseManager instance;

    private DatabaseManager(Context context) {
        this.context = context;
        xuLySaoChepCSDL();
    }

    public static DatabaseManager getInstance(){
        if (instance==null){
            instance = new DatabaseManager(App.getContext());
        }
        return instance;
    }

    private void xuLySaoChepCSDL() {
        File dbFile = context.getDatabasePath(DATABASE_NAME);
        if (!dbFile.exists()) {
            try {
                copyDataBaseFromAsset();
                Toast.makeText(context, "Sao chép thành công", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void copyDataBaseFromAsset() {
        try {
            InputStream myInput = context.getAssets().open("data/"+DATABASE_NAME);
            String outFileName = layDuongDanLuuTru();
            File f = new File(context.getApplicationInfo().dataDir + DB_PATH_SUFFIX);
            if (!f.exists()) {
                f.mkdir();
            }
            OutputStream myOutput = new FileOutputStream(outFileName);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
            }
            myOutput.flush();
            myOutput.close();
            myInput.close();
        } catch (Exception ex) {
            Log.e("Loi", ex.toString());
        }
    }

    private String layDuongDanLuuTru() {
        return context.getApplicationInfo().dataDir + DB_PATH_SUFFIX + DATABASE_NAME;
    }

    private void openDataBase() {
        //if (sqLiteDatabase == null)
            sqLiteDatabase = context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
    }

    private void closeDataBase(){
        if(sqLiteDatabase!=null && sqLiteDatabase.isOpen())
            sqLiteDatabase.close();
    }
    public void insert(String nametable, ContentValues contentValues){
        openDataBase();
        sqLiteDatabase.insert(nametable,null,contentValues);
        closeDataBase();
    }
    public void delete(String nametable,String whereclause,String[] whereargs){
        openDataBase();
        sqLiteDatabase.delete(nametable,whereclause,whereargs);
        closeDataBase();
    }
    public ArrayList<Location> getLocationByType(int type){
        openDataBase();
        ArrayList<Location> listLoc=new ArrayList<>();
        Cursor cursor =sqLiteDatabase.rawQuery("Select * from marker where id_type="+type,null);
        if(cursor==null||cursor.getCount()==0) {
            return null;
        }
        while (cursor.moveToNext()) {
            int id_point=cursor.getInt(0);
            String name_point=cursor.getString(1);
            int id_type=cursor.getInt(2);
            String description=cursor.getString(3);
            float latitude=cursor.getFloat(5);
            float longtitude=cursor.getFloat(6);
            String state=cursor.getString(7);

            listLoc.add(new Location(id_point,name_point,id_type,description,latitude,longtitude,state));
        }
        closeDataBase();
        return listLoc;
    }
    public ArrayList<Location> getLocation(){
        openDataBase();
        ArrayList<Location> listLoc=new ArrayList<>();
        Cursor cursor =sqLiteDatabase.rawQuery("Select * from marker",null);
        if(cursor==null||cursor.getCount()==0) {
            return null;
        }
        while (cursor.moveToNext()) {
            int id_point=cursor.getInt(0);
            String name_point=cursor.getString(1);
            int id_type=cursor.getInt(2);
            String description=cursor.getString(3);
            float latitude=cursor.getFloat(5);
            float longtitude=cursor.getFloat(6);
            String state=cursor.getString(7);

            listLoc.add(new Location(id_point,name_point,id_type,description,latitude,longtitude,state));
        }
        closeDataBase();
        return listLoc;
    }
}
