package com.example.httprequest;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.builder.PostFormBuilder;
import com.zhy.http.okhttp.callback.StringCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * 网络请求api
 * @author Runt02
 *
 */
public class RuntHTTPApi {

    private static final String TAG = "uploadFile";
    private static final int TIME_OUT = 10 * 1000; // 超时时间

	public final static String KEY_MES_CODE= "code";
	public final static String KEY_MES_MESSAGE= "message";

	public final static String KEY_CODE_SUCCESS= "0";//code 0 成功
	public final static String MESS_TIP_NET_ERROR = "网络连接不畅，请稍后再试！！！";
	public final static String IP = "b.baidu.com",//
	// www.soarsan.com
	PORT = ":8080",
			CHARSET = "utf-8",
			PROJECT_URL = "http://" + IP + PORT +"/index.php/Webservice/V100/",
            SERVER_URL = "http://" + IP + PORT+"/index.php/Webservice/V100/";


	/**
	 * 访问接口
	 * @param params
	 * @return
	 */
	public static Map<String, Object> toReApi(String lastUrl, Map<String, String> params){
		String url = SERVER_URL+lastUrl;
		System.out.println("---------------传输的数据-------------------");
		System.out.println("url:"+url);
		printMap(params, "");
		String jsonStr = submitPostData(url,params,CHARSET);
		return parseJsonToMap(jsonStr);
	}

	/**
	 * 访问接口
	 * @param params
	 * @return 返回json字符串
	 */
	public static String testApi(String lastUrl, Map<String, String> params){
		String url = SERVER_URL+lastUrl;
		System.out.println("---------------传输的数据-------------------");
		System.out.println("url:"+url);
		printMap(params, "");
		String jsonStr = submitPostData(url,params,CHARSET);
		return jsonStr;
	}


	/**
	 *  okhttp访问接口,多文件加参数传递
	 * @param lastUrl
	 * @param params
	 */
	public static RequestBody okHttpReApi(String lastUrl, Map<String, Object> params, String fileFormat){
		String url = SERVER_URL+lastUrl;
		System.out.println("---------------传输的数据-------------------");
		System.out.println("url:"+url);
		printMap(params, "");
		MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
		for(String key : params.keySet()){
			if (params.get(key) instanceof Collection) {
				for(File file : (Collection<File>)params.get(key)){
					builder.addFormDataPart(key,file.getName(), RequestBody.create(MediaType.parse(fileFormat),file));
				}
			} else if(params.get(key) instanceof File) {
				File file = (File)params.get(key);
				builder.addFormDataPart(key,file.getName(), RequestBody.create(MediaType.parse(fileFormat),file));
			} else {
				builder.addFormDataPart(key,params.get(key).toString());
			}
		}

		RequestBody reBody = builder.build();
		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder().url(url).post(reBody).build();
		client.newCall(request);
		return reBody;
	}

	/**
	 *  okhttp访问接口,多文件加参数传递
	 * @param lastUrl
	 * @param params
	 * @param stringCallback
	 */
	public static void toReApi(String lastUrl, Map<String, Object> params, MyStringCallBack stringCallback){
		String url = SERVER_URL+lastUrl;
		System.out.println("---------------传输的数据-------------------");
		System.out.println("url:"+url);
		printMap(params, "");
		PostFormBuilder pfBuilder = OkHttpUtils.post().url(url);
		for(String key : params.keySet()){
			if (params.get(key) instanceof Collection) {
				for(File file : (Collection<File>)params.get(key)){
					pfBuilder.addFile(key,file.getName(),file);
				}
			} else if(params.get(key) instanceof File) {
				File file = (File)params.get(key);
				pfBuilder.addFile(key,file.getName(),file);
			} else {
				pfBuilder.addParams(key,params.get(key).toString());
			}
		}
		System.out.println("stringCallback:"+stringCallback);
		if(stringCallback!=null) {
			pfBuilder.build().execute(stringCallback);
		}
	}


	/**
	 * 解析json字符串
	 * @param jsonStr
	 * @return
	 */
	public static Map<String, Object> parseJsonToMap(String jsonStr) {
		//System.out.println("jsonStr:" + jsonStr);
		Map<String, Object> map = new TreeMap<String,Object>();
		try {
			JSONObject jsonObject = new JSONObject(jsonStr);// 解析字符串
			Iterator iter = jsonObject.keys();
			while (iter.hasNext()) {
				String key = (String) iter.next();
				Object value = jsonObject.get(key).toString();
				try {
					String str = value.toString();
					if(str.indexOf('[')==0){
						value = parseJsonToCollection(key,jsonObject.getJSONArray(key));

					}else{
						JSONObject tempJson = new JSONObject(value.toString());// 解析字符串
						value = parseJsonToMap(value.toString());
					}
				} catch (Exception e) {
					//Log.e("error", e.getMessage());
				}
				//System.out.println("key:"+key+" value:"+value.toString());
				map.put(key, value);
			}
		} catch (org.json.JSONException e) {
			//System.out.println("解析json错误:" + e.getMessage());
		}
		return map;
	}


