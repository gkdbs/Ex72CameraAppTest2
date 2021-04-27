package com.mrhi2020.ex72cameraapptest2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    ImageView iv;
    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iv= findViewById(R.id.iv);
        btn= findViewById(R.id.btn);

        //외부저장소 사용에 대한 동적퍼미션
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            String[] permissions= {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if( checkSelfPermission(permissions[0]) == PackageManager.PERMISSION_DENIED ){
                requestPermissions(permissions, 0);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if(requestCode==0 && grantResults[0]==PackageManager.PERMISSION_DENIED){
//            Toast.makeText(this, "카메라 기능 사용 제한", Toast.LENGTH_SHORT).show();
//            btn.setEnabled(false);
//        }else{
//            Toast.makeText(this, "카메라 사용 가능", Toast.LENGTH_SHORT).show();
//            btn.setEnabled(true);
//        }
    }

    //사진이 저장된 경로와 파일명 [File객체가 아니라 Uri객체]
    Uri imgUri=null;

    public void clickBtn(View view) {
        Intent intent= new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //사진이 저장될 Uri를 정하는 코드가 써있는 메소드 호출
        setImageUri(); //<-- 저 아래에 만든 메소드

        //사진이 저장되도록 하려면 인텐트에게 추가데이터로
        //파일이 저장될 위치(경로 - Uri)를 미리 지정하여 전달해 주면 됨.
        if(imgUri!=null) intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUri);

        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case 100:
                Toast.makeText(this, ""+resultCode, Toast.LENGTH_SHORT).show();
                if(resultCode==RESULT_OK){
                    //디바이스 중에서 EXTRA 데이터를 주면 Intent가 안돌아 오는 경우가 있음.
                    if(data!=null){
                        Toast.makeText(this, "aaa", Toast.LENGTH_SHORT).show();
                        Uri uri= data.getData();
                        if(uri!=null) Glide.with(this).load(uri).into(iv);
                        else Glide.with(this).load(imgUri).into(iv);
                    }else{
                        Toast.makeText(this, "bbb", Toast.LENGTH_SHORT).show();
                        Glide.with(this).load(imgUri).into(iv);
                    }
                }
                break;
        }

    }

    //저장될 파일의 콘텐츠 경로 Uri를 정하는 메소드
    void setImageUri(){

        //외부 저장소에 저장하는 것을 권장.
        //이때 외부저장소의 2가지 영역 중 하나를 선택
        //1. 외부저장소의 앱 전용영역 - 앱을 삭제하면 이 곳의 사진도 삭제됨 [다른 앱에서 접근 불가]
        File path= getExternalFilesDir("photo/aaa");
        if(!path.exists()) path.mkdirs();//폴더가 없다면 생성

        //2. 외부저장소의 공용영역 - 앱을 삭제해도 저장된 사진을 유지
        path= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        //경로가 정해졌다면 저장할 파일명.jpg 지정
        //같은 이름이 있으면 덮어쓰기가 되므로... 중복되지 않도록 이름 만들기!
        //1) 자동으로 임시파일명을 만들어주는 메소드를 이용하는 방법
//        try {
//            File imgFile= File.createTempFile("IMG_", ".jpg", path);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        //2) 날짜를 이용하는 방법 [ 권장 ]
        SimpleDateFormat sdf= new SimpleDateFormat("yyyyMMddhhmmss");//"20210115103604"
        String fileName= "IMG_" + sdf.format(new Date()) + ".jpg";
        File imgFile= new File(path, fileName);//  경로/파일명

        //여기까지 File객체가 잘 경로지정이 되었는지 확인!
        //new AlertDialog.Builder(this).setMessage(imgFile.getAbsolutePath()).create().show();

        // 카메라앱에 전달해줘야 하는 저장될 파일의 경로는 File객체가 아니라 Uri객체 여야 함
        // File --> Uri 변환
        // Nougat(누가버전)이후가 달라짐.
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N){
            imgUri= Uri.fromFile(imgFile);
        }else{
            //누가버전 이후부터는 FileProvider 라는 녀석을 이용해서 파일의 경로를 제공해 줘야함 함.
            // 안드로이드의 4대 컴포넌트 중 Content Provider : 다른 앱에게 본인 앱의 DB데이터를 제공하고자 할 때 사용
            // 이 앱에서 만들어낸 파일경로(imgFile)를 카메라앱에서 인식해야 하기에...프로바이더 필요

            //두번째 파라미터 : FileProvider 객체의 명칭 문자열
            imgUri= FileProvider.getUriForFile(this, "com.mrhi2020.ex72cameraapptest2.FileProvider",imgFile);
            //FileProvider 객체 만들기 [ 4대 컴포넌트 중 하나이기에 AndroidManifest.xml에서 작성]

        }

        //잘되었는지 확인
        new AlertDialog.Builder(this).setMessage(imgUri.toString()).create().show();

    }

}