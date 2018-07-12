# DronePlay Codes

## 경로설명
+ [/api/web](https://github.com/theknightsfield/droneplaycodes/tree/master/api/web) : Open API를 활용하는 웹 기반의 예제 코드.
+ [/api/android](https://github.com/theknightsfield/droneplaycodes/tree/master/api/android) : Open API를 활용하는 안드로이드 기반의 예제 코드.

# 안드로이드 기반 코드 - DronePlay Mission 앱.
<img src="https://theknightsfield.github.io/droneplaycodes/api/android_main.png" alt="DronePlayMission" width="300">
<img src="https://theknightsfield.github.io/droneplaycodes/api/android_mission.png" alt="DronePlayMission" width="300">

+ 지도를 터치하여 마크가 된 모든 장소로 드론이 방문하는 예제 앱 입니다.
+ 각 장소를 방문할때 마다 해당 장소의 좌표를 DronePlay API를 호출하여 기록합니다.
+ 개발환경 - Android Studio, gradle
+ 반드시 넓은 공터에서 현재위치와 고도를 충분히 확인하고 확보한 후 테스트를 진행하세요.
+ 이 코드로 인해 발생하는 모든 책임은 사용자에게 있습니다. (드론이 남의 벤틀리 차량으로 돌격 할 수도 있습니다)
<pre>
<code>
minSdkVersion 24 
targetSdkVersion 26
</code>
</pre>

+ 코드의 수행을 위해 Manifest 파일에 Mapbox 지도 ACCESS TOKEN, DJI SDK API KEY, DRONEPLAY API TOKEN 정보를 입력해야 합니다.        
> AndroidManifest.xml
<pre>
<code>
... meta-data
           android:name="mapbox.sdk.ACCESSTOKEN"
           android:value="MAPBOX-SDK-ACCESS-TOKEN" ...
... meta-data
           android:name="com.dji.sdk.API_KEY"
           android:value="DJI-SDK-API-KEY" ...

... meta-data
           android:name="io.droneplay.token"
           android:value="DRONEPLAY-API-TOKEN" ...

... meta-data
           android:name="io.droneplay.email"
           android:value="DRONEPLAY-API-EMAIL" ...
</code>
</pre>

# 웹 
> [DronePlay Samples](http://dev.droneplay.io/dev/examples/index.html) : Open API를 활용한 웹 예제가 실행되는 모습을 볼 수 있습니다.