	/**
	 * 解析json字符串
	 * @param key
	 * @returnCollection
	 */
	public static Collection<Map<String,Object>> parseJsonToCollection(String key, JSONArray jsonList){
		Collection<Map<String,Object>> list = new LinkedList<Map<String,Object>>();
		try {
			for(int i = 0; i < jsonList.length(); ++i) {
				Map map = RuntHTTPApi.parseJsonToMap(jsonList.getJSONObject(i).toString());
				list.add(map);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 *  单独文件上传
	 */
	public static Map<String, Object> uploadFile(String urlstr, File file) {
		urlstr = "http://ly.renrenws.com/test2/UploadFile";
		int res = 0;
        Log.e(TAG, "urlstr : " + urlstr);
        Map<String, Object> result = null;
        String BOUNDARY = UUID.randomUUID().toString(); // 边界标识 随机生成
        String PREFIX = "--", LINE_END = "\r\n";
        String CONTENT_TYPE = "multipart/form-data"; // 内容类型

        try {
            URL url = new URL(urlstr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(TIME_OUT);
            conn.setConnectTimeout(TIME_OUT);
            conn.setDoInput(true); // 允许输入流
            conn.setDoOutput(true); // 允许输出流
            conn.setUseCaches(false); // 不允许使用缓存
            conn.setRequestMethod("POST"); // 请求方式
            conn.setRequestProperty("Charset", CHARSET); // 设置编码
            conn.setRequestProperty("connection", "keep-alive");
            conn.setRequestProperty("Content-Type", CONTENT_TYPE + ";boundary="
                    + BOUNDARY);

            if (file != null) {
                /**
                 * 当文件不为空时执行上传
                 */
                DataOutputStream dos = new DataOutputStream(
                        conn.getOutputStream());
                StringBuffer sb = new StringBuffer();
                sb.append(PREFIX);
                sb.append(BOUNDARY);
                sb.append(LINE_END);
                /**
                 * 这里重点注意： name里面的值为服务器端需要key 只有这个key 才可以得到对应的文件
                 * filename是文件的名字，包含后缀名
                 */

                sb.append("Content-Disposition: form-data; name=\"file\"; filename=\""
                        + file.getName() + "\"" + LINE_END);
                sb.append("Content-Type: application/octet-stream; charset="
                        + CHARSET + LINE_END);
                sb.append(LINE_END);
                dos.write(sb.toString().getBytes());
                InputStream is = new FileInputStream(file);
                byte[] bytes = new byte[1024];
                int len = 0;
                while ((len = is.read(bytes)) != -1) {
                    dos.write(bytes, 0, len);
                    Log.i("正在上传", String.format("bytes:%s, 0, len:%s", bytes,  len));
                }
                is.close();
                dos.write(LINE_END.getBytes());
                Log.i("正在上传", String.format("LINE_END:%s", LINE_END.getBytes().length));
                byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINE_END)
                        .getBytes();
                dos.write(end_data);
                Log.i("正在上传", String.format("end_data:%s", end_data.length));
                dos.flush();
                /**
                 * 获取响应码 200=成功 当响应成功，获取响应的流
                 */
                res = conn.getResponseCode();
                Log.e(TAG, "response code:" + res);
                if (res == 200) {
                    Log.i(TAG, "request success");
                    InputStream input = conn.getInputStream();
                    StringBuffer sb1 = new StringBuffer();
                    int ss;
                    while ((ss = input.read()) != -1) {
                        sb1.append((char) ss);
                    }
                    String resultstr = sb1.toString();
                    result = parseJsonToMap(resultstr);
                    Log.i(TAG, "result : " + result);
                } else {
                    Log.e(TAG, "request error");
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
	}


	/**
	 * 输出打印map集合
	 * @param map
	 * @param space 需要打印的空格
	 */
	public static void printMap(Map map, String space) {
		for (Object key : map.keySet()) {
			System.out.print(space + key + ":");
			if (map.get(key) instanceof Map) {
				System.out.println();
				printMap((Map<String, Object>) map.get(key), space + "\\\t");
			} else if (map.get(key) instanceof Collection) {
				System.out.println();
				printCollection((Collection)map.get(key),space + "\\\t");
			}else {
				System.out.println(map.get(key));
			}
		}
	}


	/**
	 * 输出打印Collection集合
	 * @param list
	 * @param space 需要打印的空格
	 */
	public static void printCollection(Collection list, String space) {
		for (Object param : list) {
			System.out.println(param);
			if (param instanceof Map) {
				System.out.println();
				printMap((Map<String, Object>) param, space + "\\\t");
			} else if (param instanceof Collection) {
				System.out.println();
				printCollection((Collection)param,space + "\\\t");
			}else {
				System.out.println(param);
			}
		}
	}


	/*
         * Function : 发送Post请求到服务器 Param : params请求体内容，encode编码格式
         */
	public static String submitPostData(String strUrlPath,
										Map<String,String> params, String encode) {
		Log.i("接口路径：",strUrlPath.toString());
		Log.i("接口参数",params.toString());
		byte[] data = getRequestData(params, encode).toString().getBytes();// 获得请求体
		try {

			// String urlPath
			URL url = new URL(strUrlPath);

			HttpURLConnection httpURLConnection = (HttpURLConnection) url
					.openConnection();
			httpURLConnection.setConnectTimeout(10000); // 设置连接超时时间
			httpURLConnection.setDoInput(true); // 打开输入流，以便从服务器获取数据
			httpURLConnection.setDoOutput(true); // 打开输出流，以便向服务器提交数据
			httpURLConnection.setRequestMethod("POST"); // 设置以Post方式提交数据
			httpURLConnection.setUseCaches(false); // 使用Post方式不能使用缓存
			// 设置请求体的类型是文本类型
			httpURLConnection.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			// 设置请求体的长度
			httpURLConnection.setRequestProperty("Content-Length",
					String.valueOf(data.length));
			// 获得输出流，向服务器写入数据
			OutputStream outputStream = httpURLConnection.getOutputStream();
			outputStream.write(data);

			int response = httpURLConnection.getResponseCode(); // 获得服务器的响应码
			if (response == HttpURLConnection.HTTP_OK) {
				InputStream inptStream = httpURLConnection.getInputStream();
				return dealResponseResult(inptStream); // 处理服务器的响应结果
			}
		} catch (IOException e) {
			e.printStackTrace();
			return "err: " + e.getMessage();
		}
		return "获取数据错误";
	}


	/*
	 * Function : 封装请求体信息 Param : params请求体内容，encode编码格式
	 */
	public static StringBuffer getRequestData(Map<String,String> params,
											  String encode) {
		StringBuffer stringBuffer = new StringBuffer(); // 存储封装好的请求体信息
		try {
			for (Map.Entry<String,String> entry : params.entrySet()) {
				stringBuffer.append(entry.getKey()).append("=")
						.append(URLEncoder.encode(entry.getValue(), encode))
						.append("&");
			}
			stringBuffer.deleteCharAt(stringBuffer.length() - 1); // 删除最后的一个"&"
		} catch (Exception e) {
			e.printStackTrace();
		}
		return stringBuffer;
	}

	/*
	 * Function : 处理服务器的响应结果（将输入流转化成字符串） Param : inputStream服务器的响应输入流
	 */
	public static String dealResponseResult(InputStream inputStream) {
		String resultData = null; // 存储处理结果
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byte[] data = new byte[1024];
		int len = 0;
		try {
			while ((len = inputStream.read(data)) != -1) {
				byteArrayOutputStream.write(data, 0, len);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		resultData = new String(byteArrayOutputStream.toByteArray());
		//		Log.i("resultData",resultData);
		return resultData;
	}

	/**
	 *  访问请求返回执行的类
	 * Created by EDZ on 2018/1/15.
	 */
	public abstract static class ResPonse {
		public abstract void doSuccessThing(Map<String,Object> param);
		public abstract void doErrorThing(Map<String,Object> param);
	}

	/**
	 * Created by EDZ on 2018/1/27.
	 * 接口请求返回执行
	 */
	public class MyStringCallBack extends StringCallback {
		Context mContext;
		String TAG = "MyStringCallBack";
		ResPonse resPonse;
		public  MyStringCallBack(Context context, ResPonse resPonse){
			this.resPonse = resPonse;
			mContext = context;
		}
		@Override
		public void onError(Call call, Exception e, int id) {
			Toast.makeText(mContext, MESS_TIP_NET_ERROR, Toast.LENGTH_LONG).show();
			Log.e(TAG,e.getLocalizedMessage()+" \\n"+e.getMessage()+" \\n"+e.toString());
			if(resPonse!=null) {
				//showTipDialog(message.toString());
				resPonse.doErrorThing(new HashMap<String, Object>());
			}
		}

		@Override
		public void onResponse(final String response, int id) {
			Log.i(TAG,"response:"+response);
			((Activity)mContext).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					JSONObject jsonObject = null;
					try {
						jsonObject = new JSONObject(response);
						String jsonstr = jsonObject.toString();
						Map<String,Object> param = RuntHTTPApi.parseJsonToMap(jsonstr);
						RuntHTTPApi.printMap(param," ");
						Object message = param.get(KEY_MES_MESSAGE);//获取错误信息
						Log.i(TAG,param.toString());

						if(message == null){
							message = jsonstr;
						}
						if (KEY_CODE_SUCCESS.equals(param.get(KEY_MES_CODE))) {//判断获取数据是否成功
							if(resPonse!=null) {
								resPonse.doSuccessThing(param);
							}
						}else{
							Toast.makeText(mContext, message + "", Toast.LENGTH_SHORT).show();
							if(resPonse!=null) {
								//showTipDialog(message.toString());
								resPonse.doErrorThing(param);
							}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}
}
