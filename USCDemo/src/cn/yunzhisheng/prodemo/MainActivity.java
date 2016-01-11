package cn.yunzhisheng.prodemo;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import cn.yunzhisheng.common.USCError;
import cn.yunzhisheng.nlu.basic.USCSpeechUnderstander;
import cn.yunzhisheng.tts.online.basic.OnlineTTS;
import cn.yunzhisheng.tts.online.basic.TTSPlayerListener;
import cn.yunzhisheng.understander.USCSpeechUnderstanderListener;
import cn.yunzhisheng.understander.USCUnderstanderResult;

/**
 * 云知声识别实例程序
 * 
 * @author
 * 
 */

public class MainActivity extends Activity implements OnClickListener {
	
	private int AsrType = ASR_ONLINE_TYPE;
	
	public static final int ASR_ONLINE_TYPE = 0;
	public static final int ASR_OFFLINE_TYPE = 1;
	public static final int WAKEUP_OFFLINE_TYPE = 2;
	public static final int TTS_OFFLINE_TYPE = 3;

	/**
	 * 当前识别状态
	 */
	enum AsrStatus {
		idle, recording, recognizing
	}

	private ProgressBar      mVolume;
	private EditText          mRecognizerResultText,mNluResultText;
	private Button             mRecognizerButton;
	private View    mStatusView;
	private View   mStatusLayout;
	private  ImageView   mLogoImageView;
	private TextView  mStatusTextView;
	private Button mLanguageButton;
	private Dialog mLanguageDialog;
	private Button  mDomainButton;
	private Dialog   mDomainDialog;
	private Button  mSampleButton;
	private Dialog   mSampleDialog;
	private Dialog mfunctionDialog;
    
	private static  String  arraySampleStr[] = new String[]{"RATE_AUTO  ","RATE_16K  ", "RATE_8K  "};
	private static String arrayLanguageStr[] = new String[]{USCSpeechUnderstander.LANGUAGE_CHINESE,USCSpeechUnderstander.LANGUAGE_ENGLISH,USCSpeechUnderstander.LANGUAGE_CANTONESE};
	private static String arrayLanguageChina[] = new String[]{"中文(普通话)","英语","粤语",};
	private static  String  arrayDomain[] = new String[]{"general","poi","song","movietv","medical"};
	private static  String  arrayDomainChina[] = new  String[] {"通用识别  ","地名识别  ","歌名识别  ","影视名识别  ","医药领域识别  "};
	private static  int  arraySample[] = new int[]{USCSpeechUnderstander.BANDWIDTH_AUTO,USCSpeechUnderstander.RATE_16K,USCSpeechUnderstander.RATE_8K};	
	private static  int  currentSample = 0;
	private static  int  currentDomain = 0;
	private static  int  currentLanguage = 0;


	private AsrStatus statue = AsrStatus.idle;
	private USCSpeechUnderstander mUnderstander;
	private String mRecognizerText = "";
	private OnlineTTS mTTSPlayer;

