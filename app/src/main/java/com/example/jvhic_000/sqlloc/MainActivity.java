package com.example.jvhic_000.sqlloc;

import android.*;
import android.Manifest;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.provider.BaseColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.EditText;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.content.pm.PackageManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    SQLiteHelp mSQLiteHelp;
    SQLiteDatabase mSQLDB;
    Button submitBtn;
    private EditText mMessText;
    Cursor mSQLCursor;
    SimpleCursorAdapter mSQLCursorAdapter;
    private static final int LOCATION_PERMISSION_RESULT = 17;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationListener mLocationListener;
    private LocationRequest mLocationRequest;
    private String mLatText;
    private String mLonText;

    public MainActivity() {
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //GoogleApi

        if(mGoogleApiClient == null){
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }


            //Location Request -Crazy fast and accuracy death to battery.
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(5000);

        //location Listerner - called when location is updated
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null)
                {
                    mLonText = String.valueOf(location.getLongitude());
                    mLatText = String.valueOf(location.getLatitude());
                }else{
                    mLonText ="N/A";
                }

            }
        };
        //SQL and entry stuff

        mSQLiteHelp = new SQLiteHelp(this);
        mSQLDB = mSQLiteHelp.getWritableDatabase();

        submitBtn = (Button) findViewById(R.id.btn_submit);
        submitBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                getPermissions();

                populateTable();
            }
        });


        populateTable();
    }

    //LOCATION STUFF!!!!
    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop(){
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        Dialog errDialog = GoogleApiAvailability.getInstance().getErrorDialog(this,connectionResult.getErrorCode(), 0);
        errDialog.show();
        return;
    }

    private void getPermissions(){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_RESULT);
            return;
        }else {

            updateLocation();
            addItem();
            populateTable();
        }

    }

    @Override
    public void onConnected(@Nullable Bundle bundle)
    {


    }

    private void updateLocation(){


        //if we don't have permission enter in default
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            mLatText = "44.5";
            mLonText = "-123.2";

            return;
        }



        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if(mLastLocation !=null)
        {
            mLonText = String.valueOf(mLastLocation.getLongitude());
            mLatText = String.valueOf(mLastLocation.getLatitude());
        }else{
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,mLocationListener);

        }




    }

    @Override
    public void onConnectionSuspended (int i){
        return;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,int[] grantResults){
        if(requestCode == LOCATION_PERMISSION_RESULT){

            updateLocation();
            addItem();
            populateTable();
        }
    }


    //SQLite STUFF!!!
    private void addItem(){

        mMessText = (EditText)findViewById(R.id.mess_text);

        ContentValues newValues = new ContentValues();
        newValues.put(DBContract.Tabler.COLUMN_NAME_MESS, mMessText.getText().toString());
        newValues.put(DBContract.Tabler.COLUMN_NAME_LAT, mLatText);
        newValues.put(DBContract.Tabler.COLUMN_NAME_LON, mLonText);
        mSQLDB.insert(DBContract.Tabler.TABLE_NAME,null,newValues);



    }
    private void populateTable(){


        mSQLCursor = mSQLDB.query(DBContract.Tabler.TABLE_NAME,
                new String[]{DBContract.Tabler._ID, DBContract.Tabler.COLUMN_NAME_MESS,DBContract.Tabler.COLUMN_NAME_LAT,DBContract.Tabler.COLUMN_NAME_LON},
                null,
                null,
                null,
                null,
                null

        );
        ListView SQLListView = (ListView) findViewById(R.id.sql_list_view);

        mSQLCursorAdapter = new SimpleCursorAdapter(this,
                R.layout.sql_item,
                mSQLCursor,
                new String[]{DBContract.Tabler.COLUMN_NAME_MESS,DBContract.Tabler.COLUMN_NAME_LAT, DBContract.Tabler.COLUMN_NAME_LON},
                new int[]{R.id.sql_listview_mess,R.id.sql_listview_lat,R.id.sql_listview_lon},
                0);
        SQLListView.setAdapter(mSQLCursorAdapter);
    }



}



final class DBContract {
    private DBContract(){};

    public final class Tabler implements BaseColumns {
        public static final String DB_NAME = "loc_db";
        public static final String TABLE_NAME = "messageloc";
        public static final String COLUMN_NAME_MESS = "message";
        public static final String COLUMN_NAME_LAT = "latitude";
        public static final String COLUMN_NAME_LON = "longitude";
        public static final int DB_VERSION = 9;

        // SQL Strings

        public static final String SQL_CREATE_TABLE = "CREATE TABLE " +
                Tabler.TABLE_NAME + "(" + Tabler._ID + " INTEGER PRIMARY KEY NOT NULL," +
                Tabler.COLUMN_NAME_MESS + " VARCHAR(255)," +
                Tabler.COLUMN_NAME_LAT + " REAL ," +
                Tabler.COLUMN_NAME_LON + " REAL);";

        public static final String SQL_TEST_TABLE_INSERT = "INSERT INTO " + TABLE_NAME +
                " (" + COLUMN_NAME_MESS + "," + COLUMN_NAME_LAT + ","+ COLUMN_NAME_LON + ") VALUES ('test', 123.4, 45.1);";

        public  static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS " + Tabler.TABLE_NAME;

    }

}

class SQLiteHelp extends SQLiteOpenHelper {

    public SQLiteHelp(Context context){
        super(context, DBContract.Tabler.DB_NAME, null, DBContract.Tabler.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        db.execSQL(DBContract.Tabler.SQL_CREATE_TABLE);

        ContentValues testValues = new ContentValues();
        testValues.put(DBContract.Tabler.COLUMN_NAME_MESS, "Test Create String");
        testValues.put(DBContract.Tabler.COLUMN_NAME_LAT, 22.3);
        testValues.put(DBContract.Tabler.COLUMN_NAME_LON, -102.21);
    // db.insert(DBContract.Tabler.TABLE_NAME,null,testValues);

    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DBContract.Tabler.SQL_DROP_TABLE);
        onCreate(db);
    }
}