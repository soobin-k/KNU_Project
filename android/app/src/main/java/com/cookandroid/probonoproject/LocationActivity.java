package com.cookandroid.probonoproject;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.daum.mf.map.api.MapCircle;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapReverseGeoCoder;
import net.daum.mf.map.api.MapView;

import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

import static android.speech.tts.TextToSpeech.ERROR;


public class LocationActivity extends AppCompatActivity implements MapView.CurrentLocationEventListener , MapReverseGeoCoder.ReverseGeoCodingResultListener {

    private static final String LOG_TAG = "LocationDemoActivity";
    private final int MENU_LOCATION = Menu.FIRST;
    private final int MENU_REVERSE_GEO = Menu.FIRST + 1;

    private String stationInfo;
    private String subway;
    private int threadState = 0;
    private String x;
    private String y;
    private NetworkThread thread;
    private Button searchBtn;
    private TextView searchResult;
    private TextView locationInfo;
    private MapPolyline polyline;
    private MapView mMapView;
    private MapReverseGeoCoder mReverseGeoCoder = null;
    private boolean isUsingCustomLocationMarker = false;
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.locationlayout);
        setTitle("Voice Compass");

        // 언어를 선택한다.
        // 언어를 선택한다.
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != ERROR) {
                    // 언어를 선택한다.
                    tts.setLanguage(Locale.KOREAN);
                }
                //사용안내
                tts.setPitch(1.0f);
                tts.speak("화면을 누르면 가장 가까운 지하철 역을 알려드립니다.", TextToSpeech.QUEUE_FLUSH, null);
            }
        });

        searchBtn = (Button) findViewById(R.id.searchBtn);
        searchBtn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View v) {
                threadState = 1;

            }
        });


        searchResult = (TextView) findViewById(R.id.searchResult);
        locationInfo = (TextView) findViewById(R.id.locationInfo);
        searchResult.setVisibility(View.GONE);//정보 숨기기
        locationInfo.setVisibility(View.GONE);//정보 숨기기

        mMapView = (MapView) findViewById(R.id.map_view);

        mMapView.setCurrentLocationEventListener(this);
        mMapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);

        mMapView.setCurrentLocationRadius(0);
        MapPOIItem.ImageOffset trackingImageAnchorPointOffset = new MapPOIItem.ImageOffset(256, 256); // 좌하단(0,0) 기준 앵커포인트 오프셋
        MapPOIItem.ImageOffset directionImageAnchorPointOffset = new MapPOIItem.ImageOffset(65, 65);
        MapPOIItem.ImageOffset offImageAnchorPointOffset = new MapPOIItem.ImageOffset(15, 15);
        mMapView.setCustomCurrentLocationMarkerTrackingImage(R.drawable.setlocation, trackingImageAnchorPointOffset);
        //mMapView.setCustomCurrentLocationMarkerTrackingImage(R.drawable.custom_arrow_map_present_tracking, trackingImageAnchorPointOffset);



        //지나온 경로 표시라인(한번 등록된 이후에는 사용 불가..ㅠ)
        /*
        polyline = new MapPolyline();
        polyline.setTag(1000);
        polyline.setLineColor(Color.argb(128, 255, 51, 0)); // Polyline 컬러 지정.
        */
        //검색
        thread = new NetworkThread();
        thread.start();
    }
    Handler handler = new Handler(){
        public void handleMessage(Message msg){
            Bundle bun = msg.getData();
            String searchHtml = bun.getString("JSON_DATA");
            Log.d("searchHtml", searchHtml);
            searchResult.setText(searchHtml);
            threadState = 1;
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
        mMapView.setShowCurrentLocationMarker(false);
    }
    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint currentLocation, float accuracyInMeters) {
        MapPoint.GeoCoordinate mapPointGeo = currentLocation.getMapPointGeoCoord();
        Log.i(LOG_TAG, String.format("MapView onCurrentLocationUpdate (%f,%f) accuracy (%f)", mapPointGeo.latitude, mapPointGeo.longitude, accuracyInMeters));
        locationInfo.setText(String.format("MapView onCurrentLocationUpdate (%f,%f) accuracy (%f)", mapPointGeo.latitude, mapPointGeo.longitude, accuracyInMeters));
        //polyline.addPoint(MapPoint.mapPointWithGeoCoord(mapPointGeo.latitude, mapPointGeo.longitude));
        //mapView.addPolyline(polyline);
        MapCircle circle1 = new MapCircle(
         MapPoint.mapPointWithGeoCoord(mapPointGeo.latitude, mapPointGeo.longitude), // center
          100, // radius
         Color.argb(90, 0, 0, 0), // strokeColor
         Color.argb(90, 46, 139, 87) // fillColor
        );
        mapView.addCircle(circle1);
        /*
        MapPOIItem customMarker = new MapPOIItem();
        customMarker.setItemName("Custom Marker");
        customMarker.setTag(1);
        customMarker.setMapPoint(MapPoint.mapPointWithGeoCoord(mapPointGeo.latitude, mapPointGeo.longitude));
        customMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커타입을 커스텀 마커로 지정.
        customMarker.setCustomImageResourceId(R.drawable.custom_map_present); // 마커 이미지.
        customMarker.setCustomImageAutoscale(false); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
        customMarker.setCustomImageAnchor(0.5f, 1.0f); // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정 - 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.

        mapView.addPOIItem(customMarker);
        */
        //검색
        y = String.format("%f",mapPointGeo.latitude);
        x = String.format("%f",mapPointGeo.longitude);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_REVERSE_GEO, Menu.NONE, "Reverse Geo-Coding");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        final int itemId = item.getItemId();

        switch (itemId) {
            case MENU_REVERSE_GEO: {
                mReverseGeoCoder = new MapReverseGeoCoder("b824eaf2d3058fd347ad3106059183d9", mMapView.getMapCenterPoint(), LocationActivity.this, LocationActivity.this);
                mReverseGeoCoder.startFindingAddress();
                return true;
            }
        }
        return onOptionsItemSelected(item);

    }

    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {

    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {

    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {

    }

    @Override
    public void onReverseGeoCoderFoundAddress(MapReverseGeoCoder mapReverseGeoCoder, String s) {
        mapReverseGeoCoder.toString();
        onFinishReverseGeoCoding(s);
    }

    @Override
    public void onReverseGeoCoderFailedToFindAddress(MapReverseGeoCoder mapReverseGeoCoder) {
        onFinishReverseGeoCoding("Fail");
    }

    private void onFinishReverseGeoCoding(String result) {
        Toast.makeText(LocationActivity.this, "Reverse Geo-coding : " + result, Toast.LENGTH_SHORT).show();
    }
    //검색
    class NetworkThread extends Thread {
        @Override
        public void run() {
            int count = 0;
            while(true){
                if(threadState == 1){
                    Log.d("runThread", String.format("%d",threadState));
                    String searchHtml = getStationInfo();
                    Bundle bun = new Bundle();
                    bun.putString("JSON_DATA", searchHtml);
                    Message msg = handler.obtainMessage();
                    threadState = 0;
                    count = 0;
                }else{
                    count = count + 1;
                }
            }
        }
    }
    private String getStationInfo(){
        try {
            String query = "x="+x+"&y="+y+"&radius=1000&sort=distance";
            String address = "https://dapi.kakao.com/v2/local/search/category.json?category_group_code=SW8&"+query;
            Log.d("getStationInfo", address);
            URL url = new URL(address);
            //접속
            URLConnection conn = url.openConnection();
            //요청헤더 추가
            conn.setRequestProperty("Authorization", "KakaoAK b824eaf2d3058fd347ad3106059183d9");

            //서버와 연결되어 있는 스트림을 추출한다.
            InputStream is = conn.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            BufferedReader br = new BufferedReader(isr);

            String str = null;
            StringBuffer buf = new StringBuffer();

            //읽어온다.
            do {
                str = br.readLine();
                if (str != null) {
                    buf.append(str);
                }
            } while (str != null);

            String result = buf.toString();
            stationInfo = "";
            JSONObject jsonObject = new JSONObject(result);
            JSONArray stationArray = jsonObject.getJSONArray("documents");
            JSONObject stationObject = stationArray.getJSONObject(0);
            subway = stationObject.getString("place_name");
            subway += "까지 "+stationObject.getString("distance")+" 미터 남았습니다.";
            if(subway.equals("")){
                subway = "반경 2키로미터 안에 지하철 역이 없습니다.";
            }
            stationInfo += "역명 : " + stationObject.getString("place_name");
            stationInfo += "/ x : " + stationObject.getString("x");
            stationInfo += "/ y : " + stationObject.getString("y");
            stationInfo += "/ distance : " + stationObject.getString("distance");
            stationInfo += "\n";
            /*
            for(int i =0; i<stationArray.length(); i++){
                JSONObject stationObject = stationArray.getJSONObject(i);
                subway = stationObject.getString("place_name");
                subway += "까지 "+stationObject.getString("distance")+" 남았습니다.";
                stationInfo += "역명 : " + stationObject.getString("place_name");
                stationInfo += "/ x : " + stationObject.getString("x");
                stationInfo += "/ y : " + stationObject.getString("y");
                stationInfo += "/ distance : " + stationObject.getString("distance");
                stationInfo += "\n";
            }
             */
            //place_name : ..역
            //x : 127....
            //y : 37.....
            is.close();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    searchResult.setText(stationInfo);
                    tts.setPitch(1.0f);
                    tts.speak(subway,TextToSpeech.QUEUE_FLUSH, null);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("stationInfo", stationInfo);

        threadState = 1;
        return stationInfo;
    }
}