package com.appspace.main;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

// Helper class to connect to the database
public class AppspaceDbHelper extends SQLiteOpenHelper{

	private static String DB_PATH = "/data/data/com.appspace.main/databases/";
    private static String DB_NAME = "db";
    private SQLiteDatabase myDataBase; 
    private final Context myContext;
    
    public AppspaceDbHelper(Context context) {
    	super(context, DB_NAME, null, 1);
        this.myContext = context;
    }
    
    public void createDataBase() throws IOException {
    	boolean dbExist = checkDataBase();
    	if(dbExist){
    		//Database already exists
    	}else{
    		//By calling this method and empty database will be created into the default system path
        	this.getReadableDatabase();
        	try {
        		// Copy our database into system
    			copyDataBase();
    		} catch (IOException e) {
        		throw new Error("Error copying database");
        	}
    	}
    }
    
    private boolean checkDataBase() {
    	SQLiteDatabase checkDB = null;
    	try{
    		String myPath = DB_PATH + DB_NAME;
    		checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
    	}catch(SQLiteException e){
    		//database does't exist yet.
    	}
    	if(checkDB != null){
 
    		checkDB.close();
 
    	}
    	return checkDB != null ? true : false;
    }
    
    private void copyDataBase() throws IOException {
    	//Open local db as the input stream
    	InputStream myInput = myContext.getAssets().open(DB_NAME);
 
    	// Path to the just created empty db
    	String outFileName = DB_PATH + DB_NAME;
 
    	//Open the empty db as the output stream
    	OutputStream myOutput = new FileOutputStream(outFileName);
 
    	//transfer bytes from the inputfile to the outputfile
    	byte[] buffer = new byte[1024];
    	int length;
    	while ((length = myInput.read(buffer))>0){
    		myOutput.write(buffer, 0, length);
    	}
    	System.out.println("DB copied");
    	
    	//Close the streams
    	myOutput.flush();
    	myOutput.close();
    	myInput.close();
    }
    
    public void openDataBase() throws SQLException {
    	//Open the database
        String myPath = DB_PATH + DB_NAME;
    	myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
    }
    
    @Override
	public synchronized void close() {
    	    if(myDataBase != null)
    		    myDataBase.close();
    	    super.close();
	}
 
	@Override
	public void onCreate(SQLiteDatabase db) {
	}
 
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
}
