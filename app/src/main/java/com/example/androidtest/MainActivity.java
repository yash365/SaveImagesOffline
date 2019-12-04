package com.example.androidtest;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.androidtest.database.SQLiteHelper;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String IMAGE_DIRECTORY = "/android_test_yash";

    private int GALLERY = 1, CAMERA = 2;

    ImageButton topbutton, shuffleButton, favouriteButton, bottomButton;
    Button addImageButton, clotheListButton;
    ImageView topImage, bottomImage;
    ByteArrayOutputStream streamTop, streamBottom;
    boolean isTopImage, isBottomImage;
    private Uri selectedImageUriTop;
    private Uri selectedImageUriBottom;

    public static SQLiteHelper sqLiteHelper;


    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isTopImage = false;
        isBottomImage = false;

        sqLiteHelper = new SQLiteHelper(this, "ClotheDB.sqlite", null, 1);

        sqLiteHelper.queryData("CREATE TABLE IF NOT EXISTS CLOTHES(Id INTEGER PRIMARY KEY AUTOINCREMENT, image BLOB)");


        // referencing
        topbutton = (ImageButton) findViewById(R.id.topButton);
        shuffleButton = (ImageButton) findViewById(R.id.shuffleButton);
        favouriteButton = (ImageButton) findViewById(R.id.favouriteButton);
        bottomButton = (ImageButton) findViewById(R.id.bottomButton);
        topImage = (ImageView) findViewById(R.id.topImage);
        bottomImage = (ImageView) findViewById(R.id.bottomImage);
        addImageButton = (Button) findViewById(R.id.addImageButton);
        clotheListButton = (Button) findViewById(R.id.clotheListButton);

        // setting clicklistener
        topbutton.setOnClickListener(this);
        shuffleButton.setOnClickListener(this);
        favouriteButton.setOnClickListener(this);
        bottomButton.setOnClickListener(this);
        addImageButton.setOnClickListener(this);
        clotheListButton.setOnClickListener(this);

        Log.d(TAG, "inside onCreate");
    }

    @Override
    public void onClick(View view) {

        // top clothes
        if(view == topbutton){
            isTopImage = true;
            isBottomImage = false;
            requestMultiplePermissions();
            showPictureDialog();
            Log.d(TAG, "inside topButton onClick");
        }

        // bottom clothes
        if(view == bottomButton){
            isTopImage = false;
            isBottomImage = true;
            requestMultiplePermissions();
            showPictureDialog();
            Log.d(TAG, "inside bottom onClick");
        }

        if(view == addImageButton){
            try{
                //if(isTopImage){
               // if(topImage != null){
                    sqLiteHelper.insertData(
                            imageViewToByte(topImage)
                    );
                    Toast.makeText(getApplicationContext(), "Added successfully!", Toast.LENGTH_SHORT).show();
                //}

                    //Toast.makeText(getApplicationContext(), "Added successfully!", Toast.LENGTH_SHORT).show();
               // }

               // if(isBottomImage){
                //if(bottomImage != null){
                    sqLiteHelper.insertData(
                            imageViewToByte(bottomImage)
                    );
                    Toast.makeText(getApplicationContext(), "Added successfully!", Toast.LENGTH_SHORT).show();

                //}

              //  }

            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        if(view == clotheListButton){
            Intent intent = new Intent(MainActivity.this, ClothesList.class);
            startActivity(intent);
        }
    }

    public static byte[] imageViewToByte(ImageView image) {
        Bitmap bitmap = ((BitmapDrawable)image.getDrawable()).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        return byteArray;
    }

    // showing picture dialog
    private void showPictureDialog(){
        AlertDialog.Builder pictureDialog = new AlertDialog.Builder(this);
        pictureDialog.setTitle("Select Action");
        String[] pictureDialogItems = {
                "Select photo from gallery",
                "Capture photo from camera" };
        pictureDialog.setItems(pictureDialogItems,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                choosePhotoFromGallary();
                                break;
                            case 1:
                                takePhotoFromCamera();
                                break;
                        }
                    }
                });
        pictureDialog.show();
    }

    public void choosePhotoFromGallary() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(galleryIntent, GALLERY);
    }

    private void takePhotoFromCamera() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == this.RESULT_CANCELED) {
            return;
        }
        if (requestCode == GALLERY) {
            if (data != null) {
                Uri contentURI = data.getData();
                try {
                    //Toast.makeText(CreateAdActivity.this, "Image Saved!", Toast.LENGTH_SHORT).show();
                    if(isTopImage){
                        selectedImageUriTop = data.getData();
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentURI);
                        String path = saveImage(bitmap);
                        Log.d(TAG, "gallery top image path" + path);
                        topImage.setImageBitmap(bitmap);
                        getBytesFromBitmapTop(bitmap);
                    }
                    if(isBottomImage){
                        selectedImageUriBottom = data.getData();
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentURI);
                        String path = saveImage(bitmap);
                        Log.d(TAG, "gallery bottom image path" + path);
                        bottomImage.setImageBitmap(bitmap);
                        getBytesFromBitmapBottom(bitmap);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == CAMERA) {

            if(isTopImage){
                selectedImageUriTop = data.getData();
                Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
                assert thumbnail != null;
                topImage.setImageBitmap(thumbnail);
                getBytesFromBitmapTop(thumbnail);
                saveImage(thumbnail);
            }
            if(isBottomImage){
                selectedImageUriBottom = data.getData();
                Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
                assert thumbnail != null;
                bottomImage.setImageBitmap(thumbnail);
                getBytesFromBitmapBottom(thumbnail);
                saveImage(thumbnail);
            }


            //Toast.makeText(CreateAdActivity.this, "Image Saved!", Toast.LENGTH_SHORT).show();
        }
    }

