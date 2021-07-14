package com.abhilasha.androidclass.assignment_ocr;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    ImageView galleryImage;
    Button selectButton;
    private TextView txtResultBody;
    private static final int CAMERA_REQUEST = 1888;
    private static final int CAMERA_PERMISSION_CODE = 100;
   // private static final int STORAGE_PERMISSION_CODE= 101;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtResultBody = findViewById(R.id.txtResultBody);
        galleryImage = findViewById(R.id.viewImage);
        selectButton = findViewById(R.id.btnSelectPhoto);

        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                if(checkAndRequestPermissions(MainActivity.this)){
                    selectImage(MainActivity.this);
                }
            }
        });
    }

    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 101;

    public static boolean checkAndRequestPermissions(final Activity context) {
        int ExtstorePermission = ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        int cameraPermission = ContextCompat.checkSelfPermission(context,
                Manifest.permission.CAMERA);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (ExtstorePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded
                    .add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(context, listPermissionsNeeded
                            .toArray(new String[listPermissionsNeeded.size()]),
                    REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        // Inflate the menu; this adds options to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    private void selectImage (Context context) {
        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose your profile picture");

        builder.setItems(options, new DialogInterface.OnClickListener() {

            @Override
            public void onClick (DialogInterface dialog, int item) {

                if (options[item].equals("Take Photo")) {


                    Intent takePicture = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(takePicture, 0);

                } else if (options[item].equals("Choose from Gallery")) {
                    Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pickPhoto, 1);//one can be replaced with any action code

                } else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }


    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                case 0:
                    if (resultCode == RESULT_OK && data != null) {
                        Bitmap selectedImage = (Bitmap) data.getExtras().get("data");
                        galleryImage.setImageBitmap(selectedImage);
                        String result = detectBarCode(selectedImage);
                        txtResultBody.setText(result);
                    }

                    break;
                case 1:
                    if (resultCode == RESULT_OK && data != null) {
                        Uri selectedImage = data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        if (selectedImage != null) {
                            Cursor cursor = getContentResolver().query(selectedImage,
                                    filePathColumn, null, null, null);
                            if (cursor != null) {
                                cursor.moveToFirst();

                                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                                String picturePath = cursor.getString(columnIndex);
                                Bitmap bitmap = rotate(BitmapFactory.decodeFile(picturePath), 90);
                                galleryImage.setImageBitmap(bitmap);

                                String result = detectBarCode(bitmap);

                                txtResultBody.setText(result);
                                cursor.close();
                            }
                        }

                    }
                    break;
            }
        }
    }

    public static Bitmap rotate (Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    String detectBarCode (Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
        Reader reader = new QRCodeReader();
        try {
            Result result = reader.decode(new BinaryBitmap(new HybridBinarizer(source)));
            return result.getText();
        } catch (NotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "No proper QR code found in Image", Toast.LENGTH_SHORT).show();
            return null;
        } catch (ChecksumException e) {
            e.printStackTrace();
            Toast.makeText(this, "No proper QR code found in Image", Toast.LENGTH_SHORT).show();
            return null;
        } catch (FormatException e) {
            e.printStackTrace();
            Toast.makeText(this, "No proper QR code found in Image", Toast.LENGTH_SHORT).show();

            return null;
        }
    }

//    @Override
//    public void onRequestPermissionsResult (int requestCode,
//                                            @NonNull String[] permissions,
//                                            @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//
//        if (requestCode == CAMERA_PERMISSION_CODE) {
//
//            // Checking whether user granted the permission or not.
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//
//                // Showing the toast message
//                Toast.makeText(MainActivity.this, "Camera Permission Granted", Toast.LENGTH_SHORT).show();
//            } else {
//                Toast.makeText(MainActivity.this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
//            }
//        } else if (requestCode == STORAGE_PERMISSION_CODE) {
//            if (grantResults.length > 0
//                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(MainActivity.this, "Storage Permission Granted", Toast.LENGTH_SHORT).show();
//            } else {
//                Toast.makeText(MainActivity.this, "Storage Permission Denied", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
@Override
public void onRequestPermissionsResult(int requestCode,String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    switch (requestCode) {
        case REQUEST_ID_MULTIPLE_PERMISSIONS:
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "FlagUp Requires Access to Camara.", Toast.LENGTH_SHORT)
                        .show();
                finish();
            } else if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "FlagUp Requires Access to Your Storage.",
                        Toast.LENGTH_SHORT).show();
                finish();
            } else {
                selectImage(MainActivity.this);
            }
            break;
    }
}
}