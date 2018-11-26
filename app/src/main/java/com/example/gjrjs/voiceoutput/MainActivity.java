package com.exampe.gjrjs.MainActivity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.calendar.piccalendar.HorizonCalActivity;
import com.calendar.piccalendar.R;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class ImgUpload extends AppCompatActivity {
    private static final int PICK_FROM_CAMERA = 1; //카메라 촬영으로 사진 가져오기
    private static final int PICK_FROM_ALBUM = 2; //앨범에서 사진 가져오기
    private static final int CROP_FROM_CAMERA = 3; //가져온 사진을 자르기 위한 변수

    Uri photoUri;

    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}; //권한 설정 변수
    private static final int MULTIPLE_PERMISSIONS = 101; //권한 동의 여부 문의 후 CallBack 함수에 쓰일 변수

    private ImageView imv;
    private Button done;
    private Button cancel;
    private String realPath;
    private String userKey;
    private String userName;
    private String today;
    String goDate;
    Uri dmit;

    private LinearLayout btnLayout;
    ProgressBar pb;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img_upload);
        if (Build.VERSION.SDK_INT >= 23) {
            checkPermissions();
        }


        final Intent getIntent = getIntent();
        SharedPreferences sp = getSharedPreferences("userTable", MODE_PRIVATE);
        userName = sp.getString("user", "dd");
        userKey = sp.getString("key", "dd");
        today = getIntent.getExtras().getString("date");
        goDate = getIntent.getExtras().getString("goDate");
        String selector = getIntent.getExtras().getString("result");

        imv = (ImageView) findViewById(R.id.u_preview);
        done = (Button) findViewById(R.id.u_done);
        cancel = (Button) findViewById(R.id.u_cancel);
        pb = (ProgressBar) findViewById(R.id.pb);


        btnLayout = (LinearLayout) findViewById(R.id.btnLayout);

        if (selector.equals("cam")) {
            takephoto();
        } else {
            goToAlbum();
        }


        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "업로드 중 입니다....", Toast.LENGTH_SHORT).show();
                new UploadFile().execute(realPath, userKey, userName, today);
                String text[] = goDate.split("-");
                int year = Integer.parseInt(text[0]);
                int month = Integer.parseInt(text[1]);
                int day = Integer.parseInt(text[2]);
                Calendar date = new GregorianCalendar();
                date.set(year, month, day);
                ((HorizonCalActivity) HorizonCalActivity.mContext).goDate(date);
                finish();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private String getRealPathFromUri(Uri contentUri) {
        int column_index = 0;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        }
        return cursor.getString(column_index);
    }


    private void takephoto() {
        Intent intent;

        intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException e) {
            Toast.makeText(this, "이미지 처리 오류! 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (photoFile != null) {
            photoUri = FileProvider.getUriForFile(this,
                    "com.calendar.piccalendar.provider", photoFile); //FileProvider의 경우 이전 포스트를 참고하세요.
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri); //사진을 찍어 해당 Content uri를 photoUri에 적용시키기 위함
            startActivityForResult(intent, PICK_FROM_CAMERA);
        }
    }

    // Android M에서는 Uri.fromFile 함수를 사용하였으나 7.0부터는 이 함수를 사용할 시 FileUriExposedException이
    // 발생하므로 아래와 같이 함수를 작성합니다. 이전 포스트에 참고한 영문 사이트를 들어가시면 자세한 설명을 볼 수 있습니다.
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("HHmmss").format(new Date());
        String imageFileName = "IP" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStorageDirectory() + "/test/"); //test라는 경로에 이미지를 저장하기 위함
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        realPath = image.getAbsolutePath();
        return image;
    }

    private void goToAlbum() {
        Intent intent = new Intent(Intent.ACTION_PICK); //ACTION_PICK 즉 사진을 고르겠다!
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent, PICK_FROM_ALBUM);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "이미지 처리 오류! 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (requestCode == PICK_FROM_ALBUM) {
            if (data == null) {
                return;
            }
            photoUri = data.getData();
            cropImage();
        } else if (requestCode == PICK_FROM_CAMERA) {

            cropImage();
            MediaScannerConnection.scanFile(this, //앨범에 사진을 보여주기 위해 Scan을 합니다.
                    new String[]{photoUri.getPath()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                        }
                    });
        } else if (requestCode == CROP_FROM_CAMERA) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
                Bitmap thumbImage = ThumbnailUtils.extractThumbnail(bitmap, 128, 128);
                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                thumbImage.compress(Bitmap.CompressFormat.JPEG, 100, bs); //이미지가 클 경우 OutOfMemoryException 발생이 예상되어 압축
                imv.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    //Android N crop image (이 부분에서 몇일동안 정신 못차렸습니다 ㅜ)

    //모든 작업에 있어 사전에 FALG_GRANT_WRITE_URI_PERMISSION과 READ 퍼미션을 줘야 uri를 활용한 작업에 지장을 받지 않는다는 것이 핵심입니다.
    public void cropImage() {
        this.grantUriPermission("com.android.camera", photoUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(photoUri, "image/*");

        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 0);
        grantUriPermission(list.get(0).activityInfo.packageName, photoUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        int size = list.size();
        if (size == 0) {
            Toast.makeText(this, "취소 되었습니다.", Toast.LENGTH_SHORT).show();
            return;
        } else {
            Toast.makeText(this, "용량이 큰 사진의 경우 시간이 오래 걸릴 수 있습니다.", Toast.LENGTH_SHORT).show();
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            File croppedFileName = null;
            try {
                croppedFileName = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            File folder = new File(Environment.getExternalStorageDirectory() + "/test/");
            File tempFile = new File(folder.toString(), croppedFileName.getName());

            photoUri = FileProvider.getUriForFile(this,
                    "com.calendar.piccalendar.provider", tempFile);

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);


            intent.putExtra("return-data", false);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString()); //Bitmap 형태로 받기 위해 해당 작업 진행

            Intent i = new Intent(intent);
            ResolveInfo res = list.get(0);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            grantUriPermission(res.activityInfo.packageName, photoUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            i.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            startActivityForResult(i, CROP_FROM_CAMERA);
        }
    }

    private boolean checkPermissions() {
        int result;
        List<String> permissionList = new ArrayList<>();
        for (String pm : permissions) {
            result = ContextCompat.checkSelfPermission(this, pm);
            if (result != PackageManager.PERMISSION_GRANTED) { //사용자가 해당 권한을 가지고 있지 않을 경우 리스트에 해당 권한명 추가
                permissionList.add(pm);
            }
        }
        if (!permissionList.isEmpty()) { //권한이 추가되었으면 해당 리스트가 empty가 아니므로 request 즉 권한을 요청합니다.
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (permissions[i].equals(this.permissions[0])) {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                showNoPermissionToastAndFinish();
                            }
                        } else if (permissions[i].equals(this.permissions[1])) {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                showNoPermissionToastAndFinish();

                            }
                        } else if (permissions[i].equals(this.permissions[2])) {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                showNoPermissionToastAndFinish();

                            }
                        }
                    }
                } else {
                    showNoPermissionToastAndFinish();
                }
                return;
            }
        }
    }

    //권한 획득에 동의를 하지 않았을 경우 아래 Toast 메세지를 띄우며 해당 Activity를 종료시킵니다.
    private void showNoPermissionToastAndFinish() {
        Toast.makeText(this, "권한 요청에 동의 해주셔야 이용 가능합니다. 설정에서 권한 허용 하시기 바랍니다.", Toast.LENGTH_SHORT).show();
        finish();
    }

    // upload
    public class UploadFile extends AsyncTask<String, Boolean, Boolean> {
        private String imgName = "";
        private String imgPath = "";
        private String uk = "";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pb.setVisibility(View.VISIBLE);
            btnLayout.setVisibility(View.GONE);
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            boolean result = false;
            try {
                result = upload(strings[0], strings[1], strings[2], strings[3]);
            } catch (Exception e) {

            }
            return result;
        }


        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            pb.setVisibility(View.GONE);
            resultDialog(result);
        }

        public boolean upload(String filePath, String uk, String uploader, String dir) throws Exception {
            String url = "****";
            Log.d("test", "파일 경로..? : " + filePath);
            String boundary = "^*****^";
            int s = filePath.lastIndexOf("/");
            int l = filePath.lastIndexOf(".");

            String fname = filePath.substring(s + 1);
            Log.d("test", "파일 이름..? : " + fname);

            String delimiter = "\r\n--" + boundary + "\r\n";


            StringBuffer postDataBuilder = new StringBuffer();

            //추가하고 싶은 KEY & VALUE 추가
            //KEY & value를 추가한 후 꼭 경계선을 삽입해줘야 데이터를 구분할 수 있다.

            Log.d("test", "업로드 : 유저키 ^^^^^^" + uk);
            postDataBuilder.append(delimiter);
            postDataBuilder.append(setValue("key", uk));
            postDataBuilder.append(delimiter);
            postDataBuilder.append(setValue("uploader", uploader));
            postDataBuilder.append(delimiter);
            postDataBuilder.append(setValue("dir", dir));
            postDataBuilder.append(delimiter);

            postDataBuilder.append(setFile("file", fname));
            postDataBuilder.append("\r\n");

            //커넥션 생성 및 설정
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            //전송 작업 시작
            FileInputStream in = new FileInputStream(filePath);
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(conn.getOutputStream()));
            //위에서 작성한 메타데이터를 머저 전송한다. UTF-8 메소드 사용
            out.writeUTF(postDataBuilder.toString());

            //파일 복사 작업 시작
            int maxBufferSize = 1024;
            int bufferSize = Math.min(in.available(), maxBufferSize);
            byte[] buffer = new byte[bufferSize];

            //버퍼 크기 만큼 파일로부터 바이트 데이터를 읽는다.
            int byteRead = in.read(buffer, 0, bufferSize);

            while (byteRead > 0) {
                out.write(buffer);
                bufferSize = Math.min(in.available(), maxBufferSize);
                byteRead = in.read(buffer, 0, bufferSize);
            }

            out.writeBytes(delimiter);
            out.flush();
            out.close();
            in.close();
            Log.d("test", "난 너의 행방을 찾고있단다.");
            //결과 반환
            if (conn.getInputStream() != null) {
                conn.disconnect();
                return true;
            } else {
                conn.disconnect();
                return false;
            }

        }
        /*
         * Map 형식으로 key와 value를 세팅..
         * @param key : 서버에서 사용할 변수명
         * @param value : 변수명에 해당하는 실제 값
         * @return
         */

        public String setValue(String key, String value) {
            return "Content-Disposition: form-data; name=\"" + key + "\"r\n\r\n" + value;
        }

        /*
        업로드할 파일에 대한 메타 데이터를 설정한다.
        @param key : 서버에서 사용할 파일 변수명
        @param fileName : 서버에서 저장될 파일명
        @return
        */
        public String setFile(String key, String fileName) {
            return "Content-Disposition: form-data;name=\"" + key + "\";filename=\"" + fileName + "\"\r\n";
        }

    }


    public void resultDialog(boolean result) {
        if (result) {
            Toast.makeText(this, "업로드 완료!", Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "서버와의 연결이 원활하지 않습니다.", Toast.LENGTH_LONG).show();
        }
    }


}