//    void galleryImageFunction(String value, Intent data){
//        InputStream stream;
//        String filePath = null;
//        try{
//            stream = getContentResolver().openInputStream(data.getData());
//
//            Bitmap realImage = BitmapFactory.decodeStream(stream);
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            realImage.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//            byte[] b = baos.toByteArray();
//
//            String encodedImage  = Base64.encodeToString(b, Base64.DEFAULT);
//            Log.d(TAG, "encodedImage: " + encodedImage);
//
//            SharedPreferences sharePref = PreferenceManager.getDefaultSharedPreferences(this);
//            SharedPreferences.Editor editor = sharePref.edit();
//            editor.putString(value, encodedImage);
//            editor.apply();
//
//            String previouslyEncodedImage = sharePref.getString(value, "");
//
//            if(!previouslyEncodedImage.equalsIgnoreCase("")){
//                byte[] b1 = Base64.decode(previouslyEncodedImage, Base64.DEFAULT);
//                Bitmap bitmap = BitmapFactory.decodeByteArray(b1, 0, b.length);
//                bottomImage.setImageBitmap(bitmap);
//            }
//        }
//        catch (FileNotFoundException e){
//            Log.d(TAG, "FileNotFoundException isTopImage");
//            e.printStackTrace();
//        }
//    }

    // convert image to bytes
    public byte[] getBytesFromBitmapTop(Bitmap bitmap) {
        streamTop = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, streamTop);
        return streamTop.toByteArray();
    }

    public byte[] getBytesFromBitmapBottom(Bitmap bitmap) {
        streamBottom = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, streamBottom);
        return streamBottom.toByteArray();
    }


    public String saveImage(Bitmap myBitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        File wallpaperDirectory = new File(
                Environment.getExternalStorageDirectory() + IMAGE_DIRECTORY);
        // have the object build the directory structure, if needed.
        if (!wallpaperDirectory.exists()) {
            wallpaperDirectory.mkdirs();
        }

        try {
            File f = new File(wallpaperDirectory, Calendar.getInstance()
                    .getTimeInMillis() + ".jpg");
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            MediaScannerConnection.scanFile(this,
                    new String[]{f.getPath()},
                    new String[]{"image/jpeg"}, null);
            fo.close();
            Log.d("TAG", "File Saved::--->" + f.getAbsolutePath());

            return f.getAbsolutePath();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return "";
    }

    private void  requestMultiplePermissions(){
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            //Toast.makeText(getApplicationContext(), "All permissions are granted by user!", Toast.LENGTH_SHORT).show();
                        }

                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // show alert dialog navigating to Settings
                            //openSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).
                withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        Toast.makeText(getApplicationContext(), "Some Error! ", Toast.LENGTH_SHORT).show();
                    }
                })
                .onSameThread()
                .check();
    }
}
