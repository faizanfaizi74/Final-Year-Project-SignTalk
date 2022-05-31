package com.example.signtalk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.signtalk.model.Sentence;
import com.example.signtalk.retrofit.GetDataService;
import com.example.signtalk.retrofit.RetrofitClientInstance;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {
    final int SELECT_VIDEOS = 1;
    final int CAMERA_REQUEST = 2;
    boolean videosSelected = false; // Whether the user selected at least an video or not.

    final int EXTERNAL_STORAGE_PERMISSION = 1;
    final int CAMERA_PERMISSION = 2;

    private VideoView video;
    private Button btnCapture, selectVideo, predict, t2s;
    private TextView pred_class, vidName;
    private Uri capvideoUri;
    TextToSpeech ttobj;

    private ProgressDialog mProgressDialog;

    /*Create handle for the RetrofitInstance interface*/
    GetDataService service;

    private Uri selectedVideoUri;
    private boolean isCameraRequest = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        //init UI Views
        video = findViewById(R.id.video);
        btnCapture = findViewById(R.id.btnCapture);
        selectVideo = findViewById(R.id.selectVideo);
        predict = findViewById(R.id.predict);
        pred_class = findViewById(R.id.pred_class);
        vidName = findViewById(R.id.vidName);
        t2s = findViewById(R.id.t2s);

        mProgressDialog = new ProgressDialog(HomeActivity.this);
        mProgressDialog.setMessage("Processing\nPlease wait....");

        service = RetrofitClientInstance.getRetrofitInstance().create(GetDataService.class);
        // mProgressDialog.show();

        ttobj=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                ttobj.setLanguage(Locale.UK);
            }
        });
        t2s.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String toSpeak = pred_class.getText().toString();
                Toast.makeText(getApplicationContext(), toSpeak, Toast.LENGTH_SHORT).show();
                ttobj.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            }
        });
        // select video
        selectVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("video/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Video"), SELECT_VIDEOS);
            }
        });

        // capture video
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
                } else {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }
            }
        });

        // predict
        predict.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                Toast.makeText(HomeActivity.this, "Sending the Files. Please Wait ...", Toast.LENGTH_LONG).show();
                mProgressDialog.show();

                File file;

                Log.e("isCameraRequest", isCameraRequest + "");

                if (!isCameraRequest) {
                    file = new File(getRealPathFromURIForVideo(selectedVideoUri));
                } else {
                    file = new File(getRealPathFromURI(selectedVideoUri));
                }


                Log.e("file", file.getPath());


                MultipartBody.Part video = MultipartBody.Part.createFormData("video", file.getName(), RequestBody.create(MediaType.parse("video/*"), file));

                Call<Sentence> call = service.uploadVideo(video);
                call.enqueue(new Callback<Sentence>() {
                    @Override
                    public void onResponse(@NonNull Call<Sentence> call, @NonNull Response<Sentence> response) {
                        mProgressDialog.dismiss();
                        // generateDataList(response.body());
                        Toast.makeText(HomeActivity.this, "Response code: " + response.code(), Toast.LENGTH_SHORT).show();
                        if (response.body() != null) {
                            Log.e("JSON", new Gson().toJson(response.body()));

                            Sentence serverSentence = response.body();
                            pred_class.setText(serverSentence.getSentence());

                        } else {
                            Toast.makeText(HomeActivity.this, "Invalid Response, Please try again", Toast.LENGTH_SHORT).show();
                        }

                    }

                    @Override
                    public void onFailure(Call call, Throwable t) {
                        Log.e("onFailure", t.getMessage());
                        mProgressDialog.dismiss();
                        Toast.makeText(HomeActivity.this, "onFailure:" + t.getMessage(), Toast.LENGTH_LONG).show();
                        Toast.makeText(HomeActivity.this, "Please try again", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        // ask permissions
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_PERMISSION);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.e("onRequestPermissionsResult", requestCode + "");

        switch (requestCode) {
            case EXTERNAL_STORAGE_PERMISSION: {

                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "STORAGE Permission Granted.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "STORAGE Permission Denied.", Toast.LENGTH_LONG).show();
                }

                return;
            }
            case CAMERA_PERMISSION: {

                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "CAMERA Permission Granted.", Toast.LENGTH_LONG).show();
                    Toast.makeText(this, "Try Again now.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "CAMERA Permission Denied.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == SELECT_VIDEOS && resultCode == RESULT_OK && data != null) {

                if (data.getData() != null) {
                    selectedVideoUri = data.getData();
                    Log.e("VideoDetails", "URI : " + selectedVideoUri);
                    videosSelected = true;
                    MediaController mediaController = new MediaController(this);
                    mediaController.setAnchorView(video);
                    video.setMediaController(mediaController);
                    vidName.setText(getFileName(selectedVideoUri));
                    video.setVideoURI(selectedVideoUri);
                    video.requestFocus();
                    video.start();

                    isCameraRequest = false;
                }
            } else if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK && data != null) {
                if (data.getData() != null) {
                    selectedVideoUri = data.getData();
                    Log.e("VideoDetails", "URI : " + selectedVideoUri);
                    videosSelected = true;
                    vidName.setText(getFileName(selectedVideoUri));
                    MediaController mediaController = new MediaController(this);
                    mediaController.setAnchorView(video);
                    video.setMediaController(mediaController);
                    video.setVideoURI(selectedVideoUri);
                    video.requestFocus();
                    video.start();

                    isCameraRequest = true;

                }
            } else {
                Toast.makeText(this, "You haven't Picked any Video.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something Went Wrong.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        // Text to Speech
    }

    public String getVideoUri(Uri uri) {
        String[] projection = {MediaStore.Video.Media.DATA};
        @SuppressLint("Recycle") Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else
            return null;
    }

    @SuppressLint("Range")
    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private String getRealPathFromURIForVideo(Uri selectedVideoUri) {
        String wholeID = DocumentsContract.getDocumentId(selectedVideoUri);
        String id = wholeID.split(":")[1];

        String[] column = {MediaStore.Video.Media.DATA};
        String sel = MediaStore.Video.Media._ID + "=?";
        Cursor cursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, column, sel, new String[]{id}, null);
        String filePath = "";

        int columnIndex = cursor.getColumnIndex(column[0]);
        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }
}