	private ImageView function_button;// 选择功能


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.status_bar_main);
        
		mVolume = (ProgressBar) findViewById(R.id.volume_progressbar);
		mRecognizerResultText = (EditText) findViewById(R.id.recognizer_result_et);
		mNluResultText = (EditText) findViewById(R.id.nlu_result_et);
		mDomainButton = (Button) findViewById(R.id.domain_button);
		mSampleButton = (Button) findViewById(R.id.sample_button);
		mStatusView = findViewById(R.id.status_panel);
		mStatusTextView = (TextView) findViewById(R.id.status_show_textview);
		mStatusLayout = findViewById(R.id.status_layout);
		mLogoImageView = (ImageView) findViewById(R.id.logo_imageview);
		mLanguageButton = (Button) findViewById(R.id.language_button);
		mLanguageButton.setOnClickListener(this);
		mLanguageButton.setText(arrayLanguageChina[0]);
		mDomainButton.setOnClickListener(this);
		mDomainButton.setText(arrayDomainChina[0]);
		mSampleButton.setOnClickListener(this);
		mSampleButton.setText(arraySampleStr[0]);

		
		mRecognizerButton = (Button) findViewById(R.id.recognizer_btn);

		function_button = (ImageView) findViewById(R.id.function_button);
		function_button.setOnClickListener(this);
		
		
		initData();

		// 初始化对象
		initRecognizer();
	}

	/**
	 * 初始化
	 */
	private void initRecognizer() {

		// 创建语音理解对象，appKey和 secret通过 http://dev.hivoice.cn/ 网站申请
		mUnderstander = new USCSpeechUnderstander(this, Config.appKey,Config.secret);
		//创建语音合成对象
		mTTSPlayer = new OnlineTTS(this, Config.appKey);

		//设置语音合成回调监听
		mTTSPlayer.setTTSListener(new TTSPlayerListener() {
			
			@Override
			public void onPlayEnd() {
				
			}
			
			@Override
			public void onPlayBegin() {
				
			}
			
			@Override
			public void onEnd(USCError arg0) {
				if(arg0!=null){
				  hitErrorMsg(arg0.toString());
				}
			}
			
			@Override
			public void onBuffer() {
				
			}
		});
		
		// 保存录音数据
		// recognizer.setRecordingDataEnable(true);
		mUnderstander.setListener(new USCSpeechUnderstanderListener() {
			/**
			 * 识别结果实时返回
			 */
			@Override
			public void onRecognizerResult(String result, boolean isLast) {

				// 通常onResult接口多次返回结果，保留识别结果组成完整的识别内容。
				mRecognizerResultText.append(result);
				log_v("onRecognizerResult");
			}

			/**
			 * 语音理解结束
			 */
			@Override
			public void onEnd(USCError error) {
				log_v("onEnd");

				mRecognizerButton.setEnabled(true);
				statue = AsrStatus.idle;
				mRecognizerButton.setText(R.string.click_say);
				mStatusLayout.setVisibility(View.GONE);
				mRecognizerResultText.requestFocus();
				mRecognizerResultText.setSelection(0);
				mNluResultText.requestFocus();
				mNluResultText.setSelection(0);
				if (error != null) {
					// 显示错误信息
                   hitErrorMsg(error.toString());
				} else {
					if ("".equals(mRecognizerResultText.getText().toString())) {
						mRecognizerResultText.setText(R.string.no_hear_sound);
					}else{
						mTTSPlayer.play(mRecognizerResultText.getText().toString());
						mRecognizerText = "";
					}
				}
			}

			/**
			 * 检测用户停止说话回调
			 */
			@Override
			public void onVADTimeout() {
				log_v("onVADTimeout");

				// 收到用户停止说话事件，停止录音
				stopRecord();
			}

			/**
			 * 实时返回说话音量 0~100
			 */
			@Override
			public void onUpdateVolume(int volume) {
				mVolume.setProgress(volume);
			
			}

			/**
			 * 停止录音，请等待识别结果回调
			 */
			@Override
			public void onRecordingStop() {
				log_v("onRecordingStop");

				statue = AsrStatus.recognizing;
				mRecognizerButton.setText(R.string.give_up);
				mStatusTextView.setText(R.string.just_recognizer);
			}

			/**
			 * 用户开始说话
			 */
			@Override
			public void onSpeechStart() {

				log_v("onSpeakStart");
			    mStatusTextView.setText(R.string.speaking);
			}

			/**
			 * 录音设备打开，开始识别，用户可以开始说话
			 */
			@Override
			public void onRecordingStart() {
				mStatusTextView.setText(R.string.please_speak);
				mRecognizerButton.setEnabled(true);
				statue = AsrStatus.recording;
				mRecognizerButton.setText(R.string.say_over);
			}
            /**
             * 语义解析结果
             */
			@Override
			public void onUnderstanderResult(USCUnderstanderResult result) {
				if (result != null) {
					mNluResultText.setText(result.getStringResult());
					log_v(result.getStringResult());
				}
			}
		});
	}

	/**
	 * 初始化按钮
	 */
	private void initData() {

		// 功能选择
		mfunctionDialog = new Dialog(this, R.style.dialog);
		mfunctionDialog.setContentView(R.layout.function_list_item); 
		mfunctionDialog.findViewById(R.id.asr_online_text).setOnClickListener(this);
		mfunctionDialog.findViewById(R.id.asr_offline_text).setOnClickListener(this);
		mfunctionDialog.findViewById(R.id.wakeup_offline_text).setOnClickListener(this);
		mfunctionDialog.findViewById(R.id.tts_offline_text).setOnClickListener(this);

		// 识别领域
		mDomainDialog = new Dialog(this, R.style.dialog);
		mDomainDialog.setContentView(R.layout.domain_list_item);
		mDomainDialog.findViewById(R.id.medical_text).setOnClickListener(this);
		mDomainDialog.findViewById(R.id.general_text).setOnClickListener(this);
		mDomainDialog.findViewById(R.id.movietv_text).setOnClickListener(this);
		mDomainDialog.findViewById(R.id.poi_text).setOnClickListener(this);
		mDomainDialog.findViewById(R.id.song_text).setOnClickListener(this);
		
		//采样率
		mSampleDialog  = new Dialog(this,R.style.dialog);
		mSampleDialog.setContentView(R.layout.sample_list_item);
		mSampleDialog.findViewById(R.id.rate_16k_text).setOnClickListener(this);
		mSampleDialog.findViewById(R.id.rate_8k_text).setOnClickListener(this);
		mSampleDialog.findViewById(R.id.rate_auto_text).setOnClickListener(this);
		
		//语言
		mLanguageDialog = new Dialog(this,R.style.dialog);
		mLanguageDialog.setContentView(R.layout.language_list_item);
		mLanguageDialog.findViewById(R.id.chinese_text).setOnClickListener(this);
		mLanguageDialog.findViewById(R.id.cantonese_text).setOnClickListener(this);
		mLanguageDialog.findViewById(R.id.english_text).setOnClickListener(this);

		mRecognizerButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (statue == AsrStatus.idle) {

					mRecognizerButton.setEnabled(false);
					mRecognizerResultText.setText("");
					mNluResultText.setText("");
					mStatusView.setVisibility(View.VISIBLE);
					mStatusLayout.setVisibility(View.VISIBLE);
					mLogoImageView.setVisibility(View.GONE);
					// 在收到 onRecognizerStart 回调前，录音设备没有打开，请添加界面等待提示，
					// 录音设备打开前用户说的话不能被识别到，影响识别效果。
					mStatusTextView.setText(R.string.opening_recode_devices);
					// 修改录音采样率
					mUnderstander.setBandwidth(arraySample[currentSample]);
					// 修改识别领域
					mUnderstander.setEngine(arrayDomain[currentDomain]);
					//修改识别语音
					mUnderstander.setLanguage(arrayLanguageStr[currentLanguage]);
                    mUnderstander.start();
				} else if (statue == AsrStatus.recording) {
					stopRecord();
				} else if (statue == AsrStatus.recognizing) {
                  //取消识别
					mUnderstander.cancel();

					mRecognizerButton.setText(R.string.click_say);
					statue = AsrStatus.idle;
				}
			}
		});
	}

	/**
	 * 打印日志信息
	 * 
	 * @param msg
	 */
	private void log_v(String msg) {
		Log.v("demo", msg);
	}

	private void log_e(String msg){
		Log.e("demo", msg);
	}
	
	private void hitErrorMsg(String msg){
		Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
	}
	
	/**
	 * 停止录音
	 */
	public void stopRecord() {
		mStatusTextView.setText(R.string.just_recognizer);
		mUnderstander.stop();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.domain_button:
			mDomainDialog.show();
			break;
		case R.id.sample_button:
			mSampleDialog.show();
			break;
		case R.id.general_text:
			currentDomain = 0;
			setDomain(currentDomain);
			break;

		case R.id.poi_text:
			currentDomain = 1;
			setDomain(currentDomain);
			break;

		case R.id.song_text:
			currentDomain = 2;
			setDomain(currentDomain);
			break;

		case R.id.movietv_text:
			currentDomain = 3;
			setDomain(currentDomain);
			break;

		case R.id.medical_text:
			currentDomain = 4;
			setDomain(currentDomain);
			break;
		case R.id.language_button:
			mLanguageDialog.show();
			break;
			
		case R.id.chinese_text:
			currentLanguage = 0;
			setLanguage(currentLanguage);
			break;
			
		case R.id.cantonese_text:
			currentLanguage = 2;
			setLanguage(currentLanguage);
			break;
			
		case R.id.english_text:
			currentLanguage = 1;
			setLanguage(currentLanguage);
			break;
			
		case R.id.rate_auto_text:
			currentSample = 0;
			setSample(currentSample);
			break;

		case R.id.rate_16k_text:
			currentSample = 1;
			setSample(currentSample);
			break;

		case R.id.rate_8k_text:
			currentSample = 2;
			setSample(currentSample);
			break;
			
		case R.id.function_button:
			mfunctionDialog.show();
			break;
		case R.id.asr_online_text:
			AsrType = ASR_ONLINE_TYPE;
			changeView();
			break;
		case R.id.asr_offline_text:
			AsrType = ASR_OFFLINE_TYPE;
			changeView();
			break;
		case R.id.wakeup_offline_text:
			AsrType = WAKEUP_OFFLINE_TYPE;
			changeView();
			break;
		case R.id.tts_offline_text:
			AsrType = TTS_OFFLINE_TYPE;
			changeView();
			break;
			
		default:
			break;
		}

	}
	
	private void changeView() {
		if (AsrType == ASR_ONLINE_TYPE){
			Intent intent = new Intent(this,MainActivity.class);
			this.startActivity(intent);
			this.finish();
		}
		mfunctionDialog.dismiss();
	}
	
	private void setLanguage(int index) {
		mLanguageButton.setText(arrayLanguageChina[index]);
		mLanguageDialog.dismiss();
	}

	private void setSample(int index) {
		mSampleButton.setText(arraySampleStr[index]);
		mSampleDialog.dismiss();
	}

	private void setDomain(int index) {
		mDomainButton.setText(arrayDomainChina[index]);
		mDomainDialog.dismiss();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mUnderstander != null) {
			mUnderstander.stop();
		}
		// 关闭语音合成引擎
		if (mTTSPlayer != null) {
			mTTSPlayer.stop();
		}
	}
}
