package com.team4naise.biodivrun.database;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.team4naise.biodivrun.data.Espece;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class BiodivDBAdapter {
    public static final int DB_VERSION = 1;
    public static final String DB_NAME = "biodiv_reunion.db";

    // --- TABLES ---
    private static final String TABLE_SPECIES = "species";
    private static final String TABLE_ZONES = "zones";
    private static final String TABLE_ESPECE_ZONE = "espece_zone";

    // --- COLONNES ---
    public static final String COL_ID = "id";
    public static final String COL_NOM_PEI = "nom_pei";
    public static final String COL_NOM_SCIENTIFIQUE = "nom_scientifique";
    public static final String COL_ORIGINE = "origine";
    public static final String COL_UICN = "uicn";
    public static final String COL_IDENTIFICATION = "identification";
    public static final String COL_HABITAT = "habitat";
    public static final String COL_IMAGE_PATH = "image_path";

    // --- COLONNES ZONES ---
    public static final String COL_Z_LAT_MIN = "lat_min";
    public static final String COL_Z_LAT_MAX = "lat_max";
    public static final String COL_Z_LON_MIN = "lon_min";
    public static final String COL_Z_LON_MAX = "lon_max";

    private SQLiteDatabase mDB;
    private final MyOpenHelper mOpenHelper;
    private final Context context;

    public BiodivDBAdapter(Context context) {
        this.context = context;
        this.mOpenHelper = new MyOpenHelper(context, DB_NAME, null, DB_VERSION);
    }

    public void open() throws SQLException {
        mOpenHelper.createDatabase();
        mDB = mOpenHelper.getReadableDatabase();
    }

    public void close() {
        if (mDB != null) {
            mDB.close();
        }
    }

    /**
     * Requête de jointure pour récupérer les espèces selon le carré de recherche (Bounding Box)
     */
    public ArrayList<Espece> getEspecesParIntersectionSpatiale(double userLatMin, double userLatMax, double userLonMin, double userLonMax) {
        ArrayList<Espece> listeEspeces = new ArrayList<>();

        String requeteSQL = "SELECT DISTINCT s.* FROM " + TABLE_SPECIES + " s " +
                "INNER JOIN " + TABLE_ESPECE_ZONE + " ez ON s." + COL_ID + " = ez.espece_id " +
                "INNER JOIN " + TABLE_ZONES + " z ON ez.zone_id = z.id " +
                "WHERE z." + COL_Z_LAT_MIN + " <= " + userLatMax + " AND z." + COL_Z_LAT_MAX + " >= " + userLatMin + " " +
                "AND z." + COL_Z_LON_MIN + " <= " + userLonMax + " AND z." + COL_Z_LON_MAX + " >= " + userLonMin + " " +
                "ORDER BY s." + COL_NOM_SCIENTIFIQUE + " ASC";

        Cursor c = mDB.rawQuery(requeteSQL, null);

        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                Espece espece = new Espece(
                        c.getString(c.getColumnIndexOrThrow(COL_NOM_PEI)),
                        c.getString(c.getColumnIndexOrThrow(COL_NOM_SCIENTIFIQUE)),
                        c.getString(c.getColumnIndexOrThrow(COL_ORIGINE)),
                        c.getString(c.getColumnIndexOrThrow(COL_UICN)),
                        c.getString(c.getColumnIndexOrThrow(COL_IDENTIFICATION)),
                        c.getString(c.getColumnIndexOrThrow(COL_HABITAT)),
                        c.getString(c.getColumnIndexOrThrow(COL_IMAGE_PATH))
                );
                listeEspeces.add(espece);
                c.moveToNext();
            }
        }
        c.close();
        return listeEspeces;
    }

    /**
     * Retourne la liste des zones où l'espèce a été observée.
     * Chaque zone est un double[4] = [lat_min, lat_max, lon_min, lon_max].
     * Utilisé par la vue carte pour positionner les marqueurs.
     */
    public List<double[]> getZonesForEspece(String nomScientifique) {
        List<double[]> zones = new ArrayList<>();
        String sql = "SELECT z." + COL_Z_LAT_MIN + ", z." + COL_Z_LAT_MAX + ", " +
                "       z." + COL_Z_LON_MIN + ", z." + COL_Z_LON_MAX + " " +
                "FROM " + TABLE_ZONES + " z " +
                "INNER JOIN " + TABLE_ESPECE_ZONE + " ez ON z.id = ez.zone_id " +
                "INNER JOIN " + TABLE_SPECIES + " s ON ez.espece_id = s." + COL_ID + " " +
                "WHERE s." + COL_NOM_SCIENTIFIQUE + " = ?";
        Cursor c = mDB.rawQuery(sql, new String[]{nomScientifique});
        while (c.moveToNext()) {
            zones.add(new double[]{
                    c.getDouble(0),
                    c.getDouble(1),
                    c.getDouble(2),
                    c.getDouble(3)
            });
        }
        c.close();
        return zones;
    }

    // --- HELPER (Copie de la DB depuis assets) ---
    private class MyOpenHelper extends SQLiteOpenHelper {
        private final Context mContext;

        public MyOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
            this.mContext = context;
        }

        public void createDatabase() {
            File dbFile = mContext.getDatabasePath(DB_NAME);
            if (!dbFile.exists()) {
                dbFile.getParentFile().mkdirs();
                try {
                    copyDatabase(dbFile);
                    Log.d("DB", "Base de données copiée avec succès !");
                } catch (IOException e) {
                    throw new Error("Erreur lors de la copie", e);
                }
            }
        }

        private void copyDatabase(File dbFile) throws IOException {
            InputStream myInput = mContext.getAssets().open(DB_NAME);
            OutputStream myOutput = new FileOutputStream(dbFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
            }
            myOutput.flush();
            myOutput.close();
            myInput.close();
        }

        @Override
        public void onCreate(SQLiteDatabase db) {}

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            mContext.deleteDatabase(DB_NAME);
        }
    }
}