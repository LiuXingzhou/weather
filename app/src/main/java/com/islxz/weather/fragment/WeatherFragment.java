package com.islxz.weather.fragment;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.islxz.weather.R;
import com.islxz.weather.database.City;
import com.islxz.weather.database.County;
import com.islxz.weather.database.Province;
import com.islxz.weather.entity.HttpUrl;
import com.islxz.weather.gson.Forecast;
import com.islxz.weather.gson.Weather;
import com.islxz.weather.util.HttpUtil;
import com.islxz.weather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Qingsu on 2017/6/12.
 */

public class WeatherFragment extends Fragment implements View.OnClickListener {

    /*
    遍历省市县数据，填充DrawerLayout里面的ListView
     */
    private static final int LEVEL_PROVINCE=0;
    private static final int LEVEL_CITY=1;
    private static final int LEVEL_COUNTY=2;
    private ProgressDialog mProgressDialog;
    private ArrayAdapter<String> mCityAdapter;
    private List<String> mDataList =new ArrayList<>();
    /*
    省列表
     */
    private List<Province> mProvinceList;
    /*
    市列表
     */
    private List<City> mCityList;
    /*
    县列表
     */
    private List<County> mCountyList;
    /*
    选中的省份
     */
    private Province mSelectProvince;
    /*
    选中的城市
     */
    private City mSelectCity;
    /*
    当前选中的级别
     */
    private int currentLevel;

    private DrawerLayout mDrawerLayout;
    private ScrollView mScrollView;
    private Button mMoreCityBtn;
    private TextView mCityNameTV;
    private TextView mNowTimeTV;
    private TextView mTmpTV;
    private TextView mTxtTV;
    private LinearLayout mHourlyForecastLL;
    private TextView mAQITV;
    private TextView mPM25TV;
    private TextView mComfTV;
    private TextView mCwTV;
    private TextView mSportTV;

    private Button mBackBtn;
    private TextView mSelectCityName;
    private ListView mCityListLV;

    private ImageView mBingPicIV;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private String mWeatherId;

