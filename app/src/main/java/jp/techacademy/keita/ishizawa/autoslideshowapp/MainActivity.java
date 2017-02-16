package jp.techacademy.keita.ishizawa.autoslideshowapp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Handler;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    Timer playTimer;
    Handler playHandler = new Handler();
    Cursor cursor;
    Button MoveOnButton;
    Button returnButton;
    Button playAndStopButton;
    int cursorPosition;
    boolean curRestartAndStop;

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final String BUTTON_NAME_STOP = "停止";
    private static final String BUTTON_NAME_REPLAY = "再生";
    private static final String CURRENT_CURSOR_POSITION = "positionKey";
    private static final String RESTART_AND_STOP_FLG = "flgKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //情報が保存されている場合
        if(savedInstanceState != null){
             cursorPosition = savedInstanceState.getInt(CURRENT_CURSOR_POSITION);
            curRestartAndStop = savedInstanceState.getBoolean(RESTART_AND_STOP_FLG);
        }

        //各ボタンをViewクラスから取得してセット
        MoveOnButton = (Button) findViewById(R.id.MoveOnButton);
        returnButton = (Button) findViewById(R.id.returnButton);
        playAndStopButton = (Button) findViewById(R.id.playAndStopButton);

        //「進む」ボタン押下時の処理をセット
        MoveOnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!cursor.moveToNext()) cursor.moveToFirst();
                showImage();
            }
        });

        //「戻る」ボタン押下時の処理をセット
        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!cursor.moveToPrevious()) cursor.moveToLast();
                showImage();
            }
        });

        //「再生/停止」ボタン押下時の処理をセット
        playAndStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //タイマーが初期化されている場合
                if(playTimer == null) {
                    curRestartAndStop = true;   //再生停止フラグをセット
                    //各ボタンのテキストと活性・非活性をセット
                    settingBtn(curRestartAndStop);
                    //タイマーセット
                    playTimer = new Timer();
                    playTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            playHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (!cursor.moveToNext()) cursor.moveToFirst();
                                    showImage();
                                }
                            });
                        }
                    }, 0, 2000);
                //タイマーがセットされている場合
                }else{
                    curRestartAndStop = false;  //再生停止フラグをセット
                    //各ボタンのテキストと活性・非活性をセット
                    settingBtn(curRestartAndStop);
                    playTimer.cancel(); //タイマーを停止
                    playTimer = null;   //タイマーを初期化
                }
            }
        });

        // Android 6.0以降の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
                Initialization(cursorPosition, curRestartAndStop);
            } else {
                // 許可されていないので許可ダイアログを表示する
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
            }
            // Android 5系以下の場合
        } else {
            Initialization(cursorPosition, curRestartAndStop);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:
                if (grantResults.length >= 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("ANDROID", "許可された");
                    Initialization(cursorPosition, curRestartAndStop);
                } else {
                    Log.d("ANDROID", "許可されなかった");
                    Toast.makeText(this,
                            "このアプリはストレージを許可しないと使用できません。", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            default:
                break;
        }
    }

    //画面表示前に実行する初期化用関数
    private void Initialization(int pos, boolean flg) {

        // URIで指定した画像の情報を全件取得する
        ContentResolver resolver = getContentResolver();
        cursor = resolver.query(    //「cursor」とは、データベースから条件に合致したデータを格納するもの
                //URIとは目的のデータを示すために使われるもの
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null
        );
        //画面回転などで情報保持していた場合
        if((pos != -1) && (!flg) ){
            cursor.moveToPosition(pos);
            //各ボタンのテキストと活性・非活性をセット
            settingBtn(flg);
        }
        cursor.moveToFirst();   //カーソルを先頭に移動（ポジションを0にセット）
        //初期表示の画像を設定
        showImage();
    }

    private void showImage() {
            // indexからIDを取得し、そのIDから画像のURIを取得する
            int fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
            Long id = cursor.getLong(fieldIndex);
            Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            ImageView imageVIew = (ImageView) findViewById(R.id.imageView);
            imageVIew.setImageURI(imageUri);
    }

    //各ボタンのテキストと活性・非活性をセット
    private void settingBtn(boolean flg){
        if(flg == true){
            playAndStopButton.setText(BUTTON_NAME_STOP);    //ボタンのテキストを「停止」に変更
            //「進む」「戻る」ボタンをタップ不可に設定
            MoveOnButton.setEnabled(false);
            returnButton.setEnabled(false);
        } else if(flg == false) {
            playAndStopButton.setText(BUTTON_NAME_REPLAY);  //ボタンのテキストを「再生」に変更
            //「進む」「戻る」ボタンをタップ可に設定
            MoveOnButton.setEnabled(true);
            returnButton.setEnabled(true);
        }
    }

    //画面破棄前に情報を保存
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        //復元する情報を取得
        cursorPosition = cursor.getPosition();   //破棄前のカーソルのポジションを取得
        savedInstanceState.putInt(CURRENT_CURSOR_POSITION, cursorPosition); //カーソルポジションを保存
        savedInstanceState.putBoolean(RESTART_AND_STOP_FLG, curRestartAndStop); //再生停止フラグを保存
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(playTimer != null) playTimer.cancel();
        if (cursor != null) cursor.close();
    }
}