    SharedPreferences mPrefs;
    SharedPreferences.Editor mEditor;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle
            savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather, null);
        /*
        绑定ID
         */
        bindID(inflater, view);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mEditor=mPrefs.edit();

        /*
        下拉刷新
         */
        mSwipeRefreshLayout=view.findViewById(R.id.fragment_srl);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);

        /*
        初始化各控件
         */
        mBingPicIV =view.findViewById(R.id.main_iv);
        String bingPic=mPrefs.getString("bing_pic",null);
        if(bingPic!=null){
            Glide.with(getContext()).load(bingPic).into(mBingPicIV);
        }

        /*
        加载天气信息
         */
        String weatherString= mPrefs.getString("weather",null);
        mWeatherId= mPrefs.getString("weather_id","CN101120101");
        if(weatherString!=null){
            //有缓存时直接解析天气数据
            Weather weather=Utility.handleWeatherResponse(weatherString);
            mWeatherId=weather.basic.weatherId;
            showWeatherInfo(weather);
        }else {
            //无缓存时去服务器查询天气
            requestWeather(mWeatherId);
        }
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });
        /*
        DrawerLayout-ListView填充数据
         */
        mCityAdapter =new ArrayAdapter<String>(getContext(),R.layout.listview_item1, mDataList);
        mCityListLV.setAdapter(mCityAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mMoreCityBtn.setOnClickListener(this);
        mCityListLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(currentLevel==LEVEL_PROVINCE){
                    mSelectProvince=mProvinceList.get(i);
                    queryCities();
                }else if (currentLevel==LEVEL_CITY){
                    mSelectCity=mCityList.get(i);
                    queryCounties();
                }else if(currentLevel==LEVEL_COUNTY){
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                    String weatherId=mCountyList.get(i).getWeatherId();
                    mEditor.putString("weather_id",weatherId);
                    mEditor.apply();
                    requestWeather(weatherId);
                }
            }
        });
        mBackBtn.setOnClickListener(this);
        queryProvinces();
    }

    private void bindID(LayoutInflater inflater, View view) {
        mDrawerLayout = view.findViewById(R.id.fragment_drawer);
        mScrollView = view.findViewById(R.id.fragment_sv);
        mMoreCityBtn = view.findViewById(R.id.fragment_btn_more_city);
        mCityNameTV = view.findViewById(R.id.fragment_tv_city_name);
        mNowTimeTV = view.findViewById(R.id.fragment_tv_now_time);
        mTmpTV = view.findViewById(R.id.fragment_tv_tmp);
        mTxtTV = view.findViewById(R.id.fragment_tv_txt);
        mHourlyForecastLL = view.findViewById(R.id.fragment_ll_hourly_forecast);
        mAQITV = view.findViewById(R.id.fragment_tv_aqi);
        mPM25TV = view.findViewById(R.id.fragment_tv_pm25);
        mComfTV = view.findViewById(R.id.fragment_tv_comf);
        mCwTV = view.findViewById(R.id.fragment_tv_cw);
        mSportTV = view.findViewById(R.id.fragment_tv_sport);

        mBackBtn = view.findViewById(R.id.fragment_btn_back);
        mSelectCityName = view.findViewById(R.id.fragment_tv_select_city_name);
        mCityListLV = view.findViewById(R.id.fragment_lv_city_list);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fragment_btn_more_city:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;
            case R.id.fragment_btn_back:
                if(currentLevel==LEVEL_COUNTY){
                    queryCities();
                }else if(currentLevel==LEVEL_CITY){
                    queryProvinces();
                }
                break;
        }
    }

    /*
    根据天气id请求城市天气信息
     */
    public void requestWeather(final String weatherId){
        mSwipeRefreshLayout.setRefreshing(true);
        String weatherUrl=HttpUrl.WEATHER_URL_START+weatherId+HttpUrl.WEATHER_URL_END;
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(),"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText=response.body().string();
                final Weather weather=Utility.handleWeatherResponse(responseText);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather!=null&&"ok".equals(weather.status)){
                            SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        }else {
                            Toast.makeText(getContext(),"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        }
                        mWeatherId=weather.basic.weatherId;
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
        loadBingPic();
    }

    /*
    加载必应每日一图
     */
    public void loadBingPic(){
        String requestBingPic=HttpUrl.BING_PIC;
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic=response.body().string();
                mEditor.putString("bing_pic",bingPic);
                mEditor.apply();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(getContext()).load(bingPic).into(mBingPicIV);
                    }
                });
            }
        });
    }

    /*
    处理并展示Weather实体类中的数据
     */
    private void showWeatherInfo(Weather weather){
        String cityName=weather.basic.cityName;
        String updateTime=weather.basic.update.updateTime.split(" ")[1];
        String degree=weather.now.temperature+"℃";
        String weatherInfo=weather.now.more.info;
        mCityNameTV.setText(cityName);
        mNowTimeTV.setText(updateTime);
        mTmpTV.setText(degree);
        mTxtTV.setText(weatherInfo);
        mHourlyForecastLL.removeAllViews();
        for(Forecast forecast:weather.forecastList){
            View view=LayoutInflater.from(getContext()).inflate(R.layout.listview_item_hourly_forecast,mHourlyForecastLL,false);
            TextView dateTV=view.findViewById(R.id.listview_item_tv_time);
            TextView infoTV=view.findViewById(R.id.listview_item_tv_txt);
            TextView maxTV=view.findViewById(R.id.listview_item_tv_max);
            TextView minTV=view.findViewById(R.id.listview_item_tv_min);
            dateTV.setText(forecast.date);
            infoTV.setText(forecast.more.info);
            maxTV.setText(forecast.temperature.max);
            minTV.setText(forecast.temperature.min);
            mHourlyForecastLL.addView(view);
        }
        if(weather.aqi!=null){
            mAQITV.setText(weather.aqi.city.aqi);
            mPM25TV.setText(weather.aqi.city.pm25);
        }
        String comfort="舒适度指数："+weather.suggestion.comfort.brfTxt+"（"+weather.suggestion.comfort.info+"）";
        String carWash="洗车指数："+weather.suggestion.carWash.brfTxt+"（"+weather.suggestion.carWash.info+"）";
        String sport="运动指数："+weather.suggestion.sport.brfTxt+"（"+weather.suggestion.sport.info+"）";
        mComfTV.setText(comfort);
        mCwTV.setText(carWash);
        mSportTV.setText(sport);
    }

    /*
    查询全国所有的省，优先从数据库中查询，如果没有查询到再去服务器上查询
     */
    private void queryProvinces(){
        mSelectCityName.setText("中国");
        mBackBtn.setVisibility(View.GONE);
        mProvinceList= DataSupport.findAll(Province.class);
        if(mProvinceList.size()>0){
            mDataList.clear();
            for(Province province:mProvinceList){
                mDataList.add(province.getProvinceName());
            }
            mCityAdapter.notifyDataSetChanged();
            mCityListLV.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }else{
            String address= HttpUrl.CHINA_BASE_URL;
            queryFromServer(address,"province");
        }
    }

    /*
    查询全国所有的市，优先从数据库中查询，如果没有查询到再去服务器上查询
     */
    private void queryCities() {
        mSelectCityName.setText(mSelectProvince.getProvinceName());
        mBackBtn.setVisibility(View.VISIBLE);
        mCityList=DataSupport.where("provinceid = ?",String.valueOf(mSelectProvince.getId())).find(City.class);
        if(mCityList.size()>0){
            mDataList.clear();
            for(City city:mCityList){
                mDataList.add(city.getCityName());
            }
            mCityAdapter.notifyDataSetChanged();
            mCityListLV.setSelection(0);
            currentLevel=LEVEL_CITY;
        }else{
            int provinceCode=mSelectProvince.getProvinceCode();
            String address=HttpUrl.CHINA_BASE_URL+provinceCode;
            queryFromServer(address,"city");
        }
    }

    /*
   查询全国所有的市，优先从数据库中查询，如果没有查询到再去服务器上查询
    */
    private void queryCounties() {
        mSelectCityName.setText(mSelectCity.getCityName());
        mBackBtn.setVisibility(View.VISIBLE);
        mCountyList=DataSupport.where("cityid = ?",String.valueOf(mSelectCity.getId())).find(County.class);
        if(mCountyList.size()>0){
            mDataList.clear();
            for(County county:mCountyList){
                mDataList.add(county.getCountyName());
            }
            mCityAdapter.notifyDataSetChanged();
            mCityListLV.setSelection(0);
            currentLevel=LEVEL_COUNTY;
        }else{
            int provinceCode=mSelectProvince.getProvinceCode();
            int cityCode=mSelectCity.getCityCode();
            String address=HttpUrl.CHINA_BASE_URL+provinceCode+"/"+cityCode;
            queryFromServer(address,"county");
        }
    }

    /*
    根据传入的地址和类型从服务器上查询省市县的数据
     */
    private void queryFromServer(String address, final String province) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //通过runOnUIThread方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                final String responseText=response.body().string();
                boolean result=false;
                if("province".equals(province)){
                    result= Utility.handleProvinceResponse(responseText);
                }else if("city".equals(province)){
                    result=Utility.handleCityResponse(responseText,mSelectProvince.getId());
                }else if("county".equals(province)){
                    Log.e("WeatherFragment","111");
                    result=Utility.handleCountyResponse(responseText,mSelectCity.getId());
                    Log.e("WeatherFragment","222");
                }
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(province)){
                                    queryProvinces();
                            }else if("city".equals(province)){
                                queryCities();
                            }else if("county".equals(province)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });
    }

    /*
    显示进度对话框
     */
    private void showProgressDialog() {
        if(mProgressDialog==null){
            mProgressDialog=new ProgressDialog(getActivity());
            mProgressDialog.setMessage("正在加载...");
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        mProgressDialog.show();
    }

    /*
    关闭进度对话框
     */
    private void closeProgressDialog(){
        if (mProgressDialog!=null)
            mProgressDialog.dismiss();
    }
}
